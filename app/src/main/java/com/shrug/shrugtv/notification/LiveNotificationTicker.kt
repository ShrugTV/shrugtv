package com.shrug.shrugtv.notification

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.google.gson.JsonObject
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground-only ticker for live stream notifications.
 *
 * Runs only while ShrugTV is actively in the foreground (via ProcessLifecycleOwner).
 * Refreshes followed streams every 60 seconds (shared with the Home screen) and
 * shows an in-app notice when followed channels go live. Stops automatically when
 * the app is backgrounded.
 */
class LiveNotificationTicker(
    private val appContext: Context
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "LiveNotificationTicker"
        private const val POLL_INTERVAL_MS = 60_000L

        @Volatile
        private var instance: LiveNotificationTicker? = null

        @JvmStatic
        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = LiveNotificationTicker(context.applicationContext)
                    }
                }
            }
        }

        @JvmStatic
        fun get(): LiveNotificationTicker? = instance

        /** Followed channels that just went live, for the card in the open player. */
        val playerLive = kotlinx.coroutines.flow.MutableSharedFlow<Movie>(extraBufferCapacity = 8)
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null
    private var isFirstPoll = true
    private val knownLiveChannelIds = mutableSetOf<Int>()

    override fun onStart(owner: LifecycleOwner) {
        startTicker()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopTicker()
    }

    @Synchronized
    fun startTicker() {
        if (tickerJob?.isActive == true) return

        tickerJob = scope.launch {
            Log.d(TAG, "Live notification ticker started in foreground")
            while (isActive) {
                try {
                    pollFollowedStreams()
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking live streams: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    @Synchronized
    fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        Log.d(TAG, "Live notification ticker stopped (background or disabled)")
    }

    private suspend fun pollFollowedStreams() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        val notificationsEnabled = prefs.getBoolean("pref_notification_on", false)
        // The card in the player is its own setting, on by default
        val playerCard = prefs.getBoolean("pref_notification_player_card", true)
        if ((!notificationsEnabled && !playerCard) || AppState.userLoggedOut) {
            return
        }

        val favoritesOnly = prefs.getBoolean("pref_notification_foreground_favorite_only", false)
        val type = prefs.getString("prefs_notification_type", "0")?.toIntOrNull() ?: 0
        val fontSize = prefs.getString("prefs_notification_font_size", "20")?.toFloatOrNull() ?: 20.0f
        val position = prefs.getString("prefs_notification_position", "1")?.toIntOrNull() ?: 1
        val duration = prefs.getString("pref_notification_duration", "3000")?.toLongOrNull() ?: 3000L

        val settings = NotificationSettings(fontSize, position, duration)
        // Quiet hours: nothing pops up, the known-live bookkeeping below still runs
        val quiet = QuietHours.isQuiet(
            prefs.getBoolean(QuietHours.PREF_ON, false),
            prefs.getString(QuietHours.PREF_FROM, "22")?.toIntOrNull() ?: 22,
            prefs.getString(QuietHours.PREF_TO, "7")?.toIntOrNull() ?: 7,
            java.time.LocalTime.now().hour)

        withContext(Dispatchers.IO) {
            HelixApi.getFollowedStreams(appContext) { exc, obj ->
                if (exc != null) {
                    Log.w(TAG, "Failed to load followed streams for notifications: ${exc.message}")
                    return@getFollowedStreams
                }

                val json = obj as? JsonObject ?: return@getFollowedStreams
                val data = json.getAsJsonArray("data") ?: return@getFollowedStreams
                val currentStreams: List<Movie> = HelixParser.parseStreams(appContext, Enums.RowType.FOLLOWED, data)

                val currentLiveIds = currentStreams.map { it.userId }.toSet()

                if (!isFirstPoll) {
                    val newlyLive = currentStreams.filter { movie ->
                        !knownLiveChannelIds.contains(movie.userId)
                    }

                    if (newlyLive.isNotEmpty()) {
                        Handler(Looper.getMainLooper()).post {
                            for (movie in newlyLive) {
                                // For Home Assistant automations, independent of the toast settings
                                com.shrug.shrugtv.hub.PlaybackStateHub.emit(com.shrug.shrugtv.hub.HubEvent(
                                    com.shrug.shrugtv.hub.HubEvent.FOLLOWED_LIVE,
                                    mapOf("channel" to movie.displayName, "login" to movie.login,
                                        "category" to movie.gameName, "title" to movie.title, "favorite" to movie.isFavorite)
                                ))
                                if (favoritesOnly && !movie.isFavorite) {
                                    continue
                                }
                                if (quiet) continue
                                if (playerCard && com.shrug.shrugtv.hub.PlaybackStateHub.commands != null) {
                                    playerLive.tryEmit(movie)
                                    continue
                                }
                                if (notificationsEnabled) showLiveNotification(movie, settings, type)
                            }
                        }
                    }
                }

                knownLiveChannelIds.clear()
                knownLiveChannelIds.addAll(currentLiveIds)
                isFirstPoll = false
            }
        }
    }

    private fun showLiveNotification(movie: Movie, settings: NotificationSettings, type: Int) {
        try {
            if (type == 1) {
                ToastNotification(appContext, movie, settings)
            } else {
                val name = movie.displayName ?: movie.login ?: ""
                ToastNotification(appContext, name, settings, ToastNotification.LAYOUT_LIVE_TEXT)
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }
}
