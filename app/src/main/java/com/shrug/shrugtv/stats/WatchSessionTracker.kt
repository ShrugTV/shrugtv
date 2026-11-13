package com.shrug.shrugtv.stats

import java.time.Instant
import java.time.ZoneId

/** One watched stretch of one channel or recording. */
data class WatchSessionData(
    /** Epoch ms of the start; also the primary key, so a checkpoint replaces the row it wrote before. */
    val id: Long,
    val channelId: Int,
    val login: String,
    val channelName: String,
    val category: String,
    val isVod: Boolean,
    val playedSec: Long,
    /** Local date of the start, `yyyy-MM-dd`. */
    val day: String,
    /** Local hour of the start, 0-23. */
    val hour: Int
)

/**
 * Counts how long the picture actually played, per channel or recording.
 *
 * Played time is the wall time during which the player reported "playing", not the playback
 * position: a live stream has no useful position and seeking would falsify it. Buffering and pause
 * do not count. A session under [MIN_SECONDS] is dropped (zapping through channels), a running one is
 * written every [CHECKPOINT_MS] by [tick], so the plug being pulled costs at most that minute.
 *
 * No Android here: the clocks, the switch and the writing are handed in, so it runs in a unit test.
 */
class WatchSessionTracker(
    private val elapsedMs: () -> Long,
    private val epochMs: () -> Long,
    private val enabled: () -> Boolean,
    private val save: (WatchSessionData) -> Unit,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) {
    private class Open(val start: WatchSessionData) {
        var playedMs = 0L
        var playingSince = -1L
    }

    private var open: Open? = null

    /** Starts a session; an open one ends first. Nothing is recorded while the switch is off. */
    fun start(channelId: Int, login: String, channelName: String, category: String, isVod: Boolean, playingNow: Boolean) {
        stop()
        if (!enabled()) return
        val at = epochMs()
        val local = Instant.ofEpochMilli(at).atZone(zone())
        open = Open(
            WatchSessionData(
                id = at, channelId = channelId, login = login, channelName = channelName, category = category,
                isVod = isVod, playedSec = 0, day = local.toLocalDate().toString(), hour = local.hour
            )
        ).also { if (playingNow) it.playingSince = elapsedMs() }
    }

    /** The player started or stopped playing. */
    fun playing(isPlaying: Boolean) {
        val s = open ?: return
        val now = elapsedMs()
        if (isPlaying) {
            if (s.playingSince < 0) s.playingSince = now
        } else if (s.playingSince >= 0) {
            s.playedMs += now - s.playingSince
            s.playingSince = -1
        }
    }

    /** Checkpoint: writes the running session if it is long enough. */
    fun tick() {
        val s = open ?: return
        if (!enabled()) return
        val sec = playedSec(s)
        if (sec >= MIN_SECONDS) save(s.start.copy(playedSec = sec))
    }

    /** Ends the session and writes it if it is long enough. */
    fun stop() {
        val s = open ?: return
        open = null
        if (!enabled()) return
        val sec = playedSec(s)
        if (sec >= MIN_SECONDS) save(s.start.copy(playedSec = sec))
    }

    private fun playedSec(s: Open): Long {
        val running = if (s.playingSince >= 0) elapsedMs() - s.playingSince else 0L
        return (s.playedMs + running) / 1000
    }

    companion object {
        /** Shorter sessions do not count (decided 2026-11-04). */
        const val MIN_SECONDS = 60L
        const val CHECKPOINT_MS = 60_000L
    }
}
