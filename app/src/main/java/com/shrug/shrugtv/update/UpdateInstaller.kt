package com.shrug.shrugtv.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.shrug.shrugtv.net.Http
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * Downloads the APK of a release, checks its SHA-256 and hands it to the installer of the system.
 * The user always confirms in the system dialog; the system also refuses an APK that is not signed with the same key.
 * Only the public edition has the permission (REQUEST_INSTALL_PACKAGES, BuildConfig.UPDATE_INSTALL).
 */
object UpdateInstaller {

    sealed class Outcome {
        object Started : Outcome()
        object NeedPermission : Outcome()
        object PermissionScreenMissing : Outcome()
        object NoFile : Outcome()
        object BadChecksum : Outcome()
        object DownloadFailed : Outcome()
        data class Failed(val message: String) : Outcome()
    }

    private const val ACTION = "com.shrug.shrugtv.UPDATE_INSTALL_RESULT"

    /** SHA-256 of [file] as 64 lower-case hex digits. */
    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** True when the system lets this app install packages (Android 8 and newer: per-app switch "Install unknown apps"). */
    fun mayInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    /** Opens the system screen where the user allows this app to install apps; false when the TV has no such screen. */
    fun openPermissionScreen(ctx: Context): Boolean = try {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName)))
        true
    } catch (e: Exception) {
        false
    }

    /**
     * Blocking: download, check, install session. [progress] gets 0..100 while downloading. Call from a background thread.
     * [Outcome.Started] means the system took over (its dialog follows or is shown already); failures after that arrive
     * through [register].
     */
    fun install(ctx: Context, release: UpdateCheck.Release, progress: (Int) -> Unit): Outcome {
        if (release.apkUrl.isEmpty() || release.sha256.isEmpty()) return Outcome.NoFile
        if (!mayInstall(ctx)) return if (openPermissionScreen(ctx)) Outcome.NeedPermission else Outcome.PermissionScreenMissing
        val dir = File(ctx.cacheDir, "update").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "shrugtv-${release.version}.apk")
        try {
            val req = Request.Builder().url(release.apkUrl).build()
            Http.client().newCall(req).execute().use { resp ->
                val body = resp.body
                if (!resp.isSuccessful || body == null) return Outcome.DownloadFailed
                val total = body.contentLength()
                var done = 0L
                body.byteStream().use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var last = -1
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            val pct = if (total > 0) (done * 100 / total).toInt() else 0
                            if (pct != last) { last = pct; progress(pct) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            file.delete()
            return Outcome.DownloadFailed
        }
        if (sha256(file) != release.sha256.lowercase()) {
            file.delete()
            return Outcome.BadChecksum
        }
        return try {
            val installer = ctx.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(ctx.packageName) }
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("shrugtv.apk", 0, file.length()).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                // The result intent carries extras the system fills in, so the PendingIntent has to be mutable from Android 12
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val pending = PendingIntent.getBroadcast(ctx, id, Intent(ACTION).setPackage(ctx.packageName), flags)
                session.commit(pending.intentSender)
            }
            Outcome.Started
        } catch (e: Exception) {
            Outcome.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Listens for the answer of the installer while the update card is open: opens the system dialog when the system asks
     * for the user's confirmation and reports a failure. Returns the receiver to unregister in onDestroy.
     */
    fun register(ctx: Context, onFailure: (String) -> Unit, launch: (Intent) -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                        else @Suppress("DEPRECATION") intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        confirm?.let(launch)
                    }
                    PackageInstaller.STATUS_SUCCESS -> Unit // the app is replaced and restarted by the system
                    else -> onFailure(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "?")
                }
            }
        }
        val filter = IntentFilter(ACTION)
        if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") ctx.registerReceiver(receiver, filter)
        return receiver
    }
}
