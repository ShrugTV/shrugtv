package com.shrug.shrugtv.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class StatsPrefsTest {
    private fun parse(strings: Map<String, String> = emptyMap(), bools: Map<String, Boolean> = emptyMap()) =
        StatsPrefs.parse({ strings[it] }, { k, d -> bools[k] ?: d })

    @Test
    fun `without settings everything is as before the customizing`() {
        assertEquals(StatsPrefs(), parse())
        assertEquals(StatsPeriod.WEEK, parse().period)
        assertEquals(5, parse().topCount)
        assertTrue(parse().showLive && parse().compare && parse().showClips)
    }

    @Test
    fun `every choice is read, wrong values fall back`() {
        val p = parse(
            mapOf(StatsPrefs.PERIOD to "year", StatsPrefs.TIME_FORMAT to "decimal", StatsPrefs.TOP_COUNT to "8", StatsPrefs.RANK_BY to "sessions",
                StatsPrefs.ACTIVITY_STYLE to "line", StatsPrefs.HOURS_STYLE to "dayparts", StatsPrefs.LIVE_STYLE to "bar"),
            mapOf(StatsPrefs.SHOW_HOURS to false, StatsPrefs.COMPARE to false, StatsPrefs.HIGHLIGHT to false)
        )
        assertEquals(StatsPeriod.YEAR, p.period)
        assertEquals(TimeFormat.DECIMAL, p.timeFormat)
        assertEquals(8, p.topCount)
        assertEquals(RankBy.SESSIONS, p.rankBy)
        assertEquals(ActivityStyle.LINE, p.activityStyle)
        assertEquals(HoursStyle.DAY_PARTS, p.hoursStyle)
        assertEquals(LiveStyle.BAR, p.liveStyle)
        assertFalse(p.showHours || p.compare || p.highlightBusiest)
        val bad = parse(mapOf(StatsPrefs.PERIOD to "decade", StatsPrefs.TOP_COUNT to "7", StatsPrefs.RANK_BY to "x"))
        assertEquals(StatsPeriod.WEEK, bad.period)
        assertEquals(5, bad.topCount)
        assertEquals(RankBy.TIME, bad.rankBy)
    }

    @Test
    fun `times are written in the chosen format`() {
        assertEquals("1 h 35 min", formatDuration(95 * 60L, TimeFormat.HOURS_MINUTES))
        assertEquals("35 min", formatDuration(35 * 60L, TimeFormat.HOURS_MINUTES))
        assertEquals("1:35 h", formatDuration(95 * 60L, TimeFormat.COLON))
        assertEquals("0:35 h", formatDuration(35 * 60L, TimeFormat.COLON))
        assertEquals("1.6 h", formatDuration(95 * 60L, TimeFormat.DECIMAL, Locale.US))
        assertEquals("1,6 h", formatDuration(95 * 60L, TimeFormat.DECIMAL, Locale.GERMANY))
        assertEquals("< 1 min", formatDuration(40, TimeFormat.DECIMAL))
    }
}
