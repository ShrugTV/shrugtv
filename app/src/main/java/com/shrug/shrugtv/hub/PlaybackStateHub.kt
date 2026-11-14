package com.shrug.shrugtv.hub

import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What ShrugTV is playing, for everything outside the player: Home Assistant via MQTT and the phone
 * companion (docs/mqtt_ha_plan.md, docs/companion_plan.md). The player writes, the integrations
 * read. Lives as long as the process, independent of the player activity.
 */
data class PlaybackSnapshot(
    /** idle | buffering | playing | paused | ended | error */
    val status: String = "idle",
    /** live | vod | none */
    val kind: String = "none",
    val channel: String? = null,
    val login: String? = null,
    val userId: String? = null,
    val title: String? = null,
    val category: String? = null,
    val viewers: Int = 0,
    /** Stream start (epoch ms) for the uptime; null for VODs or unknown. */
    val startedAtMs: Long? = null,
    val language: String? = null,
    val avatarUrl: String? = null,
    val thumbnailUrl: String? = null,
    val quality: String? = null,
    val qualities: List<String> = emptyList(),
    val behindLiveSeconds: Int = 0,
    val vodId: Long? = null,
    val vodPositionSeconds: Int? = null,
    val vodLengthSeconds: Int? = null,
    val chatVisible: Boolean = false,
    val chatLayout: String? = null,
    val multiview: Boolean = false,
    /** Stream volume 0-100 (not the TV volume). */
    val volume: Int = 100,
    /** "Audio only" is the current quality (black screen, sound only). */
    val audioOnly: Boolean = false,
    val followed: Boolean = false,
    val favorite: Boolean = false
) {
    val isActive: Boolean get() = kind != "none"

    /** Minutes since the stream started at [nowMs]; null without a start time. */
    fun uptimeMinutes(nowMs: Long): Long? = startedAtMs?.let { ((nowMs - it) / 60_000L).coerceAtLeast(0) }

    /** The JSON published to MQTT (`state` topic) and sent to phones. */
    fun toJson(nowMs: Long, appVersion: String): JsonObject = JsonObject().apply {
        addProperty("status", status)
        addProperty("kind", kind)
        addProperty("channel", channel)
        addProperty("login", login)
        addProperty("user_id", userId)
        addProperty("title", title)
        addProperty("category", category)
        addProperty("viewers", viewers)
        addProperty("uptime_min", uptimeMinutes(nowMs))
        addProperty("language", language)
        addProperty("avatar_url", avatarUrl)
        addProperty("thumbnail_url", thumbnailUrl)
        addProperty("quality", quality)
        add("qualities", com.google.gson.JsonArray().apply { qualities.forEach { add(it) } })
        addProperty("behind_live_s", behindLiveSeconds)
        addProperty("vod_id", vodId)
        addProperty("vod_position_s", vodPositionSeconds)
        addProperty("vod_length_s", vodLengthSeconds)
        addProperty("chat_visible", chatVisible)
        addProperty("chat_layout", chatLayout)
        addProperty("multiview", multiview)
        addProperty("volume", volume)
        addProperty("audio_only", audioOnly)
        addProperty("followed", followed)
        addProperty("favorite", favorite)
        addProperty("app_version", appVersion)
    }

    companion object {
        @JvmField
        val IDLE = PlaybackSnapshot()
    }
}

/** Something that happened, for automations and the phone (raid, stream ended, live, ad break). */
data class HubEvent(val type: String, val data: Map<String, Any?> = emptyMap()) {
    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("event_type", type)
        for ((k, v) in data) {
            when (v) {
                null -> add(k, com.google.gson.JsonNull.INSTANCE)
                is Number -> addProperty(k, v)
                is Boolean -> addProperty(k, v)
                else -> addProperty(k, v.toString())
            }
        }
    }

    companion object {
        const val RAID = "raid"
        const val STREAM_ENDED = "stream_ended"
        const val FOLLOWED_LIVE = "followed_live"
        const val AD_BREAK_START = "ad_break_start"
        const val AD_BREAK_END = "ad_break_end"

        @JvmField
        val TYPES = listOf(RAID, STREAM_ENDED, FOLLOWED_LIVE, AD_BREAK_START, AD_BREAK_END)
    }
}

/**
 * Actions on the open player. Implemented by PlayerViewModel and registered in [PlaybackStateHub]
 * while the player exists. Always called on the main thread.
 */
interface PlayerCommands {
    fun playPause()
    fun play()
    fun pause()
    fun jumpToLive()
    fun zap(direction: Int)
    fun close()
    fun watchLogin(login: String)
    fun watchVod(videoId: Long, positionSeconds: Int?)
    fun selectQualityByName(name: String)
    fun setChatVisible(visible: Boolean)
    fun setChatLayout(value: String)
    fun setVolume(percent: Int)
    /** Instant replay: jumps back by the replay length of the settings (live streams only). */
    fun replay()
    fun setAudioOnly(on: Boolean)
    fun sendChat(text: String)
    fun showNotice(title: String, text: String?, imageUrl: String?, seconds: Int)
}

object PlaybackStateHub {
    private val _state = MutableStateFlow(PlaybackSnapshot.IDLE)
    val state: StateFlow<PlaybackSnapshot> = _state.asStateFlow()

    private val _events = MutableSharedFlow<HubEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<HubEvent> = _events.asSharedFlow()

    /** The open player, or null. */
    @Volatile
    var commands: PlayerCommands? = null

    /** Chat lines of the open player for the phone (the player's ChatViewModel flow), or null. */
    @Volatile
    var chatLines: StateFlow<List<com.shrug.shrugtv.ui.tv.player.ChatLine>>? = null

    /** Badge image URL by set and version in the open player's channel, or null; set by the player for the phone chat. */
    @Volatile
    var badgeUrl: ((String, String) -> String?)? = null

    fun publish(snapshot: PlaybackSnapshot) {
        _state.value = snapshot
    }

    fun emit(event: HubEvent) {
        _events.tryEmit(event)
    }

    /** The player went away. */
    fun clear(owner: PlayerCommands) {
        if (commands === owner) {
            commands = null
            chatLines = null
            badgeUrl = null
            _state.value = PlaybackSnapshot.IDLE
        }
    }
}
