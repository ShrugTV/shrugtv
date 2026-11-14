package com.shrug.shrugtv.ha

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * The followed channels as one JSON message for Home Assistant: who is live right now (name, game,
 * title, viewers) and the names of all channels followed. No pictures and nothing else, so every channel fits. Pure, so
 * the size limit and the shrinking are unit-tested.
 *
 * HA keeps sensor attributes only up to 16 KB; the message stays below [MAX_BYTES]. With 70 followed channels it is about
 * 4 KB. Should it ever not fit, the live titles are cut shorter, then left out, then the games; only as a last resort the
 * followed list is shortened (the counts stay true).
 */
object FollowedList {
    const val MAX_BYTES = 14_000

    data class Item(
        val login: String,
        val name: String,
        val game: String = "",
        val title: String = "",
        val viewers: Int = 0,
        /** Marked with the star in ShrugTV: a dashboard shows these first. */
        val favorite: Boolean = false
    )

    /** [live] are the streams on air now, [followed] every followed channel; [nowMs] goes into `updated`. */
    @JvmStatic
    fun build(live: List<Item>, followed: List<Item>, nowMs: Long): String {
        val liveLogins = live.map { it.login.lowercase() }.toSet()
        for (level in 0..3) {
            val json = json(live, followed, liveLogins, nowMs, level)
            if (json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) return json
        }
        // Still too big: shorten the lists, followed first, until it fits
        var l = live
        var f = followed
        while (true) {
            if (f.size > 20) f = f.take(f.size * 2 / 3) else if (l.size > 10) l = l.take(l.size * 2 / 3) else if (f.isNotEmpty()) f = f.take(f.size / 2) else l = l.take(l.size / 2)
            val json = json(l, f, liveLogins, nowMs, 3, liveCount = live.size, followedCount = followed.size)
            if (json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES || (l.isEmpty() && f.isEmpty())) return json
        }
    }

    /** Level 0: full titles (120 characters); 1: titles of 40; 2: no titles; 3: no games either. */
    private fun json(
        live: List<Item>, followed: List<Item>, liveLogins: Set<String>, nowMs: Long, level: Int,
        liveCount: Int = live.size, followedCount: Int = followed.size
    ): String = JsonObject().apply {
        addProperty("updated", nowMs)
        addProperty("live_count", liveCount)
        addProperty("followed_count", followedCount)
        add("live", JsonArray().apply {
            for (i in live) add(JsonObject().apply {
                addProperty("login", i.login)
                addProperty("name", i.name)
                if (level < 3) addProperty("game", i.game)
                if (level < 2) addProperty("title", i.title.take(if (level == 0) 120 else 40))
                addProperty("viewers", i.viewers)
                if (i.favorite) addProperty("favorite", true)
            })
        })
        add("followed", JsonArray().apply {
            for (i in followed) add(JsonObject().apply {
                addProperty("login", i.login)
                addProperty("name", i.name)
                addProperty("live", i.login.lowercase() in liveLogins)
                if (i.favorite) addProperty("favorite", true)
            })
        })
    }.toString()

    /** The message without `updated`: equal contents are not sent again. */
    @JvmStatic
    fun signature(live: List<Item>, followed: List<Item>): String = build(live, followed, 0L)
}
