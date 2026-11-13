package com.shrug.shrugtv.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** Counting of watch time. */
class WatchSessionTrackerTest {
    private var elapsed = 0L
    private var epoch = 1_762_200_000_000L // 2025-11-03 20:00 UTC
    private var on = true
    private val saved = mutableListOf<WatchSessionData>()

    private val tracker = WatchSessionTracker(
        elapsedMs = { elapsed }, epochMs = { epoch }, enabled = { on }, save = { saved += it },
        zone = { ZoneId.of("UTC") }
    )

    private fun begin(playing: Boolean = true, vod: Boolean = false) =
        tracker.start(7, "lirik", "LIRIK", "Just Chatting", vod, playing)

    private fun pass(sec: Long) { elapsed += sec * 1000; epoch += sec * 1000 }

    @Test
    fun `played time counts while playing, not while paused or buffering`() {
        begin()
        pass(100)
        tracker.playing(false)
        pass(500)
        tracker.playing(true)
        pass(50)
        tracker.stop()
        assertEquals(1, saved.size)
        assertEquals(150L, saved.single().playedSec)
    }

    @Test
    fun `a session under a minute is dropped`() {
        begin()
        pass(59)
        tracker.stop()
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `a session of exactly a minute counts`() {
        begin()
        pass(60)
        tracker.stop()
        assertEquals(60L, saved.single().playedSec)
    }

    @Test
    fun `a session that never played is not written`() {
        begin(playing = false)
        pass(600)
        tracker.stop()
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `a checkpoint writes the running session and the end replaces the same row`() {
        begin()
        pass(90)
        tracker.tick()
        pass(60)
        tracker.stop()
        assertEquals(2, saved.size)
        assertEquals(saved[0].id, saved[1].id)
        assertEquals(90L, saved[0].playedSec)
        assertEquals(150L, saved[1].playedSec)
    }

    @Test
    fun `a checkpoint before a minute writes nothing`() {
        begin()
        pass(30)
        tracker.tick()
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `starting the next channel ends the first`() {
        begin()
        pass(120)
        tracker.start(8, "zack", "Zack", "Chess", true, true)
        pass(200)
        tracker.stop()
        assertEquals(listOf(120L, 200L), saved.map { it.playedSec })
        assertEquals(listOf(false, true), saved.map { it.isVod })
    }

    @Test
    fun `nothing is recorded while the switch is off`() {
        on = false
        begin()
        pass(600)
        tracker.tick()
        tracker.stop()
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `day and hour are the local ones of the start`() {
        val berlin = WatchSessionTracker({ elapsed }, { epoch }, { true }, { saved += it }, { ZoneId.of("Europe/Berlin") })
        berlin.start(7, "lirik", "LIRIK", "", false, true)
        pass(120)
        berlin.stop()
        // 20:00 UTC on 2025-11-03 is 21:00 in Berlin (UTC+1 in November)
        assertEquals("2025-11-03", saved.single().day)
        assertEquals(21, saved.single().hour)
    }
}
