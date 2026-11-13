package com.shrug.shrugtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rewinding a live stream into its recording. */
class LiveRewindTest {
    private val start = 1_762_000_000_000L

    private fun c(id: Long, stream: String, offsetMs: Long?) = LiveRewind.Candidate(id, stream, offsetMs?.let { start + it })

    @Test
    fun `the recording of the stream is found by the stream id first`() {
        val videos = listOf(c(1, "old", -86_400_000), c(2, "322136234586", 4_000), c(3, "other", 2_000))
        assertEquals(2L, LiveRewind.match("322136234586", start, videos)?.videoId)
    }

    @Test
    fun `without a matching stream id the one that started closest within three minutes counts`() {
        val videos = listOf(c(1, "a", 170_000), c(2, "b", -20_000), c(3, "c", 400_000))
        assertEquals(2L, LiveRewind.match("", start, videos)?.videoId)
        assertEquals(2L, LiveRewind.match("unknown", start, videos)?.videoId)
    }

    @Test
    fun `no recording if nothing is close or the start is unknown`() {
        assertNull(LiveRewind.match("x", start, listOf(c(1, "a", 600_000), c(2, "b", null))))
        assertNull(LiveRewind.match("", null, listOf(c(1, "a", 0))))
        assertNull(LiveRewind.match("x", start, emptyList()))
    }

    @Test
    fun `the live timeline ends at the live offset before now`() {
        val t = LiveRewind.liveTimeline(nowMs = start + 3_600_000, streamStartMs = start, liveOffsetMs = 22_000)
        assertEquals(3_578_000L, t[0])
        assertEquals(3_600_000L, t[1])
    }

    @Test
    fun `an unknown or negative live offset and a start in the future do not break the timeline`() {
        assertEquals(3_600_000L, LiveRewind.liveTimeline(start + 3_600_000, start, -1)[0])
        val t = LiveRewind.liveTimeline(start - 5_000, start, 0)
        assertEquals(0L, t[0])
        assertEquals(0L, t[1])
    }

    @Test
    fun `small steps back stay in the live window`() {
        val p = LiveRewind.planFromLive(-10_000, positionMs = 3_578_000, durationMs = 3_600_000, windowPositionMs = 26_000)
        assertTrue(p is LiveRewind.Plan.InWindow)
        assertEquals(-10_000L, (p as LiveRewind.Plan.InWindow).deltaMs)
    }

    @Test
    fun `forward in live always stays in the live window`() {
        assertTrue(LiveRewind.planFromLive(30_000, 3_578_000, 3_600_000, 5_000) is LiveRewind.Plan.InWindow)
    }

    @Test
    fun `a step that would leave the live window goes into the recording at the right place`() {
        val p = LiveRewind.planFromLive(-300_000, positionMs = 3_578_000, durationMs = 3_600_000, windowPositionMs = 26_000)
        assertTrue(p is LiveRewind.Plan.IntoArchive)
        assertEquals(3_278_000L, (p as LiveRewind.Plan.IntoArchive).targetMs)
    }

    @Test
    fun `the window margin counts, so the next step from the window start already uses the recording`() {
        val p = LiveRewind.planFromLive(-10_000, positionMs = 3_000_000, durationMs = 3_600_000, windowPositionMs = 11_000)
        assertTrue(p is LiveRewind.Plan.IntoArchive)
    }

    @Test
    fun `close to the end of the recording the live window is used as far back as it goes`() {
        val p = LiveRewind.planFromLive(-15_000, positionMs = 3_590_000, durationMs = 3_600_000, windowPositionMs = 10_000)
        assertTrue(p is LiveRewind.Plan.InWindow)
        assertEquals(-8_000L, (p as LiveRewind.Plan.InWindow).deltaMs)
    }

    @Test
    fun `the target never goes before the start of the stream`() {
        val p = LiveRewind.planFromLive(-300_000, positionMs = 100_000, durationMs = 3_600_000, windowPositionMs = 5_000)
        assertEquals(0L, (p as LiveRewind.Plan.IntoArchive).targetMs)
    }

    @Test
    fun `in the recording a seek stays in it, and near its end it returns to live`() {
        val inside = LiveRewind.planInArchive(60_000, positionMs = 600_000, durationMs = 3_600_000)
        assertEquals(660_000L, (inside as LiveRewind.Plan.InArchive).targetMs)
        assertTrue(LiveRewind.planInArchive(60_000, 3_550_000, 3_600_000) is LiveRewind.Plan.BackToLive)
        assertTrue(LiveRewind.planInArchive(20_000, 3_500_000, 3_600_000) is LiveRewind.Plan.InArchive)
        // backwards stays in the recording, even in its last minute
        assertEquals(3_560_000L, (LiveRewind.planInArchive(-10_000, 3_570_000, 3_600_000) as LiveRewind.Plan.InArchive).targetMs)
        assertEquals(0L, (LiveRewind.planInArchive(-60_000, 10_000, 3_600_000) as LiveRewind.Plan.InArchive).targetMs)
    }

    @Test
    fun `playing into the last seconds of the recording leaves it`() {
        assertTrue(LiveRewind.shouldLeaveArchive(3_590_000, 3_600_000))
        assertFalse(LiveRewind.shouldLeaveArchive(3_500_000, 3_600_000))
        assertFalse(LiveRewind.shouldLeaveArchive(10_000, 0))
    }
}
