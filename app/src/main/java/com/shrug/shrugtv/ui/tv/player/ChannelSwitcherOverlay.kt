package com.shrug.shrugtv.ui.tv.player

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.components.ContentCard
import com.shrug.shrugtv.ui.tv.components.cardImageUrlFor
import com.shrug.shrugtv.ui.tv.home.ImageCache
import com.shrug.shrugtv.ui.tv.components.glassButton
import com.shrug.shrugtv.ui.tv.components.whiteButton
import com.shrug.shrugtv.ui.tv.home.HomeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Channel switcher in the player as a modern film strip with lists.
 *
 * 1. On top the name of the current list, the other lists faded beside it.
 * 2. Up/Down step through the lists (setting `pref_switcher_updown`, default on); Left/Right page
 *    through the cards. Off: the lists are buttons, as before 1.37.
 * 3. 16:9 cards as on the home screen with LIVE badge and viewer count. The current channel is missing from the list,
 *    empty lists are skipped when stepping.
 */
/** [key] is the switch in the settings that shows or hides the list. */
enum class ChannelSwitcherTab(val titleRes: Int, val key: String) {
    FOLLOWED(R.string.peek_tab_followed, "pref_switcher_list_followed"),
    SAME_GAME(R.string.switcher_tab_same_game, "pref_switcher_list_same_game"),
    SAME_GAME_LANGUAGE(R.string.peek_tab_game_language, "pref_switcher_list_same_game_language"),
    HISTORY(R.string.peek_tab_recent, "pref_switcher_list_history"),
    TOP_STREAMS(R.string.peek_tab_top_streams, "pref_switcher_list_top_streams"),
    TOP_LANGUAGE(R.string.switcher_tab_top_language, "pref_switcher_list_top_language"),
}

@Composable
fun ChannelSwitcherOverlay(
    visible: Boolean,
    onSelect: (Movie) -> Unit,
    onMultiview: (Movie) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPreview: (Movie) -> Unit = {},
    current: Movie? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        val context = LocalContext.current
        val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
        val stepKeys = remember { prefs.getBoolean("pref_switcher_updown", true) }
        // Only the lists switched on in the settings; read once per opening
        val tabs = remember(visible) { SwitcherNav.enabled { prefs.getBoolean(it.key, true) } }
        var currentTab by remember { mutableStateOf(tabs.first()) }
        // Every list is loaded when the switcher opens, so a list that turns out empty is known at once
        val lists = remember { mutableStateMapOf<ChannelSwitcherTab, List<Movie>>() }
        var lastDelta by remember { mutableStateOf(1) }

        val tabFocusRequesters = remember { List(tabs.size) { FocusRequester() } }
        val rootFocus = remember { FocusRequester() }
        val rowFocus = remember { FocusRequester() }

        // The lists that are shown in the header: without the ones known to be empty (the open one stays)
        val shown = if (stepKeys) tabs.filter { lists[it]?.isEmpty() != true || it == currentTab } else tabs
        val entries = lists[currentTab] ?: emptyList()
        val loading = lists[currentTab] == null

        LaunchedEffect(visible) {
            if (visible) {
                kotlinx.coroutines.delay(50)
                runCatching {
                    if (stepKeys) rootFocus.requestFocus() else tabFocusRequesters[tabs.indexOf(currentTab)].requestFocus()
                }
            }
        }

        LaunchedEffect(visible) {
            if (!visible) return@LaunchedEffect
            kotlinx.coroutines.coroutineScope {
                // The category of the playing channel: known from the card, else looked up by login (a link has none)
                val gameId = async(Dispatchers.IO) {
                    val known = current?.gameId ?: 0L
                    val login = current?.login.orEmpty()
                    if (known > 0L) known else if (login.isNotEmpty()) HomeData.gameIdOfLogin(context, login) else 0L
                }
                tabs.forEach { tab ->
                    launch {
                        val loaded = withContext(Dispatchers.IO) {
                            try {
                                SwitcherLists.load(context, tab) { gameId.await() }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                        // The channel that is playing is not a choice
                        lists[tab] = loaded.filter { current == null || current.userId == 0 || it.userId != current.userId }
                        // Thumbnails of the first cards into the image cache now, so switching shows finished cards
                        lists[tab]?.take(8)?.forEach { m ->
                            cardImageUrlFor(m.cardImageUrl)?.let { url -> launch { ImageCache.load(context.applicationContext, url, 440, 248) } }
                        }
                    }
                }
            }
        }

        // The open list turns out empty: on to the next one in the direction of the last key (never back to a known empty one)
        LaunchedEffect(currentTab, lists[currentTab]) {
            if (!stepKeys || lists[currentTab]?.isEmpty() != true) return@LaunchedEffect
            var index = tabs.indexOf(currentTab)
            repeat(tabs.size - 1) {
                index = SwitcherNav.skip(index, lastDelta, tabs.size)
                if (lists[tabs[index]]?.isEmpty() != true) {
                    currentTab = tabs[index]
                    return@LaunchedEffect
                }
            }
        }

        // Focus on the first card after every list (or on the frame itself while there is none)
        LaunchedEffect(currentTab, loading) {
            if (!stepKeys || !visible || loading) return@LaunchedEffect
            val focus = { if (entries.isNotEmpty()) rowFocus.requestFocus() else rootFocus.requestFocus() }
            // No fixed wait: the row exists after composition; if the node is not attached yet, try once more next frame
            if (runCatching { focus() }.isFailure) {
                androidx.compose.runtime.withFrameNanos { }
                runCatching { focus() }
            }
        }

        val accent = TvColors.Accent

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!stepKeys) Modifier else Modifier
                    .focusRequester(rootFocus)
                    .onPreviewKeyEvent { ke ->
                        val delta = when (ke.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> 1
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> -1
                            else -> return@onPreviewKeyEvent false
                        }
                        if (ke.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            val next = SwitcherNav.step(shown.indexOf(currentTab), delta, shown.size)
                            if (next == SwitcherNav.CLOSE) onDismiss()
                            else {
                                lastDelta = delta
                                currentTab = shown[next]
                            }
                        }
                        true
                    }
                    .focusable())
                .background(
                    Brush.verticalGradient(
                        // Dark early enough that the chat behind the header row does not show through
                        0.0f to Color.Transparent,
                        0.3f to Color(0xB3000000),
                        0.5f to TvColors.Background.copy(alpha = 0xE6 / 255f),
                        1.0f to TvColors.Background.copy(alpha = 0xF2 / 255f)
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Header & Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!stepKeys) Text(
                        text = stringResource(R.string.channel_switcher_title),
                        color = TvColors.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 24.dp)
                    )

                    shown.forEachIndexed { index, tab ->
                        val isSelected = tab == currentTab
                        if (stepKeys) {
                            // Not focusable: up/down switch, the focus stays on the cards. Same size for all lists, the
                            // open one is marked by colour and an accent line, so the names do not move
                            Box(
                                modifier = Modifier
                                    .padding(end = 22.dp)
                                    .padding(bottom = 6.dp)
                                    .drawBehind {
                                        if (isSelected) {
                                            val y = size.height + 4.dp.toPx()
                                            drawLine(accent, Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                                        }
                                    }
                            ) {
                                Text(
                                    text = stringResource(tab.titleRes),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) TvColors.Text else TvColors.TextMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            Button(
                                onClick = { currentTab = tab },
                                colors = if (isSelected) whiteButton() else glassButton(),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .focusRequester(tabFocusRequesters[index])
                            ) {
                                Text(
                                    text = stringResource(tab.titleRes),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    if (stepKeys) {
                        Spacer(modifier = Modifier.weight(1f))
                        // Number of channels in the open list, the key hint and the place among the lists
                        val count = if (!loading && entries.isNotEmpty()) stringResource(R.string.switcher_count, entries.size) + "   " else ""
                        Text(
                            text = count + "\u25B2 \u25BC   " + (shown.indexOf(currentTab) + 1) + "/" + shown.size,
                            color = TvColors.TextMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cards Row
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_loading),
                            color = TvColors.TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_nobody_live),
                            color = TvColors.TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Own scroll state per list: otherwise the row keeps the position of the card with the same key
                    val listState = androidx.compose.runtime.key(currentTab) { rememberLazyListState() }
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .focusRequester(rowFocus)
                    ) {
                        items(entries, key = { it.playbackId ?: "${it.userId}_${it.login}_${it.gameId}" }, contentType = { "card" }) { movie ->
                            ContentCard(
                                m = movie,
                                click = { onSelect(movie) },
                                focus = { onPreview(movie) },
                                longPress = { onMultiview(movie) },
                                width = 180.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
