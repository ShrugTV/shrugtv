package com.shrug.shrugtv.api.gql

import android.content.Context
import com.google.gson.JsonObject
import com.shrug.shrugtv.net.Http
import com.shrug.shrugtv.net.ResultCallback
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter

/**
 * Twitch GraphQL client for queries not available on Helix (replaces decompiled GqlApi.java,
 * ).
 */
object GqlApi {

    const val WEB_CLIENT_ID = "kd1unb4b3q4t58fwlpcbzcbnm76a8fp"
    private const val TIMEOUT_MS = 3000

    private fun request(body: String): Http.HttpRequest =
        Http.load("POST", "https://gql.twitch.tv/gql")
            .setHeader("Client-ID", WEB_CLIENT_ID)
            .setHeader("Content-Type", "application/json")
            .setTimeout(TIMEOUT_MS)
            .setStringBody(body)

    @JvmStatic
    fun postQuery(context: Context?, body: String): JsonObject? = try {
        request(body).asJsonObject().get()
    } catch (e: Exception) {
        CrashReporter.recordException(e)
        null
    }

    private fun buildTopGamesQuery(cursor: String): String {
        val after = if (cursor.isEmpty()) "" else ", after: \\\"$cursor\\\""
        return "{\"query\":\"query {\\r\\n    games(first:100$after){    \\r\\n        edges {\\r\\n            node {\\r\\n                id\\r\\n                displayName\\r\\n                viewersCount\\r\\n            boxArtURL(width:245, height:342)\\r\\n            }\\r\\n            cursor\\r\\n        }\\r\\n    }\\r\\n}\",\"variables\":{}}"
    }

    @JvmStatic
    fun getUserProfile(context: Context?, userId: Int, callback: ResultCallback<JsonObject>) {
        val body = "{\"query\":\"query {\\r\\n  user(id: $userId) {\\r\\n    description\\r\\n    profileImageURL(width:300)\\r\\n    bannerImageURL\\r\\n    followers {\\r\\n      totalCount\\r\\n    }\\r\\n  }\\r\\n}\",\"variables\":{}}"
        request(body).asJsonObject().setCallback(callback)
    }

    @JvmStatic
    fun getFollowedGames(context: Context?): JsonObject? {
        val body = "{\"query\":\"query {\\r\\n  user(login: \\\"${AppState.userLogin}\\\") {\\r\\n    followedGames(first: 100)\\r\\n    {\\r\\n        nodes\\r\\n        {\\r\\n            id\\r\\n            displayName\\r\\n            boxArtURL\\r\\n            viewersCount \\r\\n        }\\r\\n    }\\r\\n  }\\r\\n}\"}"
        return postQuery(context, body)
    }

    @JvmStatic
    fun getTopGames(context: Context?, cursor: String): JsonObject? =
        postQuery(context, buildTopGamesQuery(cursor))

    @JvmStatic
    fun getVideoChapters(context: Context?, videoId: Long): JsonObject? {
        val body = "{\"query\":\"query {\\r\\n  video(id: \\\"$videoId\\\"){\\r\\n    moments(first: 100, after: \\\"\\\", sort: ASC, types: GAME_CHANGE,  momentRequestType: VIDEO_CHAPTER_MARKERS){\\r\\n      edges {\\r\\n        node {\\r\\n          description \\r\\n          type\\r\\n          positionMilliseconds\\r\\n          durationMilliseconds\\r\\n          details{\\r\\n            ... on GameChangeMomentDetails {\\r\\n              game {\\r\\n                boxArtURL(width:245, height:342)\\r\\n              }\\r\\n            }\\r\\n          }\\r\\n        }\\r\\n      }\\r\\n    }\\r\\n  }\\r\\n}\",\"variables\":{}}"
        return postQuery(context, body)
    }
}
