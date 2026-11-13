package com.shrug.shrugtv.apihelpers

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import java.net.URLEncoder

/**
 * Helix search for channels, categories, and live streams (replaces decompiled Search.java,
 * ).
 */
class Search {

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val main = Handler(Looper.getMainLooper())

        private fun returnList(callback: SearchCallback, list: List<Movie>) {
            main.post {
                try {
                    callback.onList(list)
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                }
            }
        }

        private fun collectUserIds(jsonArray: JsonArray): String {
            val ids = ArrayList<String>(jsonArray.size())
            try {
                for (elem in jsonArray) {
                    val id = elem.asJsonObject.get("id")?.asString
                    if (!id.isNullOrEmpty()) ids.add(id)
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
            return ids.joinToString("&user_id=")
        }

        fun getChannels(context: Context, searchText: String, callback: SearchCallback) {
            scope.launch {
                try {
                    val response = HelixApi.searchChannels(context, URLEncoder.encode(searchText, "utf-8"), false)
                    val data = response?.get("data")?.takeIf { !it.isJsonNull }?.asJsonArray
                    if (data == null || data.size() == 0) {
                        returnList(callback, emptyList())
                        return@launch
                    }
                    val channels = HelixParser.parseChannelSearchResults(data, Enums.RowType.SEARCH_CHANNEL)
                    returnList(callback, channels)
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                    returnList(callback, emptyList())
                }
            }
        }

        fun getGames(context: Context, searchText: String, callback: SearchCallback) {
            scope.launch {
                try {
                    val response = HelixApi.searchCategories(context, URLEncoder.encode(searchText, "utf-8"))
                    val data = response?.get("data")?.takeIf { !it.isJsonNull }?.asJsonArray
                    if (data == null || data.size() == 0) {
                        returnList(callback, emptyList())
                        return@launch
                    }
                    val games = HelixParser.parseCategories(data, Enums.RowType.SEARCH_GAME)
                    returnList(callback, games)
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                    returnList(callback, emptyList())
                }
            }
        }

        fun getLiveStreams(context: Context, searchText: String, callback: SearchCallback) {
            scope.launch {
                try {
                    val response = HelixApi.searchChannels(context, URLEncoder.encode(searchText, "utf-8"), true)
                    val channelData = response?.get("data")?.takeIf { !it.isJsonNull }?.asJsonArray
                    if (channelData == null || channelData.size() == 0) {
                        returnList(callback, emptyList())
                        return@launch
                    }
                    val userIds = collectUserIds(channelData)
                    if (userIds.isEmpty()) {
                        returnList(callback, emptyList())
                        return@launch
                    }
                    val streamsResponse = HelixApi.getStreamsByQuery(context, "user_id=$userIds")
                    val streamsData = streamsResponse?.get("data")?.takeIf { !it.isJsonNull }?.asJsonArray
                    if (streamsData == null || streamsData.size() == 0) {
                        returnList(callback, emptyList())
                        return@launch
                    }
                    val streams = HelixParser.parseStreams(context, Enums.RowType.SEARCH_STREAM, streamsData)
                    returnList(callback, streams)
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                    returnList(callback, emptyList())
                }
            }
        }
    }
}
