package com.shrug.shrugtv.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Figures of the statistics page. */
class WatchStatsTest {
    private val today = LocalDate.of(2026, 11, 4)
    private var nextId = 1L

    private fun s(day: String, sec: Long, ch: Int = 1, name: String = "A", cat: String = "Chess", vod: Boolean = false, hour: Int = 20) =
        WatchSessionData(nextId++, ch, name.lowercase(), name, cat, vod, sec, day, hour)

    private fun compute(rows: List<WatchSessionData>, range: StatsRange, oldest: String? = null) =
        WatchStats.compute(rows, range, today, (oldest ?: rows.minOfOrNull { it.day })?.let { LocalDate.parse(it) })

    @Test
    fun `week covers seven days up to and including today`() {
        assertEquals(LocalDate.of(2026, 10, 29) to today, StatsRange.WEEK.bounds(today))
        val rows = listOf(s("2026-10-28", 3600), s("2026-10-29", 3600), s("2026-11-04", 3600), s("2026-11-05", 3600))
        assertEquals(7200L, compute(rows, StatsRange.WEEK).totalSec)
    }

    @Test
    fun `thirty days and a year reach back thirty and 365 days`() {
        assertEquals(LocalDate.of(2026, 10, 6), StatsRange.DAYS_30.bounds(today).first)
        assertEquals(LocalDate.of(2025, 11, 5), StatsRange.YEAR.bounds(today).first)
    }

    @Test
    fun `the previous period is the same length right before`() {
        assertEquals(LocalDate.of(2026, 10, 22) to LocalDate.of(2026, 10, 28), StatsRange.WEEK.previous(today))
        assertEquals(LocalDate.of(2026, 9, 6) to LocalDate.of(2026, 10, 5), StatsRange.DAYS_30.previous(today))
    }

    @Test
    fun `a calendar month has its own borders, also in February of a leap year`() {
        val m = StatsRange.Month(YearMonth.of(2028, 2))
        assertEquals(LocalDate.of(2028, 2, 1) to LocalDate.of(2028, 2, 29), m.bounds(today))
        assertEquals(LocalDate.of(2028, 1, 1) to LocalDate.of(2028, 1, 31), m.previous(today))
    }

    @Test
    fun `month borders include the first and the last day`() {
        val rows = listOf(s("2026-09-30", 100), s("2026-10-01", 200), s("2026-10-31", 400), s("2026-11-01", 800))
        assertEquals(600L, compute(rows, StatsRange.Month(YearMonth.of(2026, 10))).totalSec)
    }

    @Test
    fun `days, sessions, longest and the live share add up`() {
        val rows = listOf(
            s("2026-11-01", 3600), s("2026-11-01", 1800, vod = true), s("2026-11-03", 7200)
        )
        val p = compute(rows, StatsRange.WEEK)
        assertEquals(12_600L, p.totalSec)
        assertEquals(2, p.daysWatched)
        assertEquals(3, p.sessions)
        assertEquals(7200L, p.longestSec)
        assertEquals(10_800L, p.liveSec)
        assertEquals(1800L, p.vodSec)
    }

    @Test
    fun `top channels are summed by channel, sorted, and cut at five`() {
        val rows = (1..7).map { s("2026-11-02", it * 100L, ch = it, name = "C$it") } + s("2026-11-03", 1000, ch = 7, name = "C7")
        val top = compute(rows, StatsRange.WEEK).topChannels
        assertEquals(5, top.size)
        assertEquals(Ranked("C7", 1700), top[0])
        assertEquals("C6", top[1].name)
    }

    @Test
    fun `a channel without id is grouped by login and a blank category is left out`() {
        val rows = listOf(s("2026-11-02", 100, ch = 0, name = "Zed", cat = ""), s("2026-11-03", 200, ch = 0, name = "Zed", cat = ""))
        val p = compute(rows, StatsRange.WEEK)
        assertEquals(listOf(Ranked("Zed", 300)), p.topChannels)
        assertTrue(p.topCategories.isEmpty())
    }

    @Test
    fun `hours of the day sum the playing time of sessions started in that hour`() {
        val p = compute(listOf(s("2026-11-02", 600, hour = 20), s("2026-11-03", 900, hour = 20), s("2026-11-03", 100, hour = 7)), StatsRange.WEEK)
        assertEquals(1500L, p.secPerHour[20])
        assertEquals(20, p.peakHour)
    }

    @Test
    fun `comparison is only offered if there is data before the period`() {
        val rows = listOf(s("2026-11-02", 3600))
        assertEquals(-1L, compute(rows, StatsRange.WEEK).previousTotalSec)
        val withHistory = rows + s("2026-10-25", 1800)
        assertEquals(1800L, compute(withHistory, StatsRange.WEEK).previousTotalSec)
    }

    @Test
    fun `an empty period is empty and has no peak hour`() {
        val p = compute(emptyList(), StatsRange.WEEK)
        assertTrue(p.isEmpty)
        assertNull(p.peakHour)
        assertEquals(0L, p.totalSec)
    }

    @Test
    fun `the month picker runs from the oldest month with data to now`() {
        val months = WatchStats.months(LocalDate.of(2026, 8, 17), today)
        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11)), months)
        assertEquals(listOf(YearMonth.of(2026, 11)), WatchStats.months(null, today))
        assertFalse(WatchStats.months(LocalDate.of(2024, 1, 1), today).isEmpty())
    }
}
