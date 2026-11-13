package com.shrug.shrugtv.ui.tv.player

import android.os.SystemClock

/**
 * Counts the frames animated chat emotes deliver, shown in the player's stats overlay:
 * frames per second over all emotes and the number of different emotes that moved, so "each" can be
 * read off. Main thread only (drawable callbacks and the stats timer both run there).
 */
object EmoteFrameMeter {
    private const val WINDOW_MS = 3_000L
    private var windowStart = SystemClock.uptimeMillis()
    private var frames = 0
    private val seen = HashSet<Int>()

    /** Last finished window: frames per second, number of emotes. */
    var framesPerSecond = 0f
        private set
    var emotes = 0
        private set

    fun tick(drawable: Any) {
        roll()
        frames++
        seen.add(System.identityHashCode(drawable))
    }

    /** Called by the stats timer, so a stalled chat shows 0 instead of the last value. */
    fun roll() {
        val now = SystemClock.uptimeMillis()
        val span = now - windowStart
        if (span >= WINDOW_MS) {
            framesPerSecond = frames * 1000f / span
            emotes = seen.size
            frames = 0
            seen.clear()
            windowStart = now
        }
    }
}
