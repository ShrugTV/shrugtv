package com.shrug.shrugtv.eventSub

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatEventTest {

    private fun event(json: String) = JsonParser.parseString(json).asJsonObject

    @Test
    fun `resub notice keeps system message and user text`() {
        val e = ChatEvent.parse("channel.chat.notification", event("""
            {"chatter_user_login":"fan","chatter_user_name":"Fan","notice_type":"resub","message_id":"n1",
             "system_message":"Fan subscribed at Tier 1. They've subscribed for 12 months!",
             "message":{"text":"hype","fragments":[]}}""")) as ChatEvent.Notice
        assertTrue(e.isSub)
        assertEquals("hype", e.messageText)
        assertEquals("Fan", e.chatterName)
    }

    @Test
    fun `gift notice is a gift, empty text is null`() {
        val e = ChatEvent.parse("channel.chat.notification", event("""
            {"notice_type":"community_sub_gift","system_message":"X is gifting 5 subs","message":{"text":""}}""")) as ChatEvent.Notice
        assertTrue(e.isGift)
        assertNull(e.messageText)
    }

    @Test
    fun `moderation events`() {
        assertEquals(ChatEvent.Delete("abc"), ChatEvent.parse("channel.chat.message_delete", event("""{"message_id":"abc","target_user_login":"x"}""")))
        assertEquals(ChatEvent.ClearUser("troll"), ChatEvent.parse("channel.chat.clear_user_messages", event("""{"target_user_login":"troll","target_user_id":"1"}""")))
        assertSame(ChatEvent.ClearAll, ChatEvent.parse("channel.chat.clear", event("{}")))
        assertNull(ChatEvent.parse("channel.unknown", event("{}")))
    }

    @Test
    fun `room settings from EventSub and from Helix use different field names`() {
        val fromEventSub = ChatEvent.parse("channel.chat_settings.update", event("""
            {"emote_mode":false,"follower_mode":true,"follower_mode_duration_minutes":10,
             "slow_mode":true,"slow_mode_wait_time_seconds":30,"subscriber_mode":false,"unique_chat_mode":false}""")) as ChatEvent.RoomSettings
        assertEquals(10, fromEventSub.followersOnlyMinutes)
        assertEquals(30, fromEventSub.slowSeconds)

        val fromHelix = ChatEvent.roomSettings(event("""
            {"emote_mode":true,"follower_mode":false,"follower_mode_duration":null,
             "slow_mode":true,"slow_mode_wait_time":120,"subscriber_mode":true}"""), eventSub = false)
        assertNull(fromHelix.followersOnlyMinutes)
        assertEquals(120, fromHelix.slowSeconds)
        assertTrue(fromHelix.emoteOnly && fromHelix.subsOnly && fromHelix.isAnyActive)
    }
}
