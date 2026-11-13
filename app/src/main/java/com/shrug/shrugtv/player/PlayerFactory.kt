package com.shrug.shrugtv.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.shrug.shrugtv.player.hls.TwitchHlsPlaylistParserFactory

/**
 * Zentrale Player-Factory (Slice 4b).
 *
 * Uniform ExoPlayer creation for main player, preview and multiview:
 * - Adjusted DefaultLoadControl (~1.0-1.5 s faster first picture):
 *   bufferForPlaybackMs = 1200 ms, bufferForPlaybackAfterRebufferMs = 2000 ms,
 *   maxBufferMs = 15000-30000 ms to protect memory (2 GB Fire TV).
 * - Android audio focus (handleAudioFocus = true) on the main player.
 * - Geteilte Twitch-Header und TwitchHlsPlaylistParserFactory.
 */
object PlayerFactory {

    private const val USER_AGENT = "ShrugTV"
    private val TWITCH_HEADERS = mapOf(
        "Referer" to "https://player.twitch.tv",
        "Origin" to "https://player.twitch.tv"
    )

    /**
     * Start estimate for adaptive streaming. ExoPlayer's default estimate made the Fire TV Stick start
     * low and stay there; a minimum video size of 1080p "fixed" that by turning
     * adaptation off, so weak Wi-Fi buffered instead of stepping down (docs/bug_review.md B6). With a
     * high estimate the first segments come from the top variant and ABR can still step down.
     */
    private const val INITIAL_BITRATE_ESTIMATE = 12_000_000L

    private fun createLiveTrackSelectionFactory(): AdaptiveTrackSelection.Factory =
        AdaptiveTrackSelection.Factory(
            /* minDurationForQualityIncreaseMs = */ 1_000,
            /* maxDurationForQualityDecreaseMs = */ 2_000,
            /* minDurationToRetainAfterDiscardMs = */ 2_000,
            /* bandwidthFraction = */ 0.85f
        )

    fun createMainPlayer(context: Context): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context, createLiveTrackSelectionFactory()).apply {
            setParameters(buildUponParameters().setMaxVideoSize(1920, 1080).setForceHighestSupportedBitrate(true))
        }
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(INITIAL_BITRATE_ESTIMATE)
            .build()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setDefaultRequestProperties(TWITCH_HEADERS)
            .setTransferListener(bandwidthMeter)

        val mediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
            .setPlaylistParserFactory(TwitchHlsPlaylistParserFactory())

        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    fun createPreviewPlayer(context: Context): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context, createLiveTrackSelectionFactory()).apply {
            setParameters(buildUponParameters().setMaxVideoSize(1920, 1080).setForceHighestSupportedBitrate(true))
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 5_000,
                /* maxBufferMs = */ 15_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .build()

        val mediaSourceFactory = HlsMediaSource.Factory(
            DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setDefaultRequestProperties(TWITCH_HEADERS)
        ).setPlaylistParserFactory(TwitchHlsPlaylistParserFactory())

        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply { volume = 0f }
    }

    fun createMultiviewPlayer(context: Context): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context, createLiveTrackSelectionFactory()).apply {
            setParameters(buildUponParameters().setMaxVideoSize(1920, 1080).setForceHighestSupportedBitrate(true))
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 5_000,
                /* maxBufferMs = */ 15_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .build()

        val mediaSourceFactory = HlsMediaSource.Factory(
            DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setDefaultRequestProperties(TWITCH_HEADERS)
        ).setPlaylistParserFactory(TwitchHlsPlaylistParserFactory())

        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply { volume = 0f }
    }
}
