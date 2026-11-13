package com.shrug.shrugtv.api.gql

import com.google.gson.JsonObject

/**
 * GraphQL persisted-query request builders (replaces decompiled GqlRequests.java,
 * ).
 */
class GqlRequests {

    companion object {
        fun buildVideoCommentsRequest(videoId: Long, contentOffsetSeconds: Long, nextCursor: String): JsonObject {
            val variables = JsonObject().apply {
                addProperty("videoID", videoId.toString())
                if (contentOffsetSeconds > -1) {
                    addProperty("contentOffsetSeconds", contentOffsetSeconds)
                } else {
                    addProperty("cursor", nextCursor)
                }
            }
            val persistedQuery = JsonObject().apply {
                addProperty("version", 1)
                addProperty("sha256Hash", "b70a3591ff0f4e0313d126c6a1502d79a1c02baebb288227c582044aa76adf6a")
            }
            val extensions = JsonObject().apply {
                add("persistedQuery", persistedQuery)
            }
            return JsonObject().apply {
                addProperty("operationName", "VideoCommentsByOffsetOrCursor")
                add("variables", variables)
                add("extensions", extensions)
            }
        }
    }
}
