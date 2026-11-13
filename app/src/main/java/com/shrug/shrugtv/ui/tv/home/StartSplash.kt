package com.shrug.shrugtv.ui.tv.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import kotlinx.coroutines.delay

/**
 * Splash over the home screen on a cold start. The logo sits where the start window of the
 * system drew it (centred, 160 dp, res/drawable/splash_logo.xml; the drawing carries the name), so the hand-over does
 * not jump; greeting, version and the glow in the accent colour fade in, and the whole splash fades out once the home screen is there.
 */
@Composable
fun StartSplash(settings: SplashRules.Settings, ready: () -> Boolean, name: () -> String) {
    if (settings.style == SplashRules.Style.OFF) return
    var visible by remember { mutableStateOf(true) }
    var extras by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val start = android.os.SystemClock.elapsedRealtime()
        extras = true
        while (!SplashRules.hide(android.os.SystemClock.elapsedRealtime() - start, ready(), settings.minMs)) delay(50)
        visible = false
    }
    val full = settings.style == SplashRules.Style.FULL
    val animate = com.shrug.shrugtv.ui.tv.TvStyle.animations
    val extrasAlpha by animateFloatAsState(if (extras && full) 1f else 0f, tween(if (animate) 450 else 0), label = "splashExtras")
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(initialAlpha = 1f),
        exit = if (animate) fadeOut(tween(350)) + scaleOut(targetScale = 1.04f, animationSpec = tween(350)) else androidx.compose.animation.ExitTransition.None,
    ) {
        Box(Modifier.fillMaxSize().background(TvColors.Background), contentAlignment = Alignment.Center) {
            if (full) {
                Box(
                    Modifier.size(520.dp).alpha(extrasAlpha).background(
                        Brush.radialGradient(listOf(TvColors.Accent.copy(alpha = 0.28f), Color.Transparent))
                    )
                )
            }
            Image(painterResource(R.drawable.shrugtv_logo), contentDescription = null, modifier = Modifier.size(160.dp))
            if (full) {
                // The logo already carries the name; the greeting goes below it
                if (settings.greeting) {
                    Text(
                        greetingText(name()), fontFamily = Inter, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TvColors.TextMuted,
                        modifier = Modifier.offset(y = 104.dp).alpha(extrasAlpha),
                    )
                }
                Text(
                    "v" + BuildConfig.VERSION_NAME, fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp).alpha(extrasAlpha),
                )
            }
        }
    }
}

@Composable
private fun greetingText(name: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val res = when (SplashRules.greeting(hour)) {
        SplashRules.Greeting.MORNING -> R.string.splash_greeting_morning
        SplashRules.Greeting.DAY -> R.string.splash_greeting_day
        SplashRules.Greeting.EVENING -> R.string.splash_greeting_evening
        SplashRules.Greeting.NIGHT -> R.string.splash_greeting_night
    }
    val base = stringResource(res)
    return if (name.isBlank()) base else stringResource(R.string.splash_greeting_named, base, name)
}
