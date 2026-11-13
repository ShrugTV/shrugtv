package com.shrug.shrugtv.player

/** The recording of the running live stream, found for rewinding. */
data class LiveArchiveInfo(
    val userId: Int,
    val streamId: String,
    val videoId: Long,
    /** Epoch ms at which the stream started; the recording begins a few seconds later. */
    val streamStartMs: Long
)

/**
 * Rules for rewinding a live stream into its recording, without Android so they
 * can be tested. Measured 2026-11-04 on a real stream: the recording is found by the stream id, its
 * length follows the wall clock to within one 10 s segment, the playlist keeps growing (EVENT, no
 * ENDLIST), the live window of the stream is only 32 s long, and 15 s from the end of the growing
 * recording playback stalled while it waited for the next segment.
 */
object LiveRewind {
    /** A recording belongs to the stream if it started this close to it (the stream id is checked first). */
    const val MATCH_TOLERANCE_MS = 3 * 60_000L

    /** Targets closer than this to the end of the recording stay in the live stream. */
    const val EDGE_GUARD_MS = 30_000L

    /** Playing in the recording this close to its end goes back to live by itself. */
    const val ARCHIVE_EXIT_MS = 15_000L

    /** Margin kept at the start of the live window: a seek into it still has to find its segment. */
    const val LIVE_WINDOW_MARGIN_MS = 2_000L

    /** A video of the channel as the lookup sees it. */
    class Candidate(val videoId: Long, val streamId: String, val createdAtMs: Long?)

    /** The recording of the stream: same stream id, else the one that started within the tolerance. */
    @JvmStatic
    fun match(streamId: String, streamStartMs: Long?, videos: List<Candidate>): Candidate? {
        if (streamId.isNotEmpty()) videos.firstOrNull { it.streamId == streamId }?.let { return it }
        if (streamStartMs == null) return null
        return videos
            .filter { it.createdAtMs != null && kotlin.math.abs(it.createdAtMs - streamStartMs) <= MATCH_TOLERANCE_MS }
            .minByOrNull { kotlin.math.abs(it.createdAtMs!! - streamStartMs) }
    }

    /** Where the timeline of a live stream stands: position and length in ms since the stream started. */
    @JvmStatic
    fun liveTimeline(nowMs: Long, streamStartMs: Long, liveOffsetMs: Long): LongArray {
        val duration = (nowMs - streamStartMs).coerceAtLeast(0L)
        val position = (duration - liveOffsetMs.coerceAtLeast(0L)).coerceIn(0L, duration)
        return longArrayOf(position, duration)
    }

    /** What a seek does. */
    sealed class Plan {
        /** Seek by [deltaMs] inside the live window (or the recording, in recording mode: then [targetMs] counts). */
        class InWindow(val deltaMs: Long) : Plan()

        /** From live into the recording at [targetMs]. */
        class IntoArchive(val targetMs: Long) : Plan()

        /** Inside the recording to [targetMs]. */
        class InArchive(val targetMs: Long) : Plan()

        /** Out of the recording, back to the live stream. */
        object BackToLive : Plan()
    }

    /**
     * A seek by [deltaMs] while watching live. [positionMs]/[durationMs] come from [liveTimeline],
     * [windowPositionMs] is the position inside the live window of the player.
     */
    @JvmStatic
    fun planFromLive(deltaMs: Long, positionMs: Long, durationMs: Long, windowPositionMs: Long): Plan {
        if (deltaMs >= 0 || windowPositionMs + deltaMs >= LIVE_WINDOW_MARGIN_MS) return Plan.InWindow(deltaMs)
        val target = (positionMs + deltaMs).coerceIn(0L, durationMs)
        // Too close to the end of the recording to rely on it: as far back as the live window goes
        if (target > durationMs - EDGE_GUARD_MS) return Plan.InWindow(-(windowPositionMs - LIVE_WINDOW_MARGIN_MS).coerceAtLeast(0L))
        return Plan.IntoArchive(target)
    }

    /**
     * A seek by [deltaMs] while watching the recording. Forward into the last minute goes back to
     * live; backwards stays in the recording, also near its end (the user asked for that place).
     */
    @JvmStatic
    fun planInArchive(deltaMs: Long, positionMs: Long, durationMs: Long): Plan {
        val target = (positionMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L))
        return if (deltaMs > 0 && durationMs > 0 && target >= durationMs - EDGE_GUARD_MS) Plan.BackToLive else Plan.InArchive(target)
    }

    /** Playing in the recording and nearly at its end: time to go back to live. */
    @JvmStatic
    fun shouldLeaveArchive(positionMs: Long, durationMs: Long): Boolean =
        durationMs > 0 && durationMs - positionMs < ARCHIVE_EXIT_MS
}
