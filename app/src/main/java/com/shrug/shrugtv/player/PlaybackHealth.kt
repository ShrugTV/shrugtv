package com.shrug.shrugtv.player

/**
 * Watches the main player for stalls: the player should play but buffers, or it is READY and
 * the position does not move. Every stall is reported (that is the measurement), and after [stallLimitMs] the caller
 * reloads the stream, at most [maxReloads] times per item; then it gives up.
 *
 * Pure: the caller feeds [check] once a second with the player's state, so it runs in unit tests without a player.
 */
class PlaybackHealth(
    private val stallLimitMs: Long = 10_000L,
    private val maxReloads: Int = 3,
    /** Position change that counts as movement (live positions jump a little when the window moves). */
    private val moveToleranceMs: Long = 200L,
    /** Healthy playback this long resets the reload counter. */
    private val healthyResetMs: Long = 60_000L
) {
    enum class Kind { BUFFERING, FROZEN }

    sealed class Verdict {
        object Ok : Verdict()
        /** A stall began (log it). */
        data class StallStarted(val kind: Kind) : Verdict()
        /** A stall ended by itself after [ms] (log it: the measurement). */
        data class StallEnded(val kind: Kind, val ms: Long) : Verdict()
        /** Reload the stream now; [attempt] counts from 1. */
        data class Reload(val kind: Kind, val ms: Long, val attempt: Int) : Verdict()
        /** Reloading did not help [maxReloads] times: show the error. */
        data class GiveUp(val kind: Kind, val ms: Long) : Verdict()
    }

    private var stallKind: Kind? = null
    private var stallSince = 0L
    private var lastPosition = Long.MIN_VALUE
    private var lastMoveAt = 0L
    private var reloads = 0
    private var healthySince = 0L
    private var gaveUp = false

    /** Loading an item is no stall: only after its first READY does buffering count. */
    private var started = false

    /** New item (zap, archive, reload by the caller): stall timers start again; the counter only for a new item. */
    fun reset(newItem: Boolean) {
        stallKind = null
        lastPosition = Long.MIN_VALUE
        started = false
        if (newItem) {
            reloads = 0
            gaveUp = false
        }
        healthySince = 0L
    }

    /**
     * @param wantsToPlay playWhenReady and nothing else (ad break, loading the recording, paused) holds the picture.
     * @param buffering the player is in STATE_BUFFERING.
     * @param ready the player is in STATE_READY.
     */
    fun check(nowMs: Long, wantsToPlay: Boolean, buffering: Boolean, ready: Boolean, positionMs: Long): Verdict {
        if (gaveUp) return Verdict.Ok
        if (!started) {
            if (!ready) return Verdict.Ok
            started = true
        }
        val moved = lastPosition == Long.MIN_VALUE || kotlin.math.abs(positionMs - lastPosition) > moveToleranceMs
        if (moved) {
            lastPosition = positionMs
            lastMoveAt = nowMs
        }
        // A pause (Home button, ad break, user pause) is no standstill: the frozen timer starts again when playing resumes
        if (!wantsToPlay) lastMoveAt = nowMs
        val kind = when {
            !wantsToPlay -> null
            buffering -> Kind.BUFFERING
            ready && nowMs - lastMoveAt >= FROZEN_AFTER_MS -> Kind.FROZEN
            else -> null
        }
        if (kind == null) {
            val ended = stallKind
            stallKind = null
            if (wantsToPlay && ready) {
                if (healthySince == 0L) healthySince = nowMs
                if (nowMs - healthySince >= healthyResetMs) reloads = 0
            } else {
                healthySince = 0L
            }
            return if (ended != null) Verdict.StallEnded(ended, nowMs - stallSince) else Verdict.Ok
        }
        healthySince = 0L
        if (stallKind == null) {
            stallKind = kind
            // A frozen picture started when the position last moved, a buffering phase now
            stallSince = if (kind == Kind.FROZEN) lastMoveAt else nowMs
            return Verdict.StallStarted(kind)
        }
        stallKind = kind
        val ms = nowMs - stallSince
        if (ms < stallLimitMs) return Verdict.Ok
        if (reloads >= maxReloads) {
            gaveUp = true
            return Verdict.GiveUp(kind, ms)
        }
        reloads++
        stallKind = null
        lastPosition = Long.MIN_VALUE
        started = false
        return Verdict.Reload(kind, ms, reloads)
    }

    companion object {
        /** READY without movement for this long counts as frozen (a single slow segment is no stall). */
        const val FROZEN_AFTER_MS = 3_000L
    }
}
