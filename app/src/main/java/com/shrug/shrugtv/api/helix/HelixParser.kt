package com.shrug.shrugtv.api.helix

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.shrug.shrugtv.apihelpers.ChannelAvatarLoader
import com.shrug.shrugtv.filter.FilterSettings
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.telemetry.CrashReporter
import com.shrug.shrugtv.util.TimeUtil
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Parsers for Twitch Helix JSON responses (replaces decompiled HelixParser.java,
 * ).
 */
object HelixParser {

    private fun JsonObject.str(key: String, default: String = ""): String {
        val el = get(key)
        return if (el == null || el.isJsonNull) default else el.asString
    }

    private fun JsonObject.int(key: String, default: Int = 0): Int {
        val el = get(key)
        return if (el == null || el.isJsonNull) default else el.asInt
    }

    private fun JsonObject.long(key: String, default: Long = -1L): Long {
        val el = get(key)
        if (el == null || el.isJsonNull) return default
        val s = el.asString
        return if (s.isEmpty()) default else s.toLongOrNull() ?: default
    }

    @JvmStatic
    fun addIfNotFiltered(list: ArrayList<Movie>?, movie: Movie?) {
        FilterSettings.addIfNotFiltered(list, movie)
    }

    @JvmStatic
    fun parseStreams(context: Context?, rowType: Enums.RowType, jsonArray: JsonArray): ArrayList<Movie> {
        val list = ArrayList<Movie>(jsonArray.size())
        for (i in 0 until jsonArray.size()) {
            try {
                val stream = parseStream(jsonArray.get(i).asJsonObject, rowType)
                if (!stream.displayName.isNullOrEmpty()) {
                    addIfNotFiltered(list, stream)
                }
            } catch (_: Exception) {
            }
        }
        if (context != null) {
            ChannelAvatarLoader.loadAvatars(context, list)
        }
        return list
    }

    @JvmStatic
    fun getPaginationCursor(jsonObject: JsonObject): String = try {
        val cursor = jsonObject.getAsJsonObject("pagination")?.get("cursor")
        if (cursor == null || cursor.isJsonNull) "" else cursor.asString
    } catch (e: Exception) {
        CrashReporter.recordException(e)
        ""
    }

    @JvmStatic
    fun parseFollowedChannels(jsonArray: JsonArray): List<Movie> {
        val list = ArrayList<Movie>(jsonArray.size())
        for (i in 0 until jsonArray.size()) {
            val obj = jsonArray.get(i)?.asJsonObject ?: return list
            val movie = Movie().apply {
                source = Enums.RowType.FOLLOWED_CHANNELS
                itemType = Enums.ContentType.CHANNEL
                gameName = ""
                setViewerCount("0")
                userId = obj.int("broadcaster_id", -1)
                login = obj.str("broadcaster_login")
                displayName = obj.str("broadcaster_name")
                setFollowedAt(obj.str("followed_at"))
                setThumbnailUrl("")
            }
            list.add(movie)
        }
        return list
    }

    @JvmStatic
    fun parseCategories(jsonArray: JsonArray, rowType: Enums.RowType): ArrayList<Movie> {
        val list = ArrayList<Movie>(jsonArray.size())
        for (i in 0 until jsonArray.size()) {
            try {
                val obj = jsonArray.get(i)?.asJsonObject ?: continue
                val name = obj.str("name")
                val movie = Movie().apply {
                    source = rowType
                    itemType = Enums.ContentType.GAME
                    gameId = obj.long("id", -1L)
                    title = name
                    setBoxArtUrl(obj.str("box_art_url"))
                    gameName = name
                    streamType = ""
                    avatarUrl = ""
                    setLanguage("")
                    setVideoUrl("")
                }
                addIfNotFiltered(list, movie)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
        return list
    }

    @JvmStatic
    fun parseChannelSearchResults(jsonArray: JsonArray, rowType: Enums.RowType): ArrayList<Movie> {
        val list = ArrayList<Movie>(jsonArray.size())
        for (i in 0 until jsonArray.size()) {
            try {
                val obj = jsonArray.get(i)?.asJsonObject ?: continue
                val thumb = obj.str("thumbnail_url")
                val movie = Movie().apply {
                    if (rowType == Enums.RowType.SEARCH_CHANNEL) {
                        source = rowType
                        itemType = Enums.ContentType.CHANNEL
                    }
                    userId = obj.int("id", 0)
                    title = obj.str("title")
                    displayName = obj.str("display_name")
                    login = obj.str("broadcaster_login")
                    val liveEl = obj.get("is_live")
                    setLive(liveEl != null && !liveEl.isJsonNull && liveEl.asBoolean)
                    gameName = obj.str("game_name")
                    gameId = obj.long("game_id", -1L)
                    streamType = "rerun"
                    avatarUrl = thumb
                    setThumbnailUrl(thumb)
                    setLanguage(obj.str("broadcaster_language"))
                    setVideoUrl("")
                }
                addIfNotFiltered(list, movie)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
        return list
    }

    @JvmStatic
    fun parseStream(obj: JsonObject, rowType: Enums.RowType): Movie = Movie().apply {
        itemType = Enums.ContentType.STREAM
        source = rowType
        userId = obj.int("user_id", 0)
        title = obj.str("title")
        login = obj.str("user_login")
        displayName = obj.str("user_name")
        gameName = obj.str("game_name")
        gameId = obj.long("game_id", -1L)
        setThumbnailUrl(obj.str("thumbnail_url"))
        setViewerCount(obj.str("viewer_count"))
        streamType = obj.str("type")
        setLanguage(obj.str("language"))
        setStartedAt(obj.str("started_at"))
    }

    @JvmStatic
    fun parseFirstUser(jsonObject: JsonObject): Movie? {
        var movie: Movie? = null
        try {
            val data = jsonObject.get("data")?.asJsonArray
            if (data == null || data.size() == 0) return null
            val obj = data.get(0)?.asJsonObject ?: return null
            movie = parseUserObject(obj)
            return movie
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            return movie
        }
    }

    @JvmStatic
    fun parseUsers(jsonObject: JsonObject?): List<Movie> {
        val list = ArrayList<Movie>()
        if (jsonObject == null) return list
        return try {
            val data = jsonObject.get("data")?.asJsonArray ?: return list
            for (i in 0 until data.size()) {
                val obj = data.get(i)?.asJsonObject ?: return list
                list.add(parseUserObject(obj))
            }
            ChannelAvatarLoader.cacheUsers(list)
            list
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            list
        }
    }

    private fun parseUserObject(obj: JsonObject): Movie {
        val profileImg = obj.str("profile_image_url")
        return Movie().apply {
            source = Enums.RowType.FOLLOWED_CHANNELS
            itemType = Enums.ContentType.CHANNEL
            gameName = ""
            setViewerCount("0")
            streamType = ""
            userId = obj.int("id", -1)
            title = ""
            login = obj.str("login")
            displayName = obj.str("display_name")
            setThumbnailUrl(profileImg)
            followerCount = -1
            setLanguage("")
            description = obj.str("description")
            bannerUrl = ""
            avatarUrl = profileImg
        }
    }

    private fun parseDurationSeconds(duration: String): Int = try {
        TimeUtil.twitchDuration(duration)
    } catch (e: Exception) {
        CrashReporter.recordException(e)
        -1
    }

    @JvmStatic
    fun parseVideos(jsonArray: JsonArray, rowType: Enums.RowType, avatarUrl: String): ArrayList<Movie> {
        val list = ArrayList<Movie>(jsonArray.size())
        for (i in 0 until jsonArray.size()) {
            try {
                val obj = jsonArray.get(i).asJsonObject
                val durationStr = obj.str("duration")
                val movie = Movie().apply {
                    itemType = Enums.ContentType.VOD
                    source = rowType
                    gameName = ""
                    userId = obj.int("user_id", 0)
                    displayName = obj.str("user_name")
                    login = obj.str("user_login")
                    this.avatarUrl = avatarUrl
                    title = obj.str("title")
                    setVideoIdFromString(obj.str("id"))
                    viewCount = obj.int("view_count", 0)
                    setVideoUrl(obj.str("url"))
                    setPublishedAt(obj.str("published_at"))
                    setVodThumbnailUrl(obj.str("thumbnail_url"))
                    if (durationStr.isNotEmpty()) {
                        durationSeconds = parseDurationSeconds(durationStr)
                    }
                }
                list.add(movie)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
        return list
    }

    @JvmStatic
    fun parseClips(jsonArray: JsonArray?, rowType: Enums.RowType, avatarUrl: String?): ArrayList<Movie> {
        val list = ArrayList<Movie>()
        if (jsonArray == null) return list
        for (i in 0 until jsonArray.size()) {
            try {
                val obj = jsonArray.get(i).asJsonObject
                val slug = obj.str("id")
                if (slug.isEmpty()) continue
                val movie = Movie().apply {
                    setClipSlug(slug)
                    val urlStr = obj.str("url")
                    if (urlStr.isNotEmpty()) setVideoUrl(urlStr)
                    val titleStr = obj.str("title")
                    if (titleStr.isNotEmpty()) title = titleStr
                    val bId = obj.str("broadcaster_id")
                    if (bId.isNotEmpty()) userId = bId.toInt()
                    val bName = obj.str("broadcaster_name")
                    if (bName.isNotEmpty()) {
                        displayName = bName
                        login = bName.lowercase(Locale.ROOT)
                    }
                    val thumb = obj.str("thumbnail_url")
                    if (thumb.isNotEmpty()) setVodThumbnailUrl(thumb)
                    val durEl = obj.get("duration")
                    if (durEl != null && !durEl.isJsonNull) {
                        durationSeconds = maxOf(1, durEl.asDouble.roundToInt())
                    }
                    val viewsEl = obj.get("view_count")
                    if (viewsEl != null && !viewsEl.isJsonNull) {
                        viewCount = viewsEl.asInt
                        setViewerCount(viewsEl.asString)
                    }
                    val created = obj.str("created_at")
                    if (created.isNotEmpty()) setPublishedAt(created)
                    val gId = obj.str("game_id")
                    if (gId.isNotEmpty()) gId.toLongOrNull()?.let { gameId = it }
                    val lang = obj.str("language")
                    if (lang.isNotEmpty()) setLanguage(lang)
                    this.avatarUrl = avatarUrl ?: ""
                    source = rowType
                    itemType = Enums.ContentType.VOD
                }
                list.add(movie)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
        return list
    }

    @JvmStatic
    fun parseClips(context: Context?, jsonArray: JsonArray?, rowType: Enums.RowType, avatarUrl: String?): ArrayList<Movie> {
        val list = parseClips(jsonArray, rowType, avatarUrl)
        if (context != null && avatarUrl.isNullOrEmpty() && list.isNotEmpty()) {
            ChannelAvatarLoader.loadAvatars(context, list)
        }
        return list
    }
}
