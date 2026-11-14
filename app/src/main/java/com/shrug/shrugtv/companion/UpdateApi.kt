package com.shrug.shrugtv.companion

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.google.gson.JsonObject
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.update.UpdateCheck
import com.shrug.shrugtv.update.UpdateChecker
import com.shrug.shrugtv.update.UpdateInstaller

/**
 * Version and update menu of the phone page: what runs, whether a newer release exists, a check on request and
 * the install of the update (same download, checksum and system dialog as the TV card).
 * The system always asks on the TV for the last confirmation.
 */
object UpdateApi {
    const val PREF_INSTALL = "pref_update_install"

    /** Progress of a running install for the phone: "", "downloading", "verifying" or "error". */
    @Volatile private var phase = ""
    @Volatile private var percent = 0
    @Volatile private var message = ""
    @Volatile private var receiverSet = false
    @Volatile private var checking = false

    private fun installAllowed(ctx: Context) =
        BuildConfig.UPDATE_INSTALL && PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_INSTALL, false)

    fun info(ctx: Context): JsonObject = JsonObject().apply {
        addProperty("version", BuildConfig.VERSION_NAME)
        addProperty("edition", if (BuildConfig.UPDATE_CHECK) "public" else "private")
        addProperty("device", android.os.Build.MODEL)
        addProperty("android", android.os.Build.VERSION.RELEASE)
        addProperty("check_supported", BuildConfig.UPDATE_CHECK)
        addProperty("check_enabled", UpdateChecker.enabled(ctx))
        addProperty("install_supported", BuildConfig.UPDATE_INSTALL)
        addProperty("install_enabled", installAllowed(ctx))
        addProperty("may_install", BuildConfig.UPDATE_INSTALL && UpdateInstaller.mayInstall(ctx))
        addProperty("last_check", UpdateChecker.lastCheck(ctx))
        addProperty("checking", checking)
        val latest = if (BuildConfig.UPDATE_CHECK) UpdateChecker.latest(ctx) else null
        addProperty("status", when {
            !BuildConfig.UPDATE_CHECK -> "unsupported"
            latest != null -> "available"
            UpdateChecker.lastCheck(ctx) > 0L -> "current"
            else -> "unknown"
        })
        if (latest != null) add("latest", JsonObject().apply {
            addProperty("version", latest.version)
            addProperty("url", latest.url)
            addProperty("notes", UpdateCheck.shortNotes(latest.notes, maxLines = 12, maxChars = 900))
            addProperty("installable", latest.apkUrl.isNotEmpty() && latest.sha256.isNotEmpty())
        })
        add("install", JsonObject().apply {
            addProperty("phase", phase)
            addProperty("percent", percent)
            addProperty("message", message)
        })
    }

    /** Asks the release page now (background thread); the phone polls [info] for the answer. */
    fun check(ctx: Context): JsonObject {
        if (BuildConfig.UPDATE_CHECK && !checking) {
            checking = true
            val app = ctx.applicationContext
            Thread {
                try { UpdateChecker.check(app, force = true) } finally { checking = false }
            }.start()
        }
        return info(ctx).apply { addProperty("checking", checking) }
    }

    /** Starts download, checksum and the system installer for the newest release; does nothing when not allowed or already running. */
    fun install(ctx: Context): JsonObject {
        val app = ctx.applicationContext
        val release = UpdateChecker.latest(app)
        when {
            !installAllowed(app) -> fail("disabled")
            release == null -> fail("none")
            phase == "downloading" || phase == "verifying" -> Unit
            else -> {
                phase = "downloading"; percent = 0; message = ""
                registerReceiver(app)
                Thread {
                    when (val outcome = UpdateInstaller.install(app, release) { percent = it }) {
                        UpdateInstaller.Outcome.Started -> phase = "verifying"
                        UpdateInstaller.Outcome.NeedPermission -> fail("need_permission")
                        UpdateInstaller.Outcome.PermissionScreenMissing -> fail("permission_unavailable")
                        UpdateInstaller.Outcome.NoFile -> fail("no_file")
                        UpdateInstaller.Outcome.BadChecksum -> fail("bad_checksum")
                        UpdateInstaller.Outcome.DownloadFailed -> fail("download_failed")
                        is UpdateInstaller.Outcome.Failed -> fail(outcome.message)
                    }
                }.start()
            }
        }
        return info(app)
    }

    private fun fail(code: String) {
        phase = "error"
        message = code
    }

    /** The system asks for confirmation through this receiver; its dialog opens on the TV. Registered once per process. */
    private fun registerReceiver(app: Context) {
        if (receiverSet) return
        receiverSet = true
        UpdateInstaller.register(app, onFailure = { fail(it) }, launch = { app.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) })
    }
}
