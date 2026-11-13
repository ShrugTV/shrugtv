package com.shrug.shrugtv.apihelpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.shrug.shrugtv.api.gql.GqlApi
import com.shrug.shrugtv.api.gql.GqlParser
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.telemetry.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Loads followed categories/games via GraphQL (replaces decompiled FollowedGames.java,
 * ).
 */
class FollowedGames {

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val main = Handler(Looper.getMainLooper())

        private fun fetchFollowedGames(context: Context): List<Movie> {
            return try {
                val json = GqlApi.getFollowedGames(context) ?: return emptyList()
                val nodes = json.getAsJsonObject("data")
                    ?.getAsJsonObject("user")
                    ?.getAsJsonObject("followedGames")
                    ?.getAsJsonArray("nodes")
                if (nodes == null || nodes.size() == 0) return emptyList()
                val parsed = GqlParser.Companion.parseFollowedGames(nodes) ?: return emptyList()
                parsed.sortedByDescending { it.viewerCount }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                emptyList()
            }
        }

        fun getList(context: Context, callback: FollowedGamesCallback) {
            scope.launch {
                val list = fetchFollowedGames(context)
                main.post { callback.onList(ArrayList(list)) }
            }
        }

        fun isFollowed(context: Context, gameId: Long, callback: IsFollowedCallback) {
            scope.launch {
                val followed = fetchFollowedGames(context).any { it.gameId == gameId }
                main.post { callback.onResult(followed) }
            }
        }
    }
}
