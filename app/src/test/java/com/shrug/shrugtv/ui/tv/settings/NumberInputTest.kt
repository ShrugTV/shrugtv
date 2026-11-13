package com.shrug.shrugtv.ui.tv.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** Number dialog of the settings. */
class NumberInputTest {
    @Test
    fun `typing keeps digits only and no more than the maximum has`() {
        assertEquals("12", NumberInput.digits("1a2-", 300))
        assertEquals("123", NumberInput.digits("12345", 300))
        assertEquals("", NumberInput.digits("x", 300))
    }

    @Test
    fun `left and right change the number by one step inside the limits`() {
        assertEquals("83", NumberInput.step("78", 1, 5, 20, 300, 78))
        assertEquals("73", NumberInput.step("78", -1, 5, 20, 300, 78))
        assertEquals("300", NumberInput.step("298", 1, 5, 20, 300, 78))
        assertEquals("20", NumberInput.step("22", -1, 5, 20, 300, 78))
    }

    @Test
    fun `an empty field steps from the default`() {
        assertEquals("83", NumberInput.step("", 1, 5, 20, 300, 78))
    }

    @Test
    fun `saving takes the typed number inside the limits, the default for nothing`() {
        assertEquals(150, NumberInput.parse("150", 20, 300, 78))
        assertEquals(20, NumberInput.parse("5", 20, 300, 78))
        assertEquals(300, NumberInput.parse("999", 20, 300, 78))
        assertEquals(78, NumberInput.parse("", 20, 300, 78))
    }
}
