package com.shrug.shrugtv.stats

import android.content.Context
import androidx.preference.PreferenceManager
import java.util.Locale

/** The period the page opens with (setting "Opens with"). */
enum class StatsPeriod(val value: String) { WEEK("week"), DAYS_30("days30"), YEAR("year"), MONTH("month") }

/** How times are written: 1 h 35 min, 1:35 h, 1.6 h. */
enum class TimeFormat(val value: String) { HOURS_MINUTES("hm"), COLON("colon"), DECIMAL("decimal") }

enum class ActivityStyle(val value: String) { BARS("bars"), LINE("line") }

/** The time of day as 24 hours or as four parts of the day. */
enum class HoursStyle(val value: String) { HOURS("hours"), DAY_PARTS("dayparts") }

enum class LiveStyle(val value: String) { RING("ring"), BAR("bar") }

/**
 * How the statistics page looks, set in Settings -> Home -> Statistics page. Pure parsing, so wrong or
 * missing values fall back to the defaults and the tests need no Android.
 */
data class StatsPrefs(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val timeFormat: TimeFormat = TimeFormat.HOURS_MINUTES,
    val compare: Boolean = true,
    val showTiles: Boolean = true,
    val showActivity: Boolean = true,
    val showHours: Boolean = true,
    val showChannels: Boolean = true,
    val showCategories: Boolean = true,
    val showLive: Boolean = true,
    val showClips: Boolean = true,
    val topCount: Int = 5,
    val rankBy: RankBy = RankBy.TIME,
    val activityStyle: ActivityStyle = ActivityStyle.BARS,
    val highlightBusiest: Boolean = true,
    val hoursStyle: HoursStyle = HoursStyle.HOURS,
    val liveStyle: LiveStyle = LiveStyle.RING
) {
    companion object {
        const val PERIOD = "pref_stats_default_period"
        const val TIME_FORMAT = "pref_stats_time_format"
        const val COMPARE = "pref_stats_compare"
        const val SHOW_TILES = "pref_stats_show_tiles"
        const val SHOW_ACTIVITY = "pref_stats_show_activity"
        const val SHOW_HOURS = "pref_stats_show_hours"
        const val SHOW_CHANNELS = "pref_stats_show_channels"
        const val SHOW_CATEGORIES = "pref_stats_show_categories"
        const val SHOW_LIVE = "pref_stats_show_live"
        const val SHOW_CLIPS = "pref_stats_show_clips"
        const val TOP_COUNT = "pref_stats_top_count"
        const val RANK_BY = "pref_stats_rank_by"
        const val ACTIVITY_STYLE = "pref_stats_activity_style"
        const val HIGHLIGHT = "pref_stats_highlight_busiest"
        const val HOURS_STYLE = "pref_stats_hours_style"
        const val LIVE_STYLE = "pref_stats_live_style"
        const val KEEP_MONTHS = "pref_stats_keep_months"

        /** Allowed lengths of the top lists. */
        val TOP_COUNTS = listOf(3, 5, 8, 10)

        /** How long sessions are kept in months; the default is the decision of 2026-11-04. */
        val KEEP_CHOICES = listOf(12, 25, 60)

        @JvmStatic
        fun parse(string: (String) -> String?, bool: (String, Boolean) -> Boolean): StatsPrefs {
            val d = StatsPrefs()
            fun <T> choice(key: String, all: Array<T>, valueOf: (T) -> String, default: T): T =
                string(key)?.let { v -> all.firstOrNull { valueOf(it) == v } } ?: default
            return StatsPrefs(
                period = choice(PERIOD, StatsPeriod.values(), { it.value }, d.period),
                timeFormat = choice(TIME_FORMAT, TimeFormat.values(), { it.value }, d.timeFormat),
                compare = bool(COMPARE, d.compare),
                showTiles = bool(SHOW_TILES, d.showTiles),
                showActivity = bool(SHOW_ACTIVITY, d.showActivity),
                showHours = bool(SHOW_HOURS, d.showHours),
                showChannels = bool(SHOW_CHANNELS, d.showChannels),
                showCategories = bool(SHOW_CATEGORIES, d.showCategories),
                showLive = bool(SHOW_LIVE, d.showLive),
                showClips = bool(SHOW_CLIPS, d.showClips),
                topCount = string(TOP_COUNT)?.toIntOrNull()?.takeIf { it in TOP_COUNTS } ?: d.topCount,
                rankBy = choice(RANK_BY, arrayOf(RankBy.TIME, RankBy.SESSIONS), { it.name.lowercase() }, d.rankBy),
                activityStyle = choice(ACTIVITY_STYLE, ActivityStyle.values(), { it.value }, d.activityStyle),
                highlightBusiest = bool(HIGHLIGHT, d.highlightBusiest),
                hoursStyle = choice(HOURS_STYLE, HoursStyle.values(), { it.value }, d.hoursStyle),
                liveStyle = choice(LIVE_STYLE, LiveStyle.values(), { it.value }, d.liveStyle)
            )
        }

        @JvmStatic
        fun from(context: Context): StatsPrefs {
            val p = PreferenceManager.getDefaultSharedPreferences(context)
            return parse({ k -> p.all[k]?.toString() }, { k, def -> runCatching { p.getBoolean(k, def) }.getOrDefault(def) })
        }

        /** Months to keep: one of [KEEP_CHOICES], otherwise the default of 25. */
        @JvmStatic
        fun keepMonths(context: Context): Long =
            PreferenceManager.getDefaultSharedPreferences(context).all[KEEP_MONTHS]?.toString()?.toIntOrNull()?.takeIf { it in KEEP_CHOICES }?.toLong() ?: 25L
    }
}

/** 95 min -> "1 h 35 min" / "1:35 h" / "1.6 h"; under a minute "< 1 min" in every format. */
/** Like [formatDuration], but under a minute in seconds ("34 s"): the length of a clip. */
fun formatShortDuration(sec: Long, format: TimeFormat, locale: Locale = Locale.getDefault()): String =
    if (sec < 60) "$sec s" else formatDuration(sec, format, locale)

fun formatDuration(sec: Long, format: TimeFormat, locale: Locale = Locale.getDefault()): String {
    val min = sec / 60
    return when {
        min < 1 -> "< 1 min"
        format == TimeFormat.COLON -> "${min / 60}:${"%02d".format(min % 60)} h"
        format == TimeFormat.DECIMAL -> String.format(locale, "%.1f h", sec / 3600.0)
        min < 60 -> "$min min"
        else -> "${min / 60} h ${"%02d".format(min % 60)} min"
    }
}
