package com.shrug.shrugtv.apihelpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.google.gson.JsonArray
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.telemetry.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Live stream history channel ID list and live status lookup (replaces decompiled
 * LiveStreamHistory.java).
 */
class LiveStreamHistory {

    companion object {
        const val PREF_KEY = "pref_live_stream_history_channel_ids"
        const val MAX_ENTRIES = 100

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val main = Handler(Looper.getMainLooper())

        private fun splitIds(raw: String): List<String> =
            if (raw.isEmpty()) emptyList() else raw.split(",").filter { it.isNotEmpty() }

        private fun updateIdList(raw: String, channelId: String, add: Boolean): String {
            if (raw.isEmpty() && add) return channelId
            if (raw.isEmpty() && !add) return ""
            val list = splitIds(raw).toMutableList()
            list.remove(channelId)
            if (add) {
                list.add(0, channelId)
            }
            while (list.size > MAX_ENTRIES) {
                list.removeAt(list.size - 1)
            }
            return list.joinToString(",")
        }

        private fun loadIds(context: Context): String =
            PreferenceManager.getDefaultSharedPreferences(context)?.getString(PREF_KEY, "") ?: ""

        private fun saveIds(context: Context, ids: String) {
            PreferenceManager.getDefaultSharedPreferences(context)
                ?.edit()
                ?.putString(PREF_KEY, ids)
                ?.apply()
        }

        fun addToHistory(context: Context, userId: Int) {
            scope.launch {
                try {
                    saveIds(context, updateIdList(loadIds(context), userId.toString(), add = true))
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                }
            }
        }

        fun removeFromHistory(context: Context, userId: Int) {
            scope.launch {
                try {
                    saveIds(context, updateIdList(loadIds(context), userId.toString(), add = false))
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                }
            }
        }

        fun clearLiveStreamHistory(context: Context) {
            scope.launch {
                try {
                    saveIds(context, "")
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                }
            }
        }

        fun toUserIdQuery(input: String): String =
            splitIds(input).joinToString(separator = "&user_id=", prefix = "user_id=")

        fun getLiveChannels(context: Context, callback: LiveStreamHistoryCallback) {
            scope.launch {
                try {
                    val rawIds = loadIds(context)
                    if (rawIds.isEmpty()) {
                        postList(callback, ArrayList())
                        return@launch
                    }
                    val response = HelixApi.getStreamsByQueryNoAccept(context, toUserIdQuery(rawIds))
                    if (response != null && response.get("error") == null) {
                        val data = response.get("data") as? JsonArray
                        if (data == null) {
                            postList(callback, ArrayList())
                            return@launch
                        }
                        val streams = HelixParser.parseStreams(context, Enums.RowType.LIVE_CHANNEL_HISTORY, data)
                        ChannelAvatarLoader.loadAvatars(context, streams)
                        if (streams.isNotEmpty()) {
                            postList(callback, sortByHistoryOrder(rawIds, streams))
                        } else {
                            postList(callback, ArrayList())
                        }
                    } else {
                        postList(callback, ArrayList())
                    }
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                    postList(callback, ArrayList())
                }
            }
        }

        private fun sortByHistoryOrder(rawIds: String, streams: ArrayList<Movie>): ArrayList<Movie> {
            return try {
                val byUserId = streams.associateBy { it.userId }
                val ordered = ArrayList<Movie>(streams.size)
                for (idStr in splitIds(rawIds)) {
                    val id = idStr.toIntOrNull() ?: continue
                    val movie = byUserId[id] ?: continue
                    ordered.add(movie)
                }
                ordered
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                streams
            }
        }

        private fun postList(callback: LiveStreamHistoryCallback, list: ArrayList<Movie>) {
            main.post {
                try {
                    callback.onList(list)
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                }
            }
        }
    }
}
