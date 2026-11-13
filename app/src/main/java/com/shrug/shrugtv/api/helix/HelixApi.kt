package com.shrug.shrugtv.api.helix

import android.content.Context
import com.google.gson.JsonObject
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.net.Http
import com.shrug.shrugtv.net.ResultCallback
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Twitch Helix REST API client (replaces decompiled HelixApi.java).
 * Centralizes Bearer token and Client-ID headers and removes the obsolete Kraken Accept header.
 */
object HelixApi {

    private const val CONTENT_TYPE_JSON = "application/json"
    private const val TIMEOUT_MS = 3000

    private fun helixRequest(method: String, url: String): Http.HttpRequest =
        Http.load(method, url)
            .setHeader("Authorization", "Bearer ${AppState.accessToken}")
            .setHeader("Client-ID", BuildConfig.TWITCH_CLIENT_ID)
            .setTimeout(TIMEOUT_MS)

    private fun helixGet(url: String): Http.HttpRequest = helixRequest("GET", url)

    private fun helixGetSync(url: String): JsonObject? = try {
        helixGet(url).asJsonObject().get()
    } catch (e: Exception) {
        CrashReporter.recordException(e)
        null
    }

    @JvmStatic
    fun afterParam(cursor: String): String =
        if (cursor.isEmpty()) "" else "&after=$cursor"

    @JvmStatic
    fun first100Param(): String = "&first=100"

    @JvmStatic
    fun first50Param(): String = "&first=50"

    @JvmStatic
    fun deleteEventSubSubscription(context: Context?, subscriptionId: String) {
        helixRequest("DELETE", "https://api.twitch.tv/helix/eventsub/subscriptions?id=$subscriptionId")
            .asJsonObject()
            .setCallback(null)
    }

    @JvmStatic
    fun searchCategories(context: Context?, query: String): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/search/categories?query=$query&first=5")

    @JvmStatic
    fun searchChannels(context: Context?, query: String, liveOnly: Boolean): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/search/channels?query=$query&first=5&live_only=$liveOnly")

    @JvmStatic
    fun createEventSubSubscription(context: Context?, body: String, callback: ResultCallback<JsonObject>) {
        helixRequest("POST", "https://api.twitch.tv/helix/eventsub/subscriptions")
            .setHeader("Content-Type", CONTENT_TYPE_JSON)
            .setStringBody(body)
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getCurrentUser(context: Context?, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/users").asJsonObject().setCallback(callback)
    }

    @JvmStatic
    fun getChannelChatBadges(context: Context?, broadcasterId: Int): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/chat/badges?broadcaster_id=$broadcasterId")

    /** Global badge sets (moderator, vip, founder, bits, …); channel sets override them. */
    @JvmStatic
    fun getGlobalChatBadges(context: Context?): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/chat/badges/global")

    /** Room state of a channel: slow, follower-only, subscriber-only, emote-only mode. */
    @JvmStatic
    fun getChatSettings(context: Context?, broadcasterId: Int, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/chat/settings?broadcaster_id=$broadcasterId")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getEventSubSubscriptions(context: Context?): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/eventsub/subscriptions")

    @JvmStatic
    fun getFollowRelation(context: Context?, broadcasterId: String, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/channels/followed?user_id=${AppState.userId}&broadcaster_id=$broadcasterId")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getFollowedStreams(context: Context?, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/streams/followed?user_id=${AppState.userId}${first100Param()}")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getFollowedChannelsPage(context: Context?, cursor: String): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/channels/followed?first=100&user_id=${AppState.userId}${afterParam(cursor)}")

    @JvmStatic
    fun getStreamsByGameAndLanguage(context: Context?, gameId: Long, cursor: String, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/streams?${first100Param()}&game_id=$gameId&language=${AppState.streamLanguageCode}${afterParam(cursor)}")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getStreamsByGameAndLanguageSync(context: Context?, gameId: Long, cursor: String): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/streams?${first100Param()}&game_id=$gameId&language=${AppState.streamLanguageCode}${afterParam(cursor)}")

    @JvmStatic
    fun getStreamsByGame(context: Context?, gameId: Long, cursor: String, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/streams?${first100Param()}&game_id=$gameId${afterParam(cursor)}")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getStreamsByLanguage(context: Context?, cursor: String, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/streams?${first100Param()}&language=${AppState.streamLanguageCode}${afterParam(cursor)}")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getStreamsByQueryNoAccept(context: Context?, query: String): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/streams?$query")

    @JvmStatic
    fun getRecentArchives(context: Context?, userId: String): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/videos?user_id=$userId&first=5&type=archive")

    @JvmStatic
    fun getStreamByUserId(context: Context?, userId: Long, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/streams?user_id=$userId")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getStreamsByQuery(context: Context?, query: String): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/streams?$query")

    @JvmStatic
    fun getTopStreams(context: Context?, cursor: String, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/streams?${first100Param()}${afterParam(cursor)}")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getUsersById(context: Context?, userId: String, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/users?id=$userId")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getUsersByQuery(context: Context?, query: String): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/users$query")

    @JvmStatic
    fun getVideos(context: Context?, userId: String, cursor: String, type: String, callback: ResultCallback<JsonObject>) {
        helixGet("https://api.twitch.tv/helix/videos?user_id=$userId${first50Param()}&type=$type${afterParam(cursor)}")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getVideoById(context: Context?, videoId: Long): JsonObject? =
        helixGetSync("https://api.twitch.tv/helix/videos?id=$videoId")

    @JvmStatic
    fun getClipsByBroadcaster(context: Context?, broadcasterId: String, cursor: String, callback: ResultCallback<JsonObject>) {
        val startedAt = Instant.now().minus(Duration.ofDays(7)).truncatedTo(ChronoUnit.SECONDS).toString()
        helixGet("https://api.twitch.tv/helix/clips?broadcaster_id=$broadcasterId${first50Param()}&started_at=$startedAt${afterParam(cursor)}")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun getClipsByGame(context: Context?, gameId: Long, cursor: String, callback: ResultCallback<JsonObject>) {
        val startedAt = Instant.now().minus(Duration.ofDays(7)).truncatedTo(ChronoUnit.SECONDS).toString()
        helixGet("https://api.twitch.tv/helix/clips?game_id=$gameId${first50Param()}&started_at=$startedAt${afterParam(cursor)}")
            .asJsonObject()
            .setCallback(callback)
    }

    @JvmStatic
    fun sendChatMessage(context: Context?, broadcasterId: String, senderId: String, text: String, replyParentId: String? = null, callback: ResultCallback<JsonObject>) {
        val body = JsonObject().apply {
            addProperty("broadcaster_id", broadcasterId)
            addProperty("sender_id", senderId)
            addProperty("message", text)
            if (replyParentId != null) addProperty("reply_parent_message_id", replyParentId)
        }
        helixRequest("POST", "https://api.twitch.tv/helix/chat/messages")
            .setHeader("Content-Type", CONTENT_TYPE_JSON)
            .setStringBody(body.toString())
            .asJsonObject()
            .setCallback(callback)
    }
}
