package com.shrug.shrugtv.chat

import com.shrug.shrugtv.eventSub.ChatEvent
import com.shrug.shrugtv.model.RaidNotification

/**
 * What a chat client hands to the player. Replaces the `Handler` with
 * `Message.arg1` codes: the events are typed, and the sink is held strongly by the client, so it
 * can no longer be garbage collected like the weakly held handler that once left the chat empty.
 * Events arrive on the clients' own threads; the receiver moves them to the main thread.
 */
sealed interface ChatUiEvent {
    /** A rendered chat message or notice. */
    class Message(val message: ChatMessage) : ChatUiEvent

    /** The channel raids another one. */
    class Raid(val raid: RaidNotification) : ChatUiEvent

    /** A moderator deleted one message. */
    class Delete(val messageId: String) : ChatUiEvent

    /** Timeout or ban: every message of this user (login) goes. */
    class ClearUser(val userLogin: String) : ChatUiEvent

    /** "/clear". */
    object ClearAll : ChatUiEvent

    /** Slow, followers-only, subs-only, emote-only, unique chat. */
    class RoomSettings(val settings: ChatEvent.RoomSettings) : ChatUiEvent
}

fun interface ChatUiSink {
    fun onEvent(event: ChatUiEvent)
}
