package com.shrug.shrugtv.ui.tv.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashRulesTest {
    @Test
    fun staysUntilReadyAndMinimumTime() {
        assertFalse(SplashRules.hide(elapsedMs = 300, ready = true, minMs = 600))
        assertFalse(SplashRules.hide(elapsedMs = 2_000, ready = false, minMs = 600))
        assertTrue(SplashRules.hide(elapsedMs = 600, ready = true, minMs = 600))
        assertTrue(SplashRules.hide(elapsedMs = 0, ready = true, minMs = 0))
    }

    @Test
    fun neverLongerThanTheMaximum() {
        assertTrue(SplashRules.hide(elapsedMs = SplashRules.MAX_MS, ready = false, minMs = 600))
    }

    @Test
    fun unknownStyleMeansDefault() {
        assertEquals(SplashRules.Style.FULL, SplashRules.Style.of(null))
        assertEquals(SplashRules.Style.FULL, SplashRules.Style.of("rubbish"))
        assertEquals(SplashRules.Style.OFF, SplashRules.Style.of("off"))
    }

    @Test
    fun greetingByHour() {
        assertEquals(SplashRules.Greeting.MORNING, SplashRules.greeting(7))
        assertEquals(SplashRules.Greeting.DAY, SplashRules.greeting(14))
        assertEquals(SplashRules.Greeting.EVENING, SplashRules.greeting(20))
        assertEquals(SplashRules.Greeting.NIGHT, SplashRules.greeting(2))
        assertEquals(SplashRules.Greeting.NIGHT, SplashRules.greeting(23))
    }
}
