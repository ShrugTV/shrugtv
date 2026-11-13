package com.shrug.shrugtv.ui.tv.home

import android.content.Context
import com.google.gson.JsonObject
import com.shrug.shrugtv.MainApp
import com.shrug.shrugtv.apihelpers.FollowedChannels
import com.shrug.shrugtv.apihelpers.FollowedGames
import com.shrug.shrugtv.apihelpers.LiveStreamHistory
import com.shrug.shrugtv.apihelpers.PastBroadcast
import com.shrug.shrugtv.apihelpers.TopGames
import com.shrug.shrugtv.database.AppDatabase
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.session.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Data sources of the new home screen. Thin wrappers around the existing data layer (HelixApi,
 * FollowedChannels, TopGames) so the Compose UI can work with coroutines; none of it is reimplemented
 * (step 3).
 */
sealed class Login {
    object LoggedIn : Login()
    object LoggedOut : Login()
    data class Error(val text: String) : Login()
}

object HomeData {

    /** Loads the profile (AppState.userId etc.); without it the follow queries stay empty. */
    suspend fun loadProfile(context: Context): Login {
        if (AppState.accessToken.isNullOrEmpty()) return Login.LoggedOut
        return suspendCancellableCoroutine { cont ->
            UserSession.loadUserProfile(context, object : UserSession.ProfileCallback {
                override fun onError(str: String) { if (cont.isActive) cont.resume(Login.Error(str)) }
                override fun onUnauthorized() { if (cont.isActive) cont.resume(Login.LoggedOut) }
                override fun onEmptyResponse() {
                    if (cont.isActive) cont.resume(if (AppState.userProfileLoaded) Login.LoggedIn else Login.LoggedOut)
                }
                override fun onRequestFailed() { if (cont.isActive) cont.resume(Login.Error(context.getString(com.shrug.shrugtv.R.string.home_twitch_unreachable))) }
            })
        }
    }

    private suspend fun helix(call: (com.shrug.shrugtv.net.ResultCallback<JsonObject>) -> Unit): JsonObject? =
        suspendCancellableCoroutine { cont ->
            call(com.shrug.shrugtv.net.ResultCallback { e, json -> if (cont.isActive) cont.resume(if (e == null) json else null) })
        }

    /**
     * Parse in the background: parseStreams loads the channel avatars; on the main thread that happens
     * deferred since , and Compose does not see avatars set later (circles stayed
     * empty, 1.5.0). Here they load blocking, before the row appears.
     */
    private suspend fun streamsFrom(context: Context, json: JsonObject?, source: Enums.RowType): List<Movie> =
        withContext(Dispatchers.IO) {
            val data = json?.get("data")
            if (data == null || !data.isJsonArray) emptyList()
            else HelixParser.parseStreams(context, source, data.asJsonArray).sortedByDescending { it.viewerCount }
        }

    suspend fun followedLive(context: Context): List<Movie> =
        streamsFrom(context, helix { HelixApi.getFollowedStreams(context, it) }, Enums.RowType.FOLLOWED)

    suspend fun topStreams(context: Context): List<Movie> =
        streamsFrom(context, helix { HelixApi.getTopStreams(context, "", it) }, Enums.RowType.TOP_STREAMS)

    /** Live streams of one category, optionally only in the app language (channel switcher). */
    suspend fun gameStreams(context: Context, gameId: Long, byLanguage: Boolean): List<Movie> =
        if (byLanguage) streamsFrom(context, helix { HelixApi.getStreamsByGameAndLanguage(context, gameId, "", it) }, Enums.RowType.GAME_LANGUAGE)
        else streamsFrom(context, helix { HelixApi.getStreamsByGame(context, gameId, "", it) }, Enums.RowType.GAME)

    /** Category id of a live channel by its login; 0 if it is offline or unknown (a channel opened by link has none yet). */
    suspend fun gameIdOfLogin(context: Context, login: String): Long = withContext(Dispatchers.IO) {
        try {
            val data = HelixApi.getStreamsByQuery(context, "user_login=" + java.net.URLEncoder.encode(login, "UTF-8"))?.get("data")
            if (data == null || !data.isJsonArray) 0L
            else HelixParser.parseStreams(context, Enums.RowType.GAME, data.asJsonArray).firstOrNull()?.gameId?.takeIf { it > 0 } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /** Top live streams in the app language. */
    suspend fun languageStreams(context: Context): List<Movie> =
        streamsFrom(context, helix { HelixApi.getStreamsByLanguage(context, "", it) }, Enums.RowType.LANGUAGE)

    suspend fun followedChannels(context: Context): List<Movie> = suspendCancellableCoroutine { cont ->
        FollowedChannels.getList(context) { list -> if (cont.isActive) cont.resume(list?.toList() ?: emptyList()) }
    }

    suspend fun topGames(context: Context): List<Movie> = suspendCancellableCoroutine { cont ->
        TopGames.getList(context, "") { list, _ -> if (cont.isActive) cont.resume(list ?: emptyList()) }
    }

    suspend fun followedGames(context: Context): List<Movie> = suspendCancellableCoroutine { cont ->
        FollowedGames.getList(context) { list -> if (cont.isActive) cont.resume(list ?: emptyList()) }
    }

    suspend fun liveHistory(context: Context): List<Movie> = suspendCancellableCoroutine { cont ->
        LiveStreamHistory.getLiveChannels(context) { list -> if (cont.isActive) cont.resume(list ?: emptyList()) }
    }

    suspend fun pastBroadcasts(context: Context): List<Movie> = suspendCancellableCoroutine { cont ->
        PastBroadcast.getList(context, { list -> if (cont.isActive) cont.resume(list?.toList() ?: emptyList()) }, emptyList())
    }

    /** Continue watching: VOD history from Room, newest first (sorted in SQL, re-sorted defensively). */
    suspend fun continueWatching(context: Context): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val history = AppDatabase.getInstance(context)?.vodInfoDao()?.getHistory().orEmpty()
            if (history.isEmpty()) emptyList()
            else UserSession.vodHistoryToItems(history.sortedWith(MainApp::compareByLastAccessDesc))
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Playback URL of a live stream for the preview (auto playlist, goes through the ad blocker). */
    suspend fun previewUrl(context: Context, m: Movie): String? {
        // Keep URL and qualities on the Movie: the player uses them if they are
        // younger than 15 min (getPlaybackUrlIfFresh) and saves the token request on opening (Fire TV:
        // about 0.5 s). Only with the same token kind as in the player (developer setting).
        m.playbackUrlIfFresh?.let { return it }
        val likeInPlayer = !androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("pref_dev_use_stream_token", false)
        return suspendCancellableCoroutine { cont ->
            val r = com.shrug.shrugtv.player.PlaybackUrlResolver(context, m.playbackId, m.playbackType) { qualities, cluster ->
                if (likeInPlayer && !qualities.isNullOrEmpty()) {
                    m.manifestCluster = cluster
                    m.qualities = qualities
                    m.setPlaybackUrl(qualities[0].url)
                }
                if (cont.isActive) cont.resume(qualities?.firstOrNull()?.url)
            }
            r.setUseStreamToken(false)
            r.start()
        }
    }
}
