package com.shrug.shrugtv.ui.tv.player

import androidx.lifecycle.ViewModel
import com.shrug.shrugtv.chat.ChatMessage
import com.shrug.shrugtv.eventSub.ChatEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One line of the chat panel; [id] is stable for the list diff, [deleted] after a moderator action. */
data class ChatLine(
    val id: Long,
    val message: ChatMessage,
    val deleted: Boolean = false,
    /** SystemClock.uptimeMillis() when added; the transparent layout hides old lines. */
    val addedAt: Long = 0L,
    /** Wall clock when the line arrived, for the optional time in front of it. */
    val wallMs: Long = System.currentTimeMillis(),
    /** How many people sent the same text; shown as "x N" behind it. */
    val repeat: Int = 1,
    /** Clock of the last repetition; the window for merging counts from here. */
    val lastAt: Long = addedAt
)

/** Holds the chat lines and the room state for the player. Main thread. */
class ChatViewModel : ViewModel() {
    companion object {
        const val MAX_MESSAGES = 200
        /** Lines searched for a repetition: spam comes in bursts, older lines are long out of view. */
        const val MERGE_LOOKBACK = 30
        private val SPACES = Regex("\\s+")

        /**
         * Text for comparing: case, spaces and invisible characters do not count; null for empty text. Chat tools append
         * invisible ones (U+E0000, U+034F, zero-width) to get past Twitch's own duplicate check, so without removing them
         * no "!stuhl" matched the one before (emulator 2026-11-08).
         */
        @JvmStatic
        fun mergeKey(text: String?): String? {
            if (text == null) return null
            val visible = StringBuilder()
            var i = 0
            while (i < text.length) {
                val cp = text.codePointAt(i)
                val type = Character.getType(cp)
                if (type != Character.FORMAT.toInt() && type != Character.NON_SPACING_MARK.toInt() && type != Character.UNASSIGNED.toInt()) visible.appendCodePoint(cp)
                i += Character.charCount(cp)
            }
            return visible.toString().trim().lowercase().replace(SPACES, " ").takeIf { it.isNotEmpty() }
        }
    }

    private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
    val lines: StateFlow<List<ChatLine>> = _lines.asStateFlow()

    private val _roomSettings = MutableStateFlow<ChatEvent.RoomSettings?>(null)
    val roomSettings: StateFlow<ChatEvent.RoomSettings?> = _roomSettings.asStateFlow()

    private val seenIds = ArrayDeque<String>(MAX_MESSAGES)
    private var nextId = 0L

    /**
     * Same text within this many ms becomes one line with a counter instead of a new line (
     * setting `pref_chat_merge_repeats`); 0 = off, the default.
     */
    var mergeWindowMs: Long = 0L

    fun addMessage(msg: ChatMessage) {
        val id = msg.messageId
        if (id != null) {
            if (seenIds.contains(id)) return
            seenIds.addLast(id)
            if (seenIds.size > MAX_MESSAGES) seenIds.removeFirst()
        }
        val t = now()
        if (mergeWindowMs > 0L && mergeInto(msg, t)) return
        _lines.value = (_lines.value + ChatLine(nextId++, msg, addedAt = t)).takeLast(MAX_MESSAGES)
    }

    /**
     * Counts [msg] on an earlier line with the same text, if one of the last [MERGE_LOOKBACK] lines repeated within the
     * window. The line keeps its place, so the chat does not move for spam.
     */
    private fun mergeInto(msg: ChatMessage, t: Long): Boolean {
        if (msg.isNotice) return false
        val key = mergeKey(msg.text) ?: return false
        val lines = _lines.value
        val from = maxOf(0, lines.size - MERGE_LOOKBACK)
        for (i in lines.size - 1 downTo from) {
            val line = lines[i]
            if (line.deleted || line.message.isNotice || t - line.lastAt > mergeWindowMs) continue
            if (mergeKey(line.message.text) == key) {
                _lines.value = lines.toMutableList().also { it[i] = line.copy(repeat = line.repeat + 1, lastAt = t) }
                return true
            }
        }
        return false
    }

    /** A moderator deleted one message. */
    fun deleteMessage(messageId: String) = markDeleted { it.messageId == messageId }

    /** Timeout or ban: every message of the user. */
    fun deleteUserMessages(login: String) {
        val l = login.lowercase()
        markDeleted { !it.isNotice && it.chatterLogin == l }
    }

    /** "/clear": every message so far. */
    fun deleteAll() = markDeleted { !it.isNotice }

    private fun markDeleted(match: (ChatMessage) -> Boolean) {
        var changed = false
        val updated = _lines.value.map { line ->
            if (!line.deleted && match(line.message)) {
                changed = true
                line.copy(deleted = true)
            } else line
        }
        if (changed) _lines.value = updated
    }

    fun setRoomSettings(settings: ChatEvent.RoomSettings) {
        _roomSettings.value = settings
    }

    /** Clock for [ChatLine.addedAt]; replaceable in tests (android.os.SystemClock is a stub there). */
    var now: () -> Long = { android.os.SystemClock.uptimeMillis() }

    fun clear() {
        _lines.value = emptyList()
        _roomSettings.value = null
        seenIds.clear()
    }
}
