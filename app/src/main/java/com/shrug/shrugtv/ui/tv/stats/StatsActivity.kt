package com.shrug.shrugtv.ui.tv.stats

import android.os.Bundle
import com.shrug.shrugtv.ui.tv.themeBackground
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.stats.ActivityStyle
import com.shrug.shrugtv.stats.BucketUnit
import com.shrug.shrugtv.stats.HoursStyle
import com.shrug.shrugtv.stats.LiveStyle
import com.shrug.shrugtv.stats.PeriodStats
import com.shrug.shrugtv.stats.RankBy
import com.shrug.shrugtv.stats.Ranked
import com.shrug.shrugtv.stats.StatsPeriod
import com.shrug.shrugtv.stats.StatsPrefs
import com.shrug.shrugtv.stats.StatsRange
import com.shrug.shrugtv.stats.TimeFormat
import com.shrug.shrugtv.stats.WatchStats
import com.shrug.shrugtv.stats.WatchStatsStore
import com.shrug.shrugtv.stats.formatDuration
import com.shrug.shrugtv.stats.formatShortDuration
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
 * Makeover and settings: cards on one screen that can be switched on and off, chart styles, time format,
 * length and ranking of the top lists (Settings -> Home -> Statistics page, [StatsPrefs]).
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
    val prefs = remember { StatsPrefs.from(context) }
    val today = remember { LocalDate.now() }
    var tab by remember {
        mutableStateOf(when (prefs.period) {
            StatsPeriod.WEEK -> Tab.WEEK
            StatsPeriod.DAYS_30 -> Tab.DAYS_30
            StatsPeriod.YEAR -> Tab.YEAR
            StatsPeriod.MONTH -> Tab.MONTH
        })
    }
    val startTab = remember { tab }
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
            Loaded(WatchStats.compute(rows, range, today, oldest, prefs.topCount, prefs.rankBy), oldest)
        }
    }

    // The tab the page opens with has the focus, so left and right browse from there
    val firstTab = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstTab.requestFocus() } }

    Column(
        Modifier.fillMaxSize().themeBackground().verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title on the left, the periods on the right
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.stats_title), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TvColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Tab.values().forEach { t ->
                    Chip(
                        text = stringResource(t.titleRes), selected = tab == t,
                        modifier = if (t == startTab) Modifier.focusRequester(firstTab) else Modifier,
                        // Selecting by focus: left and right browse the periods without pressing OK
                        onFocus = { tab = t }
                    )
                }
                if (tab == Tab.MONTH) {
                    val months = WatchStats.months(loaded?.oldest, today)
                    val i = months.indexOf(month).coerceAtLeast(0)
                    Chip("◀", false, onClick = { months.getOrNull(i - 1)?.let { month = it } })
                    Text(
                        "${month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${month.year}",
                        fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TvColors.Text
                    )
                    Chip("▶", false, onClick = { months.getOrNull(i + 1)?.let { month = it } })
                }
            }
        }

        val l = loaded
        if (l == null) {
            Text(stringResource(R.string.stats_loading), fontFamily = Inter, fontSize = 15.sp, color = TvColors.TextMuted)
        } else if (l.stats.isEmpty) {
            Card(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stats_empty), fontFamily = Inter, fontSize = 15.sp, color = TvColors.TextMuted)
            }
        } else {
            Content(l.stats, prefs)
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
private fun Content(p: PeriodStats, prefs: StatsPrefs) {
    val fmt = prefs.timeFormat
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Total time with the comparison, then three small figures
        Row(Modifier.fillMaxWidth().height(88.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1.5f).fillMaxHeight()) { Hero(p, prefs) }
            if (prefs.showTiles) {
                Tile(p.daysWatched.toString(), stringResource(R.string.stats_tile_days), Modifier.weight(0.8f))
                Tile(p.sessions.toString(), stringResource(R.string.stats_tile_sessions), Modifier.weight(0.8f))
                Tile(formatDuration(p.longestSec, fmt), stringResource(R.string.stats_tile_longest), Modifier.weight(1f))
            }
        }
        // When did you watch: per day (or month) and per hour of the day
        if (prefs.showActivity || prefs.showHours) {
            Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (prefs.showActivity) Card(Modifier.weight(1.7f).fillMaxHeight()) { Activity(p, prefs) }
                if (prefs.showHours) Card(Modifier.weight(1f).fillMaxHeight()) { Hours(p, prefs) }
            }
        }
        // Clips: how many, how long, the average and where they came from
        if (prefs.showClips) Card(Modifier.fillMaxWidth().height(64.dp)) { Clips(p, prefs) }
        // Who and what, and live against recordings
        if (prefs.showChannels || prefs.showCategories || prefs.showLive) {
            val height: Dp = (38 + prefs.topCount * 25).dp.coerceAtLeast(110.dp)
            Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (prefs.showChannels) Card(Modifier.weight(1.25f).fillMaxHeight()) { Rank(stringResource(R.string.stats_top_channels), p.topChannels, prefs) }
                if (prefs.showCategories) Card(Modifier.weight(1.25f).fillMaxHeight()) { Rank(stringResource(R.string.stats_top_categories), p.topCategories, prefs) }
                if (prefs.showLive) Card(Modifier.weight(0.9f).fillMaxHeight()) { LiveShare(p, prefs) }
            }
        }
    }
}

/** A rounded panel in the surface colour of the skin. */
@Composable
private fun Card(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(TvShapes.r(14.dp)).background(TvColors.Surface).padding(horizontal = 14.dp, vertical = 10.dp)) { content() }
}

@Composable
private fun Label(text: String) =
    Text(text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TvColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)

@Composable
private fun Hero(p: PeriodStats, prefs: StatsPrefs) {
    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
        Label(stringResource(R.string.stats_total_label))
        Text(formatDuration(p.totalSec, prefs.timeFormat), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = TvColors.Text, maxLines = 1, softWrap = false)
        if (prefs.compare && p.previousTotalSec >= 0) {
            val diff = p.totalSec - p.previousTotalSec
            Text(
                (if (diff >= 0) "▲ " else "▼ ") + stringResource(R.string.stats_vs_before, formatDuration(kotlin.math.abs(diff), prefs.timeFormat)),
                fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (diff >= 0) TvColors.AccentLight else TvColors.TextMuted
            )
        }
    }
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier) {
    Card(modifier.fillMaxHeight()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TvColors.Text, maxLines = 1, softWrap = false)
            Label(label)
        }
    }
}

/** One bar (or one point of a line) per day, per month for the year; the busiest one in the accent colour. */
@Composable
private fun Activity(p: PeriodStats, prefs: StatsPrefs) {
    val busiest = p.busiest
    val accent = TvColors.Accent
    val soft = TvColors.AccentLight.copy(alpha = 0.5f)
    val empty = TvColors.SurfaceHigh
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(stringResource(R.string.stats_activity))
            if (busiest != null && prefs.highlightBusiest) {
                Text(
                    stringResource(R.string.stats_busiest, bucketName(busiest.start, p.bucketUnit), formatDuration(busiest.seconds, prefs.timeFormat)),
                    fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TvColors.AccentLight, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        val max = p.buckets.maxOfOrNull { it.seconds }?.coerceAtLeast(1L) ?: 1L
        val gap = if (p.buckets.size > 14) 2.dp else 6.dp
        if (prefs.activityStyle == ActivityStyle.LINE) {
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val n = p.buckets.size
                if (n < 2) return@Canvas
                val pad = 4.dp.toPx()
                val w = size.width
                val h = size.height - pad
                val step = w / n
                fun x(i: Int) = step * i + step / 2
                fun y(sec: Long) = pad + h * (1f - sec.toFloat() / max)
                val line = Path().apply { p.buckets.forEachIndexed { i, b -> if (i == 0) moveTo(x(i), y(b.seconds)) else lineTo(x(i), y(b.seconds)) } }
                val area = Path().apply {
                    moveTo(x(0), size.height)
                    p.buckets.forEachIndexed { i, b -> lineTo(x(i), y(b.seconds)) }
                    lineTo(x(n - 1), size.height)
                    close()
                }
                drawPath(area, soft.copy(alpha = 0.22f))
                drawPath(line, soft.copy(alpha = 0.95f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (prefs.highlightBusiest && busiest != null) {
                    val i = p.buckets.indexOf(busiest)
                    drawCircle(accent, 4.5.dp.toPx(), Offset(x(i), y(busiest.seconds)))
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
                p.buckets.forEach { b ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxWidth().fillMaxHeight((b.seconds.toFloat() / max).coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    when {
                                        b.seconds <= 0L -> empty
                                        prefs.highlightBusiest && b == busiest -> accent
                                        else -> soft
                                    }
                                )
                        )
                    }
                }
            }
        }
        // Axis: weekday names for a week, every fifth day number for longer spans, month letters for a year
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (prefs.activityStyle == ActivityStyle.LINE) 0.dp else gap)) {
            p.buckets.forEachIndexed { i, b ->
                val show = when {
                    p.bucketUnit == BucketUnit.MONTH -> true
                    p.buckets.size <= 7 -> true
                    else -> i % 5 == 0 || i == p.buckets.lastIndex
                }
                Text(
                    if (show) axisLabel(b.start, p.bucketUnit, p.buckets.size) else "",
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                    fontFamily = Inter, fontSize = 10.sp, color = TvColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun Hours(p: PeriodStats, prefs: StatsPrefs) {
    val parts = prefs.hoursStyle == HoursStyle.DAY_PARTS
    val values: LongArray = if (parts) WatchStats.dayParts(p.secPerHour) else p.secPerHour
    val max = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val peakIndex = values.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
    val peakText = when {
        peakIndex == null -> null
        parts -> partName(peakIndex)
        else -> "%02d:00".format(peakIndex)
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(stringResource(R.string.stats_hours_title))
            if (peakText != null) {
                Text(stringResource(R.string.stats_hours_peak, peakText), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TvColors.AccentLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(if (parts) 6.dp else 2.dp), verticalAlignment = Alignment.Bottom) {
            values.forEachIndexed { h, sec ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight((sec.toFloat() / max).coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(if (sec <= 0L) TvColors.SurfaceHigh else if (h == peakIndex) TvColors.Accent else TvColors.AccentLight.copy(alpha = 0.5f))
                    )
                }
            }
        }
        if (parts) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..3).forEach { Text(partName(it), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, fontFamily = Inter, fontSize = 10.sp, color = TvColors.TextMuted) }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0", "6", "12", "18", "24").forEach { Text(it, fontFamily = Inter, fontSize = 10.sp, color = TvColors.TextMuted) }
            }
        }
    }
}

@Composable
private fun partName(i: Int): String = stringResource(
    when (i) { 0 -> R.string.stats_part_night; 1 -> R.string.stats_part_morning; 2 -> R.string.stats_part_afternoon; else -> R.string.stats_part_evening }
)

/** The clips of the period in one line: three figures and the channels most of them came from. */
@Composable
private fun Clips(p: PeriodStats, prefs: StatsPrefs) {
    val fmt = prefs.timeFormat
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(stringResource(R.string.stats_clips_title))
            Text(p.clipCount.toString(), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TvColors.Text, maxLines = 1)
        }
        ClipFigure(formatDuration(p.clipSec, fmt), stringResource(R.string.stats_clips_time))
        ClipFigure(if (p.clipCount > 0) formatShortDuration(p.clipAverageSec, fmt) else "–", stringResource(R.string.stats_clips_average))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(stringResource(R.string.stats_clips_channels))
            Text(
                if (p.topClipChannels.isEmpty()) "–" else p.topClipChannels.take(3).joinToString("   ·   ") { "${it.name} ${it.sessions}" },
                fontFamily = Inter, fontSize = 13.sp, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ClipFigure(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Label(label)
        Text(value, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TvColors.Text, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun Rank(title: String, rows: List<Ranked>, prefs: StatsPrefs) {
    val bySessions = prefs.rankBy == RankBy.SESSIONS
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Label(title)
        if (rows.isEmpty()) Text("–", fontFamily = Inter, fontSize = 14.sp, color = TvColors.TextMuted)
        val max = rows.maxOfOrNull { if (bySessions) it.sessions.toLong() else it.seconds }?.coerceAtLeast(1L) ?: 1L
        rows.forEachIndexed { i, r ->
            val value = if (bySessions) r.sessions.toLong() else r.seconds
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (i == 0) TvColors.Accent else TvColors.TextMuted, modifier = Modifier.width(16.dp))
                    Text(
                        r.name, fontFamily = Inter, fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, color = TvColors.Text,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (bySessions) stringResource(R.string.stats_sessions_short, r.sessions) else formatDuration(r.seconds, prefs.timeFormat),
                        fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMedium, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(4.dp).clip(TvShapes.r(2.dp)).background(TvColors.SurfaceHigh)) {
                    Box(Modifier.fillMaxWidth(value.toFloat() / max).fillMaxHeight().background(if (i == 0) TvColors.Accent else TvColors.AccentLight.copy(alpha = 0.6f)))
                }
            }
        }
    }
}

/** Live against recordings, as a ring with the live share in the middle or as one bar. */
@Composable
private fun LiveShare(p: PeriodStats, prefs: StatsPrefs) {
    val total = (p.liveSec + p.vodSec).coerceAtLeast(1L)
    val live = p.liveSec.toFloat() / total
    val liveColor = TvColors.Live
    val vodColor = TvColors.AccentLight
    val track = TvColors.SurfaceHigh
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label(stringResource(R.string.stats_live_title))
        if (prefs.liveStyle == LiveStyle.BAR) {
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center) {
                Text("${(live * 100).toInt()} %", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = TvColors.Text)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(10.dp).clip(TvShapes.r(5.dp)).background(track)) {
                    if (p.liveSec > 0) Box(Modifier.weight(p.liveSec.toFloat()).fillMaxHeight().background(liveColor))
                    if (p.vodSec > 0) Box(Modifier.weight(p.vodSec.toFloat()).fillMaxHeight().background(vodColor))
                }
                Legend(liveColor, stringResource(R.string.stats_legend_live), formatDuration(p.liveSec, prefs.timeFormat))
                Legend(vodColor, stringResource(R.string.stats_legend_recordings), formatDuration(p.vodSec, prefs.timeFormat))
            }
        } else {
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(74.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 9.dp.toPx()
                        val d = size.minDimension - stroke
                        val topLeft = Offset(stroke / 2, stroke / 2)
                        val arcSize = Size(d, d)
                        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                        if (p.liveSec > 0) drawArc(liveColor, -90f, 360f * live, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                        if (p.vodSec > 0) drawArc(vodColor, -90f + 360f * live, 360f * (1f - live), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    }
                    Text("${(live * 100).toInt()} %", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TvColors.Text)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Legend(liveColor, stringResource(R.string.stats_legend_live), formatDuration(p.liveSec, prefs.timeFormat))
                    Legend(vodColor, stringResource(R.string.stats_legend_recordings), formatDuration(p.vodSec, prefs.timeFormat))
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, name: String, time: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Column {
            Text(name, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TvColors.Text, maxLines = 1)
            Text(time, fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted, maxLines = 1, softWrap = false)
        }
    }
}

/** "Tue 3 Nov" for a day, "May 2026" for a month. */
private fun bucketName(start: LocalDate, unit: BucketUnit): String = when (unit) {
    BucketUnit.DAY -> "${start.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${start.dayOfMonth} ${start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
    BucketUnit.MONTH -> "${start.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${start.year}"
}

private fun axisLabel(start: LocalDate, unit: BucketUnit, count: Int): String = when {
    unit == BucketUnit.MONTH -> start.month.getDisplayName(TextStyle.NARROW, Locale.getDefault())
    count <= 7 -> start.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    else -> start.dayOfMonth.toString()
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
    ) { Text(text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
}

/** 95 min -> "1 h 35 min", 40 s -> "< 1 min"; the page does not show seconds. Used where no setting applies (the home card). */
internal fun duration(sec: Long): String = formatDuration(sec, TimeFormat.HOURS_MINUTES)
