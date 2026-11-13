package com.shrug.shrugtv.session

import android.content.Context
import com.google.gson.JsonObject
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.database.VodInfo
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.net.ResultCallback
import com.shrug.shrugtv.telemetry.CrashReporter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * User profile loader, VOD history converter, and favorite channel ordering
 * (replaces decompiled UserSession.java).
 */
object UserSession {

    interface ProfileCallback {
        fun onError(str: String)
        fun onUnauthorized()
        fun onEmptyResponse()
        fun onRequestFailed()
    }

    @JvmStatic
    fun vodHistoryToItems(list: List<VodInfo>): List<Movie> {
        val result = ArrayList<Movie>(list.size)
        for (vodInfo in list) {
            try {
                val movie = Movie().apply {
                    itemType = Enums.ContentType.VOD
                    source = Enums.RowType.VOD_HISTORY
                    title = vodInfo.title
                    setVideoIdFromString(vodInfo.broadcastId.toString())
                    displayName = vodInfo.channelName
                    avatarUrl = vodInfo.logoImage
                    viewCount = -1
                    publishedDateText = vodInfo.publishedDate
                    durationSeconds = vodInfo.vodLength
                    setThumbnailUrl(vodInfo.videoThumbnail)
                    gameName = vodInfo.categoryName
                    resumePositionText = vodInfo.positionText
                    setLastWatched(vodInfo.lastAccess)
                    setResumePositionSeconds(vodInfo.position)
                    userId = vodInfo.channelId
                    login = vodInfo.channelNameEnglish
                    setHistoryId(vodInfo.id)
                }
                result.add(movie)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
        return result
    }

    @JvmStatic
    fun moveFavoritesToFront(list: MutableList<Movie>) {
        if (!AppState.favoritesInFrontOfFollowed) return
        try {
            val favIds = AppState.favoriteChannelIds
            if (favIds.isEmpty() || list.isEmpty()) return
            val matched = ArrayList<Movie>()
            for (favId in favIds) {
                val found = list.firstOrNull { it.userId.toString() == favId }
                if (found != null) {
                    matched.add(found)
                }
            }
            for (movie in matched) {
                movie.isFavorite = true
                list.remove(movie)
                list.add(0, movie)
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    @JvmStatic
    fun loadUserProfile(context: Context, profileCallback: ProfileCallback) {
        HelixApi.getCurrentUser(context, ResultCallback { exc, json ->
            onUserProfileResponse(profileCallback, context, exc, json)
        })
    }

    private fun onUserProfileResponse(
        profileCallback: ProfileCallback,
        context: Context,
        exc: Exception?,
        jsonObject: JsonObject?,
    ) {
        try {
            if (exc != null) {
                // Helix errors arrive as StatusException since the HTTP status is checked (B17)
                val status = com.shrug.shrugtv.net.Http.StatusException.of(exc)
                if (status != null) {
                    if (status.code == 401) {
                        AppState.userProfileLoaded = false
                        profileCallback.onUnauthorized()
                    } else {
                        val message = try {
                            com.google.gson.JsonParser.parseString(status.body).asJsonObject.get("message")?.asString
                        } catch (e: Exception) {
                            null
                        }
                        profileCallback.onError(message ?: "HTTP ${status.code}")
                    }
                    return
                }
                CrashReporter.recordException(exc)
                profileCallback.onRequestFailed()
                return
            }
            if (jsonObject != null) {
                if (jsonObject.get("error") != null) {
                    if (jsonObject.get("status").asInt != 401) {
                        profileCallback.onError(jsonObject.get("message").asString)
                    } else {
                        AppState.userProfileLoaded = false
                        profileCallback.onUnauthorized()
                    }
                    return
                }
                val data = jsonObject.get("data")?.asJsonArray
                if (data != null && data.size() > 0) {
                    val userObj = data.get(0).asJsonObject
                    val login = userObj.get("login").asString
                    val displayName = userObj.get("display_name").asString
                    val id = userObj.get("id").asString
                    val profileImageUrl = userObj.get("profile_image_url").asString
                    if (login.isNotEmpty()) {
                        AppState.userLogin = login
                        applyHashedUserId(login)
                    }
                    if (id.isNotEmpty()) {
                        AppState.userId = id
                        try {
                            context.applicationContext.getSharedPreferences("User", 0).edit()
                                .putString("USER_ID", id)
                                .putString("USER_NAME", login)
                                .apply()
                        } catch (e: Exception) {
                            CrashReporter.recordException(e)
                        }
                    }
                    if (!profileImageUrl.isNullOrEmpty()) {
                        AppState.userProfileImageUrl = profileImageUrl
                    }
                    if (!displayName.isNullOrEmpty()) {
                        AppState.userDisplayName = displayName
                    }
                    SessionStore.updateProfile(
                        AppState.userId,
                        AppState.userLogin,
                        AppState.userDisplayName,
                        AppState.userProfileImageUrl,
                    )
                    profileCallback.onEmptyResponse()
                    return
                }
                AppState.userProfileLoaded = false
                return
            }
            AppState.userProfileLoaded = false
            profileCallback.onEmptyResponse()
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            AppState.userProfileLoaded = false
            profileCallback.onEmptyResponse()
        }
    }

    private fun applyHashedUserId(login: String) {
        try {
            val digest = MessageDigest.getInstance("SHA-256").digest(login.toByteArray(StandardCharsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }
            if (hex.length >= 15) {
                val shortHash = hex.substring(0, 14)
                AppState.hashedUserId = shortHash
                CrashReporter.setUserId(shortHash)
            }
        } catch (_: Exception) {
        }
    }
}
