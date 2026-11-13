package com.shrug.shrugtv.model

import com.shrug.shrugtv.R
import com.shrug.shrugtv.telemetry.CrashReporter
import java.util.regex.Pattern

object Enums {

    enum class ContentType {
        STREAM,
        GAME,
        CHANNEL,
        VOD
    }

    enum class ChatBackgroundOpacity {
        PERC_0,
        PERC_10,
        PERC_20,
        PERC_30,
        PERC_40,
        PERC_50,
        PERC_60,
        PERC_70,
        PERC_80,
        PERC_90,
        PERC_100
    }

    enum class MultiViewSize {
        PERC_25,
        PERC_33,
        PERC_50,
        PERC_66,
        PERC_75,
        PERC_100,
        PERC_5,
        PERC_10,
        PERC_15
    }

    enum class OverlayMode {
        None,
        Chat,
        MultiView
    }

    enum class EmoteScope {
        CHANNEL,
        GLOBAL
    }

    enum class PlaybackKind {
        LIVE,
        VOD
    }

    enum class DeletedMessageMode {
        DO_NOTHING,
        GREY_IT_OUT,
        REPLACE_IT
    }

    enum class ChatSize {
        SIZE1,
        SIZE2,
        SIZE3,
        SIZE4,
        SIZE5
    }

    enum class PreviewQuality {
        Q_720P,
        Q_480P,
        Q_360P
    }

    enum class ProxyServer {
        NONE,
        TOKYO,
        CUSTOM
    }

    enum class RemoteKeyAction {
        NONE,
        SHOW_HIDE_CHAT,
        MODIFY_CHAT,
        MODIFY_MULTIVIEW,
        VIDEO_SIZE,
        FAST_FORWARD,
        PLAYER_INFO,
        SEND_MESSAGE,
        VIDEO_QUALITY,
        EXTERNAL_PLAYER,
        SAVE_POSITION_FOR_VOD,
        PAUSE_CHAT,
        CHANGE_CHAT_BACKGROUND,
        SWAP_CHAT,
        PLAY_PAUSE,
        VOLUME_CONTROL,
        SEARCH,
        STREAM_NAVIGATION_PREV,
        STREAM_NAVIGATION_PREV_MV,
        STREAM_NAVIGATION_NEXT,
        STREAM_NAVIGATION_NEXT_MV,
        SWAP_MULTIVIEW,
        SWAP_MV_AUDIO,
        CHAT_BACKGROUND_OPACITY
    }

    enum class PlayerLayout {
        NORMAL,
        MULTIWATCH_NORMAL,
        MULTIWATCH_BOTTOMBAR
    }

    enum class RowType {
        NULL,
        TOP_GAMES,
        FEATURED,
        LANGUAGE,
        FOLLOWED_GAMES,
        FOLLOWED,
        FOLLOWED_CHANNELS,
        SEARCH_STREAM,
        GAME,
        GAME_LANGUAGE,
        TOP_STREAMS,
        SETTINGS,
        VOD_CHANNEL_ARCHIVED,
        VOD_CHANNEL_HIGHLIGHTS,
        VOD_CHANNEL_UPLOADED,
        PAST_BROADCAST,
        SEARCH_CHANNEL,
        SEARCH_GAME,
        VOD_HISTORY,
        LIVE_CHANNEL_HISTORY
    }

    enum class SubType {
        NULL,
        NORMAL_SUB,
        GIFTED_SUB
    }

    enum class VideoQuality {
        AUTO,
        AUTO_1080P,
        AUTO_720P,
        AUTO_480P,
        SOURCE
    }

    enum class VideoScaling {
        NORMAL,
        FIT_TO_SCREEN,
        CENTER_CROP
    }

    enum class OverlayPosition {
        RIGHT_TOP,
        RIGHT_MIDDLE,
        RIGHT_BOTTOM,
        BOTTOM_CENTER,
        LEFT_BOTTOM,
        LEFT_MIDDLE,
        LEFT_TOP,
        TOP_CENTER
    }

    private val SERVER_CODE_PATTERN = Pattern.compile("[a-zA-Z]+")

    private val SERVER_CITIES = mapOf(
        "hkg" to "Hong Kong",
        "tyo" to "Tokyo",
        "sin" to "Singapore",
        "sel" to "Seoul",
        "tpe" to "Taipei",
        "bkk" to "Bangkok",
        "syd" to "Sydney",
        "vie" to "Vienna",
        "prg" to "Prague",
        "cph" to "Copenhagen",
        "hel" to "Helsinki",
        "mrs" to "Marseille",
        "cdg" to "Paris",
        "ber" to "Berlin",
        "fra" to "Frankfurt",
        "mil" to "Milan",
        "ams" to "Amsterdam",
        "osl" to "Oslo",
        "waw" to "Warsaw",
        "mad" to "Madrid",
        "arn" to "Stockholm",
        "lhr" to "London",
        "ymq" to "Quebec",
        "yto" to "Toronto",
        "qro" to "Queretaro",
        "rio" to "Rio de Janeiro",
        "sao" to "Sao Paulo",
        "dfw" to "Dallas",
        "den" to "Denver",
        "hou" to "Houston",
        "iad" to "Ashburn",
        "atl" to "Atlanta",
        "ord" to "Chicago",
        "mia" to "Miami",
        "jfk" to "New York",
        "lax" to "Los Angeles",
        "phx" to "Phoenix",
        "pdx" to "Portland",
        "slc" to "Salt Lake City",
        "sfo" to "San Francisco",
        "sjc" to "San Jose",
        "sea" to "Seattle"
    )

    @JvmStatic
    fun serverCodeToCity(server: String?): String {
        if (server.isNullOrEmpty()) return ""
        return try {
            val matcher = SERVER_CODE_PATTERN.matcher(server)
            var code = ""
            while (matcher.find()) {
                code = matcher.group()
            }
            SERVER_CITIES[code] ?: ""
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            ""
        }
    }

    @JvmStatic
    fun chatBackgroundColors(opacity: ChatBackgroundOpacity): IntArray = when (opacity) {
        ChatBackgroundOpacity.PERC_0 -> intArrayOf(R.color.black_opacity_0p, R.color.black_opacity_0p)
        ChatBackgroundOpacity.PERC_10 -> intArrayOf(R.color.black_opacity_0p, R.color.black_opacity_11p)
        ChatBackgroundOpacity.PERC_20 -> intArrayOf(R.color.black_opacity_11p, R.color.black_opacity_11p)
        ChatBackgroundOpacity.PERC_30 -> intArrayOf(R.color.black_opacity_17p, R.color.black_opacity_17p)
        ChatBackgroundOpacity.PERC_40 -> intArrayOf(R.color.black_opacity_23p, R.color.black_opacity_23p)
        ChatBackgroundOpacity.PERC_50 -> intArrayOf(R.color.black_opacity_29p, R.color.black_opacity_29p)
        ChatBackgroundOpacity.PERC_60 -> intArrayOf(R.color.black_opacity_37p, R.color.black_opacity_37p)
        ChatBackgroundOpacity.PERC_70 -> intArrayOf(R.color.black_opacity_45p, R.color.black_opacity_45p)
        ChatBackgroundOpacity.PERC_80 -> intArrayOf(R.color.black_opacity_55p, R.color.black_opacity_55p)
        ChatBackgroundOpacity.PERC_90 -> intArrayOf(R.color.black_opacity_68p, R.color.black_opacity_68p)
        ChatBackgroundOpacity.PERC_100 -> intArrayOf(R.color.black_opacity_100p, R.color.black_opacity_100p)
    }

    @JvmStatic
    fun previewResolution(quality: PreviewQuality): IntArray = when (quality) {
        PreviewQuality.Q_480P -> intArrayOf(852, 480)
        PreviewQuality.Q_360P -> intArrayOf(640, 360)
        PreviewQuality.Q_720P -> intArrayOf(1280, 720)
    }
}
