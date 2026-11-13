package com.shrug.shrugtv.api.gql

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.shrug.shrugtv.filter.FilterSettings
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.model.VodChapter
import com.shrug.shrugtv.telemetry.CrashReporter

/**
 * Parsers for GraphQL responses (replaces decompiled GqlParser.java).
 */
class GqlParser {

    companion object {
        fun parseVideoBroadcastInfo(input: JsonObject): Pair<String, String> {
            return try {
                val video = input.get("data")?.asJsonObject?.get("video")?.asJsonObject
                    ?: return "" to ""
                val broadcastType = video.get("broadcastType")?.asString ?: ""
                val animatedPreviewUrl = try {
                    video.get("animatedPreviewURL")?.asString ?: ""
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                    ""
                }
                broadcastType to animatedPreviewUrl
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                "" to ""
            }
        }

        fun parseFollowedGames(jsonInput: JsonArray): ArrayList<Movie> {
            val list = ArrayList<Movie>()
            try {
                for (i in 0 until jsonInput.size()) {
                    val obj = jsonInput.get(i).asJsonObject
                    val displayName = if (obj.get("displayName").isJsonNull) "" else obj.get("displayName").asString
                    val movie = Movie().apply {
                        source = Enums.RowType.FOLLOWED_GAMES
                        itemType = Enums.ContentType.GAME
                        gameId = if (obj.get("id").isJsonNull) 0L else obj.get("id").asLong
                        title = displayName
                        setBoxArtUrl(if (obj.get("boxArtURL").isJsonNull) "" else obj.get("boxArtURL").asString)
                        setViewerCount(if (obj.get("viewersCount").isJsonNull) "0" else obj.get("viewersCount").asString)
                        gameName = displayName
                        streamType = ""
                        avatarUrl = ""
                        setLanguage("")
                        setVideoUrl("")
                    }
                    if (movie.viewerCount > 0) {
                        FilterSettings.addIfNotFiltered(list, movie)
                    }
                }
            } catch (_: Exception) {
            }
            return list
        }

        fun parseTopGames(jsonInput: JsonArray): Pair<ArrayList<Movie>, String> {
            val list = ArrayList<Movie>()
            var lastCursor = ""
            try {
                for (i in 0 until jsonInput.size()) {
                    val edge = jsonInput.get(i).asJsonObject
                    val node = edge.get("node").asJsonObject
                    val cursor = edge.get("cursor").asString
                    val displayName = if (node.get("displayName").isJsonNull) "" else node.get("displayName").asString
                    val movie = Movie().apply {
                        source = Enums.RowType.TOP_GAMES
                        itemType = Enums.ContentType.GAME
                        gameId = if (node.get("id").isJsonNull) 0L else node.get("id").asLong
                        title = displayName
                        setBoxArtUrl(if (node.get("boxArtURL").isJsonNull) "" else node.get("boxArtURL").asString)
                        setViewerCount(if (node.get("viewersCount").isJsonNull) "0" else node.get("viewersCount").asString)
                        gameName = displayName
                        streamType = ""
                        avatarUrl = ""
                        setLanguage("")
                        setVideoUrl("")
                    }
                    if (movie.viewerCount > 0) {
                        FilterSettings.addIfNotFiltered(list, movie)
                    }
                    lastCursor = cursor
                }
            } catch (_: Exception) {
                return list to lastCursor
            }
            return list to lastCursor
        }

        fun parseVideoChapters(input: JsonObject): ArrayList<VodChapter> {
            val list = ArrayList<VodChapter>()
            try {
                val video = input.get("data")?.asJsonObject?.get("video")?.asJsonObject ?: return list
                val edges = video.get("moments")?.asJsonObject?.get("edges")?.asJsonArray ?: return list
                for (i in 0 until edges.size()) {
                    try {
                        val node = edges.get(i).asJsonObject.get("node").asJsonObject
                        val name = node.get("description").asString
                        val posMs = node.get("positionMilliseconds").asLong
                        val durMs = node.get("durationMilliseconds").asLong
                        val gameElem = node.get("details").asJsonObject.get("game")
                        var image = if (gameElem == null || gameElem.isJsonNull) {
                            ""
                        } else {
                            gameElem.asJsonObject.get("boxArtURL")?.asString ?: ""
                        }
                        if (image.isEmpty()) {
                            image = "https://static-cdn.jtvnw.net/ttv-static/404_boxart-285x380.jpg"
                        }
                        list.add(VodChapter(name, posMs, durMs, image))
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
            return list
        }
    }
}
