package com.shrug.shrugtv.player

import com.shrug.shrugtv.player.PlaybackHealth.Kind
import com.shrug.shrugtv.player.PlaybackHealth.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHealthTest {
    /** Feeds one check per second; returns every verdict that is not Ok. */
    private fun run(h: PlaybackHealth, from: Long, seconds: Int, wants: Boolean, buffering: Boolean, ready: Boolean, pos: (Long) -> Long): List<Verdict> =
        (0 until seconds).map { from + it * 1000L }.map { t -> h.check(t, wants, buffering, ready, pos(t)) }.filter { it != Verdict.Ok }

    @Test
    fun healthyPlaybackReportsNothing() {
        val h = PlaybackHealth()
        assertTrue(run(h, 0, 120, true, false, true) { it }.isEmpty())
    }

    @Test
    fun shortBufferingIsLoggedButNotReloaded() {
        val h = PlaybackHealth()
        run(h, 0, 5, true, false, true) { it }
        val v = run(h, 5_000, 4, true, true, false) { 5_000 } + run(h, 9_000, 3, true, false, true) { it }
        assertEquals(listOf(Verdict.StallStarted(Kind.BUFFERING), Verdict.StallEnded(Kind.BUFFERING, 4_000)), v)
    }

    @Test
    fun loadingBeforeTheFirstPictureIsNoStall() {
        val h = PlaybackHealth()
        assertTrue(run(h, 0, 30, true, true, false) { 0 }.isEmpty())
    }

    @Test
    fun longBufferingReloadsAfterTenSeconds() {
        val h = PlaybackHealth()
        run(h, -1_000, 1, true, false, true) { 0 }
        val v = run(h, 0, 12, true, true, false) { 0 }
        assertEquals(Verdict.StallStarted(Kind.BUFFERING), v[0])
        assertEquals(Verdict.Reload(Kind.BUFFERING, 10_000, 1), v[1])
    }

    @Test
    fun frozenPictureCountsFromTheLastMovement() {
        val h = PlaybackHealth()
        run(h, 0, 5, true, false, true) { it }
        // position stops at 4 000
        val v = run(h, 5_000, 12, true, false, true) { 4_000 }
        assertEquals(Verdict.StallStarted(Kind.FROZEN), v[0])
        assertEquals(Verdict.Reload(Kind.FROZEN, 10_000, 1), v[1])
    }

    @Test
    fun pausedOrAdBreakIsNoStall() {
        val h = PlaybackHealth()
        assertTrue(run(h, 0, 60, false, true, false) { 0 }.isEmpty())
        assertTrue(run(h, 60_000, 60, false, false, true) { 0 }.isEmpty())
    }

    @Test
    fun givesUpAfterThreeReloadsAndOnlyOnce() {
        val h = PlaybackHealth()
        // after each reload the stream shows a picture for a second, then hangs again
        val v = (0 until 4).flatMap { k ->
            run(h, k * 20_000L, 1, true, false, true) { 0 } + run(h, k * 20_000L + 1_000, 12, true, true, false) { 0 }
        }
        assertEquals(listOf(1, 2, 3), v.filterIsInstance<Verdict.Reload>().map { it.attempt })
        assertEquals(1, v.count { it is Verdict.GiveUp })
        assertTrue(run(h, 90_000, 30, true, true, false) { 0 }.isEmpty())
    }

    @Test
    fun aMinuteOfHealthyPlaybackResetsTheCounter() {
        val h = PlaybackHealth()
        run(h, -1_000, 1, true, false, true) { 0 }
        run(h, 0, 12, true, true, false) { 0 } // reload 1
        run(h, 12_000, 61, true, false, true) { it }
        val v = run(h, 73_000, 12, true, true, false) { 0 }
        assertEquals(1, v.filterIsInstance<Verdict.Reload>().single().attempt)
    }

    @Test
    fun newItemResetsTheCounter() {
        val h = PlaybackHealth()
        repeat(4) { k -> run(h, k * 20_000L, 1, true, false, true) { 0 }; run(h, k * 20_000L + 1_000, 12, true, true, false) { 0 } } // gave up
        h.reset(newItem = true)
        run(h, 89_000, 1, true, false, true) { 0 }
        val v = run(h, 90_000, 12, true, true, false) { 0 }
        assertEquals(1, v.filterIsInstance<Verdict.Reload>().single().attempt)
    }

    @Test
    fun aPauseIsNoStandstill() {
        val h = PlaybackHealth()
        run(h, 0, 5, true, false, true) { it } // playing
        run(h, 5_000, 120, false, false, true) { 5_000 } // paused (Home button) for two minutes
        val v = run(h, 125_000, 2, true, false, true) { 5_000 } // playing again, position still there for a moment
        assertTrue(v.none { it is Verdict.StallStarted })
    }
}
