package com.shrug.shrugtv.ui.tv.stats

import android.os.Bundle
import com.shrug.shrugtv.ui.tv.themeBackground
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.stats.PeriodStats
import com.shrug.shrugtv.stats.Ranked
import com.shrug.shrugtv.stats.StatsRange
import com.shrug.shrugtv.stats.WatchStats
import com.shrug.shrugtv.stats.WatchStatsStore
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Statistics of what was watched: last week, last 30 days, last year or one
 * calendar month. Reads the local database of the watch statistics; nothing leaves the device.
 */
class StatsActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024, 1024)
        window.setBackgroundDrawable(null)
        setContent { TvHomeTheme { StatsScreen() } }
    }
}

private enum class Tab(val titleRes: Int) {
    WEEK(R.string.stats_tab_week), DAYS_30(R.string.stats_tab_30_days), YEAR(R.string.stats_tab_year), MONTH(R.string.stats_tab_month)
}

private class Loaded(val stats: PeriodStats, val oldest: LocalDate?)

@Composable
private fun StatsScreen() {
    val context = LocalContext.current.applicationContext
    val today = remember { LocalDate.now() }
    var tab by remember { mutableStateOf(Tab.WEEK) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var version by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf<Loaded?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val range: StatsRange = when (tab) {
        Tab.WEEK -> StatsRange.WEEK
        Tab.DAYS_30 -> StatsRange.DAYS_30
        Tab.YEAR -> StatsRange.YEAR
        Tab.MONTH -> StatsRange.Month(month)
    }
    LaunchedEffect(tab, month, version) {
        loaded = withContext(Dispatchers.IO) {
            val (rows, oldest) = WatchStatsStore.load(context, range, today)
            Loaded(WatchStats.compute(rows, range, today, oldest), oldest)
        }
    }

    val firstTab = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstTab.requestFocus() } }

    Column(
        Modifier.fillMaxSize().themeBackground().verticalScroll(rememberScrollState())
            .padding(horizontal = 64.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.stats_title), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = TvColors.Text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab.values().forEach { t ->
                Chip(
                    text = stringResource(t.titleRes), selected = tab == t,
                    modifier = if (t == Tab.WEEK) Modifier.focusRequester(firstTab) else Modifier,
                    // Selecting by focus: left and right browse the periods without pressing OK
                    onFocus = { tab = t }
                )
            }
            if (tab == Tab.MONTH) {
                val months = WatchStats.months(loaded?.oldest, today)
                val i = months.indexOf(month).coerceAtLeast(0)
                Chip("◀", false, onClick = { months.getOrNull(i - 1)?.let { month = it } })
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TvColors.Text
                )
                Chip("▶", false, onClick = { months.getOrNull(i + 1)?.let { month = it } })
            }
        }

        val l = loaded
        if (l == null) {
            Text(stringResource(R.string.stats_loading), fontFamily = Inter, fontSize = 15.sp, color = TvColors.TextMuted)
        } else if (l.stats.isEmpty) {
            Text(stringResource(R.string.stats_empty), fontFamily = Inter, fontSize = 15.sp, color = TvColors.TextMuted)
        } else {
            Content(l.stats)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(
                text = stringResource(if (confirmDelete) R.string.stats_delete_confirm else R.string.stats_delete),
                selected = false,
                onClick = {
                    if (confirmDelete) {
                        confirmDelete = false
                        kotlin.concurrent.thread { WatchStatsStore.clear(context); version++ }
                    } else confirmDelete = true
                },
                onFocus = { },
                onBlur = { confirmDelete = false }
            )
            Text(stringResource(R.string.stats_hint), fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMuted)
        }
    }
}

@Composable
private fun Content(p: PeriodStats) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Headline: total time and the comparison; below it days, sessions and the longest one
        Column {
            Text(duration(p.totalSec), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 40.sp, color = TvColors.Text)
            if (p.previousTotalSec >= 0) {
                val diff = p.totalSec - p.previousTotalSec
                Text(
                    stringResource(R.string.stats_vs_before, (if (diff >= 0) "+" else "−") + duration(kotlin.math.abs(diff))),
                    fontFamily = Inter, fontSize = 13.sp, color = if (diff >= 0) TvColors.AccentLight else TvColors.TextMuted
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            Figure(pluralStringResource(R.plurals.stats_days, p.daysWatched, p.daysWatched))
            Figure(pluralStringResource(R.plurals.stats_sessions, p.sessions, p.sessions))
            Figure(stringResource(R.string.stats_longest, duration(p.longestSec)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Bars(stringResource(R.string.stats_top_channels), p.topChannels, Modifier.weight(1f))
            Bars(stringResource(R.string.stats_top_categories), p.topCategories, Modifier.weight(1f))
        }
        LiveShare(p)
        Histogram(p)
    }
}

@Composable
private fun Figure(text: String) =
    Text(text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TvColors.TextMedium, maxLines = 1, softWrap = false)

@Composable
private fun Bars(title: String, rows: List<Ranked>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TvColors.TextMuted)
        if (rows.isEmpty()) Text("–", fontFamily = Inter, fontSize = 14.sp, color = TvColors.TextMuted)
        val max = rows.maxOfOrNull { it.seconds }?.coerceAtLeast(1L) ?: 1L
        rows.forEach { r ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(r.name, fontFamily = Inter, fontSize = 14.sp, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(duration(r.seconds), fontFamily = Inter, fontSize = 14.sp, color = TvColors.TextMedium, modifier = Modifier.padding(start = 12.dp))
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(TvShapes.r(3.dp)).background(TvColors.SurfaceHigh)) {
                    Box(Modifier.fillMaxWidth(r.seconds.toFloat() / max).fillMaxHeight().background(TvColors.Accent))
                }
            }
        }
    }
}

@Composable
private fun LiveShare(p: PeriodStats) {
    val total = (p.liveSec + p.vodSec).coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.stats_live_share, (p.liveSec * 100 / total).toInt(), 100 - (p.liveSec * 100 / total).toInt()),
            fontFamily = Inter, fontSize = 14.sp, color = TvColors.TextMuted
        )
        Row(Modifier.fillMaxWidth().height(8.dp).clip(TvShapes.r(4.dp)).background(TvColors.SurfaceHigh)) {
            if (p.liveSec > 0) Box(Modifier.weight(p.liveSec.toFloat()).fillMaxHeight().background(TvColors.Live))
            if (p.vodSec > 0) Box(Modifier.weight(p.vodSec.toFloat()).fillMaxHeight().background(TvColors.AccentLight))
        }
    }
}

@Composable
private fun Histogram(p: PeriodStats) {
    val max = p.secPerHour.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val peak = p.peakHour
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (peak != null) stringResource(R.string.stats_time_of_day_peak, "%02d:00".format(peak)) else stringResource(R.string.stats_time_of_day),
            fontFamily = Inter, fontSize = 14.sp, color = TvColors.TextMuted
        )
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            p.secPerHour.forEachIndexed { h, sec ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight((sec.toFloat() / max).coerceAtLeast(0.03f))
                            .background(if (h == peak) TvColors.Accent else TvColors.AccentLight.copy(alpha = 0.45f))
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach { Text(it, fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted) }
        }
    }
}

@Composable
private fun Chip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocus() else onBlur() },
        colors = ButtonDefaults.colors(
            containerColor = if (selected) TvColors.Accent else TvColors.Surface, contentColor = if (selected) TvColors.OnAccent else TvColors.TextMedium,
            focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background
        ),
        border = ButtonDefaults.border(border = androidx.tv.material3.Border(androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)))
    ) { Text(text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

/** 95 min → "1 h 35 min", 40 s → "< 1 min"; the page does not show seconds. */
internal fun duration(sec: Long): String {
    val min = sec / 60
    return when {
        min < 1 -> "< 1 min"
        min < 60 -> "$min min"
        else -> "${min / 60} h ${"%02d".format(min % 60)} min"
    }
}
