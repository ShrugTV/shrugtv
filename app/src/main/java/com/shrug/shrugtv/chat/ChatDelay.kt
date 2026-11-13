package com.shrug.shrugtv.chat

/**
 * Delay of live chat events so the chat matches the picture.
 *
 * Chat arrives in real time, the picture several seconds later (7.5 s measured on the Fire TV,
 * ). In automatic mode the delay follows the player's live offset (wall clock minus
 * the `EXT-X-PROGRAM-DATE-TIME` of the playing position), smoothed, minus [VIEWER_LEAD_MS]: viewers who
 * react in chat watch with a latency of their own. A fixed delay is used as it is; 0 switches it off.
 * Due times never go backwards, so messages keep their order when the latency shrinks.
 */
class ChatDelay(
    private val auto: Boolean,
    private val fixedMs: Long,
    private val liveOffsetMs: () -> Long?
) {
    private var smoothedMs = -1L
    private var lastDueAt = 0L

    /** Milliseconds to hold back an event that arrives at [nowMs] (any monotonic clock). */
    fun delayFor(nowMs: Long): Long {
        val target = if (auto) autoTarget() else fixedMs.coerceAtLeast(0)
        val due = maxOf(nowMs + target, lastDueAt)
        lastDueAt = due
        return due - nowMs
    }

    /** The delay in use right now, for the statistics overlay. */
    fun currentMs(): Long = if (auto) currentAutoMs() else fixedMs.coerceAtLeast(0)

    /** The delay automatic mode aims at right now. */
    fun currentAutoMs(): Long = (smoothedMs - VIEWER_LEAD_MS).coerceAtLeast(0)

    private fun autoTarget(): Long {
        val measured = liveOffsetMs()?.let { sane(it) }
        if (measured != null) {
            smoothedMs = if (smoothedMs < 0) measured else (smoothedMs * 3 + measured) / 4
        }
        return currentAutoMs()
    }

    companion object {
        /** Assumed latency of the viewers whose reactions we show; an assumption, not measured. */
        const val VIEWER_LEAD_MS = 2_000L

        /** Offsets beyond this are a broken clock or a missing PROGRAM-DATE-TIME, not latency. */
        const val MAX_MS = 60_000L

        /** The live offset if it is plausible, else null (ExoPlayer reports `C.TIME_UNSET` without one). */
        @JvmStatic
        fun sane(offsetMs: Long): Long? = offsetMs.takeIf { it in 0..MAX_MS }
    }
}
