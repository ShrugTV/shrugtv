package com.shrug.shrugtv.player

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * System media session on the main player: play/pause/stop from outside
 * (Google TV app on the phone, voice command, Home Assistant), title/channel/game/avatar for the launcher.
 *
 * Deliberately android.media.session.MediaSession instead of Media3 MediaSession: Media3 1.11.1 asks on
 * every media command for the sender (MediaSessionCompat.getCurrentControllerInfo) and threw on the
 * Fire TV "packageName should be nonempty", because Fire OS does not pass the package name; the app
 * crashed on every "Pause" from outside (release 1.7.0). This class asks for no sender.
 * Metadata comes from the MediaItem (PlayerActivity.metadataFor).
 */
class PlayerMediaSession(context: Context, private var player: ExoPlayer) : Player.Listener {

    private val session = MediaSession(context, "ShrugTV")

    init {
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                player.play()
            }

            override fun onPause() {
                player.pause()
            }

            override fun onStop() {
                player.pause()
            }
        })
        player.addListener(this)
        reportState()
        reportMetadata(player.mediaMetadata)
        session.isActive = true
    }

    /** Re-attach to another player (instant switch from the channel switcher). */
    fun switchPlayer(updated: ExoPlayer) {
        if (updated == player) return
        player.removeListener(this)
        player = updated
        player.addListener(this)
        reportState()
        reportMetadata(player.mediaMetadata)
    }

    fun release() {
        player.removeListener(this)
        session.isActive = false
        session.release()
    }

    override fun onEvents(p: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY
            )
        ) {
            reportState()
        }
        if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
            reportMetadata(p.mediaMetadata)
        }
    }

    private fun reportState() {
        val state = when (player.playbackState) {
            Player.STATE_BUFFERING -> if (player.playWhenReady) PlaybackState.STATE_BUFFERING else PlaybackState.STATE_PAUSED
            Player.STATE_READY -> if (player.playWhenReady) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackState.STATE_STOPPED
            else -> PlaybackState.STATE_NONE
        }
        val position = if (player.isCurrentMediaItemLive) PlaybackState.PLAYBACK_POSITION_UNKNOWN else player.currentPosition
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE
                            or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP
                )
                .setState(state, position, if (state == PlaybackState.STATE_PLAYING) 1f else 0f)
                .build()
        )
    }

    private fun reportMetadata(m: androidx.media3.common.MediaMetadata) {
        val b = MediaMetadata.Builder()
        if (m.title != null) {
            b.putString(MediaMetadata.METADATA_KEY_TITLE, m.title.toString())
            b.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, m.title.toString())
        }
        if (m.artist != null) {
            b.putString(MediaMetadata.METADATA_KEY_ARTIST, m.artist.toString())
            b.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, m.artist.toString())
        }
        if (m.albumTitle != null) {
            b.putString(MediaMetadata.METADATA_KEY_ALBUM, m.albumTitle.toString())
            b.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, m.albumTitle.toString())
        }
        val image = m.artworkUri
        if (image != null) {
            b.putString(MediaMetadata.METADATA_KEY_ART_URI, image.toString())
            b.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, image.toString())
        }
        val duration = player.duration
        if (duration != C.TIME_UNSET && !player.isCurrentMediaItemLive) {
            b.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
        }
        session.setMetadata(b.build())
    }
}
