package com.shrug.shrugtv.activities

import android.graphics.Bitmap
import com.shrug.shrugtv.ui.tv.TvShapes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.Fade
import android.transition.TransitionInflater
import android.util.Log
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.login.TwitchDeviceAuth
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.util.QrCode
import kotlinx.coroutines.delay

/**
 * Sign-in with a device code: twitch.tv/activate, the code and a QR code with
 * the code prefilled; in the background it polls until Twitch issues the token.
 *
 * Since  a Compose page in the dark app design: heading and three steps on the
 * left, the code in boxes and the QR code on a white card on the right, status as a pill with the
 * remaining time. Before: full-screen purple with Roboto, everything stacked in the centre.
 *
 * This activity used to load the Twitch sign-in in a WebView. Twitch rejects that
 * ("Your browser is not currently supported", measured 2026-10-20).
 *
 * Status texts are string resources; the state holds the resource ID.
 */
class LoginActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var running = false
    private var code by mutableStateOf<TwitchDeviceAuth.DeviceCode?>(null)
    private var status by mutableIntStateOf(R.string.login_status_requesting)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        val fade = TransitionInflater.from(this).inflateTransition(R.transition.activity_fade) as Fade
        window.exitTransition = fade
        window.enterTransition = fade
        window.reenterTransition = fade
        setContent { TvHomeTheme { LoginPage(code, status) } }
    }

    override fun onStart() {
        super.onStart()
        running = true
        Thread { loop() }.start()
    }

    override fun onStop() {
        running = false
        super.onStop()
    }

    /** Runs in the background: fetch the code, show it, poll; expired code -> new code. */
    private fun loop() {
        while (running) {
            val c = TwitchDeviceAuth.requestCode()
            if (c == null) {
                report(R.string.login_status_unreachable)
                if (!pause(5)) return
                continue
            }
            main.post { code = c; status = R.string.login_status_waiting }
            var spacing = maxOf(c.intervalSec, 2)
            while (running) {
                if (!pause(spacing)) return
                when (TwitchDeviceAuth.poll(applicationContext, c)) {
                    TwitchDeviceAuth.PollState.OK -> {
                        report(R.string.login_status_signed_in)
                        main.post { setResult(RESULT_OK); finish() }
                        return
                    }
                    TwitchDeviceAuth.PollState.SLOW_DOWN -> spacing += 5
                    TwitchDeviceAuth.PollState.DENIED -> { report(R.string.login_status_denied); break }
                    TwitchDeviceAuth.PollState.EXPIRED -> { report(R.string.login_status_expired); break }
                    TwitchDeviceAuth.PollState.PENDING -> report(R.string.login_status_waiting)
                    else -> {}
                }
            }
        }
    }

    private fun report(s: Int) = main.post { status = s }

    private fun pause(seconds: Int): Boolean {
        try {
            Thread.sleep(seconds * 1000L)
        } catch (e: InterruptedException) {
            return false
        }
        return running
    }
}

@Composable
private fun LoginPage(code: TwitchDeviceAuth.DeviceCode?, status: Int) {
    Row(
        Modifier.fillMaxSize().background(TvColors.Background),
        horizontalArrangement = Arrangement.spacedBy(70.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.app_name), fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TvColors.AccentLight)
                Text(stringResource(R.string.login_title), fontFamily = Inter, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
                Text(stringResource(R.string.login_intro),
                    fontFamily = Inter, fontSize = 12.sp, lineHeight = 18.sp, color = TvColors.TextMuted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Step("1", stringResource(R.string.login_step_1))
                Step("2", stringResource(R.string.login_step_2))
                Step("3", stringResource(R.string.login_step_3))
            }
            StatusPill(code, status)
        }
        CodeCard(code)
    }
}

@Composable
private fun Step(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(TvColors.AccentLight.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(number, fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.AccentLight)
        }
        Text(text, fontFamily = Inter, fontSize = 12.sp, color = TvColors.Text)
    }
}

/** Status with the code's remaining time; the remaining time counts down every minute. */
@Composable
private fun StatusPill(code: TwitchDeviceAuth.DeviceCode?, status: Int) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(code) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val rest = code?.let { ((it.expiresAt - now) / 60_000).coerceAtLeast(0) }
    val statusText = stringResource(status)
    val text = if (rest != null && status == R.string.login_status_waiting) statusText + " " + stringResource(R.string.login_code_valid, rest.toInt()) else statusText
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(TvColors.glass(0.08f)).padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(TvColors.AccentLight))
        Text(text, fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMedium)
    }
}

@Composable
private fun CodeCard(code: TwitchDeviceAuth.DeviceCode?) {
    val qrPx = with(LocalDensity.current) { 150.dp.roundToPx() }
    val qr: Bitmap? = remember(code?.verificationUri) {
        code?.let {
            try {
                QrCode.generate(it.verificationUri, qrPx, 4) // margin as BarcodeEncoder before (zxing default)
            } catch (e: Exception) {
                Log.w("LoginActivity", "QR-Code", e)
                null
            }
        }
    }
    Column(
        Modifier.width(280.dp).clip(TvShapes.r(16.dp)).background(TvColors.Surface)
            .border(1.dp, TvColors.glass(0.08f), TvShapes.r(16.dp)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val chars = code?.userCode ?: "…"
            for (c in chars) {
                Box(Modifier.width(25.dp).padding(vertical = 0.dp).clip(TvShapes.r(6.dp)).background(TvColors.SurfaceHigh),
                    contentAlignment = Alignment.Center) {
                    Text(c.toString(), fontFamily = Inter, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text,
                        modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
        Box(Modifier.size(150.dp).clip(TvShapes.r(10.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            if (qr != null) Image(qr.asImageBitmap(), contentDescription = stringResource(R.string.login_qr_description), modifier = Modifier.fillMaxSize())
        }
        Text(stringResource(R.string.login_scan_hint), fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted)
    }
}
