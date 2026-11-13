package com.shrug.shrugtv.ui.tv.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Uptime on live cards. */
class DurationTest {
    @Test
    fun shortForm() {
        assertEquals("3:51 h", durationShort("03:51"))
        assertEquals("12:05 h", durationShort("12:05"))
        assertEquals("0:07 h", durationShort("00:07"))
        assertEquals("", durationShort(""))
    }
}
