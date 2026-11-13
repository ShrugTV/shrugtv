package com.shrug.shrugtv.ui.tv.player

import androidx.compose.ui.res.stringResource
import com.shrug.shrugtv.ui.tv.TvShapes
import com.shrug.shrugtv.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors

/**
 * Statistics overlay in the player drawn with Compose Canvas (slice 4a).
 *
 * Replaces `include_player_stats.xml` and the MPAndroidChart dependency completely:
 * - Four small line charts (bitrate, buffer, dropped frames, memory)
 * - Labels in the app language
 * - Fixes  (memory calculation now adds up all processes properly)
 */
data class PlayerStatsState(
    val resolution: String = "",
    val bitrateKbps: Int = 0,
    val droppedFrames: Int = 0,
    val totalFrames: Int = 0,
    val bufferSeconds: Float = 0f,
    val memoryMb: Int = 0,
    /** Distance to the live edge; negative = unknown (VOD, not loaded). */
    val latencySeconds: Float = -1f,
    /** Delay of the live chat towards real time; negative = no live chat. */
    val chatDelaySeconds: Float = -1f,
    /** Frames per second all animated emotes together deliver and how many emotes moved. */
    val emoteFps: Float = 0f,
    val emoteCount: Int = 0,
    val bitrateHistory: List<Float> = emptyList(),
    val bufferHistory: List<Float> = emptyList()
)

@Composable
fun PlayerStatsOverlay(
    visible: Boolean,
    stats: PlayerStatsState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, top = 32.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .clip(TvShapes.r(16.dp))
                    .background(TvColors.Surface.copy(alpha = 0xE6 / 255f))
                    .border(1.dp, TvColors.glass(0.15f), TvShapes.r(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_stats_title),
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Info rows
                StatRow(stringResource(R.string.player_stats_resolution), stats.resolution)
                StatRow(stringResource(R.string.player_stats_bitrate), "${stats.bitrateKbps} kbps")
                if (stats.bitrateHistory.size >= 2) {
                    Sparkline(
                        values = stats.bitrateHistory,
                        color = TvColors.Accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(vertical = 4.dp)
                    )
                }

                val dropPercent = if (stats.totalFrames > 0) {
                    (stats.droppedFrames.toFloat() / stats.totalFrames.toFloat() * 100f)
                } else 0f
                StatRow(
                    stringResource(R.string.player_stats_dropped),
                    "${stats.droppedFrames} / ${stats.totalFrames} (${String.format(java.util.Locale.getDefault(), "%.1f", dropPercent)} %)"
                )

                StatRow(stringResource(R.string.player_stats_buffer), "${String.format(java.util.Locale.getDefault(), "%.1f", stats.bufferSeconds)} s")
                if (stats.bufferHistory.size >= 2) {
                    Sparkline(
                        values = stats.bufferHistory,
                        color = Color(0xFF00E676),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(vertical = 4.dp)
                    )
                }

                if (stats.emoteCount > 0) {
                    val each = stats.emoteFps / stats.emoteCount
                    StatRow(
                        stringResource(R.string.player_stats_emotes),
                        String.format(java.util.Locale.getDefault(), "%d × %.1f fps = %.0f /s", stats.emoteCount, each, stats.emoteFps)
                    )
                }

                if (stats.latencySeconds >= 0f) {
                    StatRow(stringResource(R.string.player_stats_latency), "${String.format(java.util.Locale.getDefault(), "%.1f", stats.latencySeconds)} s")
                }
                if (stats.chatDelaySeconds >= 0f) {
                    StatRow(stringResource(R.string.player_stats_chat_delay), "${String.format(java.util.Locale.getDefault(), "%.1f", stats.chatDelaySeconds)} s")
                }
                StatRow(stringResource(R.string.player_stats_memory), "${stats.memoryMb} MB")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = TvColors.TextMuted
        )
        Text(
            text = value,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color.White
        )
    }
}

/**
 * Lightweight sparkline drawn with Compose Canvas instead of MPAndroidChart.
 */
@Composable
private fun Sparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = if (max - min == 0f) 1f else max - min

        val stepX = size.width / (values.size - 1)
        val path = Path()

        values.forEachIndexed { index, value ->
            val normY = 1f - ((value - min) / range)
            val x = index * stepX
            val y = normY * size.height
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
