package com.shrug.shrugtv.chat.emote

/**
 * The last few channels with what the chat needs for them: the emote lists of BTTV, FFZ and 7TV and the badges
 *. Measured on 2026-11-04 in the emulator: loading these took about 1.1 s of the 1.8 s
 * until the chat of a channel was ready; a channel that was seen a moment ago needs none of it again.
 *
 * Least recently used goes first; an entry older than [ttlMs] counts as gone, so changes to a channel's
 * emotes reach the chat after at most that long. Thread-safe: the loaders run in parallel.
 */
class ChannelCache<T : Any>(
    private val maxEntries: Int = DEFAULT_ENTRIES,
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private class Stamped<T>(val at: Long, val value: T)

    // Access order: the eldest entry is the one that was used longest ago
    private val entries = object : LinkedHashMap<String, Stamped<T>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Stamped<T>>?) = size > maxEntries
    }

    /** The value for [key] if there is a fresh one; using it keeps it from being evicted next. */
    @Synchronized
    fun get(key: String): T? {
        val e = entries[key] ?: return null
        if (now() - e.at > ttlMs) {
            entries.remove(key)
            return null
        }
        return e.value
    }

    /** Changes the value of [key] (made by [create] if there is none yet); a change renews its age. */
    @Synchronized
    fun update(key: String, create: () -> T, change: (T) -> T) {
        val old = entries[key]?.takeIf { now() - it.at <= ttlMs }?.value
        entries[key] = Stamped(now(), change(old ?: create()))
    }

    @Synchronized
    fun clear() = entries.clear()

    @get:Synchronized
    val size: Int get() = entries.size

    companion object {
        const val DEFAULT_ENTRIES = 8
        const val DEFAULT_TTL_MS = 10 * 60_000L
    }
}
