package com.shrug.shrugtv.ui.tv.player

import android.view.KeyEvent
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.components.LiveBadge
import com.shrug.shrugtv.ui.tv.components.number
import com.shrug.shrugtv.ui.tv.home.NetImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Player bar in the modern glass style (slice 4a).
 *
 * Replaces `include_bottom_bar.xml`, `include_player_buttons.xml`, `include_clock.xml` and `include_player_more.xml`:
 * - Left: avatar, channel name, LIVE badge, viewers/runtime/game, stream title.
 * - Right: all items as round labelled buttons (quality, chat, channel, follow, search, adjust chat,
 *   statistics ...). If the width does not suffice, the first ones stay and "More" bundles the
 *   last ones in a glass menu.
 * - VOD control bar: scrubber, play/pause, ±10s, speed, chapters.
 */
enum class PlayerMoreAction(val iconRes: Int, val titleRes: Int) {
    VOLUME(R.drawable.speaker_wireless, R.string.player_more_volume),
    SEND_MESSAGE(R.drawable.send_message, R.string.player_more_send_message),
    ADJUST_CHAT(R.drawable.ic_resize_chat, R.string.player_more_chat_position),
    ADJUST_MULTIVIEW(R.drawable.ic_resize_multiview, R.string.player_more_multiview),
    VIDEO_SIZE(R.drawable.ic_baseline_aspect_ratio_24px, R.string.player_more_video_size),
    RELOAD_EMOTES(R.drawable.ic_emotes_reload, R.string.player_more_reload_emotes),
    SAVE_VOD(R.drawable.save_vod, R.string.player_more_save_vod),
    STATS(R.drawable.stat4nerds, R.string.player_more_stats),
    PHONE(R.drawable.ic_remote, R.string.player_more_phone),
}

/** One entry of the control bar; moves into the "More" menu when the bar is too narrow. */
private class BarItem(val iconRes: Int, val label: String, val onClick: () -> Unit)

private val BAR_BUTTON_WIDTH = 76.dp
private val BAR_BUTTON_GAP = 6.dp

/**
 * Compact bar: smaller round buttons side by side, the name only under the highlighted
 * one. Roughly 1.7 times as many buttons fit before the rest moves into "More".
 */
private val BAR_COMPACT_WIDTH = 46.dp
private val BAR_COMPACT_GAP = 4.dp
private val BAR_COMPACT_CIRCLE = 40.dp
private val BAR_COMPACT_ICON = 20.dp

/** Fade and slide of everything that comes with the control bar: one duration, one distance. */
internal const val CONTROLS_ANIM_MS = 220
internal val CONTROLS_SLIDE = 24.dp
private val BAR_LABEL_SIZE = 10.sp
private val BAR_LABEL_PADDING = 8.dp
private val BAR_LEFT_SPACER = 24.dp

@Composable
fun PlayerControlsOverlay(
    visible: Boolean,
    movie: Movie?,
    currentQuality: String,
    isFollowed: Boolean,
    isVod: Boolean = false,
    /** Live stream with a recording: timeline from the start of the stream. */
    liveTimeline: Boolean = false,
    /** Names only under the highlighted button, smaller buttons (setting "Names under the buttons"); false = names under all of them. */
    compact: Boolean = true,
    vodPositionMs: Long = 0L,
    vodDurationMs: Long = 0L,
    isPlaying: Boolean = true,
    isMultiview: Boolean = false,
    /** "Phone" in the More menu only when the phone companion is switched on. */
    phoneAvailable: Boolean = false,
    chapters: List<com.shrug.shrugtv.model.VodChapter> = emptyList(),
    onQualityClick: () -> Unit,
    onChatClick: () -> Unit,
    /** Switches the chat between next to the video and over the video. */
    onChatLayoutClick: () -> Unit = {},
    onChannelClick: () -> Unit,
    onFollowClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMoreAction: (PlayerMoreAction) -> Unit,
    onPlayPauseClick: () -> Unit = {},
    onSeekRelative: (Long) -> Unit = {},
    /** Left/right on the focused timeline: growing steps, direction -1 or +1. */
    onSeekStep: (Int) -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onChaptersClick: () -> Unit = {},
    /** Jump to the previous / next category of a recording. */
    onPreviousCategoryClick: () -> Unit = {},
    onNextCategoryClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    /** Shown as first button while far behind the live edge (docs/ideas.md 1). */
    onJumpToLive: (() -> Unit)? = null,
    /** "Replay": jump back a little in a live stream, then live again. */
    onReplay: (() -> Unit)? = null,
    // Owned by the caller so that the highlighted button survives a submenu (B34) and the menu key
    // can open "More" directly (B37)
    buttonFocusIndex: androidx.compose.runtime.MutableState<Int> = remember { mutableStateOf(0) },
    moreMenuState: androidx.compose.runtime.MutableState<Boolean> = remember { mutableStateOf(false) },
    modifier: Modifier = Modifier
) {
    var showMoreMenu by moreMenuState
    val moreMenuFocusRequester = remember { FocusRequester() }
    val buttonsRowFocusRequester = remember { FocusRequester() }
    val buttonCount = remember { mutableStateOf(0) }
    val buttonActions = remember { mutableListOf<() -> Unit>() }
    // OK acts on key-up, and only for a press whose key-down the row saw itself. Acting on key-down
    // let the key-up of the same press select the first entry of the menu it had just opened (B28).
    val okPressed = remember { booleanArrayOf(false) }
    val moreMenuWasOpen = remember { booleanArrayOf(false) }
    // The timeline of a recording can take the focus (Up from the buttons); the row keeps the real focus
    var timelineFocused by remember { mutableStateOf(false) }
    val timelineAvailable = (isVod || liveTimeline) && vodDurationMs > 0L

    // Only entries that do something in this situation (B36)
    val moreActions = remember(isMultiview, isVod, phoneAvailable) {
        PlayerMoreAction.values().filter { action ->
            when (action) {
                PlayerMoreAction.VOLUME, PlayerMoreAction.ADJUST_MULTIVIEW -> isMultiview
                PlayerMoreAction.SAVE_VOD -> isVod
                PlayerMoreAction.VIDEO_SIZE -> false
                PlayerMoreAction.PHONE -> phoneAvailable
                else -> true
            }
        }
    }

    // Everything the bar can show, in order; what does not fit moves into "More"
    val items = ArrayList<BarItem>()
    if (onJumpToLive != null) {
        items.add(BarItem(R.drawable.exo_icon_fastforward, stringResource(R.string.player_btn_live), onJumpToLive))
    }
    if (isVod) {
        items.add(BarItem(
            if (isPlaying) R.drawable.exo_icon_pause else R.drawable.exo_icon_play,
            stringResource(if (isPlaying) R.string.player_btn_pause else R.string.player_btn_play),
            onPlayPauseClick
        ))
        // Seeking while the bar is open (B35)
        items.add(BarItem(R.drawable.exo_icon_rewind, stringResource(R.string.player_btn_back10)) { onSeekRelative(-10_000L) })
        items.add(BarItem(R.drawable.exo_icon_fastforward, stringResource(R.string.player_btn_forward10)) { onSeekRelative(10_000L) })
        items.add(BarItem(R.drawable.exo_ic_speed, stringResource(R.string.player_panel_speed), onSpeedClick))
        if (chapters.isNotEmpty()) {
            items.add(BarItem(R.drawable.exo_icon_previous, stringResource(R.string.player_btn_prev_category), onPreviousCategoryClick))
            items.add(BarItem(R.drawable.exo_icon_next, stringResource(R.string.player_btn_next_category), onNextCategoryClick))
            items.add(BarItem(R.drawable.vod_chapters, stringResource(R.string.player_panel_chapters), onChaptersClick))
        }
    }
    if (!isVod && liveTimeline && chapters.isNotEmpty()) {
        items.add(BarItem(R.drawable.exo_icon_previous, stringResource(R.string.player_btn_prev_category), onPreviousCategoryClick))
        items.add(BarItem(R.drawable.exo_icon_next, stringResource(R.string.player_btn_next_category), onNextCategoryClick))
        items.add(BarItem(R.drawable.vod_chapters, stringResource(R.string.player_panel_chapters), onChaptersClick))
    }
    // The current quality is the button's label ("720p60", "Source") instead of a separate pill beside the buttons
    val qualityLabel = if (currentQuality.isEmpty()) stringResource(R.string.player_btn_quality)
    else com.shrug.shrugtv.player.QualityMatch.displayName(currentQuality, stringResource(R.string.player_quality_source), stringResource(R.string.player_quality_audio_only))
    items.add(BarItem(R.drawable.ic_baseline_high_quality_24px, qualityLabel, onQualityClick))
    // Right after quality, not first: OK out of habit on the first button must not jump back
    if (onReplay != null) items.add(BarItem(R.drawable.exo_icon_rewind, stringResource(R.string.player_btn_replay), onReplay))
    items.add(BarItem(R.drawable.ic_baseline_chat_24px, stringResource(R.string.player_btn_chat), onChatClick))
    items.add(BarItem(R.drawable.ic_baseline_aspect_ratio_24px, stringResource(R.string.player_btn_chat_layout), onChatLayoutClick))
    items.add(BarItem(R.drawable.ic_baseline_person_24px, stringResource(R.string.player_btn_channel), onChannelClick))
    if (!isVod) {
        items.add(BarItem(
            if (isFollowed) R.drawable.ic_follow_fill else R.drawable.ic_follow_outline,
            stringResource(R.string.player_btn_follow),
            onFollowClick
        ))
    }
    items.add(BarItem(R.drawable.magnify, stringResource(R.string.player_btn_search), onSearchClick))
    moreActions.forEach { action ->
        items.add(BarItem(action.iconRes, stringResource(action.titleRes)) { onMoreAction(action) })
    }

    var barWidthPx by remember { mutableStateOf(0) }
    val barWidth = if (barWidthPx > 0) with(LocalDensity.current) { barWidthPx.toDp() } else LocalConfiguration.current.screenWidthDp.dp
    val available = barWidth - BAR_LEFT_SPACER
    val moreItem = BarItem(R.drawable.ic_more, stringResource(R.string.player_more)) { showMoreMenu = !showMoreMenu }
    // Every button is as wide as its label needs (bold, as when highlighted), at least BAR_BUTTON_WIDTH, so
    // two long labels (as in some languages: "Previous category" next to "Next category") never run into each other
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelFamily = Inter
    val labelStyle = remember(labelFamily) { TextStyle(fontFamily = labelFamily, fontWeight = FontWeight.Bold, fontSize = BAR_LABEL_SIZE) }
    val buttonGap = if (compact) BAR_COMPACT_GAP else BAR_BUTTON_GAP
    fun widthOf(item: BarItem): Dp = if (compact) BAR_COMPACT_WIDTH else maxOf(
        BAR_BUTTON_WIDTH,
        with(density) { measurer.measure(item.label, labelStyle, maxLines = 1, softWrap = false).size.width.toDp() } + BAR_LABEL_PADDING
    )
    val widths = items.map { widthOf(it) }
    val moreWidth = widthOf(moreItem)
    // All fit: no "More". Otherwise "More" takes a slot itself and bundles the rest.
    var visibleCount = items.size
    if (widths.fold(0.dp) { sum, w -> sum + w } + buttonGap * (items.size - 1) > available) {
        var used = moreWidth
        var k = 0
        while (k < items.size && used + buttonGap + widths[k] <= available) {
            used += buttonGap + widths[k]
            k++
        }
        visibleCount = k.coerceAtLeast(1)
    }
    val overflow = items.drop(visibleCount)

    LaunchedEffect(visible) {
        if (visible) {
            okPressed[0] = false
            kotlinx.coroutines.delay(350)
            // The menu key opens the bar with "More" already open: leave the focus in the menu
            if (!showMoreMenu || overflow.isEmpty()) runCatching { buttonsRowFocusRequester.requestFocus() }
        } else {
            showMoreMenu = false
            timelineFocused = false
        }
    }

    LaunchedEffect(showMoreMenu) {
        if (showMoreMenu) {
            moreMenuWasOpen[0] = true
            kotlinx.coroutines.delay(50)
            runCatching { moreMenuFocusRequester.requestFocus() }
        } else if (moreMenuWasOpen[0]) {
            // Menu closed (Back or an entry): focus back on the button row, "More" stays highlighted
            moreMenuWasOpen[0] = false
            kotlinx.coroutines.delay(50)
            runCatching { buttonsRowFocusRequester.requestFocus() }
        }
    }

    val slidePx = with(LocalDensity.current) { CONTROLS_SLIDE.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        // The container (gradient) only fades; clock and bar slide a short way with the same timing as the info corner
        enter = fadeIn(tween(CONTROLS_ANIM_MS)),
        exit = fadeOut(tween(CONTROLS_ANIM_MS)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.6f to Color(0x66000000),
                        0.8f to TvColors.Background.copy(alpha = 0xCC / 255f),
                        1.0f to TvColors.Background.copy(alpha = 0xF2 / 255f)
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            ControlsClock(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .animateEnterExit(
                        enter = slideInVertically(tween(CONTROLS_ANIM_MS)) { -slidePx },
                        exit = slideOutVertically(tween(CONTROLS_ANIM_MS)) { -slidePx }
                    )
                    .padding(top = 28.dp, end = 40.dp)
            )

            // "More" menu popup
            if (showMoreMenu && overflow.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 140.dp, end = 32.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    PlayerMoreMenu(
                        items = overflow,
                        firstItemFocusRequester = moreMenuFocusRequester,
                        onAction = { item ->
                            showMoreMenu = false
                            item.onClick()
                        },
                        onDismiss = { showMoreMenu = false }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateEnterExit(
                        enter = slideInVertically(tween(CONTROLS_ANIM_MS)) { slidePx },
                        exit = slideOutVertically(tween(CONTROLS_ANIM_MS)) { slidePx }
                    )
                    .padding(horizontal = 32.dp, vertical = 20.dp)
                    .onSizeChanged { barWidthPx = it.width }
            ) {
                // VOD Progress Scrubber
                if (timelineAvailable) {
                    VodProgressBar(
                        positionMs = vodPositionMs,
                        durationMs = vodDurationMs,
                        chapters = chapters,
                        focused = timelineFocused,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Spacer(modifier = Modifier.width(24.dp))

                    // Right: quality pill + round buttons
                    Row(
                        // The row is the only focus target; the buttons only draw the highlight. The
                        // requester must come before focusable(): placed after it, it handed the focus
                        // to the first button, so OK always opened "Quality" (B32).
                        modifier = Modifier
                            .focusRequester(buttonsRowFocusRequester)
                            .focusable()
                            .onKeyEvent { ke ->
                                val native = ke.nativeKeyEvent
                                val down = native.action == KeyEvent.ACTION_DOWN
                                when (native.keyCode) {
                                    // Back right after opening "More", before the menu has the focus
                                    KeyEvent.KEYCODE_BACK -> {
                                        if (timelineFocused) {
                                            if (down) timelineFocused = false
                                            return@onKeyEvent true
                                        }
                                        if (!showMoreMenu) return@onKeyEvent false
                                        if (down) showMoreMenu = false
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        if (!timelineAvailable || showMoreMenu) return@onKeyEvent false
                                        if (down) timelineFocused = true
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        if (!timelineFocused) return@onKeyEvent false
                                        if (down) timelineFocused = false
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        if (timelineFocused) {
                                            if (down) onSeekStep(1)
                                            return@onKeyEvent true
                                        }
                                        val n = buttonCount.value
                                        if (down && n > 0) buttonFocusIndex.value = (buttonFocusIndex.value + 1).coerceAtMost(n - 1)
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        if (timelineFocused) {
                                            if (down) onSeekStep(-1)
                                            return@onKeyEvent true
                                        }
                                        val n = buttonCount.value
                                        if (down && n > 0) buttonFocusIndex.value = (buttonFocusIndex.value - 1).coerceAtLeast(0)
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                        if (down) {
                                            okPressed[0] = true
                                        } else if (native.action == KeyEvent.ACTION_UP && okPressed[0]) {
                                            okPressed[0] = false
                                            // On the timeline OK goes back to the buttons
                                            if (timelineFocused) timelineFocused = false
                                            else buttonActions.getOrNull(buttonFocusIndex.value)?.invoke()
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(buttonGap)
                    ) {
                        val shown = if (overflow.isEmpty()) items else items.take(visibleCount) + moreItem
                        val shownWidths = if (overflow.isEmpty()) widths else widths.take(visibleCount) + moreWidth
                        buttonActions.clear()
                        shown.forEachIndexed { index, item ->
                            PlayerRoundButton(
                                iconRes = item.iconRes,
                                label = item.label,
                                onClick = item.onClick,
                                width = shownWidths[index],
                                highlighted = !timelineFocused && index == buttonFocusIndex.value,
                                compact = compact
                            )
                            buttonActions.add(item.onClick)
                        }
                        SideEffect {
                            buttonCount.value = shown.size
                            // Remembered index may point past the end (fewer buttons, or "last" requested)
                            if (shown.isNotEmpty() && buttonFocusIndex.value > shown.size - 1) buttonFocusIndex.value = shown.size - 1
                        }
                    }
                }
            }
        }
    }
}

/**
 * Time at the top right while the bar is open. Wired to the existing settings
 * `pref_clock_showclock`, `pref_clock_showlocal`, `pref_clock_time_format` and, for the date line,
 * `pref_clock_show_date_v2` and `pref_clock_date_format`.
 */
@Composable
private fun ControlsClock(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    if (!prefs.getBoolean("pref_clock_showclock", true) || !prefs.getBoolean("pref_clock_showlocal", true)) return
    val timeFormat = remember { SimpleDateFormat(prefs.getString("pref_clock_time_format", "H:mm") ?: "H:mm", Locale.getDefault()) }
    val showDate = remember { prefs.getBoolean("pref_clock_show_date_v2", false) }
    val dateFormat = remember { SimpleDateFormat(prefs.getString("pref_clock_date_format", "yyyy-MM-dd") ?: "yyyy-MM-dd", Locale.getDefault()) }
    // Ticks at the full minute
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L - value % 60_000L + 50L)
        }
    }
    val drift = prefs.getBoolean("pref_player_burn_in", false)
    // A dark rounded plate: over the chat (layout "over the video") or a bright picture the bare digits were hard to read
    Column(
        modifier = modifier
            .burnInDrift(drift, "clock")
            .clip(TvShapes.r(12.dp))
            .background(TvColors.Background.copy(alpha = 0xCC / 255f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = timeFormat.format(Date(now)),
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = TvColors.Text
        )
        if (showDate) {
            Text(
                text = dateFormat.format(Date(now)),
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TvColors.TextMuted
            )
        }
    }
}

/**
 * A single round labelled button for the player bar.
 */
@Composable
private fun PlayerRoundButton(
    iconRes: Int,
    label: String,
    @Suppress("UNUSED_PARAMETER") onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = BAR_BUTTON_WIDTH,
    highlighted: Boolean = false,
    compact: Boolean = false
) {
    // Not focusable on purpose: the button row owns focus and keys and calls the action of the
    // highlighted button (see the row in PlayerControlsOverlay). A focusable button here took the
    // real focus away from the row (B32).
    val focused = highlighted

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(width)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) BAR_COMPACT_CIRCLE else 44.dp)
                .clip(CircleShape)
                .background(if (focused) Color.White else TvColors.glass(0.12f))
                .border(
                    width = if (focused) 2.dp else 0.dp,
                    color = if (focused) TvColors.Focus else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = if (focused) TvColors.Background else TvColors.Text,
                modifier = Modifier.size(if (compact) BAR_COMPACT_ICON else 22.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontFamily = Inter,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
            fontSize = BAR_LABEL_SIZE,
            // Compact: the name is only drawn for the highlighted button (it may be wider than the button, it
            // overflows to both sides); the line stays reserved, so the bar does not change its height
            color = if (compact && !focused) Color.Transparent else if (focused) TvColors.Focus else TvColors.TextMedium,
            maxLines = 1,
            softWrap = false,
            modifier = if (compact) Modifier.wrapContentWidth(unbounded = true) else Modifier
        )
    }
}

/**
 * "More" menu as a modal glass panel.
 */
@Composable
private fun PlayerMoreMenu(
    items: List<BarItem>,
    firstItemFocusRequester: FocusRequester,
    onAction: (BarItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(280.dp)
            .clip(TvShapes.r(16.dp))
            .background(TvColors.Panel)
            .border(1.dp, TvColors.glass(0.15f), TvShapes.r(16.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
        ) {
            Text(
                text = stringResource(R.string.player_more),
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TvColors.TextMuted,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            // The entries scroll with the focus; a menu taller than the space above the bar used to be cut
            // off at its end
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            items.forEachIndexed { index, item ->
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                val itemModifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TvShapes.r(8.dp))
                        .background(if (isFocused) Color.White else Color.Transparent)
                        .then(itemModifier)
                        .onKeyEvent { keyEvent ->
                            val native = keyEvent.nativeKeyEvent
                            // Keep the focus inside the menu (B31) and let Back close only the menu (B30)
                            when (native.keyCode) {
                                KeyEvent.KEYCODE_BACK -> {
                                    if (native.action == KeyEvent.ACTION_DOWN) onDismiss()
                                    return@onKeyEvent true
                                }
                                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> return@onKeyEvent true
                                KeyEvent.KEYCODE_DPAD_UP -> if (index == 0) return@onKeyEvent true
                                KeyEvent.KEYCODE_DPAD_DOWN -> if (index == items.lastIndex) return@onKeyEvent true
                            }
                            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    onAction(item)
                                }
                                true
                            } else false
                        }
                        .clickable(interactionSource = interactionSource, indication = null) {
                            onAction(item)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        tint = if (isFocused) TvColors.Background else TvColors.Text,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = item.label,
                        fontFamily = Inter,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (isFocused) TvColors.Background else TvColors.Text
                    )
                }
            }
            }
        }
    }
}

/**
 * Timeline of a recording: one coloured section per category (same game,
 * same colour), the played part strong, the rest faint, a gap at every change; above it the position,
 * the name of the current category and the length.
 */
@Composable
private fun VodProgressBar(
    positionMs: Long,
    durationMs: Long,
    chapters: List<com.shrug.shrugtv.model.VodChapter> = emptyList(),
    /** The timeline has the focus: taller, bigger playhead, the name of the category in the accent colour. */
    focused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val posText = formatTime(positionMs)
    val durText = formatTime(durationMs)
    val currentName = chapters.getOrNull(com.shrug.shrugtv.player.VodChapters.indexAt(chapters, positionMs))?.name.orEmpty()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = posText,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TvColors.Text
            )
            if (currentName.isNotEmpty()) {
                Text(
                    text = currentName,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (focused) TvColors.Focus else TvColors.TextMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Text(
                text = durText,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TvColors.TextMuted
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val accent = TvColors.Accent
        val focusColor = TvColors.Focus
        val track = TvColors.glass(0.2f)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val w = size.width
            val h = (if (focused) 8.dp else 4.dp).toPx()
            val top = (size.height - h) / 2
            val played = w * progress
            drawRect(track, androidx.compose.ui.geometry.Offset(0f, top), androidx.compose.ui.geometry.Size(w, h))
            if (durationMs <= 0L || chapters.isEmpty()) {
                drawRect(accent, androidx.compose.ui.geometry.Offset(0f, top), androidx.compose.ui.geometry.Size(played, h))
            } else {
                val gap = 2.dp.toPx()
                chapters.forEachIndexed { i, chapter ->
                    val x0 = w * chapter.positionMs / durationMs
                    val x1 = w * com.shrug.shrugtv.player.VodChapters.endMs(chapters, i, durationMs) / durationMs
                    val start = x0 + if (i > 0) gap / 2 else 0f
                    val end = x1 - if (i < chapters.lastIndex) gap / 2 else 0f
                    if (end > start) {
                        val color = chapterColor(chapter.name)
                        drawRect(color.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(start, top), androidx.compose.ui.geometry.Size(end - start, h))
                        val playedEnd = minOf(end, played)
                        if (playedEnd > start) {
                            drawRect(color, androidx.compose.ui.geometry.Offset(start, top), androidx.compose.ui.geometry.Size(playedEnd - start, h))
                        }
                    }
                }
            }
            // Playhead
            val headRadius = (if (focused) 7.dp else 4.dp).toPx()
            drawCircle(Color.White, radius = headRadius, center = androidx.compose.ui.geometry.Offset(played.coerceIn(headRadius, w - headRadius), size.height / 2))
            if (focused) drawCircle(focusColor, radius = headRadius, center = androidx.compose.ui.geometry.Offset(played.coerceIn(headRadius, w - headRadius), size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        }
    }
}

private val CHAPTER_COLORS = listOf(
    Color(0xFF9146FF), Color(0xFF00B8A9), Color(0xFFF6416C), Color(0xFFFFC857),
    Color(0xFF4D96FF), Color(0xFFFF9F45), Color(0xFF6BCB77), Color(0xFFC780FA)
)

/** The same game always gets the same colour. */
private fun chapterColor(name: String): Color = CHAPTER_COLORS[Math.floorMod(name.hashCode(), CHAPTER_COLORS.size)]

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
