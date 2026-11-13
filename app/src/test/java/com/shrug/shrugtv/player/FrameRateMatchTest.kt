package com.shrug.shrugtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Choice of display mode for a stream's frame rate. */
class FrameRateMatchTest {
    private val m60 = FrameRateMatch.Mode(1, 3840, 2160, 60f)
    private val m50 = FrameRateMatch.Mode(2, 3840, 2160, 50f)
    private val m24 = FrameRateMatch.Mode(3, 3840, 2160, 23.976f)
    private val hd50 = FrameRateMatch.Mode(4, 1920, 1080, 50f)
    private val m120 = FrameRateMatch.Mode(5, 3840, 2160, 120f)
    private val all = listOf(m60, m50, m24, hd50, m120)

    @Test
    fun switchesTo50ForFiftyFps() = assertEquals(m50, FrameRateMatch.choose(all, m60, 50f))

    @Test
    fun keepsCurrentWhenItFits() {
        assertNull(FrameRateMatch.choose(all, m60, 60f))
        assertNull(FrameRateMatch.choose(all, m60, 30f))
        assertNull(FrameRateMatch.choose(all, m50, 25f))
    }

    @Test
    fun backTo60FromFifty() = assertEquals(m60, FrameRateMatch.choose(all, m50, 60f))

    @Test
    fun onlySameResolution() =
        assertNull(FrameRateMatch.choose(listOf(m60, hd50), m60, 50f))

    @Test
    fun ignoresUnknownFrameRate() = assertNull(FrameRateMatch.choose(all, m60, -1f))
}
