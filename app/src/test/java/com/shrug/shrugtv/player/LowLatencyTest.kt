package com.shrug.shrugtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Usher URL handling for low latency. */
class LowLatencyTest {
    private val base = "https://usher.ttvnw.net/api/channel/hls/x.m3u8?player=twitchweb&token=t&sig=s&p=3"

    @Test
    fun addsAndRemovesParameterOnce() {
        val on = LowLatency.usherUrl(base, true)
        assertEquals("$base&fast_bread=true", on)
        assertEquals(on, LowLatency.usherUrl(on, true))
        assertEquals(base, LowLatency.usherUrl(on, false))
    }

    @Test
    fun cachedUrlMustMatchSetting() {
        assertTrue(LowLatency.matches(LowLatency.usherUrl(base, true), true))
        assertFalse(LowLatency.matches(base, true))
        assertTrue(LowLatency.matches(base, false))
    }
}
