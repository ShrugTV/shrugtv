package com.shrug.shrugtv.stats

import android.content.Context
import com.shrug.shrugtv.database.WatchSession
import com.shrug.shrugtv.database.WatchStatsDatabase
import java.time.LocalDate

/**
 * Reading and writing of the watch statistics. All calls block on the database:
 * run them off the main thread.
 */
object WatchStatsStore {
    /** How long sessions are kept (decided 2026-11-04). */
    const val KEEP_MONTHS = 25L

    private fun dao(context: Context) = WatchStatsDatabase.getInstance(context)?.watchSessionDao()

    fun save(context: Context, s: WatchSessionData) {
        dao(context)?.upsert(WatchSession().apply {
            id = s.id
            channelId = s.channelId
            channelLogin = s.login
            channelName = s.channelName
            category = s.category
            isVod = s.isVod
            isClip = s.isClip
            playedSec = s.playedSec
            day = s.day
            hour = s.hour
        })
    }

    fun rows(context: Context, from: LocalDate, to: LocalDate): List<WatchSessionData> =
        dao(context)?.between(from.toString(), to.toString())?.map {
            WatchSessionData(
                id = it.id, channelId = it.channelId, login = it.channelLogin ?: "", channelName = it.channelName ?: "",
                category = it.category ?: "", isVod = it.isVod, isClip = it.isClip, playedSec = it.playedSec, day = it.day, hour = it.hour
            )
        } ?: emptyList()

    fun oldest(context: Context): LocalDate? = dao(context)?.oldestDay()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Rows of the period and of the one before it, plus the first day with data. */
    fun load(context: Context, range: StatsRange, today: LocalDate): Pair<List<WatchSessionData>, LocalDate?> {
        val (from, _) = range.previous(today)
        val (_, to) = range.bounds(today)
        return rows(context, from, to) to oldest(context)
    }

    fun clear(context: Context) {
        dao(context)?.deleteAll()
    }

    /** Once at app start: drops what is older than the months to keep (setting, default [KEEP_MONTHS]). */
    fun prune(context: Context, today: LocalDate) {
        dao(context)?.deleteBefore(today.minusMonths(StatsPrefs.keepMonths(context)).toString())
    }
}
