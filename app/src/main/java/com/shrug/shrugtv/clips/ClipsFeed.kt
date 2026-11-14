package com.shrug.shrugtv.clips

import android.content.Context
import androidx.preference.PreferenceManager
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.apihelpers.FollowedChannels
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.coroutines.resume

/** How far back clips count. [days] null = no limit. */
enum class ClipsPeriod(val key: String, val days: Long?) {
    DAY("day", 1), WEEK("week", 7), MONTH("month", 30), YEAR("year", 365), ALL("all", null);

    companion object {
        fun of(key: String?) = values().firstOrNull { it.key == key } ?: WEEK
    }
}

/** Whose clips are shown: every followed channel or only the starred ones. */
enum class ClipsSource(val key: String) {
    FOLLOWED("followed"), FAVORITES("favorites");

    companion object {
        fun of(key: String?) = values().firstOrNull { it.key == key } ?: FOLLOWED
    }
}

/** What happens to clips that were opened before. */
enum class ClipsSeenMode(val key: String) {
    SHOW("show"), DIM("dim"), HIDE("hide");

    companion object {
        /** Dimmed is the default (Michael, 2026-11-14). */
        fun of(key: String?) = values().firstOrNull { it.key == key } ?: DIM
    }
}

enum class ClipsSort(val key: String) {
    /** [RELATIVE]: views compared with the other clips of the same channel, so big and small channels meet on equal terms. */
    VIEWS("views"), RELATIVE("relative"), NEWEST("newest"), SHORTEST("shortest");

    companion object {
        fun of(key: String?) = values().firstOrNull { it.key == key } ?: VIEWS
    }
}

/** The settings of the clips page and row, read from the preferences (pref_clips_*). */
data class ClipsSettings(
    val source: ClipsSource = ClipsSource.FOLLOWED,
    val period: ClipsPeriod = ClipsPeriod.WEEK,
    val sort: ClipsSort = ClipsSort.VIEWS,
    val count: Int = 20,
    /** Show, dim or hide clips that were opened before. */
    val seen: ClipsSeenMode = ClipsSeenMode.DIM,
    /** After a clip ends, the next one of the list starts. */
    val autoplay: Boolean = true,
    /** Clips that were opened before are left out of that queue. */
    val queueSkipsSeen: Boolean = true,
    /** Only clips in the stream language of the app settings ("app"), or every language ("all"). */
    val onlyAppLanguage: Boolean = false,
) {
    companion object {
        const val PREF_SOURCE = "pref_clips_source"
        const val PREF_PERIOD = "pref_clips_period"
        const val PREF_SORT = "pref_clips_sort"
        const val PREF_COUNT = "pref_clips_count"
        const val PREF_ROW = "pref_clips_row"
        const val PREF_SEEN = "pref_clips_seen"
        const val PREF_AUTOPLAY = "pref_clips_autoplay"
        const val PREF_QUEUE_SEEN = "pref_clips_queue_seen"
        /** The first version of this option (1.45.0) was a switch: on meant hide. */
        const val PREF_HIDE_SEEN_OLD = "pref_clips_hide_seen"
        const val PREF_LANGUAGE = "pref_clips_language"

        fun load(ctx: Context): ClipsSettings {
            val p = PreferenceManager.getDefaultSharedPreferences(ctx)
            return ClipsSettings(
                ClipsSource.of(p.getString(PREF_SOURCE, null)),
                ClipsPeriod.of(p.getString(PREF_PERIOD, null)),
                ClipsSort.of(p.getString(PREF_SORT, null)),
                (p.getString(PREF_COUNT, null)?.toIntOrNull() ?: 20).coerceIn(10, 100),
                p.getString(PREF_SEEN, null)?.let { ClipsSeenMode.of(it) }
                    ?: if (p.getBoolean(PREF_HIDE_SEEN_OLD, false)) ClipsSeenMode.HIDE else ClipsSeenMode.DIM,
                p.getBoolean(PREF_AUTOPLAY, true),
                p.getString(PREF_QUEUE_SEEN, "skip") != "include",
                p.getString(PREF_LANGUAGE, "all") == "app",
            )
        }

        fun rowOn(ctx: Context) = PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_ROW, true)
    }
}

/**
 * The best clips of the channels you follow, in one list. Twitch hands out clips for one channel per
 * request, so this asks for each channel (a few at a time), puts the answers together and sorts them. The pure parts
 * ([startedAt], [channels], [merge]) are tested; [load] is the network wrapper around them.
 */
object ClipsFeed {
    /** More channels than this are not asked: favorites first, then the rest in the order of the follow list. */
    const val MAX_CHANNELS = 100

    /** Clips asked for per channel; together they make the pool the page and the queue draw from. */
    const val PER_CHANNEL = 30

    /** Sorting relative to the channel needs more clips of each channel to know what is usual there. */
    const val PER_CHANNEL_RELATIVE = 50

    /** The pool is cut to the best this many clips after merging, so memory stays small. */
    const val POOL_LIMIT = 500

    /** How many clips show after the page grew by one step: [step] more, never more than the pool holds. */
    fun grow(shown: Int, step: Int, total: Int): Int = minOf(total, shown + step.coerceAtLeast(1))
    private const val PARALLEL = 6
    private const val CACHE_MS = 10 * 60 * 1000L

    /** RFC 3339 start of the period for the `started_at` parameter; null = no limit. */
    fun startedAt(period: ClipsPeriod, now: Instant): String? =
        period.days?.let { now.minus(Duration.ofDays(it)).truncatedTo(ChronoUnit.SECONDS).toString() }

    /** The channels to ask: favorites only, or all, at most [MAX_CHANNELS] and favorites in front. */
    fun channels(followed: List<Movie>, source: ClipsSource): List<Movie> {
        val usable = followed.filter { it.userId > 0 }
        val chosen = if (source == ClipsSource.FAVORITES) usable.filter { it.isFavorite } else usable
        return (chosen.filter { it.isFavorite } + chosen.filter { !it.isFavorite }).take(MAX_CHANNELS)
    }

    /**
     * The answers of all channels as one list: every clip once, without [seen] ones and, with [language], only clips in
     * that language (blank = any), sorted, at most [limit].
     */
    fun merge(lists: List<List<Movie>>, sort: ClipsSort, limit: Int, seen: Set<String> = emptySet(), language: String = ""): List<Movie> {
        val score = if (sort == ClipsSort.RELATIVE) relativeScores(lists) else emptyMap()
        val all = lists.flatten().distinctBy { it.clipSlug }
            .filter { it.clipSlug !in seen }
            .filter { language.isBlank() || it.languageCode.equals(language, ignoreCase = true) }
        val sorted = when (sort) {
            ClipsSort.VIEWS -> all.sortedByDescending { it.viewCount }
            ClipsSort.RELATIVE -> all.sortedByDescending { score[it.clipSlug] ?: 0.0 }
            ClipsSort.NEWEST -> all.sortedByDescending { it.publishedAt?.toInstant() ?: Instant.EPOCH }
            // Clips without a length (0) go last
            ClipsSort.SHORTEST -> all.sortedBy { if (it.durationSeconds > 0) it.durationSeconds else Int.MAX_VALUE }
        }
        return sorted.take(limit)
    }

    /**
     * Views of each clip divided by what is usual for its channel: the middle value of that channel's clips in the period
     * (with fewer than five clips the average). A clip with 3 000 views in a small channel whose clips usually have 300 then
     * beats one with 20 000 views in a big channel where 10 000 is normal.
     */
    internal fun relativeScores(lists: List<List<Movie>>): Map<String, Double> {
        val out = HashMap<String, Double>()
        for (list in lists) {
            if (list.isEmpty()) continue
            val views = list.map { it.viewCount.toDouble() }.sorted()
            val usual = maxOf(1.0, if (views.size >= 5) views[(views.size - 1) / 2] else views.average())
            for (c in list) out.putIfAbsent(c.clipSlug, c.viewCount / usual)
        }
        return out
    }

    /** The last few pools by settings, so switching back and forth between the choices on the page is instant. */
    private val cache = object : LinkedHashMap<String, Pair<Long, List<Movie>>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, List<Movie>>>?) = size > 4
    }
    @Volatile private var lastPool: List<Movie> = emptyList()

    /** The same settings within ten minutes answer from memory (the home screen and the page share it). */
    fun forget() { synchronized(cache) { cache.clear() } }

    /** The pool of the last [load] (empty before): the queue of the home row plays on through all of it. */
    fun pool(): List<Movie> = lastPool

    suspend fun load(ctx: Context, s: ClipsSettings, force: Boolean = false): List<Movie> {
        val seenAll = if (s.seen != ClipsSeenMode.SHOW) ClipsSeen.all(ctx) else emptySet()
        val seen = if (s.seen == ClipsSeenMode.HIDE) seenAll else emptySet()
        val language = if (s.onlyAppLanguage) com.shrug.shrugtv.session.AppState.streamLanguageCode.orEmpty() else ""
        val key = "${com.shrug.shrugtv.session.AppState.userId}|${s.source}|${s.period}|${s.sort}|$language|${s.seen}|${seenAll.size}"
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(cache) { cache[key] }?.let { (at, list) ->
            if (!force && now - at < CACHE_MS) { lastPool = list; return list }
        }
        val followed = suspendCancellableCoroutine<List<Movie>> { cont ->
            FollowedChannels.getList(ctx) { list -> if (cont.isActive) cont.resume(list?.toList() ?: emptyList()) }
        }
        val asked = channels(followed, s.source)
        val from = startedAt(s.period, Instant.now())
        val gate = Semaphore(PARALLEL)
        val lists = withContext(Dispatchers.IO) {
            coroutineScope {
                asked.map { ch ->
                    async {
                        gate.withPermit {
                            runCatching {
                                val data = HelixApi.getClipsOfBroadcasterSync(ctx, ch.userId.toString(), from, if (s.sort == ClipsSort.RELATIVE) PER_CHANNEL_RELATIVE else PER_CHANNEL)?.get("data")
                                if (data != null && data.isJsonArray) {
                                    HelixParser.parseClips(data.asJsonArray, Enums.RowType.VOD_CHANNEL_HIGHLIGHTS, ch.avatarUrl ?: "")
                                } else emptyList<Movie>()
                            }.getOrDefault(emptyList())
                        }
                    }
                }.awaitAll()
            }
        }
        // The whole pool, best first; the page shows [ClipsSettings.count] of it and grows while you scroll
        val result = merge(lists, s.sort, POOL_LIMIT, seen, language)
        // Dimming: every clip knows whether it was opened before (the card draws it dimmer)
        for (c in result) c.clipSeen = s.seen == ClipsSeenMode.DIM && c.clipSlug in seenAll
        // A failed run (no answer from any channel) is not kept, the next call asks again
        if (result.isNotEmpty()) {
            synchronized(cache) { cache[key] = now to result }
            lastPool = result
        }
        return result
    }
}
