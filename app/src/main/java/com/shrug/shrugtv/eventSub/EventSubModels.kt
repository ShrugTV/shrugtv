package com.shrug.shrugtv.eventSub

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Clean Kotlin models for Twitch EventSub WebSockets and subscriptions.
 */
object EventSubModels {
    const val VERSION: String = "1"

    @JvmStatic
    fun getRaidSub(channelId: String, sessionId: String): String {
        val root = JsonObject().apply {
            addProperty("type", "channel.raid")
            addProperty("version", VERSION)
            add("condition", JsonObject().apply {
                addProperty("from_broadcaster_user_id", channelId)
            })
            add("transport", JsonObject().apply {
                addProperty("method", "websocket")
                addProperty("session_id", sessionId)
            })
        }
        return root.toString()
    }

    /**
     * Chat subscription for [broadcasterId] read as [userId] (needs `user:read:chat`).
     * All chat events require `broadcaster_user_id` and `user_id`.
     */
    @JvmStatic
    fun getChatSub(type: String, broadcasterId: String, userId: String, sessionId: String): String {
        val root = JsonObject().apply {
            addProperty("type", type)
            addProperty("version", "1")
            add("condition", JsonObject().apply {
                addProperty("broadcaster_user_id", broadcasterId)
                if (userId.isNotBlank()) {
                    addProperty("user_id", userId)
                }
            })
            add("transport", JsonObject().apply {
                addProperty("method", "websocket")
                addProperty("session_id", sessionId)
            })
        }
        return root.toString()
    }

    /** Chat types the player subscribes to besides `channel.raid`. */
    @JvmField
    val CHAT_TYPES = listOf(
        "channel.chat.message",
        "channel.chat.notification",
        "channel.chat.message_delete",
        "channel.chat.clear_user_messages",
        "channel.chat.clear",
        "channel.chat_settings.update"
    )

    fun getChatMessageSub(broadcasterId: String, userId: String, sessionId: String): String {
        val root = JsonObject().apply {
            addProperty("type", "channel.chat.message")
            addProperty("version", "1")
            add("condition", JsonObject().apply {
                addProperty("broadcaster_user_id", broadcasterId)
                addProperty("user_id", userId)
            })
            add("transport", JsonObject().apply {
                addProperty("method", "websocket")
                addProperty("session_id", sessionId)
            })
        }
        return root.toString()
    }

    fun getChatNotificationSub(broadcasterId: String, userId: String, sessionId: String): String {
        val root = JsonObject().apply {
            addProperty("type", "channel.chat.notification")
            addProperty("version", "1")
            add("condition", JsonObject().apply {
                addProperty("broadcaster_user_id", broadcasterId)
                addProperty("user_id", userId)
            })
            add("transport", JsonObject().apply {
                addProperty("method", "websocket")
                addProperty("session_id", sessionId)
            })
        }
        return root.toString()
    }
}

/**
 * EventSub welcome message received immediately after WebSocket connection is established.
 */
data class EventSubWelcomeModel(
    @SerializedName("metadata") val metadata: MetadataDTO? = null,
    @SerializedName("payload") val payload: PayloadDTO? = null
) {
    data class MetadataDTO(
        @SerializedName("message_id") val messageId: String? = null,
        @SerializedName("message_timestamp") val messageTimestamp: String? = null,
        @SerializedName("message_type") val messageType: String? = null
    )

    data class PayloadDTO(
        @SerializedName("session") val session: SessionDTO? = null
    ) {
        data class SessionDTO(
            @SerializedName("id") val id: String? = null,
            @SerializedName("status") val status: String? = null,
            @SerializedName("connected_at") val connectedAt: String? = null,
            @SerializedName("keepalive_timeout_seconds") val keepaliveTimeoutSeconds: Int? = null,
            @SerializedName("reconnect_url") val reconnectUrl: String? = null
        )
    }
}

/**
 * EventSub raid notification message received when a raid occurs on the watched channel.
 */
data class EventSubRaidNotificationModel(
    @SerializedName("metadata") val metadata: MetadataDTO? = null,
    @SerializedName("payload") val payload: PayloadDTO? = null
) {
    data class MetadataDTO(
        @SerializedName("message_timestamp") val messageTimestamp: String? = null,
        @SerializedName("message_type") val messageType: String? = null,
        @SerializedName("subscription_type") val subscriptionType: String? = null,
    )

    data class PayloadDTO(
        @SerializedName("event") val event: EventDTO? = null,
        @SerializedName("subscription") val subscription: SubscriptionDTO? = null
    ) {
        data class EventDTO(
            @SerializedName("from_broadcaster_user_id") val fromBroadcasterUserId: String? = null,
            @SerializedName("from_broadcaster_user_login") val fromBroadcasterUserLogin: String? = null,
            @SerializedName("from_broadcaster_user_name") val fromBroadcasterUserName: String? = null,
            @SerializedName("to_broadcaster_user_id") val toBroadcasterUserId: String? = null,
            @SerializedName("to_broadcaster_user_login") val toBroadcasterUserLogin: String? = null,
            @SerializedName("to_broadcaster_user_name") val toBroadcasterUserName: String? = null,
            @SerializedName("viewers") val viewers: Int = 0
        )

        data class SubscriptionDTO(
            @SerializedName("id") val id: String? = null,
            @SerializedName("status") val status: String? = null,
            @SerializedName("type") val type: String? = null,
            @SerializedName("version") val version: String? = null
        )
    }
}

/**
 * Response when querying active EventSub subscriptions or creating a subscription.
 */
data class EventSubSubscriptionResponse(
    @SerializedName("data") val data: List<SubscriptionDataDTO>? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("total_cost") val totalCost: Int? = null,
    @SerializedName("max_total_cost") val maxTotalCost: Int? = null
) {
    data class SubscriptionDataDTO(
        @SerializedName("id") val id: String? = null,
        @SerializedName("status") val status: String? = null,
        @SerializedName("type") val type: String? = null,
        @SerializedName("version") val version: String? = null,
        @SerializedName("cost") val cost: Int? = null,
        @SerializedName("condition") val condition: Map<String, Any?>? = null,
        @SerializedName("transport") val transport: Map<String, Any?>? = null,
        @SerializedName("created_at") val createdAt: String? = null
    )
}

typealias EventSubActiveEvents = EventSubSubscriptionResponse
typealias EventSubLiveModel = EventSubSubscriptionResponse

data class EventSubChatMessageModel(
    @SerializedName("metadata") val metadata: MetadataDTO? = null,
    @SerializedName("payload") val payload: PayloadDTO? = null
) {
    data class MetadataDTO(
        @SerializedName("message_type") val messageType: String? = null,
        @SerializedName("message_id") val messageId: String? = null,
        @SerializedName("subscription_type") val subscriptionType: String? = null
    )
    data class PayloadDTO(@SerializedName("event") val event: ChatEventDTO? = null)
    data class ChatEventDTO(
        @SerializedName("chatter_user_name") val chatterUserName: String? = null,
        @SerializedName("chatter_user_login") val chatterUserLogin: String? = null,
        @SerializedName("chatter_user_id") val chatterUserId: String? = null,
        @SerializedName("message_id") val messageId: String? = null,
        @SerializedName("message") val message: MessageDTO? = null,
        @SerializedName("color") val color: String? = null,
        @SerializedName("badges") val badges: List<BadgeDTO>? = null,
        /** text, channel_points_highlighted, channel_points_sub_only, user_intro, power_ups_… */
        @SerializedName("message_type") val messageType: String? = null,
        @SerializedName("cheer") val cheer: CheerDTO? = null,
        @SerializedName("reply") val reply: ReplyDTO? = null
    )
    data class CheerDTO(@SerializedName("bits") val bits: Int = 0)
    data class ReplyDTO(
        @SerializedName("parent_message_id") val parentMessageId: String? = null,
        @SerializedName("parent_message_body") val parentMessageBody: String? = null,
        @SerializedName("parent_user_login") val parentUserLogin: String? = null,
        @SerializedName("parent_user_name") val parentUserName: String? = null
    )

    /** The text plus its fragments (text, emote, cheermote, mention); fragments concatenate to [text]. */
    data class MessageDTO(
        @SerializedName("text") val text: String? = null,
        @SerializedName("fragments") val fragments: List<FragmentDTO>? = null
    )
    data class FragmentDTO(
        @SerializedName("type") val type: String? = null,
        @SerializedName("text") val text: String? = null,
        @SerializedName("emote") val emote: FragmentEmoteDTO? = null
    )
    data class FragmentEmoteDTO(@SerializedName("id") val id: String? = null)
    data class BadgeDTO(@SerializedName("set_id") val setId: String? = null, @SerializedName("id") val id: String? = null)

    companion object {
        /**
         * Twitch emotes of a `channel.chat.message` as (emote id, "start-end") with inclusive UTF-16
         * offsets into the message text, the format of [com.shrug.shrugtv.chat.emote.Emote.positions].
         * EventSub has no emote offsets; they follow from the fragment lengths.
         */
        @JvmStatic
        fun twitchEmotePositions(fragments: List<FragmentDTO>?): List<Pair<String, String>> {
            if (fragments.isNullOrEmpty()) return emptyList()
            val result = ArrayList<Pair<String, String>>()
            var offset = 0
            for (f in fragments) {
                val len = f.text?.length ?: 0
                val id = f.emote?.id
                if (f.type == "emote" && !id.isNullOrEmpty() && len > 0) {
                    result.add(id to "$offset-${offset + len - 1}")
                }
                offset += len
            }
            return result
        }
    }
}
