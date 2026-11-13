package com.shrug.shrugtv.player

import android.os.Handler
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer
import com.shrug.shrugtv.model.Movie

/**
 * Handover of the running hero preview from the home screen to the player.
 *
 * "Watch" used to restart the stream in the player (Fire TV: source, playlist and first segment, about
 * 1.2-1.5 s until the first picture) although the preview was already playing the same stream. Now the
 * preview hands over its ExoPlayer, the player takes it in initPlayers. Both activities run in the same
 * process on the main thread, so the ExoPlayer may move. If it is not picked up within [DEADLINE_MS]
 * (player not started), it is released.
 */
object HeroHandover {
    private const val DEADLINE_MS = 10_000L
    private val clock = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null

    /** Set by the hero preview: hands over its player if it is currently playing this channel. */
    var provider: ((Movie) -> ExoPlayer?)? = null

    /** Home screen before starting the player: have the preview hand over and keep it ready. */
    @JvmStatic
    fun prepare(m: Movie): Boolean {
        val p = provider?.invoke(m) ?: return false
        discard()
        player = p
        clock.postDelayed({ discard() }, DEADLINE_MS)
        return true
    }

    /** PlayerActivity: take over the prepared player (or null). */
    @JvmStatic
    fun take(): ExoPlayer? {
        clock.removeCallbacksAndMessages(null)
        val p = player
        player = null
        return p
    }

    private fun discard() {
        clock.removeCallbacksAndMessages(null)
        player?.release()
        player = null
    }
}
