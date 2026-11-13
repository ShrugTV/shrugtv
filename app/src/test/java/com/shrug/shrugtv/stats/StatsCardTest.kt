package com.shrug.shrugtv.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Card of the last 30 days on the home screen. */
class StatsCardTest {
    @Test
    fun `due in the first seven days of a month, once`() {
        assertTrue(StatsCardGate.due(LocalDate.of(2026, 12, 1), "2026-11"))
        assertTrue(StatsCardGate.due(LocalDate.of(2026, 12, 7), null))
        assertFalse(StatsCardGate.due(LocalDate.of(2026, 12, 8), "2026-11"))
        assertFalse(StatsCardGate.due(LocalDate.of(2026, 12, 3), "2026-12"))
    }

    @Test
    fun `the month key sorts and names the month`() {
        assertEquals("2026-01", StatsCardGate.monthKey(LocalDate.of(2026, 1, 31)))
    }

    @Test
    fun `no card without data, otherwise total and favourites`() {
        val empty = WatchStats.compute(emptyList(), StatsRange.DAYS_30, LocalDate.of(2026, 12, 2), null)
        assertNull(StatsCardGate.cardFor(empty))
        val rows = listOf(WatchSessionData(1, 5, "a", "Alpha", "Chess", false, 7200, "2026-11-20", 20))
        val card = StatsCardGate.cardFor(WatchStats.compute(rows, StatsRange.DAYS_30, LocalDate.of(2026, 12, 2), LocalDate.of(2026, 11, 20)))
        assertEquals(StatsCardData(7200, "Alpha", "Chess"), card)
    }
}
