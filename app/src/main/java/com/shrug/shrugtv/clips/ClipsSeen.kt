package com.shrug.shrugtv.clips

import android.content.Context

/** The clips that were opened (slugs, newest last, at most [MAX]), so the list can leave them out. */
object ClipsSeen {
    private const val FILE = "clips_seen"
    private const val KEY = "slugs"
    const val MAX = 500

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun all(ctx: Context): Set<String> = decode(prefs(ctx).getString(KEY, null))

    fun mark(ctx: Context, slug: String) {
        if (slug.isEmpty()) return
        val p = prefs(ctx)
        p.edit().putString(KEY, encode(add(decode(p.getString(KEY, null)), slug))).apply()
        ClipsFeed.forget()
    }

    /** [slug] last in the list (a repeated one moves to the end), cut to the newest [MAX]. */
    internal fun add(old: Set<String>, slug: String): List<String> = (old - slug + slug).toList().takeLast(MAX)

    internal fun decode(text: String?): Set<String> = text.orEmpty().split(',').filter { it.isNotEmpty() }.toCollection(LinkedHashSet())

    internal fun encode(list: List<String>) = list.joinToString(",")
}
