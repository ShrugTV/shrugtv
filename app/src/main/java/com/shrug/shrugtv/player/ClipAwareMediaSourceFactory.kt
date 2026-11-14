package com.shrug.shrugtv.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Picks the media source by the address: Twitch clips are plain MP4 files, everything else the app
 * plays is HLS. The player only knew HLS, so a clip address ended in a source error and a black screen (clips were never
 * playable from the lists, measured on the Fire TV 2026-11-14).
 */
class ClipAwareMediaSourceFactory(
    private val hls: MediaSource.Factory,
    private val progressive: MediaSource.Factory,
) : MediaSource.Factory {

    override fun setDrmSessionManagerProvider(provider: DrmSessionManagerProvider): MediaSource.Factory {
        hls.setDrmSessionManagerProvider(provider)
        progressive.setDrmSessionManagerProvider(provider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(policy: LoadErrorHandlingPolicy): MediaSource.Factory {
        hls.setLoadErrorHandlingPolicy(policy)
        progressive.setLoadErrorHandlingPolicy(policy)
        return this
    }

    override fun getSupportedTypes(): IntArray = (hls.supportedTypes + progressive.supportedTypes).distinct().toIntArray()

    override fun createMediaSource(mediaItem: MediaItem): MediaSource =
        if (isFile(mediaItem.localConfiguration?.uri?.path)) progressive.createMediaSource(mediaItem) else hls.createMediaSource(mediaItem)

    companion object {
        /** A clip address ends in ".mp4" (the query with signature and token comes after the path). */
        fun isFile(path: String?): Boolean = path != null && path.endsWith(".mp4", ignoreCase = true)
    }
}
