package com.shrug.shrugtv.stats

import java.time.LocalDate
import java.time.YearMonth

/** The periods the statistics page shows. */
sealed class StatsRange {
    /** First and last day, both inclusive, as seen from [today]. */
    abstract fun bounds(today: LocalDate): Pair<LocalDate, LocalDate>

    /** The period of the same kind right before this one, for the comparison. */
    abstract fun previous(today: LocalDate): Pair<LocalDate, LocalDate>

    /** The last [days] days up to and including today. */
    class Rolling(val days: Int) : StatsRange() {
        override fun bounds(today: LocalDate) = today.minusDays(days - 1L) to today
        override fun previous(today: LocalDate) = today.minusDays(2L * days - 1) to today.minusDays(days.toLong())
    }

    /** One calendar month. */
    class Month(val month: YearMonth) : StatsRange() {
        override fun bounds(today: LocalDate) = month.atDay(1) to month.atEndOfMonth()
        override fun previous(today: LocalDate) = month.minusMonths(1).let { it.atDay(1) to it.atEndOfMonth() }
    }

    companion object {
        val WEEK: StatsRange = Rolling(7)
        val DAYS_30: StatsRange = Rolling(30)
        val YEAR: StatsRange = Rolling(365)
    }
}

data class Ranked(val name: String, val seconds: Long)

/** Figures of one period. */
data class PeriodStats(
    val totalSec: Long,
    val daysWatched: Int,
    val sessions: Int,
    val topChannels: List<Ranked>,
    val topCategories: List<Ranked>,
    val liveSec: Long,
    val vodSec: Long,
    val longestSec: Long,
    /** Seconds per local hour of the day at which the session started (24 entries). */
    val secPerHour: LongArray,
    /** Total of the period right before this one; negative = nothing to compare (no data before). */
    val previousTotalSec: Long
) {
    val isEmpty: Boolean get() = sessions == 0

    /** The hour with the most playing time, null without data. */
    val peakHour: Int? get() = secPerHour.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index

    override fun equals(other: Any?): Boolean = other is PeriodStats && other.totalSec == totalSec &&
        other.daysWatched == daysWatched && other.sessions == sessions && other.topChannels == topChannels &&
        other.topCategories == topCategories && other.liveSec == liveSec && other.vodSec == vodSec &&
        other.longestSec == longestSec && other.secPerHour.contentEquals(secPerHour) && other.previousTotalSec == previousTotalSec

    override fun hashCode(): Int = totalSec.hashCode() * 31 + sessions
}

/** Turns stored sessions into the figures of the page; pure Kotlin, tested without Android. */
object WatchStats {
    const val TOP = 5

    /**
     * [rows] must contain at least the sessions of the period and of the one before it. [oldest] is the
     * first day with any data: the comparison is only meaningful if the previous period reaches back
     * that far.
     */
    fun compute(rows: List<WatchSessionData>, range: StatsRange, today: LocalDate, oldest: LocalDate?): PeriodStats {
        val (from, to) = range.bounds(today)
        val (pFrom, pTo) = range.previous(today)
        val inRange = rows.filter { inside(it, from, to) }
        val before = rows.filter { inside(it, pFrom, pTo) }.sumOf { it.playedSec }
        val comparable = oldest != null && !oldest.isAfter(pTo)

        val perHour = LongArray(24)
        inRange.forEach { perHour[it.hour.coerceIn(0, 23)] += it.playedSec }

        return PeriodStats(
            totalSec = inRange.sumOf { it.playedSec },
            daysWatched = inRange.map { it.day }.distinct().size,
            sessions = inRange.size,
            topChannels = rank(inRange, { (if (it.channelId > 0) "id:${it.channelId}" else "login:${it.login.lowercase()}") }) {
                it.channelName.ifBlank { it.login }
            },
            topCategories = rank(inRange.filter { it.category.isNotBlank() }, { it.category.lowercase() }) { it.category },
            liveSec = inRange.filter { !it.isVod }.sumOf { it.playedSec },
            vodSec = inRange.filter { it.isVod }.sumOf { it.playedSec },
            longestSec = inRange.maxOfOrNull { it.playedSec } ?: 0L,
            secPerHour = perHour,
            previousTotalSec = if (comparable) before else -1L
        )
    }

    /** The oldest month that has data and the current one, for the month picker. */
    fun months(oldest: LocalDate?, today: LocalDate): List<YearMonth> {
        val last = YearMonth.from(today)
        val first = oldest?.let { YearMonth.from(it) } ?: last
        return generateSequence(first) { if (it < last) it.plusMonths(1) else null }.toList()
    }

    private fun inside(s: WatchSessionData, from: LocalDate, to: LocalDate): Boolean {
        val d = LocalDate.parse(s.day)
        return !d.isBefore(from) && !d.isAfter(to)
    }

    private fun rank(rows: List<WatchSessionData>, key: (WatchSessionData) -> String, label: (WatchSessionData) -> String): List<Ranked> =
        rows.groupBy(key).values
            .map { group -> Ranked(label(group.maxByOrNull { it.id }!!), group.sumOf { it.playedSec }) }
            .sortedWith(compareByDescending<Ranked> { it.seconds }.thenBy { it.name.lowercase() })
            .take(TOP)
}
