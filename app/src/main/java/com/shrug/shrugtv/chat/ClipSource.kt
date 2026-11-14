package com.shrug.shrugtv.chat

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shrug.shrugtv.api.gql.GqlApi

/**
 * Where a clip came from. A clip has no chat of its own, but it remembers the recording it was cut from
 * and the second where it starts, so the chat of that recording can be played along with the clip. If the recording is gone
 * (they expire after a few weeks), there is no chat to show.
 */
object ClipSource {
    class Info(val videoId: Long, val offsetSeconds: Int, val broadcasterId: Int, val login: String?, val displayName: String?, val title: String? = null)

    /** The source of the clip, or null if the answer is unusable or the recording is no longer there. */
    fun parse(response: JsonObject?): Info? {
        val clip = response?.getAsJsonObject("data")?.get("clip")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val video = clip.get("video")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val videoId = video.get("id")?.takeIf { it.isJsonPrimitive }?.asString?.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val offset = clip.get("videoOffsetSeconds")?.takeIf { it.isJsonPrimitive }?.asInt ?: return null
        val owner = clip.get("broadcaster")?.takeIf { it.isJsonObject }?.asJsonObject
        return Info(
            videoId,
            offset.coerceAtLeast(0),
            owner?.get("id")?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull() ?: 0,
            owner?.get("login")?.takeIf { it.isJsonPrimitive }?.asString,
            owner?.get("displayName")?.takeIf { it.isJsonPrimitive }?.asString,
            clip.get("title")?.takeIf { it.isJsonPrimitive }?.asString,
        )
    }

    /** Blocking; call from a background thread. */
    fun fetch(context: Context?, slug: String): Info? = try {
        val query = JsonObject().apply {
            addProperty("query", "{ clip(slug: \"" + slug.filter { it.isLetterOrDigit() || it == '-' || it == '_' } +
                "\") { title videoOffsetSeconds video { id } broadcaster { id login displayName } } }")
        }
        parse(GqlApi.postQuery(context, query.toString()))
    } catch (e: Exception) {
        null
    }

    internal fun parseText(text: String): Info? = parse(JsonParser.parseString(text).asJsonObject)
}
