package com.shrug.shrugtv.apihelpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.database.AppDatabase
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Loads the user's followed channels via Helix pagination, enriches them with user avatars and
 * descriptions, and sorts favorites first (replaces decompiled FollowedChannels.java,
 * ).
 */
class FollowedChannels {

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val main = Handler(Looper.getMainLooper())

        fun getList(context: Context, callback: FollowedChannelsCallback) {
            scope.launch {
                loadPages(context, maxPages = 10, callback = callback)
            }
        }

        private fun returnList(callback: FollowedChannelsCallback, list: List<Movie>, fromCatch: Boolean) {
            main.post {
                if (fromCatch) {
                    callback.onList(list)
                    return@post
                }
                try {
                    callback.onList(list)
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                    callback.onList(emptyList())
                }
            }
        }

        private fun loadPages(context: Context, maxPages: Int, callback: FollowedChannelsCallback) {
            var cursor = ""
            var hasMore = true
            try {
                AppState.favoriteChannelIds = AppDatabase.getInstance(context).favoriteChannelDao().allChannelIds
                val allChannels = ArrayList<Movie>()
                var page = 0
                while (page <= maxPages) {
                    val response = HelixApi.getFollowedChannelsPage(context, cursor)
                    if (response == null || response.get("error") != null || response.get("errors") != null || response.get("data") == null) {
                        returnList(callback, emptyList(), false)
                        return
                    }
                    val dataArray = response.getAsJsonArray("data")
                    val pagination = response.getAsJsonObject("pagination")
                    cursor = if (pagination != null && pagination.has("cursor")) {
                        pagination.getAsJsonPrimitive("cursor").asString
                    } else {
                        ""
                    }
                    if (dataArray == null) {
                        returnList(callback, emptyList(), false)
                        return
                    }
                    if (cursor.isEmpty()) {
                        hasMore = false
                    }
                    val pageChannels = HelixParser.parseFollowedChannels(dataArray)
                    if (pageChannels.isNotEmpty()) {
                        val query = pageChannels.joinToString(separator = "&id=", prefix = "?id=") { it.userId.toString() }
                        val users = HelixParser.parseUsers(HelixApi.getUsersByQuery(context, query))
                        if (users.isNotEmpty()) {
                            val byUserId = users.associateBy { it.userId }
                            for (movie in pageChannels) {
                                val user = byUserId[movie.userId] ?: continue
                                movie.avatarUrl = user.avatarUrl
                                movie.setThumbnailUrl(user.cardImageUrl)
                                movie.description = user.description
                            }
                        }
                        allChannels.addAll(pageChannels)
                    }
                    page++
                    if (!hasMore) break
                }
                markFavorites(allChannels)
                returnList(callback, sortFavoritesFirst(allChannels), false)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                returnList(callback, emptyList(), true)
            }
        }

        private fun markFavorites(list: List<Movie>) {
            val favSet = AppState.favoriteChannelIds.mapNotNull { it.toIntOrNull() }.toHashSet()
            if (favSet.isEmpty()) return
            for (movie in list) {
                if (movie.userId in favSet) {
                    movie.isFavorite = true
                }
            }
        }

        private fun sortFavoritesFirst(list: List<Movie>): List<Movie> {
            val sortedByName = list.sortedBy { it.login }
            val favorites = sortedByName.filter { it.isFavorite }
            if (favorites.isEmpty()) return sortedByName
            val nonFavorites = sortedByName.filterNot { it.isFavorite }
            return favorites + nonFavorites
        }
    }
}
