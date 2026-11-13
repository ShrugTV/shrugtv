package com.shrug.shrugtv.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    @Test fun `off is never quiet`() = assertFalse(QuietHours.isQuiet(false, 22, 7, 23))

    @Test fun `a window across midnight covers evening and early morning`() {
        assertTrue(QuietHours.isQuiet(true, 22, 7, 23))
        assertTrue(QuietHours.isQuiet(true, 22, 7, 3))
        assertTrue(QuietHours.isQuiet(true, 22, 7, 22))
        assertFalse(QuietHours.isQuiet(true, 22, 7, 7))
        assertFalse(QuietHours.isQuiet(true, 22, 7, 15))
    }

    @Test fun `a window inside one day`() {
        assertTrue(QuietHours.isQuiet(true, 9, 17, 9))
        assertFalse(QuietHours.isQuiet(true, 9, 17, 17))
        assertFalse(QuietHours.isQuiet(true, 9, 17, 8))
    }

    @Test fun `equal ends mean the whole day`() = assertTrue(QuietHours.isQuiet(true, 5, 5, 12))
}
