package com.shrug.shrugtv.clips

import com.shrug.shrugtv.model.Movie

/**
 * The clips that play one after the other: whoever opens a clip from the Clips page or the home row hands
 * over the list as it is shown, and the player takes the next clip from it when one ends.
 */
object ClipQueue {
    @Volatile
    private var items: List<Movie> = emptyList()

    fun set(list: List<Movie>) {
        items = list.filter { it.isClip }
    }

    fun clear() {
        items = emptyList()
    }

    /** The clip right before ([step] -1) or after (+1) [current] in the list, whatever was opened; null at the ends. */
    fun neighbor(current: Movie, step: Int): Movie? = neighborIn(items, current.clipSlug, step)

    /** Place of [current] in the list and the length of the list, or null if it is alone or not in it. */
    fun position(current: Movie): Pair<Int, Int>? {
        val list = items
        val at = list.indexOfFirst { it.clipSlug == current.clipSlug }
        return if (at < 0 || list.size < 2) null else (at + 1) to list.size
    }

    internal fun neighborIn(list: List<Movie>, slug: String, step: Int): Movie? {
        val at = list.indexOfFirst { it.clipSlug == slug }
        return if (at < 0) null else list.getOrNull(at + step)
    }

    /** The clip after [current] in the list, without the ones in [skip]; null at the end or if [current] is not in the list. */
    fun next(current: Movie, skip: Set<String>): Movie? = nextIn(items, current.clipSlug, skip)

    internal fun nextIn(list: List<Movie>, slug: String, skip: Set<String>): Movie? {
        val at = list.indexOfFirst { it.clipSlug == slug }
        if (at < 0) return null
        return list.drop(at + 1).firstOrNull { it.clipSlug !in skip }
    }
}
