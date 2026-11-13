package com.shrug.shrugtv.ui.tv.player

import androidx.compose.foundation.background
import com.shrug.shrugtv.ui.tv.TvShapes
import com.shrug.shrugtv.ui.tv.TvColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.home.HomeIcons
import com.shrug.shrugtv.ui.tv.home.NetImage
import com.shrug.shrugtv.util.TimeUtil
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Texts of the info corner, without Android types (testable). */
object StreamInfoText {
    /** "3:56" for a stream that runs 3 h 56 min. */
    @JvmStatic
    fun uptime(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        return "${s / 3600}:${"%02d".format((s / 60) % 60)}"
    }

    /** Delay behind live: "0:42", "12:05", "1:02:03". */
    @JvmStatic
    fun behind(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s / 60) % 60
        val sec = s % 60
        return if (h > 0) "$h:${"%02d".format(m)}:${"%02d".format(sec)}" else "$m:${"%02d".format(sec)}"
    }

    /** Twitch's box art for a category id, small (CDN template, as in Helix `box_art_url`). */
    @JvmStatic
    fun boxArtUrl(gameId: Long): String? =
        if (gameId <= 0) null else "https://static-cdn.jtvnw.net/ttv-boxart/$gameId-52x72.jpg"

    /** Language code worth showing ("DE"); none for unknown. */
    @JvmStatic
    fun language(code: String?): String? = code?.takeIf { it.isNotBlank() && it != "N/A" }
}

/**
 * Who is streaming, top left in the player while the control bar is open: profile picture, name,
 * followed/favourite, LIVE with viewers and uptime, language, category with its cover and the stream
 * title. VODs show the date and length instead of LIVE data.
 */
@Composable
fun PlayerStreamInfo(
    movie: Movie,
    avatarUrl: String?,
    isFollowed: Boolean,
    isVod: Boolean,
    /** Seconds behind the live edge when far behind; null = at live. */
    behindLiveSeconds: Int? = null,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    // Re-read the clock every 30 s so the uptime keeps going while the bar is open
    val now by produceState(TimeUtil.nowUtc(), movie) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            value = TimeUtil.nowUtc()
        }
    }
    val muted = TvColors.glass(0.75f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .clip(TvShapes.r(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        NetImage(
            url = avatarUrl,
            modifier = Modifier.size(56.dp).clip(CircleShape),
            width = 112,
            height = 112
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.widthIn(max = 640.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(movie.displayName.ifEmpty { movie.login ?: "" }, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                if (isFollowed) {
                    Icon(HomeIcons.Heart, contentDescription = stringResource(R.string.player_info_followed), tint = TvColors.AccentLight, modifier = Modifier.size(16.dp))
                }
                if (movie.isFavorite) {
                    Icon(HomeIcons.Star, contentDescription = stringResource(R.string.player_info_favorite), tint = Color(0xFFFFC34D), modifier = Modifier.size(16.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val parts = ArrayList<String>()
                if (isVod) {
                    movie.publishedAt?.let { parts.add(it.withZoneSameInstant(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))) }
                    if (movie.durationSeconds > 0) parts.add(stringResource(R.string.player_info_length, StreamInfoText.uptime(movie.durationSeconds.toLong())))
                } else {
                    // Grey LIVE with the delay when far behind the live edge (docs/ideas.md 1)
                    Text(
                        "LIVE",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.clip(TvShapes.r(4.dp))
                            .background(if (behindLiveSeconds != null) Color(0xFF5C5C66) else TvColors.Live)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                    if (behindLiveSeconds != null) {
                        parts.add(stringResource(R.string.player_behind_live, StreamInfoText.behind(behindLiveSeconds.toLong())))
                    }
                    if (movie.viewerCount > 0) {
                        val n = NumberFormat.getIntegerInstance(locale).format(movie.viewerCount)
                        parts.add(pluralStringResource(R.plurals.player_viewers, movie.viewerCount, n))
                    }
                    movie.startedAt?.let { parts.add(stringResource(R.string.player_info_since, StreamInfoText.uptime(TimeUtil.secondsBetween(it, now)))) }
                }
                StreamInfoText.language(movie.languageCode)?.let { parts.add(it) }
                if (parts.isNotEmpty()) Text(parts.joinToString(" · "), fontFamily = Inter, fontSize = 14.sp, color = muted)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamInfoText.boxArtUrl(movie.gameId)?.let { url ->
                    NetImage(url = url, modifier = Modifier.size(width = 18.dp, height = 24.dp).clip(TvShapes.r(3.dp)), width = 52, height = 72)
                }
                val line = listOf(movie.gameName, movie.title ?: "").filter { it.isNotBlank() }.joinToString(" · ")
                Text(line, fontFamily = Inter, fontSize = 14.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
