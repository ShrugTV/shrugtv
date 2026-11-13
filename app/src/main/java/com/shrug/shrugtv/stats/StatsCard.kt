package com.shrug.shrugtv.stats

import java.time.LocalDate
import java.time.YearMonth

/** What the card on the home screen says. */
data class StatsCardData(val totalSec: Long, val topChannel: String?, val topCategory: String?)

/** When the card of the last 30 days shows on the home screen. */
object StatsCardGate {
    /** Only in the first days of a month; later in the month it would be old news. */
    const val FIRST_DAYS = 7

    /** How long the card stays. */
    const val SHOW_MS = 15_000L

    /** [lastShownMonth] is the `yyyy-MM` the card was shown or checked for last, null if never. */
    @JvmStatic
    fun due(today: LocalDate, lastShownMonth: String?): Boolean =
        today.dayOfMonth <= FIRST_DAYS && lastShownMonth != YearMonth.from(today).toString()

    @JvmStatic
    fun monthKey(today: LocalDate): String = YearMonth.from(today).toString()

    /** The card for the figures of the last 30 days, null if there is nothing to say. */
    @JvmStatic
    fun cardFor(stats: PeriodStats): StatsCardData? =
        if (stats.isEmpty) null else StatsCardData(stats.totalSec, stats.topChannels.firstOrNull()?.name, stats.topCategories.firstOrNull()?.name)
}
