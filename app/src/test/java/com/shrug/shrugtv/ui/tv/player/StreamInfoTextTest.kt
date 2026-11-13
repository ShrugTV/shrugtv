package com.shrug.shrugtv.ui.tv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamInfoTextTest {
    @Test
    fun `uptime as hours and minutes`() {
        assertEquals("3:56", StreamInfoText.uptime(3 * 3600 + 56 * 60 + 12))
        assertEquals("0:05", StreamInfoText.uptime(5 * 60))
        assertEquals("12:00", StreamInfoText.uptime(12 * 3600))
        assertEquals("0:00", StreamInfoText.uptime(-10))
    }

    @Test
    fun `delay behind live`() {
        assertEquals("0:42", StreamInfoText.behind(42))
        assertEquals("12:05", StreamInfoText.behind(12 * 60 + 5))
        assertEquals("1:02:03", StreamInfoText.behind(3723))
    }

    @Test
    fun `box art and language`() {
        assertEquals("https://static-cdn.jtvnw.net/ttv-boxart/27471-52x72.jpg", StreamInfoText.boxArtUrl(27471))
        assertNull(StreamInfoText.boxArtUrl(0))
        assertEquals("DE", StreamInfoText.language("DE"))
        assertNull(StreamInfoText.language("N/A"))
        assertNull(StreamInfoText.language(null))
    }
}
