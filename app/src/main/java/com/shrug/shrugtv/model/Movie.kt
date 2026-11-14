package com.shrug.shrugtv.model

import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import com.shrug.shrugtv.util.TimeUtil
import java.io.Serializable
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.Locale
import java.util.regex.Pattern

class Movie : Serializable {


    var startedAt: ZonedDateTime? = null
        private set
    private var followedAt: ZonedDateTime? = null
    var selectedQuality: String? = null
    var description: String? = null
    var isFavorite: Boolean = false
    /** A clip that was opened before; cards draw it dimmed. */
    var clipSeen: Boolean = false
    /** The recording a clip was cut from and the second where the clip starts in it (for the chat of the clip); 0 = not known. */
    var clipVideoId: Long = 0L
    var clipOffsetSeconds: Int = 0
    var durationText: String? = null
        private set
    private var resumePositionSeconds: Int = 0
    var manifestCluster: String? = null
    var itemType: Enums.ContentType? = null
    var videoId: Long = 0L
        private set
    /** The recording of a live stream that is played to rewind it; no watch progress is kept. */
    var archiveOfLive: Boolean = false
    var viewCount: Int = 0
    var publishedDateText: String? = null
    var publishedAt: ZonedDateTime? = null
        private set
    var durationSeconds: Int = 0
        set(value) {
            field = value
            updateDurationText()
        }
    var resumePositionText: String? = null
    var followerCount: Int = 0
    var bannerUrl: String? = null
    private var playbackUrl: String? = null
    var isPrefetching: Boolean = false
    var playbackUrlFetchedAt: ZonedDateTime? = null
        private set
    private var lastWatchedText: String? = null
    var lastWatchedAt: ZonedDateTime? = null
        private set
    private var boxArt: String? = null
    val boxArtUrl: String?
        get() = boxArt
    private var historyId: Int = 0
    var adapterPosition: Int = 0
    private var live: Boolean = false
    private var watchStartedAt: ZonedDateTime? = null
    var userId: Int = 0
    var title: String? = null
        set(value) {
            field = value?.replace("\n", "")
        }
    var login: String? = null
    private var thumbnailUrl: String? = null
    private var videoUrl: String? = null
    var source: Enums.RowType? = null
    var viewerCountText: String? = null
        private set
    var viewerCount: Int = 0
        private set
    /** Stream language as upper-case code ("DE"), "N/A" when unknown, null when never set. */
    var languageCode: String? = null
        private set
    var avatarUrl: String? = null
    var gameId: Long = 0L
    var streamType: String = ""
    var displayName: String = ""
    var gameName: String = ""
    var qualities: ArrayList<StreamQuality> = ArrayList()
    private var languageFlag: String = ""
    private var slug: String = ""
    val clipSlug: String
        get() = slug

    fun setClipSlug(value: String?) {
        slug = value ?: ""
    }

    val isClip: Boolean
        get() = clipSlug.isNotEmpty()

    fun setResumePositionSeconds(seconds: Int) {
        resumePositionSeconds = seconds
    }

    fun updateDurationText() {
        durationText = TimeUtil.hms(durationSeconds.toLong())
    }

    val watchDurationSuffix: String
        get() {
            return try {
                val started = watchStartedAt ?: return ""
                val seconds = TimeUtil.secondsBetween(started, TimeUtil.now())
                if (seconds < 60) "" else " (${TimeUtil.hm(seconds)})"
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                ""
            }
        }

    val playingGameText: CharSequence
        get() {
            val builder = SpannableStringBuilder()
            if (displayName.isNotEmpty()) {
                builder.append(displayName)
                builder.setSpan(AbsoluteSizeSpan(22), 0, builder.length, 33)
                val middle = SpannableStringBuilder(" playing ")
                middle.setSpan(AbsoluteSizeSpan(15), 0, middle.length, 33)
                builder.append(middle)
                val game = SpannableStringBuilder(gameName)
                game.setSpan(AbsoluteSizeSpan(18), 0, game.length, 33)
                builder.append(game)
            }
            return builder
        }

    fun setVideoUrl(url: String?) {
        videoUrl = url
    }

    fun setViewerCount(str: String?) {
        if (str.isNullOrEmpty()) {
            viewerCountText = ""
        } else {
            viewerCountText = try {
                String.format(Locale.getDefault(), "%,d", str.toInt())
            } catch (_: NumberFormatException) {
                "N/A"
            }
        }
        parseViewerCount(str)
    }

    fun parseViewerCount(str: String?) {
        if (str.isNullOrEmpty()) {
            viewerCount = 0
            return
        }
        viewerCount = str.toIntOrNull() ?: 0
    }

    val uptimeText: String
        get() {
            val started = startedAt ?: return ""
            return try {
                "${String(Character.toChars(128343))} ${TimeUtil.hm(TimeUtil.secondsBetween(started, TimeUtil.nowUtc()))}"
            } catch (_: Exception) {
                ""
            }
        }

    fun setHistoryId(id: Int) {
        historyId = id
    }

    val resumeProgressPercent: Int
        get() {
            return try {
                if (resumePositionSeconds <= 0 || durationSeconds <= 0) 0
                else (resumePositionSeconds * 100) / durationSeconds
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                0
            }
        }

    fun setVideoIdFromString(str: String) {
        val matcher = DIGITS_PATTERN.matcher(str)
        while (matcher.find()) {
            val parsed = matcher.group().toLongOrNull()
            if (parsed != null) {
                videoId = parsed
                return
            }
        }
        videoId = str.toLongOrNull() ?: 0L
    }

    fun setThumbnailUrl(str: String?) {
        thumbnailUrl = str?.replace("{width}", "800")?.replace("{height}", "450")
    }

    fun setBoxArtUrl(str: String?) {
        if (str == null) {
            boxArt = null
            return
        }
        boxArt = if (source == Enums.RowType.SEARCH_GAME) {
            str.replace("52x72", "272x380").replace("{width}", "272").replace("{height}", "380")
        } else {
            str.replace("{width}", "272").replace("{height}", "380")
        }
    }

    val gameNameUrlEncoded: String
        get() = gameName.replace("+", "%2B").replace(" ", "+").replace("&", "%26")

    val cardImageUrl: String?
        get() = thumbnailUrl ?: boxArt

    fun setFollowedAt(str: String) {
        try {
            followedAt = TimeUtil.parseIso(str)
        } catch (_: Exception) {
        }
    }

    val popoutChatUrl: String
        get() = "https://www.twitch.tv/$login/chat"

    fun setPlaybackUrl(url: String?) {
        playbackUrl = url
        playbackUrlFetchedAt = TimeUtil.nowUtc()
    }

    fun setPlaybackUrlWithTime(url: String?, dateTime: ZonedDateTime?) {
        playbackUrl = url
        playbackUrlFetchedAt = dateTime
    }

    val webUrl: String?
        get() {
            if (itemType != Enums.ContentType.GAME) {
                return "https://www.twitch.tv/$login"
            }
            return try {
                "https://www.twitch.tv/directory/game/" +
                    URLEncoder.encode(gameName, StandardCharsets.UTF_8.toString()).replace("+", "%20")
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                null
            }
        }

    fun setLanguage(str: String?) {
        val map = AppState.languageFlags ?: return
        if (str == null) return
        try {
            if (map.isNotEmpty() && str.isNotEmpty() && map.containsKey(str)) {
                val points = map[str]
                if (!points.isNullOrEmpty()) {
                    val sb = StringBuilder()
                    for (cp in points) {
                        sb.append(String(Character.toChars(cp)))
                    }
                    languageFlag = sb.toString()
                    languageCode = str.uppercase()
                    return
                }
            }
            languageCode = if (str.isEmpty()) "N/A" else str.uppercase()
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            languageCode = "N/A"
        }
    }

    val followerCountText: String
        get() = try {
            String.format(Locale.getDefault(), "%,d", followerCount)
        } catch (_: Exception) {
            ""
        }

    fun setLastWatched(str: String?) {
        if (str == null) {
            lastWatchedText = ""
            lastWatchedAt = null
            return
        }
        try {
            lastWatchedAt = TimeUtil.parseIso(str)
            lastWatchedText = str
        } catch (_: RuntimeException) {
            lastWatchedText = ""
            lastWatchedAt = null
        }
    }

    fun setLive(isLive: Boolean) {
        live = isLive
    }

    fun setPublishedAt(str: String) {
        try {
            val dt = TimeUtil.parseIso(str)
            publishedAt = dt
            publishedDateText = TimeUtil.date(dt)
        } catch (_: RuntimeException) {
            publishedDateText = ""
        }
    }

    val playbackType: Enums.PlaybackKind?
        get() = when (itemType) {
            Enums.ContentType.STREAM -> Enums.PlaybackKind.LIVE
            Enums.ContentType.VOD -> Enums.PlaybackKind.VOD
            else -> null
        }

    fun setStartedAt(str: String) {
        try {
            startedAt = TimeUtil.parseIso(str)
        } catch (_: Exception) {
        }
    }

    /** Cached playback URL if younger than 15 min; clears it otherwise. */
    val playbackUrlIfFresh: String?
        get() {
            val fetched = playbackUrlFetchedAt ?: return null
            if (TimeUtil.secondsBetween(fetched, TimeUtil.nowUtc()) < 15 * 60) {
                return playbackUrl
            }
            playbackUrl = null
            isPrefetching = false
            return null
        }

    fun setStartedAtDateTime(dateTime: ZonedDateTime?) {
        startedAt = dateTime
    }

    fun markWatchStarted() {
        if (watchStartedAt == null) {
            watchStartedAt = TimeUtil.now()
        }
    }

    val languageBadgeText: String
        get() = if (!isFavorite) {
            languageFlag
        } else {
            "${String(Character.toChars(AppState.titlePrefixCodePoint))} $languageFlag"
        }

    val playbackId: String?
        get() = when (itemType) {
            Enums.ContentType.STREAM -> login
            Enums.ContentType.VOD -> if (isClip) clipSlug else videoId.toString()
            else -> null
        }

    fun setVodThumbnailUrl(str: String) {
        thumbnailUrl = str.replace("%{width}", "800").replace("%{height}", "450")
    }

    companion object {
        /**
         * Fixed, so the home screen cache (HomeCache, Java serialisation) survives app updates; the
         * computed default changed with every R8 build (docs/bug_review.md B25). Raise it only when a
         * field change makes old caches unreadable.
         */
        private const val serialVersionUID: Long = 1L
        private val DIGITS_PATTERN = Pattern.compile("\\d+")
    }
}
