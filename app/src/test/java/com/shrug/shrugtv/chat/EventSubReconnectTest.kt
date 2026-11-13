package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Reconnect timing of the EventSub chat connection (docs/chat_plan.md phase 1). */
class EventSubReconnectTest {

    @Test
    fun `backoff doubles up to 30 s`() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L),
            (0..6).map { LiveChatClient.reconnectDelayMs(it) })
        assertEquals(1_000L, LiveChatClient.reconnectDelayMs(-3))
        assertEquals(30_000L, LiveChatClient.reconnectDelayMs(1000))
    }

    @Test
    fun `keepalive deadline adds slack and has sane bounds`() {
        assertEquals(15_000L, LiveChatClient.keepaliveDeadlineMs(10))
        assertEquals(35_000L, LiveChatClient.keepaliveDeadlineMs(30))
        assertEquals(15_000L, LiveChatClient.keepaliveDeadlineMs(null))
        assertEquals(15_000L, LiveChatClient.keepaliveDeadlineMs(2))
        assertEquals(605_000L, LiveChatClient.keepaliveDeadlineMs(10_000))
    }

    @Test
    fun `only stale websocket subscriptions are cleaned up`() {
        val list = com.google.gson.JsonParser.parseString("""{"data":[
          {"id":"a","status":"enabled","transport":{"method":"websocket","session_id":"mine-old"}},
          {"id":"b","status":"enabled","transport":{"method":"websocket","session_id":"other-device"}},
          {"id":"c","status":"websocket_disconnected","transport":{"method":"websocket","session_id":"x"}},
          {"id":"d","status":"enabled","transport":{"method":"webhook"}}
        ]}""").asJsonObject
        assertEquals(listOf("a", "c"), LiveChatClient.staleSubscriptionIds(list, setOf("mine-old")))
        assertEquals(emptyList<String>(), LiveChatClient.staleSubscriptionIds(null, emptySet()))
    }
}
