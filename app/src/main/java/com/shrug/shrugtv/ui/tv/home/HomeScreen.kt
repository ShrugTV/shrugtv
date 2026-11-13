package com.shrug.shrugtv.ui.tv.home

import androidx.compose.foundation.BorderStroke
import com.shrug.shrugtv.ui.tv.themeBackground
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.components.FocusBorder
import com.shrug.shrugtv.ui.tv.components.focusEdgeLeft
import com.shrug.shrugtv.ui.tv.components.rowKeepsRight
import com.shrug.shrugtv.ui.tv.components.ContentCard
import com.shrug.shrugtv.ui.tv.components.LiveBadge
import com.shrug.shrugtv.ui.tv.components.glassButton
import com.shrug.shrugtv.ui.tv.components.duration
import com.shrug.shrugtv.ui.tv.components.whiteButton
import com.shrug.shrugtv.ui.tv.components.viewers

/**
 * Home screen, variant A "Hero": icon bar on the left, the selected live stream
 * fixed at the top with a muted live preview and buttons, scrolling card rows below. Sizes are the
 * mockup pixels halved (1920 px = 960 dp at xhdpi).
 *
 * [id] is the language-independent row key (LazyColumn key, cache, merging); [title] is the displayed,
 * localized title.
 */
@androidx.compose.runtime.Immutable
data class HomeRow(val id: String, val title: String, val entries: List<Movie>) {
    /** Unique keys for the LazyRow; duplicate entries get a counter. */
    val keys: List<String> by lazy {
        val seen = HashMap<String, Int>()
        entries.map { m ->
            val k = "${m.itemType}:${m.playbackId ?: m.login ?: m.gameId}"
            val n = seen.merge(k, 1, Int::plus)!!
            if (n == 1) k else "$k#$n"
        }
    }
}

/**
 * Everything except the hero. The hero is kept separate on purpose ([HomeScreen] gets it as a lambda):
 * it changes on every card focus, and as part of this state every change recomposed the whole page
 * including all rows.
 */
@androidx.compose.runtime.Immutable
data class HomeState(
    val loginState: Login? = null,
    val rows: List<HomeRow> = emptyList(),
    val loading: Boolean = true,
    val active: Boolean = true,
)

class HomeActions(
    val watch: (Movie) -> Unit,
    val toChannel: (Movie) -> Unit,
    val multiview: (Movie) -> Unit,
    val open: (Movie) -> Unit,
    val selectHero: (Movie) -> Unit,
    val search: () -> Unit,
    val settings: () -> Unit,
    val stats: () -> Unit = {},
    val profile: () -> Unit,
    val contextMenu: (Movie) -> Unit = {},
    /** The row list is scrolling (true) or at rest (false); the hero waits for it. */
    val scrolls: (Boolean) -> Unit = {},
)

private val HeroHeight = 340.dp
private val RowStart = 262.dp

@Composable
fun HomeScreen(z: HomeState, hero: () -> Movie?, a: HomeActions, statsCard: () -> com.shrug.shrugtv.stats.StatsCardData? = { null }) {
    // Fixed remote-control paths: vertically the icon bar is closer to the hero buttons than the first
    // row; without these, key down jumped into the bar (2026-10-21).
    val heroFocus = remember { FocusRequester() }
    val rowFocus = remember { FocusRequester() }
    // Key left only reaches the bar if a bar button is at the same height; since the bar has 4 instead
    // of 5 buttons, "Watch" sat too high (2026-10-21). So if the focus search finds nothing to the
    // left, focus goes to "Home".
    val bar = remember { FocusRequester() }
    val fm = androidx.compose.ui.platform.LocalFocusManager.current
    Box(Modifier.fillMaxSize().onKeyEvent { e ->
        if (e.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && e.key == androidx.compose.ui.input.key.Key.DirectionLeft) {
            if (!fm.moveFocus(androidx.compose.ui.focus.FocusDirection.Left)) runCatching { bar.requestFocus() }
            true
        } else false
    }) {
        // Background colour only below the hero: the hero is always covered by image, placeholder or
        // video; the surface underneath was one layer too many for the Fire TV GPU.
        // The window itself has no background any more (HomeActivity).
        Box(Modifier.fillMaxSize().padding(top = HeroHeight).themeBackground())
        HeroBackground(hero, z.active)

        HeroInfo(hero, z, a, heroFocus, rowFocus, Modifier.padding(start = 80.dp, top = 36.dp))

        // Prefetch window: upcoming rows are built in idle frames, not in the frame in
        // which they slide in. Fire TV before: a new row cost up to 39 ms of measure and layout in a single
        // frame (Perfetto, AndroidOwner:measureAndLayout).
        @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
        val list = rememberLazyListState(cacheWindow = LazyLayoutCacheWindow(ahead = 400.dp, behind = 200.dp))
        LaunchedEffect(list) { snapshotFlow { list.isScrollInProgress }.collect { a.scrolls(it) } }
        LazyColumn(
            state = list,
            // Outer padding: otherwise the list lies over the hero and key down does not find the rows
            modifier = Modifier.padding(start = 80.dp, top = RowStart).fillMaxWidth().fillMaxHeight(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // Row id as key: rows arrive one by one, focus must not jump
            items(z.rows.size, key = { z.rows[it].id }) { i ->
                CardRow(z.rows[i], a, if (i == 0) rowFocus else null, if (i == 0) heroFocus else null)
            }
        }

        NavBar(a, bar, Modifier.align(Alignment.CenterStart).padding(start = 16.dp).focusGroup())
        StatsCardOverlay(statsCard, Modifier.align(Alignment.TopEnd))
    }
}

/**
 * Card of the last 30 days at the start of a month. No focus and no keys: it
 * only informs and goes by itself, so it cannot get in the way of the remote. Separate composable, so
 * that only it recomposes when the card comes and goes.
 */
@Composable
private fun StatsCardOverlay(card: () -> com.shrug.shrugtv.stats.StatsCardData?, modifier: Modifier) {
    val c = card() ?: return
    Column(
        modifier
            .padding(top = 28.dp, end = 40.dp)
            .clip(TvShapes.r(16.dp))
            .background(TvColors.Panel)
            .border(1.dp, TvColors.Accent, TvShapes.r(16.dp))
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Text(
            stringResource(R.string.home_stats_card_title, com.shrug.shrugtv.ui.tv.stats.duration(c.totalSec)),
            fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White
        )
        c.topChannel?.let {
            Text(stringResource(R.string.home_stats_card_top, it), fontFamily = Inter, fontSize = 14.sp, color = TvColors.glass(0.85f))
        }
        Text(
            stringResource(R.string.home_stats_card_hint), fontFamily = Inter, fontSize = 12.sp,
            color = TvColors.glass(0.6f), modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Separate composable so that only it (not the rows) recomposes when the hero changes. */
@Composable
private fun HeroBackground(hero: () -> Movie?, active: Boolean) {
    Box(Modifier.fillMaxWidth().height(HeroHeight)) {
        HeroPreview(hero(), active, Modifier.fillMaxSize())
        HeroGradient(Modifier.fillMaxSize())
    }
}

/**
 * Hero darkening (left for the text, bottom towards the rows) as a small pre-painted bitmap, stretched to
 * hero size. As two brush gradients it cost the Fire TV GPU (PowerVR GE9215) about 10 ms per frame, the
 * stretched bitmap almost nothing (measured 2026-10-28). A linear gradient looks the
 * same when upscaled.
 */
@Composable
private fun HeroGradient(modifier: Modifier) {
    val background = TvColors.Background
    val dim = com.shrug.shrugtv.ui.tv.TvStyle.heroDim
    val image = remember(background, dim) { heroGradientImage(background, dim) }
    androidx.compose.foundation.Image(
        image, contentDescription = null, modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
        filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
    )
}

private fun heroGradientImage(background: Color, dim: Float): androidx.compose.ui.graphics.ImageBitmap {
    val w = 240f
    val h = 85f // hero 1920 x 680 px, one eighth
    val image = androidx.compose.ui.graphics.ImageBitmap(w.toInt(), h.toInt())
    val c = androidx.compose.ui.graphics.Canvas(image)
    val p = androidx.compose.ui.graphics.Paint()
    p.shader = androidx.compose.ui.graphics.LinearGradientShader(
        from = androidx.compose.ui.geometry.Offset(0f, 0f), to = androidx.compose.ui.geometry.Offset(w * 0.7f, 0f),
        colors = listOf(background.copy(alpha = (0.96f * dim).coerceAtMost(1f)), background.copy(alpha = (0.75f * dim).coerceAtMost(1f)), Color.Transparent),
        colorStops = listOf(0f, 0.543f, 1f),
    )
    c.drawRect(0f, 0f, w * 0.7f, h, p)
    p.shader = androidx.compose.ui.graphics.LinearGradientShader(
        from = androidx.compose.ui.geometry.Offset(0f, h * 0.55f), to = androidx.compose.ui.geometry.Offset(0f, h),
        colors = listOf(Color.Transparent, background),
    )
    c.drawRect(0f, h * 0.55f, w, h, p)
    return image
}

@Composable
private fun HeroInfo(heroSource: () -> Movie?, z: HomeState, a: HomeActions, heroFocus: FocusRequester, rowFocus: FocusRequester, modifier: Modifier) {
    val hero = heroSource()
    Column(modifier.width(440.dp).height(210.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            z.loginState is Login.LoggedOut -> {
                Text(stringResource(R.string.home_sign_in_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
                Text(stringResource(R.string.home_sign_in_hint), fontSize = 13.sp, color = TvColors.TextMedium)
                Button(onClick = a.profile, colors = whiteButton()) { Text(stringResource(R.string.home_sign_in), fontWeight = FontWeight.Bold) }
            }
            hero == null -> {
                Text(stringResource(if (z.loading) R.string.home_loading else R.string.home_nobody_live), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiveBadge()
                    ImageCache.liveTick.longValue // uptime ticks along every minute
                    Text(stringResource(R.string.home_hero_stats, viewers(hero), duration(hero)), fontSize = 10.sp, color = TvColors.TextMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NetImage(hero.avatarUrl, Modifier.size(36.dp).clip(CircleShape), width = 150, height = 150)
                    Column {
                        Text(hero.displayName ?: "", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text, maxLines = 1)
                        Text(hero.gameName ?: "", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TvColors.AccentLight, maxLines = 1)
                    }
                }
                Text(hero.title ?: "", fontSize = 13.sp, color = TvColors.TextMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                LaunchedEffect(Unit) { runCatching { heroFocus.requestFocus() } }
                Row(Modifier.focusProperties { down = rowFocus }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { a.watch(hero) }, colors = whiteButton(), modifier = Modifier.focusRequester(heroFocus)) {
                        Icon(HomeIcons.Play, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_watch), fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { a.toChannel(hero) }, colors = glassButton()) { Text(stringResource(R.string.home_to_channel)) }
                    Button(onClick = { a.multiview(hero) }, colors = glassButton()) { Text(stringResource(R.string.home_multiview)) }
                }
            }
        }
    }
}

/**
 * On card focus, preload the images of the next four cards and the hero image of the focused one
 *: when moving on they are already in NetImage's memory cache. Sizes as in
 * WideCard and HeroPreview, otherwise the cache misses.
 */
private fun preload(ctx: android.content.Context, row: HomeRow, i: Int) {
    val next = row.entries.drop(i + 1).take(4).filter { it.itemType == Enums.ContentType.STREAM || it.itemType == Enums.ContentType.VOD }
    ImageCache.preload(ctx, next.map { it.cardImageUrl }, 440, 248)
    ImageCache.preload(ctx, next.map { it.avatarUrl }, 80, 80)
    val m = row.entries[i]
    if (m.itemType == Enums.ContentType.STREAM) ImageCache.preload(ctx, listOf(m.cardImageUrl), 1280, 720)
}

@Composable
private fun CardRow(row: HomeRow, a: HomeActions, focus: FocusRequester?, upTarget: FocusRequester?) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(row.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
        var m = Modifier.focusRestorer()
        if (focus != null) m = Modifier.focusRequester(focus).then(m)
        if (upTarget != null) m = m.focusProperties { up = upTarget }
        // No prefetch window here: with 360 dp no benefit was measurable on the Fire TV, in the first run
        // scrolling with a held key was even worse (2026-10-28)
        LazyRow(m.rowKeepsRight().focusEdgeLeft(), horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(start = FocusBorder, end = 48.dp)) {
            items(row.entries.size, key = { row.keys[it] }, contentType = { row.entries[it].itemType }) { i ->
                val m = row.entries[i]
                ContentCard(m, click = { a.open(m) }, longPress = { a.contextMenu(m) }, focus = {
                    preload(ctx, row, i)
                    if (m.itemType == Enums.ContentType.STREAM) a.selectHero(m)
                })
            }
        }
    }
}

@Composable
private fun NavBar(a: HomeActions, start: FocusRequester, modifier: Modifier) {
    // Up/down fixed within the bar: otherwise geometry pulled focus from "Home" up back to "Watch"
    // (2026-10-21).
    val r = remember { List(5) { FocusRequester() } }
    fun chain(i: Int) = Modifier.focusProperties {
        up = if (i > 0) r[i - 1] else FocusRequester.Cancel
        down = if (i < r.lastIndex) r[i + 1] else FocusRequester.Cancel
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        NavButton(HomeIcons.Search, stringResource(R.string.home_nav_search), false, chain(0).focusRequester(r[0]), click = a.search)
        NavButton(HomeIcons.Start, stringResource(R.string.home_nav_home), true, chain(1).focusRequester(r[1]).focusRequester(start)) {}
        NavButton(HomeIcons.Stats, stringResource(R.string.home_nav_stats), false, chain(2).focusRequester(r[2]), click = a.stats)
        NavButton(HomeIcons.Settings, stringResource(R.string.home_nav_settings), false, chain(3).focusRequester(r[3]), click = a.settings)
        NavButton(HomeIcons.Profile, stringResource(R.string.home_nav_profile), false, chain(4).focusRequester(r[4]), click = a.profile)
    }
}

@Composable
private fun NavButton(icon: ImageVector, name: String, active: Boolean, modifier: Modifier = Modifier, click: () -> Unit) {
    IconButton(
        onClick = click,
        modifier = modifier.size(32.dp),
        colors = IconButtonDefaults.colors(
            containerColor = if (active) TvColors.Accent else Color.Transparent,
            contentColor = if (active) TvColors.OnAccent else TvColors.TextMuted,
            focusedContainerColor = TvColors.Ink,
            focusedContentColor = TvColors.Background,
        ),
    ) {
        Icon(icon, contentDescription = name, modifier = Modifier.size(16.dp))
    }
}
