package com.shrug.shrugtv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.preference.PreferenceManager

/**
 * Twitch low latency, switchable in Stream settings ("pref_low_latency", on by
 * default like the Twitch web player).
 *
 * Two parts:
 * - The usher master request gets "fast_bread=true". Measured 2026-10-29 on 5 channels, 2 runs each:
 *   with it the media playlist ends with 2 #EXT-X-TWITCH-PREFETCH segments (4 of 5 channels), without
 *   it never. TwitchHlsPlaylistParser already turns those lines into regular segments.
 * - The player gets a LiveConfiguration with a small target offset and slight speed changes, so it
 *   sits close to the (now longer) live edge and catches up gently instead of jumping.
 */
object LowLatency {
    const val PREF_KEY = "pref_low_latency"
    const val USHER_PARAM = "&fast_bread=true"

    const val TARGET_OFFSET_MS = 3_000L
    const val MIN_OFFSET_MS = 1_500L
    const val MAX_OFFSET_MS = 12_000L
    const val MIN_SPEED = 0.97f
    const val MAX_SPEED = 1.03f

    @JvmStatic
    fun enabled(context: Context?): Boolean =
        context == null || PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEY, true)

    /** Master playlist URL with or without the low latency parameter. */
    @JvmStatic
    fun usherUrl(url: String, enabled: Boolean): String {
        val without = url.replace(USHER_PARAM, "")
        return if (enabled) without + USHER_PARAM else without
    }

    /** A cached master URL only fits if it was built with the current setting. */
    @JvmStatic
    fun matches(url: String, enabled: Boolean): Boolean = url.contains(USHER_PARAM) == enabled

    /** Live configuration for the main player; unchanged media item when low latency is off. */
    @JvmStatic
    fun apply(builder: MediaItem.Builder, enabled: Boolean): MediaItem.Builder {
        if (!enabled) return builder
        return builder.setLiveConfiguration(
            MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(TARGET_OFFSET_MS)
                .setMinOffsetMs(MIN_OFFSET_MS)
                .setMaxOffsetMs(MAX_OFFSET_MS)
                .setMinPlaybackSpeed(MIN_SPEED)
                .setMaxPlaybackSpeed(MAX_SPEED)
                .build()
        )
    }
}
