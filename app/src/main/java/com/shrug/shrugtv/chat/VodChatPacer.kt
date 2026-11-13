package com.shrug.shrugtv.chat

/**
 * Paces the chat of a recording: comment pages arrive all at once, but each
 * message belongs to a moment of the video. Pages are loaded by offset (the cursor paging of the
 * query needs a Twitch integrity token), so this class decides when to ask for the next one, drops
 * what overlaps, and hands out messages as the playback position passes them. Main thread only.
 */
class VodChatPacer(private val now: () -> Long) {

    /** What to do after one tick: clear the panel first, then show [due], then maybe request a page. */
    class Step(val reset: Boolean, val due: List<ChatMessage>, val fetchOffset: Long?)

    private val pending = ArrayList<ChatMessage>()
    private val seen = HashSet<String>()
    private var fetchedUntil = -1L
    private var lastPosition = -1.0
    /** -1 = nothing requested yet (the first request must not wait for the clock to pass the gap). */
    private var lastRequestAt = -1L

    /** A message from a loaded page; pages overlap, so a repeated id is ignored. */
    fun onMessage(m: ChatMessage) {
        val id = m.messageId
        if (id != null && !seen.add(id)) return
        val offset = m.vodOffsetSeconds.toLong()
        if (offset > fetchedUntil) fetchedUntil = offset
        // Sorted by offset: the answers of two requests may arrive out of order
        val at = pending.indexOfFirst { it.vodOffsetSeconds > m.vodOffsetSeconds }
        if (at < 0) pending.add(m) else pending.add(at, m)
    }

    /** Called about once a second with the playback position in seconds. */
    fun tick(position: Double): Step {
        // A jump (seek, chapter, quick fast-forward): start over at the new position
        val reset = lastPosition < 0 || position < lastPosition - 2 || position > lastPosition + 6
        if (reset) clearState()
        lastPosition = position
        // A page starts about 10 s before its offset: skip what is already stale instead of showing a burst
        while (pending.isNotEmpty() && pending[0].vodOffsetSeconds < position - STALE_SECONDS) pending.removeAt(0)
        val due = ArrayList<ChatMessage>()
        while (pending.isNotEmpty() && pending[0].vodOffsetSeconds <= position) due.add(pending.removeAt(0))

        val ahead = pending.lastOrNull()?.vodOffsetSeconds ?: -1.0
        val t = now()
        var fetch: Long? = null
        if (ahead < position + LOOKAHEAD_SECONDS && (lastRequestAt < 0 || t - lastRequestAt >= MIN_REQUEST_GAP_MS)) {
            lastRequestAt = t
            fetch = if (fetchedUntil >= 0) fetchedUntil + 1 else position.toLong()
        }
        return Step(reset, due, fetch)
    }

    fun clear() {
        clearState()
        lastPosition = -1.0
        lastRequestAt = -1L
    }

    private fun clearState() {
        pending.clear()
        seen.clear()
        fetchedUntil = -1L
    }

    companion object {
        const val STALE_SECONDS = 15
        const val LOOKAHEAD_SECONDS = 20
        const val MIN_REQUEST_GAP_MS = 3000L
    }
}
