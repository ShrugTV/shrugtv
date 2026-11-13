package com.shrug.shrugtv.hub

import com.shrug.shrugtv.hub.HubCommandRouter.Command
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubCommandRouterTest {

    @Test
    fun `simple commands`() {
        assertEquals(Command.PlayPause, HubCommandRouter.parse("play_pause", "PRESS"))
        assertEquals(Command.Zap(1), HubCommandRouter.parse("next", ""))
        assertEquals(Command.Zap(-1), HubCommandRouter.parse("previous", ""))
        assertEquals(Command.Chat(true), HubCommandRouter.parse("chat", "ON"))
        assertEquals(Command.Chat(false), HubCommandRouter.parse("chat", "off"))
        assertNull(HubCommandRouter.parse("chat", "maybe"))
        assertEquals(Command.Volume(55), HubCommandRouter.parse("volume", "55.0"))
        assertEquals(Command.Volume(100), HubCommandRouter.parse("volume", "300"))
        assertNull(HubCommandRouter.parse("unknown", "x"))
    }

    @Test
    fun `watch accepts a login or JSON, nothing else`() {
        assertEquals(Command.WatchLogin("papaplatte"), HubCommandRouter.parse("watch", "Papaplatte"))
        assertEquals(Command.WatchLogin("papaplatte"), HubCommandRouter.parse("watch", "@papaplatte"))
        assertEquals(Command.WatchLogin("bonjwa"), HubCommandRouter.parse("watch", """{"login":"Bonjwa"}"""))
        assertEquals(Command.WatchVod(123456L, 600), HubCommandRouter.parse("watch", """{"vod_id":123456,"position_s":600}"""))
        assertNull(HubCommandRouter.parse("watch", "rm -rf /"))
        assertNull(HubCommandRouter.parse("watch", "a".repeat(30)))
        assertNull(HubCommandRouter.parse("watch", "{broken"))
    }

    @Test
    fun `chat layout and notices are validated`() {
        assertEquals(Command.ChatLayout("overlay"), HubCommandRouter.parse("chat_layout", "overlay"))
        assertNull(HubCommandRouter.parse("chat_layout", "sideways"))
        assertEquals(Command.Notice("Doorbell", "Front door", "http://ha.lan/cam.jpg", 10),
            HubCommandRouter.parse("notify", """{"title":"Doorbell","text":"Front door","image_url":"http://ha.lan/cam.jpg","seconds":10}"""))
        assertEquals(Command.Notice("Hi", null, null, 8), HubCommandRouter.parse("notify", "Hi"))
        assertEquals(Command.Notice("X", null, null, 60), HubCommandRouter.parse("notify", """{"title":"X","image_url":"file:///etc/passwd","seconds":999}"""))
        assertEquals(500, (HubCommandRouter.parse("chat_message", "x".repeat(800)) as Command.ChatMessage).text.length)
    }

    @Test
    fun `snapshot json`() {
        val s = PlaybackSnapshot(status = "playing", kind = "live", channel = "Papaplatte", viewers = 5, startedAtMs = 0L, qualities = listOf("Auto"))
        val j = s.toJson(nowMs = 3_600_000L, appVersion = "1.9.2")
        assertEquals("playing", j.get("status").asString)
        assertEquals(60, j.get("uptime_min").asInt)
        assertEquals("Auto", j.getAsJsonArray("qualities")[0].asString)
        assertEquals("raid", HubEvent(HubEvent.RAID, mapOf("to" to "x", "n" to 3)).toJson().get("event_type").asString)
    }
}
