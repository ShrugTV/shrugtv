package com.shrug.shrugtv.apihelpers

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.shrug.shrugtv.api.gql.GqlApi
import com.shrug.shrugtv.api.gql.GqlParser
import com.shrug.shrugtv.model.StreamQuality

/**
 * Builds fallback HLS playlist URLs for subscriber-restricted VODs from animated preview storyboard
 * URLs (replaces decompiled RestrictedVodPlaylist.java).
 */
class RestrictedVodPlaylist {

    companion object {
        private val STORYBOARD_REGEX = Regex("/storyboards.*")

        fun buildQualities(context: Context?, id: String, videoQualities: JsonArray?): ArrayList<StreamQuality> {
            val result = ArrayList<StreamQuality>()
            if (context == null || videoQualities == null) return result
            val data = GqlApi.postQuery(
                context,
                "{\"query\":\"query { video(id: \\\"$id\\\") { animatedPreviewURL broadcastType } }\",\"variables\":{}}"
            ) ?: return result
            val (broadcastType, animatedPreviewUrl) = GqlParser.Companion.parseVideoBroadcastInfo(data)
            if (broadcastType.isEmpty() || animatedPreviewUrl.isEmpty()) return result

            val baseUrl = STORYBOARD_REGEX.replace(animatedPreviewUrl, "")
            for (elem in videoQualities) {
                val qualityName = (elem as? JsonPrimitive)?.asString ?: continue
                val url = if (broadcastType == "HIGHLIGHT") {
                    "$baseUrl/$qualityName/highlight-$id.m3u8"
                } else {
                    "$baseUrl/$qualityName/index-dvr.m3u8"
                }
                result.add(StreamQuality(qualityName, url))
            }
            return sortQualities(result)
        }

        fun sortQualities(list: List<StreamQuality>): ArrayList<StreamQuality> {
            val mutable = list.sortedByDescending { it.name }.toMutableList()
            val audioOnly = mutable.filter { it.name == "audio_only" }
            val chunked = mutable.filter { it.name == "chunked" }
            val fullHd60 = mutable.filter { it.name == "1080p60" }

            if (audioOnly.size == 1) {
                mutable.remove(audioOnly[0])
                mutable.add(audioOnly[0])
            }
            if (chunked.size == 1 && fullHd60.size == 1) {
                mutable.remove(chunked[0])
            }
            if (chunked.size == 1 && fullHd60.isEmpty()) {
                mutable.remove(chunked[0])
                mutable.add(0, chunked[0])
            }
            if (fullHd60.size == 1) {
                mutable.remove(fullHd60[0])
                mutable.add(0, fullHd60[0])
            }
            return ArrayList(mutable)
        }
    }
}
