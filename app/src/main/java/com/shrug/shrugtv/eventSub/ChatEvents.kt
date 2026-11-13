package com.shrug.shrugtv.eventSub

import com.google.gson.JsonObject

/**
 * Chat events besides plain messages, parsed from EventSub notification payloads
 * (docs/chat_plan.md phase 2). Without Android types, so the parsing is testable.
 */
sealed class ChatEvent {
    /** Sub, resub, gift, raid, announcement … (`channel.chat.notification`). */
    data class Notice(
        val noticeType: String,
        val systemMessage: String,
        val chatterName: String?,
        val chatterLogin: String?,
        val messageText: String?,
        val messageId: String?
    ) : ChatEvent() {
        val isSub: Boolean get() = noticeType == "sub" || noticeType == "resub"
        val isGift: Boolean get() = noticeType == "sub_gift" || noticeType == "community_sub_gift"
    }

    /** A moderator deleted one message (`channel.chat.message_delete`). */
    data class Delete(val messageId: String) : ChatEvent()

    /** Timeout or ban: all messages of a user go (`channel.chat.clear_user_messages`). */
    data class ClearUser(val userLogin: String) : ChatEvent()

    /** "/clear" (`channel.chat.clear`). */
    object ClearAll : ChatEvent()

    /** Room state from `channel.chat_settings.update` or Helix `chat/settings`. */
    data class RoomSettings(
        val emoteOnly: Boolean,
        /** Minutes a user must follow; null = off, 0 = any follower. */
        val followersOnlyMinutes: Int?,
        /** Seconds between messages; null = off. */
        val slowSeconds: Int?,
        val subsOnly: Boolean,
        val uniqueChat: Boolean
    ) : ChatEvent() {
        val isAnyActive: Boolean
            get() = emoteOnly || followersOnlyMinutes != null || slowSeconds != null || subsOnly || uniqueChat
    }

    companion object {
        private fun JsonObject.str(name: String): String? =
            get(name)?.takeIf { !it.isJsonNull }?.asString

        private fun JsonObject.bool(name: String): Boolean =
            get(name)?.takeIf { !it.isJsonNull }?.asBoolean ?: false

        private fun JsonObject.int(name: String): Int? =
            get(name)?.takeIf { !it.isJsonNull }?.asInt

        /** The event of a `notification` envelope of [subscriptionType], or null if not a chat event. */
        @JvmStatic
        fun parse(subscriptionType: String?, event: JsonObject?): ChatEvent? {
            if (event == null) return null
            return when (subscriptionType) {
                "channel.chat.notification" -> {
                    val noticeType = event.str("notice_type") ?: return null
                    Notice(
                        noticeType = noticeType,
                        systemMessage = event.str("system_message") ?: "",
                        chatterName = event.str("chatter_user_name"),
                        chatterLogin = event.str("chatter_user_login"),
                        messageText = event.getAsJsonObject("message")?.str("text")?.takeIf { it.isNotBlank() },
                        messageId = event.str("message_id")
                    )
                }
                "channel.chat.message_delete" -> event.str("message_id")?.let { Delete(it) }
                "channel.chat.clear_user_messages" -> event.str("target_user_login")?.let { ClearUser(it) }
                "channel.chat.clear" -> ClearAll
                "channel.chat_settings.update" -> roomSettings(event, eventSub = true)
                else -> null
            }
        }

        /**
         * Room state; EventSub and Helix name two fields differently
         * (`follower_mode_duration_minutes` vs `follower_mode_duration`,
         * `slow_mode_wait_time_seconds` vs `slow_mode_wait_time`).
         */
        @JvmStatic
        fun roomSettings(o: JsonObject, eventSub: Boolean): RoomSettings {
            val followerMinutes = if (eventSub) o.int("follower_mode_duration_minutes") else o.int("follower_mode_duration")
            val slow = if (eventSub) o.int("slow_mode_wait_time_seconds") else o.int("slow_mode_wait_time")
            return RoomSettings(
                emoteOnly = o.bool("emote_mode"),
                followersOnlyMinutes = if (o.bool("follower_mode")) (followerMinutes ?: 0) else null,
                slowSeconds = if (o.bool("slow_mode")) (slow ?: 0) else null,
                subsOnly = o.bool("subscriber_mode"),
                uniqueChat = o.bool("unique_chat_mode")
            )
        }
    }
}
