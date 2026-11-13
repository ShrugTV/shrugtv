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
 * Loads top categories/games via GraphQL (replaces decompiled TopGames.java).
 */
class TopGames {

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val main = Handler(Looper.getMainLooper())

        fun getList(context: Context, cursor: String, callback: TopGamesCallback) {
            scope.launch {
                val (games, nextCursor) = fetchPage(context, cursor)
                main.post { callback.onList(ArrayList(games), nextCursor) }
            }
        }

        private fun fetchPage(context: Context, cursorIn: String): Pair<List<Movie>, String> {
            return try {
                val edges = GqlApi.getTopGames(context, cursorIn)
                    ?.getAsJsonObject("data")
                    ?.getAsJsonObject("games")
                    ?.getAsJsonArray("edges")
                if (edges != null && edges.size() > 0) {
                    GqlParser.Companion.parseTopGames(edges)
                } else {
                    emptyList<Movie>() to ""
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                emptyList<Movie>() to ""
            }
        }
    }
}
