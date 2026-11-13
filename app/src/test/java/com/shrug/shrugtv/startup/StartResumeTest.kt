package com.shrug.shrugtv.startup

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartResumeTest {
    @Test
    fun loginIsNormalisedAndChecked() {
        assertEquals("papaplatte", StartResume.validLogin("  PapaPlatte "))
        assertEquals("a_b1", StartResume.validLogin("a_b1"))
        assertNull(StartResume.validLogin(null))
        assertNull(StartResume.validLogin(""))
        assertNull(StartResume.validLogin("a&user_login=b"))
        assertNull(StartResume.validLogin("x".repeat(26)))
    }

    @Test
    fun resumesOnlyOnAFreshLauncherStartWithTheSettingOn() {
        val main = Intent.ACTION_MAIN
        assertTrue(StartResume.shouldResume(true, false, true, main, "bonjwa"))
        assertFalse("setting off", StartResume.shouldResume(false, false, true, main, "bonjwa"))
        assertFalse("already handled", StartResume.shouldResume(true, true, true, main, "bonjwa"))
        assertFalse("recreated", StartResume.shouldResume(true, false, false, main, "bonjwa"))
        assertFalse("deep link", StartResume.shouldResume(true, false, true, Intent.ACTION_VIEW, "bonjwa"))
        assertFalse("nothing saved", StartResume.shouldResume(true, false, true, main, null))
        assertFalse("bad value", StartResume.shouldResume(true, false, true, main, "a b"))
    }
}
