package com.shrug.shrugtv.ui.tv.player

import android.view.KeyEvent
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.player.QualityMatch
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors

/**
 * Complete Compose PlayerScreen (slices 4b, 4c, 4d).
 *
 * Verwaltet:
 * - ExoPlayer video surface (single and multiview split screen)
 * - Compose overlays (bar, channel switcher, quality menu, search, message, statistics, speed, chapters)
 * - Seek HUD (+10s / -10s)
 * - Remote D-pad key mapping
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToChannel: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val movie by viewModel.currentMovie.collectAsState()
    val mainPlayer by viewModel.mainPlayerState.collectAsState()
    val overlay by viewModel.activeOverlay.collectAsState()
    val currentQuality by viewModel.currentQuality.collectAsState()
    val isFollowed by viewModel.isFollowed.collectAsState()
    val qualities by viewModel.qualities.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val isChatVisible by viewModel.isChatVisible.collectAsState()
    val chatLines by viewModel.chatViewModel.lines.collectAsState()
    val chatRoomSettings by viewModel.chatViewModel.roomSettings.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val seekHudText by viewModel.seekHudText.collectAsState()
    val isMultiview by viewModel.isMultiview.collectAsState()
    val isPrimaryAudio by viewModel.isPrimaryAudio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val streamAvatar by viewModel.streamAvatar.collectAsState()
    val pendingRaid by viewModel.pendingRaid.collectAsState()
    val liveCard by viewModel.liveCard.collectAsState()
    val liveArchive by viewModel.liveArchive.collectAsState()
    val archiveSwitch by viewModel.archiveSwitch.collectAsState()
    val endSuggestion by viewModel.endSuggestion.collectAsState()
    val endCountdown by viewModel.endCountdown.collectAsState()
    val externalNotice by viewModel.notice.collectAsState()
    // Audio only: black picture with name and clock; the chat stays on top
    val audioOnly = QualityMatch.isAudioOnly(currentQuality) && !isMultiview

    // Home Assistant / phone: "close the player"
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.closeRequests.collect { onBack() }
    }
    val playbackStatus by viewModel.playbackStatus.collectAsState()

    // Chat layout (docs/chat_plan.md phase 3): side by side is the default; "Adjust chat" changes it live
    val chatAppearance by viewModel.chatAppearance.collectAsState()
    val chatLayout = chatAppearance.layout
    val chatUnavailable by viewModel.chatUnavailable.collectAsState()
    val clipSourceReady by viewModel.clipSourceReady.collectAsState()
    val clipTitle by viewModel.clipTitle.collectAsState()
    val clipNav by viewModel.clipNav.collectAsState()
    val chatShown = isChatVisible && !chatUnavailable
    val lastBackAt = remember { longArrayOf(0L) }
    val sideBySide = chatShown && chatLayout == com.shrug.shrugtv.chat.ChatLayout.SIDE_BY_SIDE
    val chatWidth = chatAppearance.widthDp(chatAppearance.textSizeSp).dp
    val chatOnLeft = chatAppearance.onLeft
    // Animated, so the picture glides aside when the chat appears instead of jumping
    val videoStart by androidx.compose.animation.core.animateDpAsState(if (sideBySide && chatOnLeft) chatWidth else 0.dp, label = "videoStart")
    val videoEnd by androidx.compose.animation.core.animateDpAsState(if (sideBySide && !chatOnLeft) chatWidth else 0.dp, label = "videoEnd")
    val videoPadding = Modifier.padding(start = videoStart, end = videoEnd)
    // Transparent layout: only messages of the last 30 s, max. 8; re-checked every 2 s
    val transparentNow by produceState(0L, chatLayout, chatShown) {
        while (chatShown && chatLayout == com.shrug.shrugtv.chat.ChatLayout.TRANSPARENT) {
            value = android.os.SystemClock.uptimeMillis()
            kotlinx.coroutines.delay(2_000L)
        }
    }
    val adBlockLabel by viewModel.adBlockLabel.collectAsState()
    val burnIn = viewModel.burnInProtection

    val isVod = movie?.playbackType == Enums.PlaybackKind.VOD || (movie?.videoId ?: 0L) > 0L

    val playerFocusRequester = remember { FocusRequester() }
    // Highlighted button of the control bar and the "More" menu, kept across submenus (B34, B37)
    val controlsFocusIndex = remember { androidx.compose.runtime.mutableStateOf(0) }
    val moreMenuOpen = remember { androidx.compose.runtime.mutableStateOf(false) }
    // Opens the bar from the video with the first button highlighted
    val openControls = {
        controlsFocusIndex.value = 0
        moreMenuOpen.value = false
        viewModel.showOverlay(PlayerOverlayType.CONTROLS)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current

    // Bring the focus back to the root box when no overlay is active
    androidx.compose.runtime.LaunchedEffect(overlay) {
        if (overlay == PlayerOverlayType.NONE) {
            runCatching { playerFocusRequester.requestFocus() }
        }
    }

    // Initially on opening: show the control bar for 5 seconds
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.setOverlay(PlayerOverlayType.CONTROLS)
    }

    // Auto-hide after the LAST key press (setting "Hide the control bar after", default 8 s). Keyed only on the overlay, the bar used to vanish
    // 5 s after opening, also while the user was navigating it or the "More" menu (B29).
    var lastInteraction by remember { mutableLongStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(overlay, lastInteraction) {
        if (overlay == PlayerOverlayType.CONTROLS) {
            kotlinx.coroutines.delay(viewModel.controlsTimeoutMs)
            viewModel.dismissOverlay()
        }
    }

    // Paused for longer than the setting: rest screen until the next key press
    val restAfterMin = viewModel.restAfterMinutes
    val restShowing by produceState(false, isPlaying, playbackStatus, lastInteraction, restAfterMin) {
        value = false
        if (restAfterMin > 0 && !isPlaying && playbackStatus == PlaybackStatus.PAUSED) {
            kotlinx.coroutines.delay(restAfterMin * 60_000L)
            value = !viewModel.mainPlayer.playWhenReady
        }
    }

    // Statistics overlay: fresh values every second while it is open
    androidx.compose.runtime.LaunchedEffect(overlay) {
        while (overlay == PlayerOverlayType.STATS) {
            viewModel.updateStats()
            kotlinx.coroutines.delay(1_000L)
        }
    }

    // Distance to the live edge while the bar is open (live only): "Back to live" when far behind
    val liveOffsetMs by produceState(androidx.media3.common.C.TIME_UNSET, overlay, isVod) {
        while (overlay == PlayerOverlayType.CONTROLS && !isVod) {
            value = viewModel.mainPlayer.currentLiveOffset
            kotlinx.coroutines.delay(1_000L)
        }
    }
    val behindLive = viewModel.offerBackToLive && !isVod && liveOffsetMs != androidx.media3.common.C.TIME_UNSET &&
        liveOffsetMs > PlayerViewModel.BEHIND_LIVE_MS
    // Rewinding a live stream into its recording
    val inArchive = movie?.archiveOfLive == true
    // The recording of a live stream has the same timeline as the live stream, also while it loads
    val liveTimeline = liveArchive != null && (!isVod || inArchive)
    val timeline by produceState(viewModel.timelineMs(), overlay, liveTimeline) {
        while (overlay == PlayerOverlayType.CONTROLS && liveTimeline) {
            value = viewModel.timelineMs()
            kotlinx.coroutines.delay(500L)
        }
    }

    // VOD position for the progress bar: read from the player while the bar is visible (B15)
    val vodPositionMs by produceState(0L, overlay, isVod) {
        while (overlay == PlayerOverlayType.CONTROLS && isVod) {
            value = viewModel.mainPlayer.currentPosition
            kotlinx.coroutines.delay(500L)
        }
    }

    // Back key: first close an active overlay, otherwise back to the home screen
    BackHandler(enabled = true) {
        if (overlay == PlayerOverlayType.NONE && pendingRaid != null) {
            viewModel.dismissRaid()
        } else if (overlay == PlayerOverlayType.NONE && endSuggestion != null) {
            viewModel.cancelEndSuggestion()
        } else if (overlay == PlayerOverlayType.NONE && liveCard != null) {
            viewModel.dismissLiveCard()
        } else if (overlay != PlayerOverlayType.NONE) {
            viewModel.backFromOverlay()
        } else if (isMultiview) {
            viewModel.stopMultiview()
        } else {
            viewModel.saveVodProgress()
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                // Sees every key before the focused element does
                lastInteraction = android.os.SystemClock.uptimeMillis()
                val native = keyEvent.nativeKeyEvent
                // The key that ends the rest screen does nothing else
                if (restShowing) return@onPreviewKeyEvent true
                // Holding OK repeated the action (chat on/off/on…); only seeking may repeat (B33)
                native.repeatCount > 0 && (native.keyCode in NO_REPEAT_KEYS ||
                    (!isVod && overlay == PlayerOverlayType.NONE &&
                        (native.keyCode == KeyEvent.KEYCODE_DPAD_LEFT || native.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)))
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    if (overlay == PlayerOverlayType.CONTROLS) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> { focusManager.moveFocus(FocusDirection.Left); return@onKeyEvent true }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> { focusManager.moveFocus(FocusDirection.Right); return@onKeyEvent true }
                            KeyEvent.KEYCODE_DPAD_DOWN -> { focusManager.moveFocus(FocusDirection.Down); return@onKeyEvent true }
                        }
                    }
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_BACK -> {
                            if (overlay != PlayerOverlayType.NONE) {
                                viewModel.backFromOverlay()
                                return@onKeyEvent true
                            }
                            // Setting "Press BACK twice": the first press only asks, a second one within 2 s leaves
                            if (androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_stream_double_tap_back", false)) {
                                val now = android.os.SystemClock.uptimeMillis()
                                if (now - lastBackAt[0] > 2000L) {
                                    lastBackAt[0] = now
                                    android.widget.Toast.makeText(context, R.string.player_press_back_again, android.widget.Toast.LENGTH_SHORT).show()
                                    return@onKeyEvent true
                                }
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            // Raid card or "next channel" card: OK switches (docs/ideas.md 2, 3)
                            if (overlay == PlayerOverlayType.NONE && pendingRaid != null) {
                                viewModel.followRaid()
                                return@onKeyEvent true
                            }
                            if (overlay == PlayerOverlayType.NONE && endSuggestion != null) {
                                viewModel.playEndSuggestion()
                                return@onKeyEvent true
                            }
                            // "X is live" card: OK switches
                            if (overlay == PlayerOverlayType.NONE && liveCard != null) {
                                viewModel.switchToLiveCard()
                                return@onKeyEvent true
                            }
                            // Error or ended stream: OK tries again (B8)
                            if (overlay == PlayerOverlayType.NONE && (playbackStatus == PlaybackStatus.ERROR ||
                                    (playbackStatus == PlaybackStatus.ENDED && !isVod))) {
                                viewModel.retry()
                                return@onKeyEvent true
                            }
                            // Audio only: OK is the one press back to the picture
                            if (overlay == PlayerOverlayType.NONE && audioOnly) {
                                viewModel.toggleAudioOnly()
                                return@onKeyEvent true
                            }
                            if (overlay == PlayerOverlayType.NONE) {
                                openControls()
                                return@onKeyEvent true
                            }
                            false
                        }
                        // Yellow key: audio only on/off
                        KeyEvent.KEYCODE_PROG_YELLOW -> {
                            if (overlay == PlayerOverlayType.NONE || overlay == PlayerOverlayType.CONTROLS) {
                                viewModel.toggleAudioOnly()
                                return@onKeyEvent true
                            }
                        }
                        // Remote keys that were not used (B37)
                        KeyEvent.KEYCODE_MENU -> {
                            if (overlay == PlayerOverlayType.NONE || overlay == PlayerOverlayType.CONTROLS) {
                                controlsFocusIndex.value = Int.MAX_VALUE // clamped to "More", the last button
                                moreMenuOpen.value = !(overlay == PlayerOverlayType.CONTROLS && moreMenuOpen.value)
                                viewModel.showOverlay(PlayerOverlayType.CONTROLS)
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (overlay == PlayerOverlayType.NONE && !isVod && !isMultiview) {
                                viewModel.zap(1)
                                return@onKeyEvent true
                            }
                            if ((overlay == PlayerOverlayType.NONE || overlay == PlayerOverlayType.CONTROLS) && isVod) {
                                viewModel.jumpChapter(1)
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (overlay == PlayerOverlayType.NONE && !isVod && !isMultiview) {
                                viewModel.zap(-1)
                                return@onKeyEvent true
                            }
                            if ((overlay == PlayerOverlayType.NONE || overlay == PlayerOverlayType.CONTROLS) && isVod) {
                                viewModel.jumpChapter(-1)
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_INFO -> {
                            viewModel.setOverlay(PlayerOverlayType.STATS)
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_CAPTIONS -> {
                            viewModel.toggleChat()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (overlay == PlayerOverlayType.NONE) {
                                viewModel.setOverlay(PlayerOverlayType.SWITCHER)
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (overlay == PlayerOverlayType.NONE) {
                                openControls()
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (overlay == PlayerOverlayType.NONE) {
                                if (isVod) {
                                    viewModel.seekStep(-1)
                                    return@onKeyEvent true
                                } else if (isMultiview) {
                                    viewModel.swapMultiviewAudio()
                                    return@onKeyEvent true
                                } else {
                                    viewModel.zap(-1)
                                    return@onKeyEvent true
                                }
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (overlay == PlayerOverlayType.NONE) {
                                if (isVod) {
                                    viewModel.seekStep(1)
                                    return@onKeyEvent true
                                } else if (isMultiview) {
                                    viewModel.swapMultiviewAudio()
                                    return@onKeyEvent true
                                } else {
                                    viewModel.zap(1)
                                    return@onKeyEvent true
                                }
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            viewModel.togglePlayPause()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            if (!viewModel.mainPlayer.isPlaying) viewModel.togglePlayPause()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            if (viewModel.mainPlayer.isPlaying) viewModel.togglePlayPause()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            // Live: fast-forward means back to the live edge
                            if (isVod) viewModel.seekStep(1) else viewModel.jumpToLive()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            if (isVod || liveTimeline) viewModel.seekStep(-1)
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        // 1. Video (single or multiview); side by side it makes room for the chat
        Box(modifier = Modifier.fillMaxSize().then(videoPadding)) {
            // 1. Video-Ansicht (Single oder Multiview Split-Screen)
            if (isMultiview && viewModel.multiviewPlayer != null) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Hauptplayer (Links)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = if (isPrimaryAudio) 3.dp else 0.dp,
                                color = if (isPrimaryAudio) TvColors.Focus else Color.Transparent
                            )
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    controllerAutoShow = false
                                }
                            },
                            update = { playerView ->
                                if (playerView.player !== mainPlayer) {
                                    playerView.player = mainPlayer
                                }
                                playerView.useController = false
                                playerView.hideController()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Zweitplayer (Rechts)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = if (!isPrimaryAudio) 3.dp else 0.dp,
                                color = if (!isPrimaryAudio) TvColors.Focus else Color.Transparent
                            )
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    controllerAutoShow = false
                                }
                            },
                            update = { playerView ->
                                if (playerView.player !== viewModel.multiviewPlayer) {
                                    playerView.player = viewModel.multiviewPlayer
                                }
                                playerView.useController = false
                                playerView.hideController()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            controllerAutoShow = false
                            // No black flash between live and its recording
                            setKeepContentOnPlayerReset(true)
                        }
                    },
                    update = { playerView ->
                        if (playerView.player !== mainPlayer) {
                            playerView.player = mainPlayer
                        }
                        playerView.useController = false
                        playerView.hideController()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Audio only: nothing to see, so a dim screen that wanders (under the chat and the overlays)
        if (audioOnly && viewModel.audioOnlyScreen) {
            RestScreen(
                channel = movie?.displayName.orEmpty(),
                title = movie?.title,
                hint = stringResource(R.string.player_audio_only_hint),
                // Over the picture the chat covers one side: the screen keeps to the other
                modifier = Modifier.then(videoPadding).padding(
                    start = if (chatShown && !sideBySide && chatOnLeft) chatWidth else 0.dp,
                    end = if (chatShown && !sideBySide && !chatOnLeft) chatWidth else 0.dp
                )
            )
        }

        // Chat-Panel rechts (#137)
        // Notice from Home Assistant or the phone, e.g. the doorbell with a camera picture
        externalNotice?.let { n ->
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(videoPadding)
                    .padding(top = if (pendingRaid != null) 110.dp else 24.dp)
                    .clip(TvShapes.r(16.dp))
                    .background(TvColors.Panel)
                    .border(1.dp, TvColors.glass(0.25f), TvShapes.r(16.dp))
                    .padding(14.dp)
            ) {
                if (n.imageUrl != null) {
                    com.shrug.shrugtv.ui.tv.home.NetImage(
                        url = n.imageUrl,
                        modifier = Modifier.width(192.dp).height(108.dp).clip(TvShapes.r(10.dp)),
                        width = 384,
                        height = 216
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                }
                androidx.compose.foundation.layout.Column(modifier = Modifier.widthIn(max = 420.dp)) {
                    Text(n.title, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    n.text?.let { Text(it, fontFamily = Inter, fontSize = 14.sp, color = TvColors.glass(0.8f)) }
                }
            }
        }

        // Raid from this channel: offer to follow (docs/ideas.md 2)
        pendingRaid?.let { raid ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(videoPadding)
                    .padding(top = 24.dp)
                    .clip(TvShapes.r(16.dp))
                    .background(TvColors.Panel)
                    .border(1.dp, TvColors.AccentSoft, TvShapes.r(16.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.player_raid_title, movie?.displayName ?: "", raid.targetDisplayName ?: ""),
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.player_raid_hint),
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        color = TvColors.glass(0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        val transparent = chatLayout == com.shrug.shrugtv.chat.ChatLayout.TRANSPARENT
        ChatPanel(
            visible = chatShown,
            lines = if (transparent) {
                chatLines.filter { !it.deleted && transparentNow - it.addedAt < 30_000L }.takeLast(8)
            } else chatLines,
            roomSettings = if (transparent) null else chatRoomSettings,
            modifier = Modifier
                // Height and vertical place from "Adjust chat"
                .align(
                    when (chatAppearance.vertical) {
                        com.shrug.shrugtv.chat.ChatVertical.TOP -> if (chatOnLeft) Alignment.TopStart else Alignment.TopEnd
                        com.shrug.shrugtv.chat.ChatVertical.MIDDLE -> if (chatOnLeft) Alignment.CenterStart else Alignment.CenterEnd
                        com.shrug.shrugtv.chat.ChatVertical.BOTTOM -> if (chatOnLeft) Alignment.BottomStart else Alignment.BottomEnd
                    }
                )
                .burnInDrift(burnIn, "chat")
                .width(chatWidth)
                .fillMaxHeight(chatAppearance.heightFraction),
            textSizeSp = chatAppearance.textSizeSp,
            background = when (chatLayout) {
                com.shrug.shrugtv.chat.ChatLayout.SIDE_BY_SIDE -> TvColors.ChatSide
                com.shrug.shrugtv.chat.ChatLayout.OVERLAY -> Color.Black.copy(alpha = chatAppearance.backgroundAlpha)
                com.shrug.shrugtv.chat.ChatLayout.TRANSPARENT -> Color.Transparent
            },
            textShadow = when (viewModel.chatLook.textShadow) { "on" -> true; "off" -> false; else -> transparent },
            fromLeft = chatOnLeft,
            look = viewModel.chatLook
        )

        // A followed channel went live: offer to switch. Only without an open
        // overlay, because only then OK goes to the card. Drawn after the chat, so it stays on top
        liveCard?.takeIf { overlay == PlayerOverlayType.NONE }?.let { live ->
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // Beside the chat when it is on the right, side by side or over the picture
                    // Below the ad-break sign, which sits in the same corner
                    .padding(top = if (adBlockLabel != null) 80.dp else 24.dp, end = if (chatShown && !chatOnLeft) chatWidth + 24.dp else 24.dp)
                    .clip(TvShapes.r(16.dp))
                    .background(TvColors.Panel)
                    .border(1.dp, TvColors.Live, TvShapes.r(16.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                live.avatarUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    com.shrug.shrugtv.ui.tv.home.NetImage(
                        url = url,
                        modifier = Modifier.width(44.dp).height(44.dp).clip(TvShapes.r(22.dp)),
                        width = 88,
                        height = 88
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                }
                androidx.compose.foundation.layout.Column(modifier = Modifier.widthIn(max = 360.dp)) {
                    Text(
                        text = stringResource(R.string.player_live_card_title, live.displayName.ifBlank { live.login ?: "" }),
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                    if (live.gameName.isNotBlank()) {
                        Text(live.gameName, fontFamily = Inter, fontSize = 14.sp, color = TvColors.glass(0.8f), maxLines = 1)
                    }
                    Text(
                        text = stringResource(R.string.player_live_card_hint),
                        fontFamily = Inter,
                        fontSize = 13.sp,
                        color = TvColors.glass(0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Ad-break sign from the ad blocker (B13)
        adBlockLabel?.let { label ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // Inside the video area: side by side it covered the top of the chat
                    .then(videoPadding)
                    .padding(24.dp)
                    .clip(TvShapes.r(12.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(label),
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
        // Who is streaming, top left while the bar is open (avatar, LIVE data, category, title)
        // Same fade and slide as the bar and the clock (CONTROLS_ANIM_MS / CONTROLS_SLIDE), so they come and go together
        val m = movie
        val slidePx = with(androidx.compose.ui.platform.LocalDensity.current) { CONTROLS_SLIDE.roundToPx() }
        AnimatedVisibility(
            visible = overlay == PlayerOverlayType.CONTROLS && m != null && viewModel.showInfoCorner,
            enter = fadeIn(androidx.compose.animation.core.tween(CONTROLS_ANIM_MS)) +
                androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(CONTROLS_ANIM_MS)) { -slidePx },
            exit = fadeOut(androidx.compose.animation.core.tween(CONTROLS_ANIM_MS)) +
                androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(CONTROLS_ANIM_MS)) { -slidePx },
            modifier = Modifier.align(Alignment.TopStart).then(videoPadding)
        ) {
            movie?.let { mv ->
                PlayerStreamInfo(
                    movie = mv,
                    avatarUrl = streamAvatar,
                    isFollowed = isFollowed,
                    isVod = isVod,
                    behindLiveSeconds = if (behindLive) (liveOffsetMs / 1000).toInt() else null,
                    modifier = Modifier.burnInDrift(burnIn, "info").padding(24.dp)
                )
            }
        }

        // The title of a clip stays on screen (setting "Show the clip title"); while the bar is open the info corner takes its place
        AnimatedVisibility(
            visible = movie?.isClip == true && viewModel.showClipTitle && !clipTitle.isNullOrEmpty() && overlay != PlayerOverlayType.CONTROLS,
            enter = fadeIn(androidx.compose.animation.core.tween(CONTROLS_ANIM_MS)),
            exit = fadeOut(androidx.compose.animation.core.tween(CONTROLS_ANIM_MS)),
            modifier = Modifier.align(Alignment.TopStart).then(videoPadding)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .burnInDrift(burnIn, "info")
                    .padding(24.dp)
                    .clip(com.shrug.shrugtv.ui.tv.TvShapes.r(12.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(max = 560.dp)
            ) {
                Text(clipTitle.orEmpty(), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, maxLines = 2)
                val where = clipNav?.let { " · ${it.first} / ${it.second}" }.orEmpty()
                Text(movie?.displayName.orEmpty() + where, fontFamily = Inter, fontSize = 13.sp, color = TvColors.glass(0.75f))
            }
        }

        // Centre of the video area: seek HUD, loading note and error box one under the other, never on
        // top of each other
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.align(Alignment.Center).then(videoPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            // Seek HUD Anzeige (+10s / -10s)
            AnimatedVisibility(
                visible = seekHudText != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(TvShapes.r(12.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, TvColors.glass(0.2f), TvShapes.r(12.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = seekHudText ?: "",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }
            }

            // Switch between live and the recording: say what happens instead of a hanging picture
            if (archiveSwitch != 0) {
                Box(
                    modifier = Modifier
                        .clip(TvShapes.r(16.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, TvColors.glass(0.2f), TvShapes.r(16.dp))
                        .padding(horizontal = 28.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(if (archiveSwitch == 1) R.string.player_loading_archive else R.string.player_loading_live),
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            // Error or end of a live stream: say so instead of a black picture (B8)
            if (playbackStatus == PlaybackStatus.ERROR || (playbackStatus == PlaybackStatus.ENDED && !isVod)) {
                Box(
                    modifier = Modifier
                        .clip(TvShapes.r(16.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, TvColors.glass(0.2f), TvShapes.r(16.dp))
                        .padding(horizontal = 32.dp, vertical = 20.dp)
                ) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val next = endSuggestion
                        Text(
                            text = stringResource(if (playbackStatus == PlaybackStatus.ERROR) R.string.player_playback_error else R.string.player_stream_ended),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        if (next != null) {
                            // Next followed live channel, switched to when the countdown ends (docs/ideas.md 3)
                            Text(
                                text = if (endCountdown > 0) {
                                    stringResource(R.string.player_next_channel, next.displayName.ifEmpty { next.login ?: "" }, endCountdown)
                                } else {
                                    stringResource(R.string.player_next_channel_suggest, next.displayName.ifEmpty { next.login ?: "" })
                                },
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color.White,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            if (next.gameName.isNotEmpty()) {
                                Text(text = next.gameName, fontFamily = Inter, fontSize = 14.sp, color = TvColors.glass(0.7f))
                            }
                        }
                        Text(
                            text = stringResource(if (next != null) R.string.player_next_hint else R.string.player_status_hint),
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            color = TvColors.glass(0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Channel switcher overlay (#70)
        ChannelSwitcherOverlay(
            visible = overlay == PlayerOverlayType.SWITCHER,
            current = movie,
            onSelect = { selectedMovie ->
                viewModel.dismissOverlay()
                viewModel.play(selectedMovie)
            },
            onMultiview = { secondMovie ->
                viewModel.dismissOverlay()
                viewModel.startMultiview(secondMovie)
            },
            onDismiss = { viewModel.dismissOverlay() }
        )

        // 3. Player control bar overlay (#69, #74)
        PlayerControlsOverlay(
            visible = overlay == PlayerOverlayType.CONTROLS,
            movie = movie,
            currentQuality = currentQuality,
            isFollowed = isFollowed,
            isVod = isVod,
            liveTimeline = liveTimeline,
            compact = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getString("pref_player_bar_labels", "highlighted") != "all",
            vodPositionMs = if (liveTimeline) timeline[0] else vodPositionMs,
            vodDurationMs = if (liveTimeline) timeline[1] else viewModel.mainPlayer.duration.coerceAtLeast(0L),
            isPlaying = isPlaying,
            isMultiview = isMultiview,
            phoneAvailable = com.shrug.shrugtv.companion.Companion.isEnabled(),
            chapters = chapters,
            onQualityClick = { viewModel.setOverlay(PlayerOverlayType.QUALITY) },
            onChatClick = { viewModel.toggleChat() },
            onChatLayoutClick = { viewModel.toggleChatLayout() },
            onChannelClick = {
                viewModel.dismissOverlay()
                movie?.let { onNavigateToChannel(it) }
            },
            // Helix has no follow endpoint; like the channel page, follow via QR code in the official app (B14)
            onFollowClick = {
                movie?.let { m ->
                    viewModel.dismissOverlay()
                    context.startActivity(com.shrug.shrugtv.ui.tv.dialogs.FollowQrActivity.intent(context, m))
                }
            },
            onSearchClick = { viewModel.setOverlay(PlayerOverlayType.SEARCH) },
            onSpeedClick = { viewModel.setOverlay(PlayerOverlayType.SPEED) },
            onChaptersClick = { viewModel.setOverlay(PlayerOverlayType.CHAPTERS) },
            onJumpToSource = if (movie?.isClip == true) ({ viewModel.jumpToClipSource() }) else null,
            sourceAvailable = clipSourceReady,
            onPreviousClip = if (clipNav != null) ({ viewModel.playClipStep(-1) }) else null,
            hasPreviousClip = (clipNav?.first ?: 1) > 1,
            onNextClip = if (clipNav != null) ({ viewModel.playClipStep(1) }) else null,
            hasNextClip = clipNav?.let { it.first < it.second } == true,
            onPreviousCategoryClick = { viewModel.jumpChapter(-1) },
            onNextCategoryClick = { viewModel.jumpChapter(1) },
            onMoreAction = { action ->
                when (action) {
                    PlayerMoreAction.SEND_MESSAGE -> viewModel.setOverlay(PlayerOverlayType.SEND_MESSAGE)
                    PlayerMoreAction.ADJUST_CHAT -> viewModel.openChatEditor()
                    PlayerMoreAction.STATS -> viewModel.setOverlay(PlayerOverlayType.STATS)
                    PlayerMoreAction.VOLUME,
                    PlayerMoreAction.ADJUST_MULTIVIEW -> {
                        if (isMultiview) {
                            viewModel.swapMultiviewAudio()
                        }
                    }
                    PlayerMoreAction.SAVE_VOD -> viewModel.saveVodProgress()
                    PlayerMoreAction.RELOAD_EMOTES -> viewModel.reloadEmotes()
                    PlayerMoreAction.PHONE -> context.startActivity(android.content.Intent(context, com.shrug.shrugtv.companion.CompanionQrActivity::class.java))
                    else -> {}
                }
            },
            onJumpToLive = if (behindLive || inArchive) ({ viewModel.jumpToLive() }) else null,
            // The bar closes: in the recording it changes its buttons, and a second OK must not land on "-10 s"
            onReplay = if (!isVod && !inArchive && viewModel.replayEnabled) ({ viewModel.replay(); viewModel.dismissOverlay() }) else null,
            buttonFocusIndex = controlsFocusIndex,
            moreMenuState = moreMenuOpen,
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onSeekRelative = { delta -> viewModel.seekRelative(delta) },
            onSeekStep = { direction -> viewModel.seekStep(direction) },
            onDismiss = { viewModel.dismissOverlay() },
            // Side by side the bar stays under the video, not over the chat
            modifier = videoPadding
        )

        // 4. Quality menu overlay (#85)
        PlayerQualityMenuOverlay(
            visible = overlay == PlayerOverlayType.QUALITY,
            qualities = qualities,
            currentQuality = currentQuality,
            onSelectQuality = { selected -> viewModel.selectQuality(selected) },
            onDismiss = { viewModel.backFromOverlay() }
        )

        // 5. Speed menu overlay (#85, slice 4c)
        PlayerSpeedOverlay(
            visible = overlay == PlayerOverlayType.SPEED,
            currentSpeed = currentSpeed,
            onSelectSpeed = { speed -> viewModel.selectSpeed(speed) },
            onDismiss = { viewModel.backFromOverlay() }
        )

        // 6. Kapitel-Auswahl Overlay (#74, Slice 4c)
        PlayerChaptersOverlay(
            visible = overlay == PlayerOverlayType.CHAPTERS,
            chapters = chapters,
            currentPositionMs = viewModel.timelineMs()[0],
            onSelectChapter = { chapter -> viewModel.selectChapter(chapter) },
            onDismiss = { viewModel.backFromOverlay() }
        )

        // 7. Suche im Player Overlay (#73)
        PlayerSearchOverlay(
            visible = overlay == PlayerOverlayType.SEARCH,
            onSelectStream = { selectedMovie ->
                viewModel.dismissOverlay()
                viewModel.play(selectedMovie)
            },
            onDismiss = { viewModel.backFromOverlay() }
        )

        // 8. Nachricht senden Overlay (#73)
        PlayerSendMessageOverlay(
            visible = overlay == PlayerOverlayType.SEND_MESSAGE,
            channelName = movie?.displayName ?: "",
            onSendMessage = { text ->
                viewModel.sendChatMessage(text)
                viewModel.dismissOverlay()
            },
            onDismiss = { viewModel.backFromOverlay() }
        )

        // 9. Video-Statistiken Overlay (#85)
        PlayerStatsOverlay(
            visible = overlay == PlayerOverlayType.STATS,
            stats = stats
        )

        // 10. Layout-Editor Overlay (#72)
        ChatAdjustOverlay(
            visible = overlay == PlayerOverlayType.LAYOUT_EDITOR,
            appearance = chatAppearance,
            textSizeSp = chatAppearance.textSizeSp,
            onChange = { change -> viewModel.updateChatAppearance(change) },
            onDismiss = { viewModel.backFromOverlay() }
        )

        // Long pause: the whole picture goes dark, chat included
        if (restShowing) {
            RestScreen(
                channel = movie?.displayName.orEmpty(),
                title = movie?.title,
                hint = stringResource(R.string.player_rest_hint)
            )
        }
    }
}

/** Keys whose auto-repeat (holding the key) is ignored in the player: only seeking may repeat (B33). */
private val NO_REPEAT_KEYS = setOf(
    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO,
    KeyEvent.KEYCODE_CAPTIONS, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN,
    KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
)
