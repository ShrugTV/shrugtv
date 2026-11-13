package com.shrug.shrugtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstantReplayTest {
    @Test
    fun returnsAfterThePartHasPlayed() {
        val r = InstantReplay(30_000L)
        repeat(119) { assertFalse(r.tick(250L, playing = true)) }
        assertTrue(r.tick(250L, playing = true))
    }

    @Test
    fun pausesAndBufferingDoNotCount() {
        val r = InstantReplay(1_000L)
        repeat(100) { assertFalse(r.tick(250L, playing = false)) }
        repeat(3) { assertFalse(r.tick(250L, playing = true)) }
        assertTrue(r.tick(250L, playing = true))
    }

    @Test
    fun lengthFromTheSetting() {
        assertEquals(30_000L, InstantReplay.lengthMs(null))
        assertEquals(30_000L, InstantReplay.lengthMs("x"))
        assertEquals(15_000L, InstantReplay.lengthMs("15"))
        assertEquals(120_000L, InstantReplay.lengthMs("999"))
        assertEquals(5_000L, InstantReplay.lengthMs("1"))
    }
}
