package com.shrug.shrugtv.player

import com.shrug.shrugtv.model.VodChapter
import com.shrug.shrugtv.util.TimeUtil
import kotlin.math.abs

/** Categories (Twitch "chapters", one per game change) on the timeline of a recording. */
object VodChapters {

    /** Index of the chapter at [positionMs]; -1 before the first one. [chapters] is sorted by position. */
    fun indexAt(chapters: List<VodChapter>, positionMs: Long): Int = chapters.indexOfLast { it.positionMs <= positionMs }

    /** End of chapter [index]: the start of the next one, else its own length, else the end of the video. */
    fun endMs(chapters: List<VodChapter>, index: Int, durationMs: Long): Long {
        val chapter = chapters[index]
        chapters.getOrNull(index + 1)?.let { return it.positionMs }
        val byLength = chapter.positionMs + chapter.durationMs
        return when {
            chapter.durationMs > 0 && durationMs > 0 -> minOf(byLength, durationMs)
            chapter.durationMs > 0 -> byLength
            durationMs > 0 -> durationMs
            else -> chapter.positionMs
        }
    }

    /** "Next category": the following chapter; null on the last one. A second of leeway for rounding. */
    fun next(chapters: List<VodChapter>, positionMs: Long): VodChapter? =
        chapters.firstOrNull { it.positionMs > positionMs + 1000 }

    /**
     * "Previous category", like a music player: more than 5 s into a chapter goes to its start,
     * within the first 5 s to the one before. Null before the first chapter.
     */
    fun previous(chapters: List<VodChapter>, positionMs: Long): VodChapter? {
        val i = indexAt(chapters, positionMs)
        if (i < 0) return null
        val current = chapters[i]
        return if (positionMs - current.positionMs > 5000 || i == 0) current else chapters[i - 1]
    }
}

/**
 * Seek steps that grow while a key is pressed again and again in the same direction: a four-hour
 * recording needs more than ten seconds per press. A pause of [RESET_GAP_MS] or another direction
 * starts at the smallest step again.
 */
class SeekStepper(private val now: () -> Long) {

    /** [stepMs] signed step of this press; [totalMs] signed sum of the presses in a row. */
    class Step(val stepMs: Long, val totalMs: Long)

    private var streak = 0
    private var lastAt = Long.MIN_VALUE
    private var lastDirection = 0
    private var total = 0L

    fun press(direction: Int): Step {
        val t = now()
        if (direction != lastDirection || lastAt == Long.MIN_VALUE || t - lastAt > RESET_GAP_MS) {
            streak = 0
            total = 0L
        }
        lastDirection = direction
        lastAt = t
        val stepMs = STEPS_SECONDS[minOf(streak, STEPS_SECONDS.size - 1)] * 1000L * direction
        streak++
        total += stepMs
        return Step(stepMs, total)
    }

    companion object {
        val STEPS_SECONDS = longArrayOf(10, 10, 10, 20, 30, 60, 120, 180, 300)
        const val RESET_GAP_MS = 800L

        /** "+1:30 → 1:42:10": the sum of the presses and where the playback stands now. */
        fun hudText(totalMs: Long, targetMs: Long): String {
            val sec = abs(totalMs) / 1000
            val delta = if (sec < 60) "$sec s" else TimeUtil.hms(sec).removePrefix("00:").removePrefix("0")
            return (if (totalMs < 0) "−" else "+") + delta + "  →  " + TimeUtil.hms(targetMs / 1000)
        }
    }
}
