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

/** One line of a top list: [seconds] watched in [sessions] sessions. */
data class Ranked(val name: String, val seconds: Long, val sessions: Int = 0)

/** What a top list is sorted by (setting "Rank by"). */
enum class RankBy { TIME, SESSIONS }

/** What one bar of the activity chart stands for. */
enum class BucketUnit { DAY, MONTH }

/** Playing time of one day (or one month, [BucketUnit.MONTH]) starting at [start]. */
data class Bucket(val start: LocalDate, val seconds: Long)

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
    val previousTotalSec: Long,
    /** One bar per day of the period, or per month for the year view ( makeover). */
    val buckets: List<Bucket> = emptyList(),
    val bucketUnit: BucketUnit = BucketUnit.DAY,
    /** Clips watched in the period, the time they played, and the channels most of them came from. */
    val clipCount: Int = 0,
    val clipSec: Long = 0L,
    val topClipChannels: List<Ranked> = emptyList()
) {
    /** Average length of a clip watched, 0 without clips. */
    val clipAverageSec: Long get() = if (clipCount > 0) clipSec / clipCount else 0L

    val isEmpty: Boolean get() = sessions == 0

    /** The bar with the most playing time, null without data. */
    val busiest: Bucket? get() = buckets.maxByOrNull { it.seconds }?.takeIf { it.seconds > 0 }

    /** The hour with the most playing time, null without data. */
    val peakHour: Int? get() = secPerHour.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index

    override fun equals(other: Any?): Boolean = other is PeriodStats && other.totalSec == totalSec &&
        other.daysWatched == daysWatched && other.sessions == sessions && other.topChannels == topChannels &&
        other.topCategories == topCategories && other.liveSec == liveSec && other.vodSec == vodSec &&
        other.longestSec == longestSec && other.secPerHour.contentEquals(secPerHour) && other.previousTotalSec == previousTotalSec &&
        other.buckets == buckets && other.bucketUnit == bucketUnit && other.clipCount == clipCount && other.clipSec == clipSec &&
        other.topClipChannels == topClipChannels

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
    fun compute(
        rows: List<WatchSessionData>, range: StatsRange, today: LocalDate, oldest: LocalDate?,
        top: Int = TOP, rankBy: RankBy = RankBy.TIME
    ): PeriodStats {
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
            topChannels = rank(inRange, top, rankBy, { (if (it.channelId > 0) "id:${it.channelId}" else "login:${it.login.lowercase()}") }) {
                it.channelName.ifBlank { it.login }
            },
            topCategories = rank(inRange.filter { it.category.isNotBlank() }, top, rankBy, { it.category.lowercase() }) { it.category },
            liveSec = inRange.filter { !it.isVod }.sumOf { it.playedSec },
            // Clips are recordings too, but have their own figures
            vodSec = inRange.filter { it.isVod && !it.isClip }.sumOf { it.playedSec },
            longestSec = inRange.maxOfOrNull { it.playedSec } ?: 0L,
            secPerHour = perHour,
            previousTotalSec = if (comparable) before else -1L,
            buckets = buckets(inRange, range, from, to),
            bucketUnit = if (monthly(range)) BucketUnit.MONTH else BucketUnit.DAY,
            clipCount = inRange.count { it.isClip },
            clipSec = inRange.filter { it.isClip }.sumOf { it.playedSec },
            topClipChannels = rank(inRange.filter { it.isClip }, top, RankBy.SESSIONS, { (if (it.channelId > 0) "id:${it.channelId}" else "login:${it.login.lowercase()}") }) {
                it.channelName.ifBlank { it.login }
            }
        )
    }

    /** More than two months wide: one bar per month, otherwise one per day. */
    private fun monthly(range: StatsRange) = range is StatsRange.Rolling && range.days > 62

    private fun buckets(rows: List<WatchSessionData>, range: StatsRange, from: LocalDate, to: LocalDate): List<Bucket> {
        if (monthly(range)) {
            val sums = rows.groupBy { YearMonth.from(LocalDate.parse(it.day)) }.mapValues { e -> e.value.sumOf { it.playedSec } }
            return generateSequence(YearMonth.from(from)) { if (it < YearMonth.from(to)) it.plusMonths(1) else null }
                .map { Bucket(it.atDay(1), sums[it] ?: 0L) }.toList()
        }
        val sums = rows.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.playedSec } }
        return generateSequence(from) { if (it < to) it.plusDays(1) else null }.map { Bucket(it, sums[it.toString()] ?: 0L) }.toList()
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

    /** The four parts of the day (night, morning, afternoon, evening: six hours each) from the 24 hours. */
    @JvmStatic
    fun dayParts(secPerHour: LongArray): LongArray = LongArray(4) { part -> (part * 6 until part * 6 + 6).sumOf { secPerHour.getOrElse(it) { 0L } } }

    private fun rank(
        rows: List<WatchSessionData>, top: Int, rankBy: RankBy, key: (WatchSessionData) -> String, label: (WatchSessionData) -> String
    ): List<Ranked> =
        rows.groupBy(key).values
            .map { group -> Ranked(label(group.maxByOrNull { it.id }!!), group.sumOf { it.playedSec }, group.size) }
            .sortedWith(
                (if (rankBy == RankBy.SESSIONS) compareByDescending<Ranked> { it.sessions }.thenByDescending { it.seconds }
                else compareByDescending<Ranked> { it.seconds }).thenBy { it.name.lowercase() }
            )
            .take(top.coerceIn(1, 20))
}
