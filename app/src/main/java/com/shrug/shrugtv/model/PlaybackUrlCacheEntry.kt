package com.shrug.shrugtv.model

import com.shrug.shrugtv.util.TimeUtil
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class PlaybackUrlCacheEntry @JvmOverloads constructor(
    var state: LoadState = LoadState.LOADING
) {
    private var tokenExpiresAt: Long = -1L
    var playlistUrl: String? = null
    private var validUntil: ZonedDateTime = Instant.ofEpochMilli(-1L).atZone(ZoneOffset.UTC)

    fun setTokenExpiresAt(expires: Long) {
        tokenExpiresAt = expires
        val epochMillis = if (expires in 1..9_999_999_999L) expires * 1000L else expires
        validUntil = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC)
    }

    fun getPlaylistUrlIfValid(): String? {
        if (validUntil <= TimeUtil.nowUtc()) {
            return null
        }
        return playlistUrl
    }
}
