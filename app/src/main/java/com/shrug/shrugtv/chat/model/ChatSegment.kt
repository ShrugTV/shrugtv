package com.shrug.shrugtv.chat.model

import com.shrug.shrugtv.chat.emote.Emote

/**
 * A segment of a chat message (text, emote with optional overlays/modifiers, mention).
 */
sealed interface ChatSegment {
    /** Reiner Textabschnitt oder Leerzeichen */
    data class Text(val content: String) : ChatSegment

    /**
     * Ein Emote.
     * [modifiers] holds 7TV zero-width / modifier emotes that are stacked over this base emote.
     */
    data class EmoteSegment(
        val emote: Emote,
        val modifiers: List<Emote> = emptyList()
    ) : ChatSegment

    /** A user mention (@username) */
    data class Mention(val username: String) : ChatSegment
}
