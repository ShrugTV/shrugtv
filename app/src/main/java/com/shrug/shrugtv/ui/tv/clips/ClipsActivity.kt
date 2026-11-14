package com.shrug.shrugtv.ui.tv.clips

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.clips.ClipsFeed
import com.shrug.shrugtv.clips.ClipsPeriod
import com.shrug.shrugtv.clips.ClipsSeen
import com.shrug.shrugtv.clips.ClipsSeenMode
import com.shrug.shrugtv.clips.ClipsSettings
import com.shrug.shrugtv.clips.ClipsSort
import com.shrug.shrugtv.clips.ClipsSource
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.components.Destinations
import com.shrug.shrugtv.ui.tv.components.VodCard
import com.shrug.shrugtv.ui.tv.components.glassButton
import com.shrug.shrugtv.ui.tv.components.whiteButton
import com.shrug.shrugtv.ui.tv.themeBackground

/**
 * The best clips of the channels you follow: one grid, with source, period and sort chosen right on
 * the page (the same settings as under Settings → Home screen → Clips, so a change here is kept).
 */
class ClipsActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    /** Counts up whenever the page comes back (from the player), so opened clips are dimmed or gone right away. */
    private var resumes by androidx.compose.runtime.mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        resumes++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024, 1024)
        window.setBackgroundDrawable(null)
        setContent {
            TvHomeTheme {
                ClipsScreen(
                    resumes = resumes,
                    open = { Destinations.open(this, it) },
                    contextMenu = { Destinations.contextMenu(this, it) },
                )
            }
        }
    }
}

@Composable
private fun ClipsScreen(resumes: Int, open: (Movie) -> Unit, contextMenu: (Movie) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(ctx) }
    var settings by remember { mutableStateOf(ClipsSettings.load(ctx)) }
    var clips by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val firstCard = remember { FocusRequester() }

    // The focus goes to the first card once, when the page opens; after a change of the choices it stays on the button, so the
    // next button can be pressed right away
    var focusedFirstCard by remember { mutableStateOf(false) }
    LaunchedEffect(settings) {
        loading = true
        // A choice was changed: the old list goes away at once, otherwise it looks as if nothing happened while it loads
        clips = emptyList()
        clips = ClipsFeed.load(ctx, settings)
        loading = false
    }
    LaunchedEffect(clips) {
        if (clips.isNotEmpty() && !focusedFirstCard) {
            focusedFirstCard = true
            runCatching { firstCard.requestFocus() }
        }
    }
    // What was opened since the list was built: hidden or dimmed according to the setting
    // The page starts with one load of clips and grows by one load whenever you get near the end; the queue is the whole pool
    var visibleCount by remember(clips, settings) { mutableStateOf(settings.count) }
    val shown = remember(clips, settings.seen, resumes) {
        val seen = if (settings.seen == ClipsSeenMode.SHOW) emptySet() else ClipsSeen.all(ctx)
        for (c in clips) c.clipSeen = settings.seen == ClipsSeenMode.DIM && c.clipSlug in seen
        if (settings.seen == ClipsSeenMode.HIDE) clips.filter { it.clipSlug !in seen } else clips
    }

    fun change(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        settings = ClipsSettings.load(ctx)
    }

    Column(Modifier.fillMaxSize().themeBackground().padding(start = 80.dp, top = 36.dp, end = 48.dp)) {
        Text(stringResource(R.string.clips_title), fontFamily = Inter, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                for (p in ClipsPeriod.values()) Chip(periodName(p), settings.period == p) { change(ClipsSettings.PREF_PERIOD, p.key) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                for (s in ClipsSource.values()) Chip(stringResource(if (s == ClipsSource.FAVORITES) R.string.clips_source_favorites else R.string.clips_source_followed), settings.source == s) { change(ClipsSettings.PREF_SOURCE, s.key) }
                for (s in ClipsSort.values()) Chip(stringResource(when (s) { ClipsSort.NEWEST -> R.string.clips_sort_newest; ClipsSort.SHORTEST -> R.string.clips_sort_shortest; ClipsSort.RELATIVE -> R.string.clips_sort_relative; else -> R.string.clips_sort_views }), settings.sort == s) { change(ClipsSettings.PREF_SORT, s.key) }
            }
        }
        Box(Modifier.padding(top = 18.dp)) {
            if (shown.isEmpty()) {
                val text = when {
                    loading -> R.string.clips_loading
                    settings.source == ClipsSource.FAVORITES -> R.string.clips_empty_favorites
                    else -> R.string.clips_empty
                }
                Text(stringResource(text), fontFamily = Inter, fontSize = 13.sp, color = TvColors.TextMuted)
            }
            val visible = shown.take(visibleCount)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
            ) {
                itemsIndexed(visible, key = { _, m -> m.clipSlug }) { i, m ->
                    // Two rows before the end: the next load is already there when you get to it
                    if (i >= visible.size - 10 && visible.size < shown.size) {
                        LaunchedEffect(visible.size) { visibleCount = ClipsFeed.grow(visible.size, settings.count, shown.size) }
                    }
                    Box(if (i == 0) Modifier.focusRequester(firstCard) else Modifier) {
                        androidx.compose.runtime.key(resumes) {
                            // The list as shown is the queue: the next clip starts when this one ends
                            VodCard(m, click = { com.shrug.shrugtv.clips.ClipQueue.set(shown); open(m) }, longPress = { contextMenu(m) }, width = 150.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun periodName(p: ClipsPeriod) = stringResource(
    when (p) {
        ClipsPeriod.DAY -> R.string.clips_period_day
        ClipsPeriod.WEEK -> R.string.clips_period_week
        ClipsPeriod.MONTH -> R.string.clips_period_month
        ClipsPeriod.YEAR -> R.string.clips_period_year
        ClipsPeriod.ALL -> R.string.clips_period_all
    }
)

@Composable
private fun Chip(text: String, selected: Boolean, click: () -> Unit) {
    Button(onClick = click, colors = if (selected) whiteButton() else glassButton()) { Text(text, fontFamily = Inter) }
}
