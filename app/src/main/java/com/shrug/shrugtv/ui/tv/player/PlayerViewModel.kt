package com.shrug.shrugtv.ui.tv.player

import android.content.Context
import android.util.Log
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.shrug.shrugtv.api.FollowStatusChecker
import com.shrug.shrugtv.api.gql.GqlApi
import com.shrug.shrugtv.api.gql.GqlParser
import com.shrug.shrugtv.database.AppDatabase
import com.shrug.shrugtv.database.VodInfo
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.model.StreamQuality
import com.shrug.shrugtv.model.VodChapter
import com.shrug.shrugtv.player.PlayerFactory
import com.shrug.shrugtv.player.PlayerMediaSession
import com.shrug.shrugtv.player.PlaybackUrlResolver
import com.shrug.shrugtv.player.zapping.StreamZapper
import com.shrug.shrugtv.player.QualityMatch
import com.shrug.shrugtv.player.hls.TwitchAdBlocker
import com.shrug.shrugtv.chat.ChatPrefs
import com.shrug.shrugtv.util.Threads
import com.shrug.shrugtv.R
import com.shrug.shrugtv.session.AppSettings
import com.shrug.shrugtv.telemetry.CrashReporter
import com.shrug.shrugtv.util.TimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import com.shrug.shrugtv.hub.HubEvent
import com.shrug.shrugtv.hub.PlaybackSnapshot
import com.shrug.shrugtv.hub.PlaybackStateHub
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Player state management and player pool (slice 4b, 4c, 4d).
 *
 * Manages the main player, media session, VOD progress, chapters, speed and multiview:
 * - Takeover of the hero preview without restart (HeroHandover / adopted player).
 * - Direct handover of the usher master URL to ExoPlayer.
 * - Quality change without reloading through TrackSelectionOverride.
 * - VOD chapter query through GQL and display on the progress bar.
 * - Save and load the VOD playback position through Room (vod_info).
 * - Multiview secondary player with audio switching.
 * - Anbindung an PlayerMediaSession (System-API, kein Fire OS-Crash).
 */
enum class PlayerOverlayType {
    NONE,
    CONTROLS,
    SWITCHER,
    QUALITY,
    LAYOUT_EDITOR,
    SEARCH,
    SEND_MESSAGE,
    STATS,
    SPEED,
    CHAPTERS
}

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

/**
 * What the player's own events may do to the status that is shown: after a failure the
 * player goes idle and stops "playing", and neither may wipe the error that is on the screen; only a new
 * attempt (buffering, ready, playing) replaces it.
 */
object PlaybackStatusRules {
    fun afterStateChange(current: PlaybackStatus, shown: PlaybackStatus): PlaybackStatus =
        if (current == PlaybackStatus.ERROR && shown == PlaybackStatus.IDLE) current else shown

    fun afterPlayingChange(current: PlaybackStatus, isPlaying: Boolean): PlaybackStatus =
        if (isPlaying) PlaybackStatus.PLAYING else if (current == PlaybackStatus.ERROR) current else PlaybackStatus.PAUSED
}

class PlayerViewModel(private val appContext: Context) : ViewModel(), com.shrug.shrugtv.hub.PlayerCommands {
    companion object {
        /** Wait this long after a clip started before the next one is loaded in the background. */
        private const val PRELOAD_DELAY_MS = 3_000L
        private const val TAG = "PlayerViewModel"
        private const val HEALTH_TAG = "PlaybackHealth"
        /** Away from the player at least this long: a live stream is resolved again instead of resumed. */
        private const val STALE_AFTER_BACKGROUND_MS = 30_000L
        /**
         * Saving VOD progress must outlive the ViewModel: `viewModelScope` is already cancelled when
         * `onCleared` runs, so a save launched there never ran (docs/bug_review.md B3).
         */
        private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** How long the raid card waits for OK. */
        const val RAID_OFFER_SECONDS = 15

        /** Countdown before switching to the suggested channel when a stream ended. */
        const val END_COUNTDOWN_SECONDS = 15

        /** From this far behind the live edge the player offers "Back to live". */
        const val BEHIND_LIVE_MS = 30_000L
    }

    val chatViewModel = ChatViewModel().apply {
        // Same messages as one line with a counter, read once per player (default off)
        mergeWindowMs = (androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext)
            .getString("pref_chat_merge_repeats", "0")?.toLongOrNull() ?: 0L).coerceIn(0L, 60L) * 1000L
    }
    private var chatClient: com.shrug.shrugtv.chat.LiveChatClient? = null

    /** Settings group "In the player", read once per player. */
    private val playerPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    val controlsTimeoutMs: Long =
        (playerPrefs.getString("pref_player_controls_timeout", "8")?.toLongOrNull() ?: 8L).coerceIn(3L, 120L) * 1000L
    val showInfoCorner: Boolean = playerPrefs.getBoolean("pref_player_info_corner", true)
    /** The title of a clip stays on screen while it plays (setting "Show the clip title"). */
    val showClipTitle: Boolean = playerPrefs.getBoolean("pref_clips_show_title", true)
    private val _clipTitle = MutableStateFlow<String?>(null)
    val clipTitle: StateFlow<String?> = _clipTitle.asStateFlow()
    /** Place of the clip in the queue (1-based) and the length of the queue; null = no queue. */
    private val _clipNav = MutableStateFlow<Pair<Int, Int>?>(null)
    val clipNav: StateFlow<Pair<Int, Int>?> = _clipNav.asStateFlow()
    val offerBackToLive: Boolean = playerPrefs.getBoolean("pref_player_back_to_live", true)
    /** Clock, chat and info corner wander a few pixels. */
    val burnInProtection: Boolean = playerPrefs.getBoolean("pref_player_burn_in", false)
    /** Minutes of pause until the rest screen; 0 = never. */
    val restAfterMinutes: Int = playerPrefs.getString("pref_player_rest_after", "5")?.toIntOrNull() ?: 5
    /** Black picture with name and clock in "Audio only". */
    val audioOnlyScreen: Boolean = playerPrefs.getBoolean("pref_player_audio_only_screen", true)
    /** countdown | suggest | off */
    private val endOfStreamMode: String = playerPrefs.getString("pref_player_end_of_stream", "countdown") ?: "countdown"

    /** Chat settings, read once per player (B22/C0.4). */
    private val chatPrefs: ChatPrefs = ChatPrefs.from(appContext)
    /** Text size from "Adjust chat", changed live. */
    val chatTextSizeSp: Float get() = _chatAppearance.value.textSizeSp
    private val pxPerSp = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP, 1f, appContext.resources.displayMetrics
    )
    private fun chatTextPx(): Float = chatTextSizeSp * pxPerSp
    private fun chatEmotePx(): Int = ChatPrefs.emoteSizePx(_chatAppearance.value.emoteValue, chatTextPx(), pxPerSp)
    private fun chatBadgePx(): Int = (chatTextPx() * 1.15f).toInt().coerceAtLeast(1)

    /** Text or emote size changed in "Adjust chat": the chat restarts when the window closes. */
    private var chatSizesChanged = false
    val chatLook: com.shrug.shrugtv.chat.ChatLook get() = chatPrefs.look

    /** Layout, width, position, opacity of the chat; "Adjust chat" changes it live (CA12). */
    private val _chatAppearance = MutableStateFlow(
        com.shrug.shrugtv.chat.ChatAppearance.from(PreferenceManager.getDefaultSharedPreferences(appContext))
    )
    val chatAppearance: StateFlow<com.shrug.shrugtv.chat.ChatAppearance> = _chatAppearance.asStateFlow()

    fun updateChatAppearance(change: (com.shrug.shrugtv.chat.ChatAppearance) -> com.shrug.shrugtv.chat.ChatAppearance) {
        val old = _chatAppearance.value
        val updated = change(old)
        if (updated.sizesDiffer(old)) chatSizesChanged = true
        _chatAppearance.value = updated
        updated.save(PreferenceManager.getDefaultSharedPreferences(appContext))
    }

    /**
     * Shortcut button of the control bar: next to the video <-> over the video. "Transparent" (only
     * reachable through the settings) goes to next to the video. Saved like "Adjust chat".
     */
    fun toggleChatLayout() {
        updateChatAppearance {
            it.copy(layout = if (it.layout == com.shrug.shrugtv.chat.ChatLayout.SIDE_BY_SIDE) com.shrug.shrugtv.chat.ChatLayout.OVERLAY else com.shrug.shrugtv.chat.ChatLayout.SIDE_BY_SIDE)
        }
    }

    /** "Adjust chat" from the More menu: the chat must be visible to see the changes. */
    fun openChatEditor() {
        if (chatPrefs.joinChat) _isChatVisible.value = true
        showOverlay(PlayerOverlayType.LAYOUT_EDITOR)
    }

    /** Counts `play()` calls; resolver results of an older call are dropped (B4, fast zapping). */
    private var playRequest = 0

    /** The movie whose media item is actually loaded in [mainPlayer]; VOD progress is saved for it (B3). */
    private var loadedMovie: Movie? = null

    /** Ad-break sign from [TwitchAdBlocker] as a string resource, null = no break (B13). */
    private val _adBlockLabel = MutableStateFlow<Int?>(null)
    val adBlockLabel: StateFlow<Int?> = _adBlockLabel.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Stream volume 0-100, set from Home Assistant or the phone (not the TV volume). */
    private val _volume = MutableStateFlow(100)

    /** Profile picture of the current channel for the info corner; looked up if the Movie has none. */
    private val _streamAvatar = MutableStateFlow<String?>(null)
    val streamAvatar: StateFlow<String?> = _streamAvatar.asStateFlow()

    private fun loadStreamAvatar(movie: Movie) {
        _streamAvatar.value = movie.avatarUrl
        if (!movie.avatarUrl.isNullOrEmpty() || movie.userId <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Blocking off the main thread; fills movie.avatarUrl from its cache or Helix users
                com.shrug.shrugtv.apihelpers.ChannelAvatarLoader.loadAvatars(appContext, listOf(movie))
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
            if (_currentMovie.value === movie) _streamAvatar.value = movie.avatarUrl
        }
    }

    /** Resolves the numeric Twitch user ID if the stream was opened by login only (companion watchLogin, deep link). */
    private fun resolveUserId(movie: Movie, request: Int) {
        val login = movie.login ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = com.shrug.shrugtv.api.helix.HelixApi.getUsersByQuery(appContext, "?login=$login")
                val user = com.shrug.shrugtv.api.helix.HelixParser.parseFirstUser(json ?: return@launch)
                if (user != null && user.userId > 0) {
                    Log.i(TAG, "Resolved userId=${user.userId} for login=$login")
                    movie.userId = user.userId
                    if (movie.displayName.isNullOrEmpty()) movie.displayName = user.displayName
                    if (movie.avatarUrl.isNullOrEmpty()) movie.avatarUrl = user.avatarUrl
                    withContext(Dispatchers.Main) {
                        if (request == playRequest && _currentMovie.value === movie) {
                            loadStreamAvatar(movie)
                            checkFollowStatus(movie)
                            startChat(movie)
                        }
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
    }

    /** Automatic retries after a playback error, reset once the stream plays (B8). */
    private var errorRetries = 0

    /** Stall watch; [healthReload] marks a reload it started, so its counter is kept. */
    private val health = com.shrug.shrugtv.player.PlaybackHealth()
    private var healthReload = false

    // region Raid, end of stream, live edge (docs/ideas.md 1-3)

    /** A raid from the current channel: the player offers to follow it for [RAID_OFFER_SECONDS]. */
    private val _pendingRaid = MutableStateFlow<com.shrug.shrugtv.model.RaidNotification?>(null)
    val pendingRaid: StateFlow<com.shrug.shrugtv.model.RaidNotification?> = _pendingRaid.asStateFlow()
    private var raidJob: Job? = null

    /** Next followed live channel offered when a live stream ends, with seconds left to switch. */
    private val _endSuggestion = MutableStateFlow<Movie?>(null)
    val endSuggestion: StateFlow<Movie?> = _endSuggestion.asStateFlow()
    private val _endCountdown = MutableStateFlow(0)
    val endCountdown: StateFlow<Int> = _endCountdown.asStateFlow()
    private var endJob: Job? = null

    private fun onRaid(raid: com.shrug.shrugtv.model.RaidNotification) {
        if (raid.targetUserId.isNullOrEmpty()) return
        // Setting "Show incoming raid notification" (was never read before)
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        PlaybackStateHub.emit(HubEvent(HubEvent.RAID, mapOf(
            "from" to _currentMovie.value?.login, "to" to raid.targetDisplayName, "to_id" to raid.targetUserId
        )))
        if (!prefs.getBoolean("pref_stream_show_incoming_raid_notification", true)) return
        _pendingRaid.value = raid
        raidJob?.cancel()
        raidJob = viewModelScope.launch {
            delay(RAID_OFFER_SECONDS * 1000L)
            _pendingRaid.value = null
        }
    }

    /** OK on the raid card: go to the raided channel (login is looked up from the id). */
    fun followRaid() {
        val raid = _pendingRaid.value ?: return
        dismissRaid()
        val target = Movie().apply {
            userId = raid.targetUserId?.toIntOrNull() ?: 0
            displayName = raid.targetDisplayName ?: ""
            itemType = Enums.ContentType.STREAM
        }
        if (target.userId > 0) play(target)
    }

    // "X is live" card
    private val _liveCard = MutableStateFlow<Movie?>(null)
    val liveCard: StateFlow<Movie?> = _liveCard.asStateFlow()
    private var liveCardJob: Job? = null
    private var liveCardShownAt = 0L

    private val liveCardCollector = viewModelScope.launch {
        com.shrug.shrugtv.notification.LiveNotificationTicker.playerLive.collect { onFollowedLive(it) }
    }

    private fun onFollowedLive(movie: Movie) {
        val now = android.os.SystemClock.elapsedRealtime()
        val busy = _pendingRaid.value != null || _endSuggestion.value != null || _liveCard.value != null
        if (!LiveCardGate.shouldShow(now, liveCardShownAt, movie.userId, _currentMovie.value?.userId ?: 0, busy)) return
        Log.i(TAG, "Live card: ${movie.login} went live")
        liveCardShownAt = now
        _liveCard.value = movie
        liveCardJob?.cancel()
        liveCardJob = viewModelScope.launch {
            delay(LiveCardGate.SHOW_SECONDS * 1000L)
            _liveCard.value = null
        }
    }

    /** OK on the live card: switch to that channel. */
    fun switchToLiveCard() {
        val movie = _liveCard.value ?: return
        dismissLiveCard()
        play(movie)
    }

    fun dismissLiveCard() {
        liveCardJob?.cancel()
        _liveCard.value = null
    }

    fun dismissRaid() {
        raidJob?.cancel()
        _pendingRaid.value = null
    }

    /**
     * A live stream ended or stays offline: offer the next followed live channel (favourites first)
     * and switch after a countdown. A raid offer wins.
     */
    private fun offerNextChannel() {
        _currentMovie.value?.takeIf { it.isClip }?.let { playNextClip(it); return }
        if (_currentMovie.value?.archiveOfLive == true) {
            exitArchive()
            return
        }
        val current = _currentMovie.value ?: return
        if (!isVod(current) && endJob?.isActive != true) {
            PlaybackStateHub.emit(HubEvent(HubEvent.STREAM_ENDED, mapOf("channel" to current.login)))
        }
        if (endOfStreamMode == "off") return
        if (isVod(current) || _pendingRaid.value != null || endJob?.isActive == true) return
        endJob = viewModelScope.launch {
            val next = try {
                com.shrug.shrugtv.ui.tv.home.HomeData.followedLive(appContext)
                    .filter { it.userId != current.userId }
                    .sortedByDescending { it.isFavorite }
                    .firstOrNull()
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                null
            } ?: return@launch
            if (_currentMovie.value !== current) return@launch
            _endSuggestion.value = next
            if (endOfStreamMode == "suggest") return@launch
            for (s in END_COUNTDOWN_SECONDS downTo 1) {
                _endCountdown.value = s
                delay(1_000L)
            }
            if (_endSuggestion.value === next && _currentMovie.value === current) playEndSuggestion()
        }
    }

    /** The next clip of the queue, ready in its own player: address known, first seconds already loaded. */
    private class Preloaded(val movie: Movie, val player: ExoPlayer)

    private var preloaded: Preloaded? = null
    private var preloadJob: Job? = null

    private fun releasePreloaded() {
        preloadJob?.cancel()
        preloadJob = null
        preloaded?.player?.release()
        preloaded = null
    }

    /** The clip after [current] in the queue, or null (setting off, end of the list). */
    private fun nextClipOf(current: Movie): Movie? {
        val s = com.shrug.shrugtv.clips.ClipsSettings.load(appContext)
        if (!s.autoplay) return null
        val skip = if (s.queueSkipsSeen) com.shrug.shrugtv.clips.ClipsSeen.all(appContext) else emptySet()
        return com.shrug.shrugtv.clips.ClipQueue.next(current, skip)
    }

    /**
     * A few seconds after a clip started (so it does not compete for the line): look up the address of the next clip of the
     * queue and let a second player load its first seconds paused, so the change is almost instant. The player is handed over
     * by [playNextClip]; whatever is not used is released.
     */
    private fun preloadNextClip(current: Movie) {
        releasePreloaded()
        preloadJob = viewModelScope.launch {
            delay(PRELOAD_DELAY_MS)
            if (_currentMovie.value !== current) return@launch
            val next = nextClipOf(current) ?: return@launch
            val qualities = kotlinx.coroutines.suspendCancellableCoroutine<List<StreamQuality>?> { cont ->
                val resolver = PlaybackUrlResolver(appContext, next.playbackId ?: next.clipSlug, Enums.PlaybackKind.VOD) { q, _ ->
                    if (cont.isActive) cont.resume(q) {}
                }
                resolver.useStreamToken = false
                resolver.start()
            }
            if (_currentMovie.value !== current || qualities.isNullOrEmpty()) return@launch
            val player = PlayerFactory.createMainPlayer(appContext)
            player.volume = 0f
            player.playWhenReady = false
            player.setMediaItem(MediaItem.fromUri(Uri.parse(masterQuality(qualities).url)))
            player.prepare()
            next.qualities = ArrayList(qualities)
            preloaded = Preloaded(next, player)
        }
    }

    /**
     * A clip ended: the next clip of the list it was opened from starts after a short pause, unless the setting is off. Clips
     * opened before are left out by default; every clip that starts counts as opened.
     */
    private fun playNextClip(current: Movie) {
        if (endJob?.isActive == true) return
        val next = nextClipOf(current) ?: return
        if (preloaded?.movie?.clipSlug == next.clipSlug) {
            startClip(next)
            return
        }
        Log.i(TAG, "Next clip not preloaded (${if (preloaded == null) "nothing ready" else "other clip"}), loading it now")
        endJob = viewModelScope.launch {
            delay(1_200L)
            if (_currentMovie.value !== current) return@launch
            startClip(next)
        }
    }

    /** Starts [clip] (counts as opened); from the preload if that holds it: in its own player, ready, no pause and no new lookup. */
    private fun startClip(clip: Movie) {
        com.shrug.shrugtv.clips.ClipsSeen.mark(appContext, clip.clipSlug)
        val ready = preloaded?.takeIf { it.movie.clipSlug == clip.clipSlug }
        if (ready == null) {
            play(clip)
            return
        }
        preloaded = null
        Log.i(TAG, "Next clip taken over from the preload, ${ready.player.bufferedPosition} ms already loaded")
        ready.player.volume = 1.0f
        ready.player.playWhenReady = true
        play(ready.movie, adoptedPlayer = ready.player)
    }

    /** The buttons "Previous clip" (-1) and "Next clip" (+1): the neighbour in the list the clip was opened from. */
    fun playClipStep(step: Int) {
        val current = _currentMovie.value?.takeIf { it.isClip } ?: return
        val target = com.shrug.shrugtv.clips.ClipQueue.neighbor(current, step) ?: return
        startClip(target)
    }

    /** OK on the "stream ended" card with a suggestion, or the countdown ran out. */
    fun playEndSuggestion() {
        val next = _endSuggestion.value ?: return
        cancelEndSuggestion()
        play(next)
    }

    /** Back on the card: stay; the card falls back to "try again". */
    fun cancelEndSuggestion() {
        endJob?.cancel()
        endJob = null
        _endSuggestion.value = null
        _endCountdown.value = 0
    }

    /** Behind the live edge (pause, stall): back to live. */
    override fun jumpToLive() {
        replayJob?.cancel()
        replayJob = null
        if (_currentMovie.value?.archiveOfLive == true) {
            exitArchive()
            return
        }
        mainPlayer.seekToDefaultPosition()
        mainPlayer.play()
    }

    // endregion

    private val _mainPlayer = MutableStateFlow<ExoPlayer>(PlayerFactory.createMainPlayer(appContext))
    val mainPlayerState: StateFlow<ExoPlayer> = _mainPlayer.asStateFlow()
    val mainPlayer: ExoPlayer get() = _mainPlayer.value

    var multiviewPlayer: ExoPlayer? = null
        private set

    private var mediaSession: PlayerMediaSession? = null
    private var zapper: StreamZapper? = null

    private val _currentMovie = MutableStateFlow<Movie?>(null)
    val currentMovie: StateFlow<Movie?> = _currentMovie.asStateFlow()

    private val _multiviewMovie = MutableStateFlow<Movie?>(null)
    val multiviewMovie: StateFlow<Movie?> = _multiviewMovie.asStateFlow()

    private val _isMultiview = MutableStateFlow(false)
    val isMultiview: StateFlow<Boolean> = _isMultiview.asStateFlow()

    private val _isPrimaryAudio = MutableStateFlow(true)
    val isPrimaryAudio: StateFlow<Boolean> = _isPrimaryAudio.asStateFlow()

    private val _playbackStatus = MutableStateFlow(PlaybackStatus.IDLE)
    val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

    private val _qualities = MutableStateFlow<List<StreamQuality>>(emptyList())
    val qualities: StateFlow<List<StreamQuality>> = _qualities.asStateFlow()

    private val _currentQuality = MutableStateFlow("Auto")
    val currentQuality: StateFlow<String> = _currentQuality.asStateFlow()

    /** Last picture quality before "Audio only", so one press returns to it. */
    private var lastVideoQuality = "Auto"

    /** A quality picked after a reload of the master playlist, applied once its video tracks are known. */
    private var pendingTrackQuality: Pair<Int, Int>? = null

    private val _chapters = MutableStateFlow<List<VodChapter>>(emptyList())
    val chapters: StateFlow<List<VodChapter>> = _chapters.asStateFlow()

    private val _currentSpeed = MutableStateFlow(1.0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    private val _seekHudText = MutableStateFlow<String?>(null)
    val seekHudText: StateFlow<String?> = _seekHudText.asStateFlow()
    private var seekHudJob: Job? = null

    private val _isFollowed = MutableStateFlow(false)
    val isFollowed: StateFlow<Boolean> = _isFollowed.asStateFlow()

    private val _activeOverlay = MutableStateFlow(PlayerOverlayType.NONE)
    val activeOverlay: StateFlow<PlayerOverlayType> = _activeOverlay.asStateFlow()

    private val _isChatVisible = MutableStateFlow(chatPrefs.joinChat && chatPrefs.showChat)
    val isChatVisible: StateFlow<Boolean> = _isChatVisible.asStateFlow()
    /** The clip on screen has no chat to show (its recording is gone, or recorded chat is off): the panel stays away. */
    /** The recording a clip was cut from is known and still there: the button "Recording" works. */
    private val _clipSourceReady = MutableStateFlow(false)
    val clipSourceReady: StateFlow<Boolean> = _clipSourceReady.asStateFlow()
    private val _chatUnavailable = MutableStateFlow(false)
    val chatUnavailable: StateFlow<Boolean> = _chatUnavailable.asStateFlow()

    private val _stats = MutableStateFlow(PlayerStatsState())
    val stats: StateFlow<PlayerStatsState> = _stats.asStateFlow()

    /** Watch statistics: counts the playing time, writes on [saveScope]. */
    private val watchTracker = com.shrug.shrugtv.stats.WatchSessionTracker(
        elapsedMs = { android.os.SystemClock.elapsedRealtime() },
        epochMs = { System.currentTimeMillis() },
        enabled = { playerPrefs.getBoolean("pref_watch_stats", true) },
        save = { s -> saveScope.launch { com.shrug.shrugtv.stats.WatchStatsStore.save(appContext, s) } }
    )

    private fun startWatchSession(movie: Movie) {
        watchTracker.start(
            channelId = movie.userId,
            login = movie.login ?: "",
            channelName = movie.displayName.ifBlank { movie.login ?: "" },
            category = movie.gameName,
            // The recording of a live stream counts as live
            isVod = (movie.playbackType == Enums.PlaybackKind.VOD || movie.videoId > 0L) && !movie.archiveOfLive,
            playingNow = mainPlayer.isPlaying,
            isClip = movie.isClip
        )
    }

    /**
     * Once a second: every stall goes to the log (tag PlaybackHealth, the measurement); after
     * 10 s of buffering or a frozen picture the stream is resolved and loaded again, at most three times per item.
     * Ad breaks, loading the recording and switching do not count. Setting pref_player_self_heal (default on).
     */
    private fun checkHealth() {
        val movie = _currentMovie.value ?: return
        val state = mainPlayer.playbackState
        val wants = mainPlayer.playWhenReady && _adBlockLabel.value == null && !archiveLoading && switchTarget == null
        val position = mainPlayer.currentPosition
        val heal = playerPrefs.getBoolean("pref_player_self_heal", true)
        when (val v = health.check(android.os.SystemClock.elapsedRealtime(), wants, state == Player.STATE_BUFFERING, state == Player.STATE_READY, position)) {
            is com.shrug.shrugtv.player.PlaybackHealth.Verdict.StallStarted ->
                Log.i(HEALTH_TAG, "stall started: ${v.kind} at ${position / 1000} s (${movie.login})")
            is com.shrug.shrugtv.player.PlaybackHealth.Verdict.StallEnded ->
                Log.i(HEALTH_TAG, "stall ended by itself after ${v.ms} ms: ${v.kind} (${movie.login})")
            is com.shrug.shrugtv.player.PlaybackHealth.Verdict.Reload -> {
                if (!heal) {
                    Log.i(HEALTH_TAG, "stall ${v.ms} ms: ${v.kind}, would reload (setting off)")
                    return
                }
                Log.w(HEALTH_TAG, "stall ${v.ms} ms: ${v.kind}, reloading (attempt ${v.attempt}) ${movie.login}")
                healthReload = true
                play(movie, restartChat = false, resumeAtMs = if (isVod(movie)) position else null)
            }
            is com.shrug.shrugtv.player.PlaybackHealth.Verdict.GiveUp -> {
                Log.w(HEALTH_TAG, "stall ${v.ms} ms: ${v.kind}, reloading did not help, giving up")
                if (heal) {
                    _playbackStatus.value = PlaybackStatus.ERROR
                    offerNextChannel()
                }
            }
            else -> Unit
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val shown = when (state) {
                Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
                Player.STATE_READY -> if (mainPlayer.playWhenReady) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
                Player.STATE_ENDED -> PlaybackStatus.ENDED
                else -> PlaybackStatus.IDLE
            }
            _playbackStatus.value = PlaybackStatusRules.afterStateChange(_playbackStatus.value, shown)
            // Only the recording itself counts: the live item is still READY until the new one is set
            if (state == Player.STATE_READY && switchTarget != null && loadedMovie === switchTarget) {
                switchTarget = null
                _archiveSwitch.value = 0
            }
            if (state == Player.STATE_READY && archiveLoading && loadedMovie?.archiveOfLive == true) {
                archiveLoading = false
                val wanted = archiveSeekTarget
                archiveSeekTarget = null
                // Presses while the recording loaded moved the target
                if (wanted != null && kotlin.math.abs(mainPlayer.currentPosition - wanted) > 2_000L) mainPlayer.seekTo(wanted)
            }
            if (state == Player.STATE_ENDED) offerNextChannel()
            if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                saveVodProgress()
            }
            if (state == Player.STATE_READY) errorRetries = 0
            updateStats()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            watchTracker.playing(isPlaying)
            _playbackStatus.value = PlaybackStatusRules.afterPlayingChange(_playbackStatus.value, isPlaying)
            if (!isPlaying) {
                saveVodProgress()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            CrashReporter.recordException(error)
            Log.w(TAG, "onPlayerError: ${error.errorCodeName} (retries so far: $errorRetries)")
            // Live stream paused or stalled too long: the position fell out of the playlist window.
            // Jump back to the live edge instead of showing an error (B8).
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                mainPlayer.seekToDefaultPosition()
                mainPlayer.prepare()
                return
            }
            // Network hiccup or expired token: resolve the stream again, twice, then give up
            val movie = _currentMovie.value
            if (movie != null && errorRetries < 2) {
                errorRetries++
                val resumeAt = if (isVod(movie)) mainPlayer.currentPosition else null
                _playbackStatus.value = PlaybackStatus.BUFFERING
                viewModelScope.launch {
                    delay(2_000L * errorRetries)
                    if (_currentMovie.value === movie) play(movie, restartChat = false, resumeAtMs = resumeAt)
                }
                return
            }
            Log.w(TAG, "Playback failed for good: showing the error")
            _playbackStatus.value = PlaybackStatus.ERROR
            offerNextChannel()
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateStats()
            pendingTrackQuality?.let { if (overrideVideoTrack(it)) pendingTrackQuality = null }
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_VIDEO && group.isSelected) {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i)) {
                            val f = group.getTrackFormat(i)
                            Log.i("PlayerViewModel", "Selected video track: ${f.width}x${f.height}@${f.frameRate.toInt()} (${f.bitrate / 1000} kbps)")
                        }
                    }
                }
            }
        }
    }

    init {
        // Checkpoint of the running watch session, so a pulled plug costs at most a minute
        viewModelScope.launch {
            while (true) {
                delay(com.shrug.shrugtv.stats.WatchSessionTracker.CHECKPOINT_MS)
                watchTracker.tick()
            }
        }
        mainPlayer.addListener(playerListener)
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                checkHealth()
            }
        }
        mediaSession = PlayerMediaSession(appContext, mainPlayer)
        // Called on the main thread; nobody registered it since the player became Compose (B13)
        TwitchAdBlocker.setListener { label ->
            val wasInBreak = _adBlockLabel.value != null
            _adBlockLabel.value = when (label) {
                null -> null
                TwitchAdBlocker.LABEL_BLOCKING -> R.string.player_ad_blocking
                else -> R.string.player_ad_stripping
            }
            if (!wasInBreak && label != null) PlaybackStateHub.emit(HubEvent(HubEvent.AD_BREAK_START, mapOf("channel" to _currentMovie.value?.login)))
            if (wasInBreak && label == null) PlaybackStateHub.emit(HubEvent(HubEvent.AD_BREAK_END, mapOf("channel" to _currentMovie.value?.login)))
        }

        // Home Assistant (MQTT) and the phone companion read the player through the hub
        PlaybackStateHub.commands = this
        PlaybackStateHub.chatLines = chatViewModel.lines
        PlaybackStateHub.badgeUrl = { set, version -> chatClient?.emoteManager?.badgeUrl(set, version) }
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            merge(
                _currentMovie.map { }, _playbackStatus.map { }, _currentQuality.map { }, _qualities.map { },
                _isChatVisible.map { }, _chatAppearance.map { }, _isMultiview.map { }, _isFollowed.map { },
                _streamAvatar.map { }, _volume.map { }
            ).debounce(300L).collect { publishSnapshot() }
        }
        viewModelScope.launch {
            // Positions and the live delay change without an event
            while (true) {
                delay(10_000L)
                publishSnapshot()
            }
        }
    }

    // region Hub: state for Home Assistant and the phone, commands from them

    /** A notice sent by Home Assistant or the phone (e.g. doorbell), shown as a card. */
    data class ExternalNotice(val title: String, val text: String?, val imageUrl: String?)

    private val _notice = MutableStateFlow<ExternalNotice?>(null)
    val notice: StateFlow<ExternalNotice?> = _notice.asStateFlow()
    private var noticeJob: Job? = null

    private val _closeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Home Assistant asked to close the player; the screen finishes the activity. */
    val closeRequests: SharedFlow<Unit> = _closeRequests.asSharedFlow()

    private fun publishSnapshot() {
        val m = _currentMovie.value
        if (m == null) {
            PlaybackStateHub.publish(PlaybackSnapshot.IDLE)
            return
        }
        val vod = isVod(m)
        val player = mainPlayer
        val offset = if (vod) androidx.media3.common.C.TIME_UNSET else player.currentLiveOffset
        PlaybackStateHub.publish(
            PlaybackSnapshot(
                status = _playbackStatus.value.name.lowercase(),
                kind = if (vod) "vod" else "live",
                channel = m.displayName.ifEmpty { m.login },
                login = m.login,
                userId = m.userId.takeIf { it > 0 }?.toString(),
                title = m.title,
                category = m.gameName.ifEmpty { null },
                viewers = m.viewerCount,
                startedAtMs = if (vod) null else m.startedAt?.toInstant()?.toEpochMilli(),
                language = StreamInfoText.language(m.languageCode),
                avatarUrl = _streamAvatar.value,
                thumbnailUrl = m.cardImageUrl,
                quality = _currentQuality.value,
                qualities = _qualities.value.mapNotNull { it.name },
                behindLiveSeconds = if (offset == androidx.media3.common.C.TIME_UNSET) 0 else (offset / 1000).toInt(),
                vodId = if (vod) m.videoId else null,
                vodPositionSeconds = if (vod) (player.currentPosition / 1000).toInt() else null,
                vodLengthSeconds = if (vod && player.duration > 0) (player.duration / 1000).toInt() else null,
                chatVisible = _isChatVisible.value,
                chatLayout = _chatAppearance.value.layout.value,
                multiview = _isMultiview.value,
                volume = _volume.value,
                audioOnly = isAudioOnly,
                followed = _isFollowed.value,
                favorite = m.isFavorite
            )
        )
    }

    override fun playPause() = togglePlayPause()
    override fun play() { mainPlayer.play() }
    override fun pause() { mainPlayer.pause() }
    override fun close() { _closeRequests.tryEmit(Unit) }

    override fun watchLogin(login: String) {
        val clean = login.trim().removePrefix("@").lowercase()
        if (clean.isEmpty() || clean.length > 25 || !clean.all { it.isLetterOrDigit() || it == '_' }) return
        play(Movie().apply {
            this.login = clean
            displayName = clean
            itemType = Enums.ContentType.STREAM
        })
    }

    override fun watchVod(videoId: Long, positionSeconds: Int?) {
        if (videoId <= 0) return
        val movie = Movie().apply {
            setVideoIdFromString(videoId.toString())
            itemType = Enums.ContentType.VOD
        }
        play(movie, resumeAtMs = positionSeconds?.takeIf { it > 0 }?.let { it * 1000L })
    }

    override fun selectQualityByName(name: String) {
        val q = _qualities.value.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return
        selectQuality(q)
    }

    override fun setChatVisible(visible: Boolean) {
        _isChatVisible.value = visible && chatPrefs.joinChat
    }

    override fun setChatLayout(value: String) {
        updateChatAppearance { it.copy(layout = com.shrug.shrugtv.chat.ChatLayout.of(value)) }
    }

    override fun setVolume(percent: Int) {
        val p = percent.coerceIn(0, 100)
        _volume.value = p
        mainPlayer.volume = p / 100f
    }

    override fun setAudioOnly(on: Boolean) {
        if (on != isAudioOnly) toggleAudioOnly()
    }

    override fun sendChat(text: String) = sendChatMessage(text)

    override fun showNotice(title: String, text: String?, imageUrl: String?, seconds: Int) {
        _notice.value = ExternalNotice(title.take(80), text?.take(200), imageUrl?.takeIf { it.startsWith("http") })
        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(seconds.coerceIn(2, 60) * 1000L)
            _notice.value = null
        }
    }

    fun dismissNotice() {
        noticeJob?.cancel()
        _notice.value = null
    }

    // endregion

    /**
     * Starts playback for a stream or VOD.
     * If an [adoptedPlayer] is handed over from the home screen, it is used on seamlessly.
     */
    override fun zap(direction: Int) {
        val movie = _currentMovie.value ?: return
        if (movie.playbackType == Enums.PlaybackKind.VOD || movie.videoId > 0L) return
        if (_isMultiview.value) return
        // The zapper needs the row the stream came from; search results, deep links and the
        // channel page have none (B2b)
        if (movie.source == null || movie.source == Enums.RowType.SEARCH_STREAM) {
            showHud(appContext.getString(R.string.player_zap_unavailable))
            return
        }
        if (zapper == null) {
            // The zapper resolves neighbours on a background thread; ExoPlayer may only be used on
            // the main thread (Media3 throws otherwise, B2)
            zapper = StreamZapper(appContext) { neighbour, _, _, _ ->
                if (neighbour != null) {
                    Threads.doOnMainThread { play(neighbour) }
                }
            }
        }
        zapper?.zap(movie, direction, false)
    }

    private fun isVod(movie: Movie): Boolean =
        movie.playbackType == Enums.PlaybackKind.VOD || movie.videoId > 0L

    /** "Default video quality" (live) or "Default VOD video quality": 0 Auto, 1-3 Auto max 1080/720/480p, 4 Source. */
    private fun qualityPreference(vod: Boolean): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        return AppSettings.getIntString(prefs, if (vod) "prefs_VOD_video_quality_v3" else "prefs_video_quality_v3", 0)
    }

    /**
     * Applies the quality setting to the track selection of [player], which plays the master
     * playlist: the Auto variants cap the video size and stay adaptive, Source forces the highest
     * variant (B6). Clears a manually chosen quality.
     */
    private fun applyQualityPreference(player: ExoPlayer, pref: Int) {
        val (w, h) = when (pref) {
            2 -> 1280 to 720
            3 -> 854 to 480
            else -> 1920 to 1080
        }
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setMaxVideoSize(w, h)
            .setForceHighestSupportedBitrate(true)
            .build()
    }

    /** The master playlist ("Auto"); clips have no master, their first entry is used. */
    private fun masterQuality(qualities: List<StreamQuality>): StreamQuality =
        qualities.firstOrNull { it.name == "Auto" } ?: qualities[0]

    fun play(movie: Movie, adoptedPlayer: ExoPlayer? = null, restartChat: Boolean = true, resumeAtMs: Long? = null) {
        val request = ++playRequest
        releasePreloaded()
        if (movie.isClip) preloadNextClip(movie)
        _clipTitle.value = if (movie.isClip) movie.title else null
        _clipNav.value = if (movie.isClip) com.shrug.shrugtv.clips.ClipQueue.position(movie) else null
        health.reset(newItem = !healthReload)
        // A reload by the stall watch must not end "Audio only" (someone may be asleep to it)
        val keepAudioOnly = healthReload && isAudioOnly
        healthReload = false
        pendingTrackQuality = null
        if (!movie.archiveOfLive) {
            liveOrigin = null
            archiveLoading = false
            archiveSeekTarget = null
        }
        if (switchTarget !== movie) {
            switchTarget = null
            _archiveSwitch.value = 0
        }
        cancelEndSuggestion()
        dismissRaid()
        dismissLiveCard()
        // Save the outgoing VOD before anything changes (B3)
        saveVodProgress()
        watchTracker.stop()
        if (restartChat || _currentMovie.value?.userId != movie.userId) startChat(movie)
        mainPlayer.volume = 0f // mute the old stream at once when switching
        _currentMovie.value = movie
        com.shrug.shrugtv.startup.StartResume.remember(appContext, movie)
        startWatchSession(movie)
        loadStreamAvatar(movie)
        checkFollowStatus(movie)
        if (movie.userId <= 0 && !movie.login.isNullOrEmpty()) {
            resolveUserId(movie, request)
        }

        val isVod = movie.playbackType == Enums.PlaybackKind.VOD || movie.videoId > 0L

        if (isVod) {
            loadChapters(movie)
            if (!movie.archiveOfLive) {
                archiveLookupJob?.cancel()
                _liveArchive.value = null
            }
        } else {
            _chapters.value = emptyList()
            lookupLiveArchive(movie)
        }

        if (adoptedPlayer != null) {
            val old = _mainPlayer.value
            old.removeListener(playerListener)
            old.release()
            adoptedPlayer.addListener(playerListener)
            // An adopted player may be playing already (a preloaded clip): no "started playing" event will come for the watch time
            watchTracker.playing(adoptedPlayer.isPlaying)
            adoptedPlayer.volume = 1.0f
            _mainPlayer.value = adoptedPlayer
            loadedMovie = movie
            mediaSession?.switchPlayer(adoptedPlayer)
            _playbackStatus.value = PlaybackStatus.PLAYING
            _isPlaying.value = adoptedPlayer.isPlaying
            // The preview plays the master playlist: apply the quality setting without reloading,
            // and offer the qualities (the menu stayed empty after a handover, B5)
            applyQualityPreference(adoptedPlayer, qualityPreference(isVod))
            _currentQuality.value = if (qualityPreference(isVod) == 4) "Source" else "Auto"
            if (movie.qualities.isNotEmpty()) {
                _qualities.value = movie.qualities
            } else {
                _qualities.value = emptyList()
                resolveQualitiesOnly(movie, request)
            }
            return
        }

        val playbackId = movie.playbackId ?: movie.login
        if (playbackId == null) {
            // Launcher deep link with only the channel id (B9): look up the login, then start
            if (movie.userId > 0 && !isVod) {
                com.shrug.shrugtv.api.helix.HelixApi.getUsersById(appContext, movie.userId.toString()) { _, json ->
                    val login = (json as? com.google.gson.JsonObject)?.getAsJsonArray("data")?.firstOrNull()
                        ?.asJsonObject?.get("login")?.asString
                    if (login != null && request == playRequest) {
                        movie.login = login
                        if (movie.displayName.isNullOrEmpty()) movie.displayName = login
                        play(movie, restartChat = true)
                    } else if (request == playRequest) {
                        _playbackStatus.value = PlaybackStatus.ERROR
                    }
                }
            } else {
                _playbackStatus.value = PlaybackStatus.ERROR
            }
            return
        }
        val playbackType = movie.playbackType ?: if (isVod) Enums.PlaybackKind.VOD else Enums.PlaybackKind.LIVE

        viewModelScope.launch {
            try {
                _playbackStatus.value = PlaybackStatus.BUFFERING
                // Resume position is read before the stream is set, then passed with the media item;
                // a separate seek used to hit the previous item (B3)
                val startPositionMs = resumeAtMs ?: if (isVod) savedVodPositionMs(movie) else 0L
                if (request != playRequest) return@launch
                val resolver = PlaybackUrlResolver(appContext, playbackId, playbackType) { resolvedQualities, _ ->
                    // Main thread. A newer play() (fast zapping) wins, older results are dropped (B4)
                    if (request == playRequest) {
                        if (!resolvedQualities.isNullOrEmpty()) {
                            _qualities.value = resolvedQualities
                            // Always the master playlist; the setting works through the track
                            // selection, so ABR stays on and qualities switch without reload (B6)
                            val pref = qualityPreference(isVod)
                            applyQualityPreference(mainPlayer, pref)
                            var masterUrl = masterQuality(resolvedQualities).url
                            _currentQuality.value = if (pref == 4) "Source" else "Auto"
                            if (keepAudioOnly) {
                                resolvedQualities.firstOrNull { QualityMatch.isAudioOnly(it.name) }?.let {
                                    masterUrl = it.url
                                    _currentQuality.value = it.name ?: "audio_only"
                                }
                            } else {
                                lastVideoQuality = "Auto"
                            }

                            val item = MediaItem.fromUri(Uri.parse(masterUrl))
                            if (startPositionMs > 0L) {
                                mainPlayer.setMediaItem(item, startPositionMs)
                            } else {
                                mainPlayer.setMediaItem(item)
                            }
                            loadedMovie = movie
                            mainPlayer.prepare()
                            mainPlayer.volume = 1.0f
                            mainPlayer.playWhenReady = true
                        } else {
                            // No playlist: the channel is offline (or gone)
                            _playbackStatus.value = PlaybackStatus.ERROR
                            offerNextChannel()
                        }
                    }
                }
                resolver.useStreamToken = false
                resolver.start()
            } catch (e: Exception) {
                _playbackStatus.value = PlaybackStatus.ERROR
                CrashReporter.recordException(e)
            }
        }
    }

    private fun loadChapters(movie: Movie) = loadChaptersFor(movie.videoId)

    private fun loadChaptersFor(videoId: Long) {
        if (videoId <= 0L) return

        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                try {
                    val json = GqlApi.getVideoChapters(appContext, videoId)
                    if (json != null) GqlParser.parseVideoChapters(json) else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            _chapters.value = if (PreferenceManager.getDefaultSharedPreferences(appContext).getBoolean("pref_vod_chapters", true)) {
                list.sortedBy { it.positionMs }
            } else emptyList()
        }
    }

    /** Saved resume position of a VOD in ms, 0 if none. */
    private suspend fun savedVodPositionMs(movie: Movie): Long {
        val videoId = movie.videoId
        if (videoId <= 0L) return 0L
        return withContext(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(appContext).vodInfoDao()
                (dao.findByBroadcastId(videoId)?.position ?: 0) * 1000L
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                0L
            }
        }
    }

    /**
     * Saves the position of the VOD that is actually loaded in the player ([loadedMovie], not
     * [currentMovie]: while a new stream resolves, the old one still plays; B3). Reads the player on
     * the calling (main) thread, writes on [saveScope], so it also works from [onCleared].
     */
    fun saveVodProgress() {
        val movie = loadedMovie ?: return
        if (movie.archiveOfLive) return
        val isVod = movie.playbackType == Enums.PlaybackKind.VOD || movie.videoId > 0L
        if (!isVod) return

        val videoId = movie.videoId
        if (videoId <= 0L) return

        val currentSec = (mainPlayer.currentPosition / 1000).toInt()
        val durationSec = (mainPlayer.duration / 1000).toInt().coerceAtLeast(0)
        if (currentSec <= 0) return

        saveScope.launch {
            try {
                val dao = AppDatabase.getInstance(appContext).vodInfoDao()
                var info = dao.findByBroadcastId(videoId)
                if (info == null) {
                    info = VodInfo().apply {
                        broadcastId = videoId
                        position = currentSec
                        vodLength = durationSec
                        lastAccess = TimeUtil.isoNowUtc()
                        channelName = movie.displayName
                        channelNameEnglish = movie.login
                        title = movie.title
                        categoryName = movie.gameName
                        videoThumbnail = movie.cardImageUrl
                        logoImage = movie.avatarUrl
                        channelId = movie.userId
                    }
                    dao.insert(info)
                } else {
                    info.position = currentSec
                    if (durationSec > 0) info.vodLength = durationSec
                    info.lastAccess = TimeUtil.isoNowUtc()
                    dao.update(info)
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
    }

    /**
     * Switches the quality through TrackSelectionOverride (no stream restart).
     */
    /**
     * Switches the quality. On the master playlist this is a track selection override, without
     * reloading the stream (B6); "Auto" returns to adaptive. Audio-only, clips and streams that were
     * started from a single variant still reload with the chosen URL.
     */
    fun selectQuality(quality: StreamQuality) {
        dismissOverlay()
        val url = quality.url ?: return
        Log.d(TAG, "selectQuality ${quality.name} -> $url")
        val playingUrl = mainPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
        val master = _qualities.value.firstOrNull { it.name == "Auto" }
        val onMaster = master != null && playingUrl == master.url

        if (onMaster && quality.name == "Auto") {
            _currentQuality.value = "Auto"
            val movie = _currentMovie.value
            val pref = if (movie != null) qualityPreference(isVod(movie)) else 0
            applyQualityPreference(mainPlayer, pref)
            return
        }
        val wanted = QualityMatch.parse(quality.name)
        if (!QualityMatch.isAudioOnly(quality.name)) lastVideoQuality = quality.name ?: "Auto"
        if (onMaster && wanted != null && overrideVideoTrack(wanted)) {
            _currentQuality.value = quality.name ?: ""
            return
        }
        // Fallback: load the chosen playlist
        pendingTrackQuality = null
        if (url == master?.url) {
            // Back on the master playlist (e.g. from "Audio only"): restore the adaptive setting or the chosen height
            if (quality.name == "Auto") {
                val movie = _currentMovie.value
                applyQualityPreference(mainPlayer, if (movie != null) qualityPreference(isVod(movie)) else 0)
            } else {
                pendingTrackQuality = wanted
            }
        }
        _currentQuality.value = quality.name ?: ""
        if (playingUrl != url) {
            val movie = _currentMovie.value
            val item = MediaItem.fromUri(Uri.parse(url))
            if (movie != null && isVod(movie)) {
                mainPlayer.setMediaItem(item, mainPlayer.currentPosition)
            } else {
                mainPlayer.setMediaItem(item)
            }
            mainPlayer.prepare()
            mainPlayer.playWhenReady = true
        }
    }

    /** "Audio only" is the current quality. */
    val isAudioOnly: Boolean get() = QualityMatch.isAudioOnly(_currentQuality.value)

    /**
     * One press between "Audio only" and the last picture quality. Without an audio-only variant
     * (clips) nothing happens and false is returned.
     */
    fun toggleAudioOnly(): Boolean {
        if (isAudioOnly) {
            val back = _qualities.value.firstOrNull { it.name == lastVideoQuality }
                ?: _qualities.value.firstOrNull { it.name == "Auto" } ?: return false
            selectQuality(back)
            return true
        }
        val audio = _qualities.value.firstOrNull { QualityMatch.isAudioOnly(it.name) } ?: return false
        selectQuality(audio)
        return true
    }

    /** Fixes the video track of the master playlist that matches [wanted] (height, fps). */
    private fun overrideVideoTrack(wanted: Pair<Int, Int>): Boolean {
        for (group in mainPlayer.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_VIDEO) continue
            val formats = (0 until group.length).map { i ->
                val f = group.getTrackFormat(i)
                f.height to f.frameRate
            }
            val index = QualityMatch.bestTrack(formats, wanted)
            if (index >= 0) {
                mainPlayer.trackSelectionParameters = mainPlayer.trackSelectionParameters.buildUpon()
                    .setForceHighestSupportedBitrate(false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
                    .build()
                return true
            }
        }
        return false
    }

    /** OK on the error card: start the stream again. */
    fun retry() {
        val movie = _currentMovie.value ?: return
        errorRetries = 0
        play(movie, restartChat = false, resumeAtMs = if (isVod(movie)) mainPlayer.currentPosition else null)
    }

    /** Qualities for the menu after a hero handover, without touching the playing stream. */
    private fun resolveQualitiesOnly(movie: Movie, request: Int) {
        val playbackId = movie.playbackId ?: movie.login ?: return
        val type = movie.playbackType ?: Enums.PlaybackKind.LIVE
        val resolver = PlaybackUrlResolver(appContext, playbackId, type) { resolved, _ ->
            if (request == playRequest && !resolved.isNullOrEmpty()) _qualities.value = resolved
        }
        resolver.useStreamToken = false
        resolver.start()
    }

    fun selectSpeed(speed: Float) {
        _currentSpeed.value = speed
        mainPlayer.playbackParameters = PlaybackParameters(speed)
        dismissOverlay()
    }

    fun selectChapter(chapter: VodChapter) {
        seekToTimeline(chapter.positionMs)
        dismissOverlay()
    }

    // region Multiview (Slice 4d)

    fun startMultiview(second: Movie) {
        _multiviewMovie.value = second
        _isMultiview.value = true

        if (multiviewPlayer == null) {
            multiviewPlayer = PlayerFactory.createPreviewPlayer(appContext)
        }
        multiviewPlayer?.volume = 0f // sekundärer Player startet stumm

        val playbackId = second.playbackId ?: second.login ?: return
        viewModelScope.launch {
            val resolver = PlaybackUrlResolver(appContext, playbackId, second.playbackType ?: Enums.PlaybackKind.LIVE) { resolvedQualities, _ ->
                if (!resolvedQualities.isNullOrEmpty()) {
                    multiviewPlayer?.setMediaItem(MediaItem.fromUri(Uri.parse(resolvedQualities[0].url)))
                    multiviewPlayer?.prepare()
                    multiviewPlayer?.playWhenReady = true
                }
            }
            resolver.useStreamToken = false
            resolver.start()
        }
    }

    fun stopMultiview() {
        _isMultiview.value = false
        _multiviewMovie.value = null
        multiviewPlayer?.release()
        multiviewPlayer = null
        mainPlayer.volume = 1f
        _isPrimaryAudio.value = true
    }

    fun swapMultiviewAudio() {
        val newPrimary = !_isPrimaryAudio.value
        _isPrimaryAudio.value = newPrimary
        if (newPrimary) {
            mainPlayer.volume = 1.0f
            multiviewPlayer?.volume = 0.0f
        } else {
            mainPlayer.volume = 0.0f
            multiviewPlayer?.volume = 1.0f
        }
    }

    // endregion

    private fun checkFollowStatus(movie: Movie) {
        val channelId = movie.userId.toString()
        FollowStatusChecker.check(appContext, channelId) { follows, _ ->
            _isFollowed.value = follows
        }
    }

    /**
     * Real values for the statistics overlay; called on player events and every second while the
     * overlay is open. Memory, latency and dropped frames were fixed sample values before.
     */
    fun updateStats() {
        val format = mainPlayer.videoFormat
        val res = if (format != null) "${format.width} × ${format.height} @ ${format.frameRate.toInt()} fps" else "–"
        val bitrate = if (format != null && format.bitrate > 0) format.bitrate / 1000 else 0
        val bufferSec = (mainPlayer.totalBufferedDuration / 1000f)
        val counters = mainPlayer.videoDecoderCounters
        val liveOffset = mainPlayer.currentLiveOffset
        val runtime = Runtime.getRuntime()
        val old = _stats.value
        EmoteFrameMeter.roll()
        _stats.value = old.copy(
            emoteFps = EmoteFrameMeter.framesPerSecond,
            emoteCount = EmoteFrameMeter.emotes,
            resolution = res,
            bitrateKbps = bitrate,
            bufferSeconds = bufferSec,
            droppedFrames = counters?.droppedBufferCount ?: 0,
            totalFrames = (counters?.renderedOutputBufferCount ?: 0) + (counters?.droppedBufferCount ?: 0),
            latencySeconds = if (liveOffset == C.TIME_UNSET) -1f else liveOffset / 1000f,
            chatDelaySeconds = chatDelay?.currentMs()?.let { it / 1000f } ?: -1f,
            memoryMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt(),
            bitrateHistory = (old.bitrateHistory + bitrate.toFloat()).takeLast(30),
            bufferHistory = (old.bufferHistory + bufferSec).takeLast(30)
        )
    }

    // region Overlay Steuerung

    fun setOverlay(overlay: PlayerOverlayType) {
        _activeOverlay.value = if (_activeOverlay.value == overlay) PlayerOverlayType.NONE else overlay
    }

    fun dismissOverlay() {
        _activeOverlay.value = PlayerOverlayType.NONE
    }

    /** Shows [overlay] without the toggle behaviour of [setOverlay]. */
    fun showOverlay(overlay: PlayerOverlayType) {
        _activeOverlay.value = overlay
    }

    /**
     * Back from an overlay: menus opened from the control bar return to the bar (B34), the bar and
     * the channel switcher return to the video.
     */
    fun backFromOverlay() {
        if (_activeOverlay.value == PlayerOverlayType.LAYOUT_EDITOR && chatSizesChanged) {
            // Emotes and badges are drawn at their size when a message arrives: render the chat anew
            chatSizesChanged = false
            _currentMovie.value?.let { movie ->
                stopVodChat()
                startChat(movie)
            }
        }
        _activeOverlay.value = when (_activeOverlay.value) {
            PlayerOverlayType.QUALITY, PlayerOverlayType.SPEED, PlayerOverlayType.CHAPTERS,
            PlayerOverlayType.STATS, PlayerOverlayType.SEARCH, PlayerOverlayType.SEND_MESSAGE,
            PlayerOverlayType.LAYOUT_EDITOR -> PlayerOverlayType.CONTROLS
            else -> PlayerOverlayType.NONE
        }
    }

    /** Short message in the centre of the picture (same place as the seek HUD). */
    private fun showHud(text: String) {
        _seekHudText.value = text
        seekHudJob?.cancel()
        seekHudJob = viewModelScope.launch {
            delay(2000)
            _seekHudText.value = null
        }
    }

    fun toggleChat() {
        _isChatVisible.value = !_isChatVisible.value
    }

    fun togglePlayPause() {
        if (mainPlayer.isPlaying) {
            mainPlayer.pause()
            saveVodProgress()
        } else {
            mainPlayer.play()
        }
    }

    // region Rewinding a live stream into its recording

    private val _liveArchive = MutableStateFlow<com.shrug.shrugtv.player.LiveArchiveInfo?>(null)

    /** The recording of the live stream that is playing, once found; null without one or when switched off. */
    val liveArchive: StateFlow<com.shrug.shrugtv.player.LiveArchiveInfo?> = _liveArchive.asStateFlow()
    private var archiveLookupJob: Job? = null

    /** The live stream while its recording is played. */
    private var liveOrigin: Movie? = null
    private var archiveLoading = false

    /** Position the recording should start at; moves while keys are pressed during loading. */
    private var archiveSeekTarget: Long? = null
    private var archiveWatcher: Job? = null

    /** Which switch is loading: 0 none, 1 into the recording, 2 back to live. */
    private val _archiveSwitch = MutableStateFlow(0)
    val archiveSwitch: StateFlow<Int> = _archiveSwitch.asStateFlow()
    private var switchTarget: Movie? = null

    /** Looks for the recording of the live stream: once now, then every minute for five minutes. */
    private fun lookupLiveArchive(movie: Movie) {
        archiveLookupJob?.cancel()
        if (!playerPrefs.getBoolean("pref_live_rewind", true)) {
            _liveArchive.value = null
            return
        }
        // Zapping to the same channel (or back from the recording) keeps what was found
        val keep = _liveArchive.value?.takeIf { movie.userId > 0 && it.userId == movie.userId }
        if (keep == null) _liveArchive.value = null else loadChaptersFor(keep.videoId)
        archiveLookupJob = viewModelScope.launch {
            repeat(6) { i ->
                if (i > 0) delay(60_000L)
                val found = withContext(Dispatchers.IO) { findLiveArchive(movie) }
                if (found != null) {
                    Log.i(TAG, "Recording of the live stream found: video ${found.videoId}")
                    _liveArchive.value = found
                    loadChaptersFor(found.videoId)
                    return@launch
                }
            }
        }
    }

    private fun findLiveArchive(movie: Movie): com.shrug.shrugtv.player.LiveArchiveInfo? = try {
        val query = if (movie.userId > 0) "user_id=${movie.userId}" else "user_login=${movie.login}"
        val stream = com.shrug.shrugtv.api.helix.HelixApi.getStreamsByQuery(appContext, query)
            ?.getAsJsonArray("data")?.firstOrNull()?.asJsonObject
        val userId = stream?.get("user_id")?.asString?.toIntOrNull()
        if (stream == null || userId == null) null else {
            val streamId = stream.get("id")?.asString ?: ""
            val startMs = stream.get("started_at")?.asString?.let { java.time.Instant.parse(it).toEpochMilli() }
            val videos = com.shrug.shrugtv.api.helix.HelixApi.getRecentArchives(appContext, userId.toString())
                ?.getAsJsonArray("data")?.mapNotNull { e ->
                    val o = e.asJsonObject
                    val id = o.get("id")?.asString?.toLongOrNull() ?: return@mapNotNull null
                    com.shrug.shrugtv.player.LiveRewind.Candidate(
                        id, o.get("stream_id")?.takeIf { !it.isJsonNull }?.asString ?: "",
                        o.get("created_at")?.asString?.let { java.time.Instant.parse(it).toEpochMilli() }
                    )
                } ?: emptyList()
            val match = com.shrug.shrugtv.player.LiveRewind.match(streamId, startMs, videos)
            val start = startMs ?: match?.createdAtMs
            if (match == null || start == null) null else com.shrug.shrugtv.player.LiveArchiveInfo(userId, streamId, match.videoId, start)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Looking for the recording of the stream failed: ${e.message}")
        null
    }

    /** True if left/right on the timeline rewinds: a live stream with a recording, or that recording itself. */
    private fun rewindActive(): Boolean {
        val movie = _currentMovie.value ?: return false
        return movie.archiveOfLive || (!isVod(movie) && _liveArchive.value != null)
    }

    /**
     * Position and length of the timeline in ms: of the recording while it plays, since the start of
     * the stream while live (the live position is "now" minus the live offset), else of the video.
     */
    fun timelineMs(): LongArray {
        val movie = _currentMovie.value
        val info = _liveArchive.value
        if (movie != null && !isVod(movie) && info != null) {
            return com.shrug.shrugtv.player.LiveRewind.liveTimeline(System.currentTimeMillis(), info.streamStartMs, mainPlayer.currentLiveOffset)
        }
        if (movie?.archiveOfLive == true && archiveLoading && info != null) {
            val duration = com.shrug.shrugtv.player.LiveRewind.liveTimeline(System.currentTimeMillis(), info.streamStartMs, 0L)[1]
            return longArrayOf(archiveSeekTarget ?: 0L, duration)
        }
        return longArrayOf(mainPlayer.currentPosition, mainPlayer.duration.coerceAtLeast(0L))
    }

    /** Seeks by [deltaMs] on the timeline; returns where it ends up, null if it went back to live or nothing happened. */
    private fun rewindBy(deltaMs: Long): Long? {
        val movie = _currentMovie.value ?: return null
        val timeline = timelineMs()
        if (movie.archiveOfLive) {
            return when (val plan = com.shrug.shrugtv.player.LiveRewind.planInArchive(deltaMs, timeline[0], timeline[1])) {
                is com.shrug.shrugtv.player.LiveRewind.Plan.InArchive -> {
                    if (archiveLoading) archiveSeekTarget = plan.targetMs else mainPlayer.seekTo(plan.targetMs)
                    plan.targetMs
                }
                com.shrug.shrugtv.player.LiveRewind.Plan.BackToLive -> { exitArchive(); null }
                else -> null
            }
        }
        // currentPosition of a live window can be negative or stale: the position inside the window is its
        // length minus the distance to the live edge (measured 2026-11-04: window 32 s, position -14 s)
        val windowLength = mainPlayer.duration
        val offset = mainPlayer.currentLiveOffset
        val windowPosition = if (windowLength > 0 && offset != C.TIME_UNSET) (windowLength - offset).coerceIn(0L, windowLength) else 0L
        Log.i(TAG, "rewind: delta=${deltaMs / 1000} s position=${timeline[0] / 1000} duration=${timeline[1] / 1000} window=${windowPosition / 1000} of ${windowLength / 1000}")
        return when (val plan = com.shrug.shrugtv.player.LiveRewind.planFromLive(deltaMs, timeline[0], timeline[1], windowPosition)) {
            is com.shrug.shrugtv.player.LiveRewind.Plan.InWindow -> {
                mainPlayer.seekTo((windowPosition + plan.deltaMs).coerceAtLeast(0L))
                (timeline[0] + plan.deltaMs).coerceAtLeast(0L)
            }
            is com.shrug.shrugtv.player.LiveRewind.Plan.IntoArchive -> { enterArchive(plan.targetMs); plan.targetMs }
            else -> null
        }
    }

    /** Seeks to [targetMs] on the timeline (a category of the recording). */
    private fun seekToTimeline(targetMs: Long) {
        if (rewindActive()) rewindBy(targetMs - timelineMs()[0]) else mainPlayer.seekTo(targetMs)
    }

    /** Live stream to its recording at [targetMs]; the recording plays like any other one, with its chat and categories. */
    private fun enterArchive(targetMs: Long) {
        val info = _liveArchive.value ?: return
        val live = _currentMovie.value ?: return
        Log.i(TAG, "enterArchive: video ${info.videoId} at ${targetMs / 1000} s")
        val archive = Movie().apply {
            itemType = Enums.ContentType.VOD
            source = Enums.RowType.VOD_CHANNEL_ARCHIVED
            setVideoIdFromString(info.videoId.toString())
            userId = live.userId
            login = live.login
            displayName = live.displayName
            gameName = live.gameName
            title = live.title
            avatarUrl = live.avatarUrl
            archiveOfLive = true
            setStartedAtDateTime(live.startedAt)
        }
        switchTarget = archive
        play(archive, restartChat = true, resumeAtMs = targetMs)
        _archiveSwitch.value = 1
        // play() clears the state of a live stream: set after it, for the recording
        liveOrigin = live
        archiveLoading = true
        archiveSeekTarget = targetMs
        archiveWatcher?.cancel()
        archiveWatcher = viewModelScope.launch {
            while (_currentMovie.value?.archiveOfLive == true) {
                delay(1_000L)
                if (!archiveLoading && loadedMovie?.archiveOfLive == true && com.shrug.shrugtv.player.LiveRewind.shouldLeaveArchive(mainPlayer.currentPosition, mainPlayer.duration)) {
                    exitArchive()
                    return@launch
                }
            }
        }
    }

    /** Back from the recording to the live stream. */
    fun exitArchive() {
        val live = liveOrigin ?: return
        Log.i(TAG, "exitArchive: back to live")
        archiveWatcher?.cancel()
        showHud(appContext.getString(R.string.player_back_to_live_hud))
        switchTarget = live
        play(live, restartChat = true)
        _archiveSwitch.value = 2
    }

    // endregion

    private val seekStepper = com.shrug.shrugtv.player.SeekStepper { android.os.SystemClock.uptimeMillis() }

    // region Instant replay

    private var replayJob: Job? = null

    /** The "Replay" button shows in live streams while the setting is on. */
    val replayEnabled: Boolean get() = playerPrefs.getBoolean(com.shrug.shrugtv.player.InstantReplay.PREF_ENABLED, true)

    /**
     * Jumps back by the replay length (inside the live window, or into the recording like rewinding)
     * and returns to the live edge once that part has played again. Live, zapping or seeking by hand end it.
     */
    override fun replay() {
        val movie = _currentMovie.value ?: return
        if (isVod(movie) || movie.archiveOfLive) return
        val lengthMs = com.shrug.shrugtv.player.InstantReplay.lengthMs(playerPrefs.getString(com.shrug.shrugtv.player.InstantReplay.PREF_SECONDS, "30"))
        replayJob?.cancel()
        if (rewindActive()) {
            rewindBy(-lengthMs)
        } else {
            // No recording: as far back as the live window reaches
            val windowLength = mainPlayer.duration
            val offset = mainPlayer.currentLiveOffset
            val windowPosition = if (windowLength > 0 && offset != C.TIME_UNSET) (windowLength - offset).coerceIn(0L, windowLength) else 0L
            mainPlayer.seekTo((windowPosition - lengthMs).coerceAtLeast(0L))
        }
        Log.i(TAG, "replay: ${lengthMs / 1000} s back (${movie.login})")
        showHud(appContext.getString(R.string.player_replay_hud, (lengthMs / 1000).toInt()))
        val login = movie.login
        val r = com.shrug.shrugtv.player.InstantReplay(lengthMs)
        replayJob = viewModelScope.launch {
            while (true) {
                delay(250L)
                // Zapped to another channel: the replay is over without a jump
                if (_currentMovie.value?.login != login) return@launch
                if (r.tick(250L, mainPlayer.isPlaying && !archiveLoading)) break
            }
            replayJob = null
            Log.i(TAG, "replay: done, back to live")
            val inArchive = _currentMovie.value?.archiveOfLive == true
            jumpToLive()
            // exitArchive shows "Live again" itself
            if (!inArchive) showHud(appContext.getString(R.string.player_back_to_live_hud))
        }
    }

    // endregion

    /** Left/right on a recording: the step grows while the key is pressed again and again. */
    fun seekStep(direction: Int) {
        replayJob?.cancel()
        replayJob = null
        val step = seekStepper.press(direction)
        if (rewindActive()) {
            val target = rewindBy(step.stepMs) ?: return
            showSeekHud(com.shrug.shrugtv.player.SeekStepper.hudText(step.totalMs, target))
            return
        }
        val duration = mainPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val target = (mainPlayer.currentPosition + step.stepMs).coerceIn(0L, duration)
        mainPlayer.seekTo(target)
        showSeekHud(com.shrug.shrugtv.player.SeekStepper.hudText(step.totalMs, target))
    }

    /** Next (+1) or previous (-1) category of a recording. */
    fun jumpChapter(direction: Int) {
        val list = _chapters.value
        val pos = timelineMs()[0]
        val chapter = (if (direction > 0) com.shrug.shrugtv.player.VodChapters.next(list, pos)
            else com.shrug.shrugtv.player.VodChapters.previous(list, pos)) ?: return
        seekToTimeline(chapter.positionMs)
        showSeekHud("\u25B6 " + chapter.name)
    }

    private fun showSeekHud(text: String) {
        _seekHudText.value = text
        seekHudJob?.cancel()
        seekHudJob = viewModelScope.launch {
            delay(1200)
            _seekHudText.value = null
        }
    }

    fun seekRelative(deltaMs: Long) {
        replayJob?.cancel()
        replayJob = null
        val target = (mainPlayer.currentPosition + deltaMs).coerceAtLeast(0L)
        mainPlayer.seekTo(target)

        val sec = deltaMs / 1000
        val sign = if (sec > 0) "+$sec" else "$sec"
        _seekHudText.value = "$sign s"

        seekHudJob?.cancel()
        seekHudJob = viewModelScope.launch {
            delay(1200)
            _seekHudText.value = null
        }
    }

    // endregion

    /** "Reload emotes" in the More menu (B14). */
    fun reloadEmotes() {
        chatClient?.emotesReload(null)
    }

    /** Sends a chat message via Helix; the input in the player dropped the text before (C2.8). */
    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) chatClient?.sendMessage(trimmed)
    }

    /** True while playback is paused because the activity went to the background. */
    private var pausedForBackground = false
    private var backgroundSince = 0L

    /** Activity stopped (Home button): pause instead of playing on unseen (B1). */
    fun pauseForBackground() {
        Log.i(TAG, "pauseForBackground (playWhenReady=${mainPlayer.playWhenReady})")
        saveVodProgress()
        // The process may be killed while in the background: write what has been counted so far
        watchTracker.tick()
        if (mainPlayer.playWhenReady) {
            pausedForBackground = true
            backgroundSince = android.os.SystemClock.elapsedRealtime()
            mainPlayer.pause()
            multiviewPlayer?.pause()
        }
    }

    /** Activity visible again: continue; live streams jump back to the live edge. */
    fun resumeFromBackground() {
        if (!pausedForBackground) return
        pausedForBackground = false
        val movie = _currentMovie.value
        val isVod = movie != null && (movie.playbackType == Enums.PlaybackKind.VOD || movie.videoId > 0L)
        val awayMs = android.os.SystemClock.elapsedRealtime() - backgroundSince
        if (!isVod && movie != null && awayMs >= STALE_AFTER_BACKGROUND_MS) {
            // The playlist and the access token are stale after a while: jumping to the live edge of the old
            // session left a frozen picture now and then (Michael, 2026-11-09). Resolve the stream again.
            Log.i(HEALTH_TAG, "back after ${awayMs / 1000} s, reloading the live stream (${movie.login})")
            healthReload = true
            play(movie, restartChat = false)
            return
        }
        if (!isVod) {
            mainPlayer.seekToDefaultPosition()
            multiviewPlayer?.seekToDefaultPosition()
        }
        mainPlayer.play()
        multiviewPlayer?.play()
    }

    override fun onCleared() {
        PlaybackStateHub.clear(this)
        // Save first (reads the player), then release everything this player owns (B1)
        saveVodProgress()
        watchTracker.stop()
        TwitchAdBlocker.setListener(null)
        chatClient?.stopChat()
        chatClient = null
        stopVodChat()
        releasePreloaded()
        mainPlayer.removeListener(playerListener)
        mainPlayer.release()
        multiviewPlayer?.release()
        multiviewPlayer = null
        mediaSession?.release()
        mediaSession = null
        super.onCleared()
    }

    /** Chat clients deliver on their own threads; their events are moved to the main thread with this. */
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // ---- Chat of recordings ------------------------------------------------
    // The client loads comment pages by offset on its own thread; the messages arrive at once, so the
    // pacer holds them and hands them out as the playback position passes their offset.
    private var vodChatClient: com.shrug.shrugtv.chat.VodChatClient? = null
    private var vodChatJob: Job? = null
    private var vodChatVideoId = 0L
    private val vodPacer = com.shrug.shrugtv.chat.VodChatPacer { android.os.SystemClock.uptimeMillis() }

    /** The recording whose chat goes along: the video itself, or the one a clip was cut from. */
    private fun chatVideoOf(movie: Movie): Long = if (movie.isClip) movie.clipVideoId else movie.videoId

    /** Second of the recording at which the player position 0 lies (a clip starts somewhere in the middle of it). */
    private var vodChatBase = 0.0

    /** A clip has no chat of its own: look up its recording and play that chat from where the clip starts. */
    private fun startClipChat(movie: Movie) {
        viewModelScope.launch(Dispatchers.IO) {
            val info = com.shrug.shrugtv.chat.ClipSource.fetch(appContext, movie.clipSlug)
            withContext(Dispatchers.Main) {
                if (_currentMovie.value !== movie) return@withContext
                // The button to the recording needs the source even when the chat is off
                _clipSourceReady.value = info != null
                if (!chatPrefs.joinChat || !chatPrefs.vodChat || info == null) { _chatUnavailable.value = true }
                if (info == null) return@withContext
                movie.clipVideoId = info.videoId
                movie.clipOffsetSeconds = info.offsetSeconds
                if (movie.userId <= 0 && info.broadcasterId > 0) movie.userId = info.broadcasterId
                if (movie.login.isNullOrEmpty()) info.login?.let { movie.login = it }
                if (movie.displayName.isNullOrEmpty()) info.displayName?.let { movie.displayName = it }
                if (movie.title.isNullOrEmpty()) info.title?.takeIf { it.isNotEmpty() }?.let { movie.title = it; _clipTitle.value = it }
                // A clip opened by link started its watch session without a channel: fill it in
                watchTracker.updateChannel(movie.userId, movie.login ?: "", movie.displayName.ifBlank { movie.login ?: "" })
                startVodChat(movie)
            }
        }
    }

    /** The button "Recording" on a clip: the recording it was cut from, at the second where the clip starts. */
    fun jumpToClipSource() {
        val clip = _currentMovie.value?.takeIf { it.isClip && it.clipVideoId > 0L } ?: return
        val recording = Movie().apply {
            setVideoIdFromString(clip.clipVideoId.toString())
            itemType = Enums.ContentType.VOD
            userId = clip.userId
            login = clip.login
            displayName = clip.displayName
        }
        // Leaving the clips: the queue does not carry on in the recording
        com.shrug.shrugtv.clips.ClipQueue.clear()
        play(recording, resumeAtMs = clip.clipOffsetSeconds * 1000L)
    }

    private fun startVodChat(movie: Movie) {
        if (!chatPrefs.joinChat || !chatPrefs.vodChat || chatVideoOf(movie) <= 0L || vodChatClient != null) return
        if (movie.userId <= 0) {
            // Opened by link (shrugtv://vod/<id>, Home Assistant): only the video id is known
            resolveVodOwner(movie)
            return
        }
        // The client holds the sink strongly; events come in on its threads and are moved to the main thread
        val sink = com.shrug.shrugtv.chat.ChatUiSink { event ->
            if (event is com.shrug.shrugtv.chat.ChatUiEvent.Message) mainHandler.post { vodPacer.onMessage(event.message) }
        }
        vodChatVideoId = chatVideoOf(movie)
        vodChatBase = if (movie.isClip) movie.clipOffsetSeconds.toDouble() else 0.0
        vodChatClient =
            com.shrug.shrugtv.chat.ChatClient.Builder()
                .context(appContext)
                .uiSink(sink)
                .channelId(movie.userId)
                .channelName(movie.login ?: movie.displayName ?: "")
                .vodId(chatVideoOf(movie))
                .badgeSize(chatBadgePx())
                .emoteSize(chatEmotePx())
                .animatedEmotes(chatPrefs.animatedEmotes)
                .filterSettings(chatPrefs.filterSettings)
                .messageDelayMs(0)
                .showPrimeBadge(chatPrefs.showPrimeBadge)
                .highlights(chatPrefs.highlights)
                .extras(chatPrefs.extras)
                .buildVodChat()
        vodChatJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                vodChatTick()
            }
        }
    }

    private fun resolveVodOwner(movie: Movie) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = com.shrug.shrugtv.api.helix.HelixApi.getVideoById(appContext, movie.videoId)
                val obj = json?.getAsJsonArray("data")?.takeIf { it.size() > 0 }?.get(0)?.asJsonObject ?: return@launch
                val ownerId = obj.get("user_id")?.asString?.toIntOrNull() ?: return@launch
                withContext(Dispatchers.Main) {
                    if (_currentMovie.value === movie && movie.userId <= 0) {
                        movie.userId = ownerId
                        if (movie.login.isNullOrEmpty()) obj.get("user_login")?.asString?.let { movie.login = it }
                        if (movie.displayName.isNullOrEmpty()) obj.get("user_name")?.asString?.let { movie.displayName = it }
                        startVodChat(movie)
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
    }

    private fun stopVodChat() {
        vodChatJob?.cancel()
        vodChatJob = null
        vodChatClient?.stopChat()
        vodChatClient = null
        vodChatVideoId = 0L
        vodChatBase = 0.0
        vodPacer.clear()
    }

    /** Once a second: follows seeks, releases what is due, fetches ahead. */
    private fun vodChatTick() {
        if (!_isChatVisible.value) return
        val step = vodPacer.tick(vodChatBase + mainPlayer.currentPosition / 1000.0)
        if (step.reset) chatViewModel.clear()
        step.due.forEach { chatViewModel.addMessage(it) }
        step.fetchOffset?.let { offset ->
            vodChatClient?.requestVodMessages(offset)
        }
    }

    private fun startChat(movie: Movie) {
        // play() restarts the chat for the same recording after a quality or handover change: keep it running
        // The same recording but another zero point (a clip, then its recording) needs a fresh start: the offsets differ
        if (isVod(movie) && vodChatClient != null && vodChatVideoId == chatVideoOf(movie) &&
            vodChatBase == (if (movie.isClip) movie.clipOffsetSeconds.toDouble() else 0.0)) return
        chatClient?.stopChat()
        chatClient = null
        stopVodChat()
        chatViewModel.clear()
        chatGeneration++
        chatDelay = null
        _chatUnavailable.value = false
        _clipSourceReady.value = false
        if (movie.isClip) {
            if (movie.clipVideoId > 0L) {
                _clipSourceReady.value = true
                if (!chatPrefs.joinChat || !chatPrefs.vodChat) _chatUnavailable.value = true else startVodChat(movie)
            } else {
                startClipChat(movie)
            }
            return
        }
        if (isVod(movie)) {
            startVodChat(movie)
            return
        }
        if (!chatPrefs.joinChat || movie.userId <= 0) return
        // Typed events from the client; it holds the sink strongly (the old weakly held Handler was once
        // garbage collected and the chat stayed empty)
        // Delay towards the picture: computed on the main thread, where the player
        // may be read; events of a previous channel that are still pending are dropped
        val delay = com.shrug.shrugtv.chat.ChatDelay(chatPrefs.autoDelay, chatPrefs.messageDelayMs.toLong()) {
            com.shrug.shrugtv.chat.ChatDelay.sane(mainPlayer.currentLiveOffset)
        }
        val generation = ++chatGeneration
        chatDelay = delay
        val sink = com.shrug.shrugtv.chat.ChatUiSink { event ->
            mainHandler.post {
                val historical = event is com.shrug.shrugtv.chat.ChatUiEvent.Message && event.message.isHistorical
                val ms = if (historical) 0L else delay.delayFor(android.os.SystemClock.uptimeMillis())
                if (ms <= 0L) {
                    deliverChatEvent(event)
                } else {
                    mainHandler.postDelayed({ if (generation == chatGeneration) deliverChatEvent(event) }, ms)
                }
            }
        }
        // The client starts itself on its own thread (ChatClient.init); calling startChat() here
        // as well ran it twice, once blocking the main thread (C0.2)
        chatClient =
            com.shrug.shrugtv.chat.ChatClient.Builder()
                .context(appContext)
                .uiSink(sink)
                .channelId(movie.userId)
                .channelName(movie.login ?: movie.displayName ?: "")
                .badgeSize(chatBadgePx())
                .emoteSize(chatEmotePx())
                .animatedEmotes(chatPrefs.animatedEmotes)
                .filterSettings(chatPrefs.filterSettings)
                // The delay is applied above, for messages and moderation events alike
                .messageDelayMs(0)
                .showPrimeBadge(chatPrefs.showPrimeBadge)
                .highlights(chatPrefs.highlights)
                .extras(chatPrefs.extras)
                .buildLiveChat()
    }

    /** Bumped on every chat start, so delayed events of the previous channel are dropped. */
    private var chatGeneration = 0

    /** Delay of the running live chat. */
    var chatDelay: com.shrug.shrugtv.chat.ChatDelay? = null
        private set

    private fun deliverChatEvent(event: com.shrug.shrugtv.chat.ChatUiEvent) {
        when (event) {
            is com.shrug.shrugtv.chat.ChatUiEvent.Message -> chatViewModel.addMessage(event.message)
            is com.shrug.shrugtv.chat.ChatUiEvent.Raid -> onRaid(event.raid)
            is com.shrug.shrugtv.chat.ChatUiEvent.Delete -> chatViewModel.deleteMessage(event.messageId)
            is com.shrug.shrugtv.chat.ChatUiEvent.ClearUser -> chatViewModel.deleteUserMessages(event.userLogin)
            com.shrug.shrugtv.chat.ChatUiEvent.ClearAll -> chatViewModel.deleteAll()
            is com.shrug.shrugtv.chat.ChatUiEvent.RoomSettings -> chatViewModel.setRoomSettings(event.settings)
        }
    }
}
