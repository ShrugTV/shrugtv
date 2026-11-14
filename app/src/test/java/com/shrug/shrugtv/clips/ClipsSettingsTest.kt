package com.shrug.shrugtv.clips

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Reading the clips settings, including the switch of version 1.45.0 that became a three-way choice. */
@RunWith(RobolectricTestRunner::class)
class ClipsSettingsTest {
    private lateinit var ctx: Context

    @Before
    fun clear() {
        ctx = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().clear().commit()
    }

    private fun prefs() = PreferenceManager.getDefaultSharedPreferences(ctx).edit()

    @Test
    fun `defaults dim opened clips, play on and skip the opened ones`() {
        val s = ClipsSettings.load(ctx)
        assertEquals(ClipsSeenMode.DIM, s.seen)
        assertEquals(true, s.autoplay)
        assertEquals(true, s.queueSkipsSeen)
    }

    @Test
    fun `autoplay and the queue can be changed`() {
        prefs().putBoolean("pref_clips_autoplay", false).putString("pref_clips_queue_seen", "include").commit()
        val s = ClipsSettings.load(ctx)
        assertEquals(false, s.autoplay)
        assertEquals(false, s.queueSkipsSeen)
    }

    @Test
    fun `the old switch on means hide`() {
        prefs().putBoolean("pref_clips_hide_seen", true).commit()
        assertEquals(ClipsSeenMode.HIDE, ClipsSettings.load(ctx).seen)
    }

    @Test
    fun `the new choice wins over the old switch`() {
        prefs().putBoolean("pref_clips_hide_seen", true).putString("pref_clips_seen", "dim").commit()
        assertEquals(ClipsSeenMode.DIM, ClipsSettings.load(ctx).seen)
    }

    @Test
    fun `sort and period are read from their keys`() {
        prefs().putString("pref_clips_sort", "relative").putString("pref_clips_period", "month").commit()
        val s = ClipsSettings.load(ctx)
        assertEquals(ClipsSort.RELATIVE, s.sort)
        assertEquals(ClipsPeriod.MONTH, s.period)
    }
}
