package com.shrug.shrugtv.ui.tv.player

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Burn-in protection: the maths for slowly drifting elements and for the
 * places of the rest screen. No Android types, so it is testable on the JVM.
 */
object BurnIn {
    /** The rest screen changes its place this often. */
    const val REST_MOVE_MS = 60_000L

    /** Offset of a drifting element at [nowMs]: a slow loop of up to [amplitudePx] in each direction. */
    @JvmStatic
    fun drift(nowMs: Long, amplitudePx: Int, periodMs: Long = 360_000L): Pair<Int, Int> {
        val t = nowMs.toDouble()
        val x = sin(2 * PI * t / periodMs) * amplitudePx
        // A different period on the second axis, so the path is a loop and not a line
        val y = sin(2 * PI * t / (periodMs * 1.37)) * amplitudePx
        return x.roundToInt() to y.roundToInt()
    }

    /**
     * Place (top left) of a box of [boxW] x [boxH] inside [areaW] x [areaH] for step [slot]. Every step
     * lands somewhere else (golden-ratio sequence), the box always stays inside the area.
     */
    @JvmStatic
    fun restSpot(slot: Long, areaW: Int, areaH: Int, boxW: Int, boxH: Int): Pair<Int, Int> {
        val freeW = (areaW - boxW).coerceAtLeast(0)
        val freeH = (areaH - boxH).coerceAtLeast(0)
        val fx = frac(slot * 0.6180339887)
        val fy = frac(slot * 0.7548776662 + 0.3)
        return (fx * freeW).roundToInt() to (fy * freeH).roundToInt()
    }

    private fun frac(v: Double): Double = v - kotlin.math.floor(v)
}

/**
 * Lets an element wander a few dp along a slow loop, when [enabled] (setting "Burn-in protection").
 * The offset is re-computed every 15 s and glides there, so nobody sees it move.
 */
@Composable
fun Modifier.burnInDrift(enabled: Boolean, tag: String = "", amplitudeDp: Int = 8): Modifier {
    if (!enabled) return this
    val amplitudePx = with(LocalDensity.current) { amplitudeDp.dp.roundToPx() }
    val target by produceState(BurnIn.drift(System.currentTimeMillis(), amplitudePx), amplitudePx) {
        while (true) {
            value = BurnIn.drift(System.currentTimeMillis(), amplitudePx)
            Log.d("BurnIn", "drift $tag -> ${value.first},${value.second} px")
            kotlinx.coroutines.delay(15_000L)
        }
    }
    val x by animateIntAsState(target.first, tween(15_000, easing = LinearEasing), label = "driftX")
    val y by animateIntAsState(target.second, tween(15_000, easing = LinearEasing), label = "driftY")
    return this.offset { IntOffset(x, y) }
}

/**
 * Black screen with the clock, channel name and (if given) title in a changing place, in the colours of
 * the skin and dimmed. Shared by "Audio only" and the rest screen after a long pause
 *.
 */
@Composable
fun RestScreen(
    channel: String,
    title: String?,
    hint: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    val timeFormat = remember { SimpleDateFormat(prefs.getString("pref_clock_time_format", "H:mm") ?: "H:mm", Locale.getDefault()) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L - value % 60_000L + 50L)
        }
    }
    val slot by produceState(System.currentTimeMillis() / BurnIn.REST_MOVE_MS) {
        while (true) {
            kotlinx.coroutines.delay(BurnIn.REST_MOVE_MS - System.currentTimeMillis() % BurnIn.REST_MOVE_MS + 20L)
            value = System.currentTimeMillis() / BurnIn.REST_MOVE_MS
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val boxW = 420
        val boxH = 190
        val (px, py) = BurnIn.restSpot(slot, maxWidth.value.toInt(), maxHeight.value.toInt(), boxW, boxH)
        Log.d("BurnIn", "rest screen slot $slot -> $px,$py dp")
        val x by animateDpAsState(px.dp, tween(3_000), label = "restX")
        val y by animateDpAsState(py.dp, tween(3_000), label = "restY")
        Column(
            modifier = Modifier.offset(x, y).width(boxW.dp).padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = timeFormat.format(Date(now)),
                fontFamily = Inter,
                fontWeight = FontWeight.Light,
                fontSize = 56.sp,
                color = TvColors.Text.copy(alpha = 0.55f)
            )
            Text(
                text = channel,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TvColors.Accent.copy(alpha = 0.6f)
            )
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontFamily = Inter,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = TvColors.TextMuted.copy(alpha = 0.7f)
                )
            }
            Text(
                text = hint,
                fontFamily = Inter,
                fontSize = 13.sp,
                textAlign = TextAlign.Start,
                color = TvColors.TextMuted.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
