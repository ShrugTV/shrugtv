package com.shrug.shrugtv.update

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceManager
import com.shrug.shrugtv.BuildConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.TvShapes
import com.shrug.shrugtv.ui.tv.components.glassButton
import com.shrug.shrugtv.util.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "Version X is available" with the changes, a QR code to the release page and two buttons: later (just close) and
 * skip this version (remembered). Nothing is downloaded or installed here.
 */
class UpdateActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val version = intent.getStringExtra(VERSION).orEmpty()
        val url = intent.getStringExtra(URL).orEmpty()
        if (version.isEmpty() || url.isEmpty()) { finish(); return }
        val rawNotes = intent.getStringExtra(NOTES).orEmpty()
        val notes = UpdateCheck.shortNotes(rawNotes)
        val release = UpdateCheck.Release("v$version", version, url, rawNotes, intent.getStringExtra(APK).orEmpty(), intent.getStringExtra(SHA).orEmpty())
        // The install button needs the public edition and the switch "Install updates in the app"
        val canInstall = BuildConfig.UPDATE_INSTALL && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_update_install", false)
        if (canInstall) {
            receiver = UpdateInstaller.register(this, onFailure = { status.value = getString(R.string.update_install_failed, it); busy.value = false },
                launch = { startActivity(it) })
        }
        setContent {
            TvHomeTheme {
                UpdateContent(version, url, notes, canInstall, busy.value, status.value, onLater = { finish() }, onSkip = {
                    UpdateChecker.skip(this, version)
                    finish()
                }, onInstall = { install(release) })
            }
        }
    }

    private val status = mutableStateOf("")
    private val busy = mutableStateOf(false)
    private var receiver: android.content.BroadcastReceiver? = null

    private fun install(release: UpdateCheck.Release) {
        if (busy.value) return
        busy.value = true
        status.value = ""
        Thread {
            val outcome = UpdateInstaller.install(applicationContext, release) { pct ->
                runOnUiThread { status.value = getString(R.string.update_downloading, pct) }
            }
            runOnUiThread {
                // Started: the system dialog or the confirmation follows through the receiver; the others end here
                if (outcome != UpdateInstaller.Outcome.Started) busy.value = false
                status.value = when (outcome) {
                    UpdateInstaller.Outcome.Started -> getString(R.string.update_verifying)
                    UpdateInstaller.Outcome.NeedPermission -> getString(R.string.update_need_permission)
                    UpdateInstaller.Outcome.PermissionScreenMissing -> getString(R.string.update_permission_unavailable)
                    UpdateInstaller.Outcome.NoFile -> getString(R.string.update_no_file)
                    UpdateInstaller.Outcome.BadChecksum -> getString(R.string.update_bad_checksum)
                    UpdateInstaller.Outcome.DownloadFailed -> getString(R.string.update_download_failed)
                    is UpdateInstaller.Outcome.Failed -> getString(R.string.update_install_failed, outcome.message)
                }
            }
        }.start()
    }

    override fun onDestroy() {
        receiver?.let { runCatching { unregisterReceiver(it) } }
        super.onDestroy()
    }

    companion object {
        private const val VERSION = "version"
        private const val URL = "url"
        private const val NOTES = "notes"
        private const val APK = "apk"
        private const val SHA = "sha256"

        fun intent(c: Context, r: UpdateCheck.Release) = Intent(c, UpdateActivity::class.java)
            .putExtra(VERSION, r.version).putExtra(URL, r.url).putExtra(NOTES, r.notes).putExtra(APK, r.apkUrl).putExtra(SHA, r.sha256)
    }
}

private suspend fun qr(text: String): Bitmap? = withContext(Dispatchers.Default) {
    runCatching { com.shrug.shrugtv.util.QrCode.generate(text, 512, 1) }.getOrNull()
}

@Composable
private fun UpdateContent(version: String, url: String, notes: String, canInstall: Boolean, busy: Boolean, status: String,
                          onLater: () -> Unit, onSkip: () -> Unit, onInstall: () -> Unit) {
    var image by remember { mutableStateOf<Bitmap?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(url) { image = qr(url) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Row(
            Modifier.background(TvColors.Surface, TvShapes.r(20.dp)).padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(220.dp).clip(TvShapes.r(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                image?.let { Image(it.asImageBitmap(), contentDescription = stringResource(R.string.dialog_qr_code), filterQuality = FilterQuality.None, modifier = Modifier.size(204.dp)) }
            }
            Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.update_title, version), fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
                if (notes.isNotEmpty()) Text(notes, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMedium)
                Text(stringResource(R.string.update_hint), fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMuted)
                Text(url, fontFamily = Inter, fontSize = 10.sp, color = TvColors.AccentLight)
                if (status.isNotEmpty()) Text(status, fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvColors.AccentLight)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    if (canInstall) {
                        Button(onClick = onInstall, enabled = !busy, colors = glassButton(), modifier = Modifier.focusRequester(focus)) {
                            Text(stringResource(R.string.update_install), fontFamily = Inter, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(onClick = onLater, colors = glassButton(), modifier = if (canInstall) Modifier else Modifier.focusRequester(focus)) {
                        Text(stringResource(R.string.update_later), fontFamily = Inter, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onSkip, colors = glassButton()) { Text(stringResource(R.string.update_skip), fontFamily = Inter) }
                }
            }
        }
    }
}
