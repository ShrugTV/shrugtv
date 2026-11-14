package com.shrug.shrugtv.ha

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedListTest {
    private fun item(i: Int, live: Boolean = false) = FollowedList.Item(
        login = "channel$i", name = "Channel $i", game = if (live) "Just Chatting" else "",
        title = if (live) "A long stream title number $i" else "", viewers = if (live) 100 + i else 0
    )

    @Test
    fun `live and followed lists carry only what is needed and mark who is live`() {
        val live = listOf(item(1, true))
        val json = JsonParser.parseString(FollowedList.build(live, listOf(item(1), item(2)), 42L)).asJsonObject
        assertEquals(42L, json.get("updated").asLong)
        assertEquals(1, json.get("live_count").asInt)
        assertEquals(2, json.get("followed_count").asInt)
        val l = json.getAsJsonArray("live")[0].asJsonObject
        assertEquals("channel1", l.get("login").asString)
        assertEquals("Just Chatting", l.get("game").asString)
        assertEquals("A long stream title number 1", l.get("title").asString)
        assertEquals(101, l.get("viewers").asInt)
        assertFalse(l.has("avatar"))
        val f = json.getAsJsonArray("followed")
        assertTrue(f[0].asJsonObject.get("live").asBoolean)
        assertFalse(f[1].asJsonObject.get("live").asBoolean)
        assertEquals(setOf("login", "name", "live"), f[1].asJsonObject.keySet())
    }

    @Test
    fun `favorites are marked in both lists, others carry no flag`() {
        val fav = item(1, true).copy(favorite = true)
        val json = JsonParser.parseString(FollowedList.build(listOf(fav, item(2, true)), listOf(fav, item(2), item(3)), 1L)).asJsonObject
        val live = json.getAsJsonArray("live")
        assertTrue(live[0].asJsonObject.get("favorite").asBoolean)
        assertFalse(live[1].asJsonObject.has("favorite"))
        val all = json.getAsJsonArray("followed")
        assertTrue(all[0].asJsonObject.get("favorite").asBoolean)
        assertFalse(all[2].asJsonObject.has("favorite"))
    }

    @Test
    fun `70 followed channels with 10 live fit with room to spare and nothing is left out`() {
        val text = FollowedList.build((1..10).map { item(it, true) }, (1..70).map { item(it) }, 1L)
        assertTrue(text.toByteArray().size < 6_000)
        val json = JsonParser.parseString(text).asJsonObject
        assertEquals(70, json.getAsJsonArray("followed").size())
        assertEquals("A long stream title number 1", json.getAsJsonArray("live")[0].asJsonObject.get("title").asString)
    }

    @Test
    fun `a huge list stays below the attribute limit and keeps the true counts`() {
        val live = (1..200).map { item(it, true) }
        val followed = (1..1500).map { item(it) }
        val text = FollowedList.build(live, followed, 1L)
        assertTrue(text.toByteArray().size <= FollowedList.MAX_BYTES)
        val json = JsonParser.parseString(text).asJsonObject
        assertEquals(1500, json.get("followed_count").asInt)
        assertEquals(200, json.get("live_count").asInt)
        assertTrue(json.getAsJsonArray("live").size() > 0)
    }

    @Test
    fun `titles get shorter before any channel is dropped`() {
        val live = (1..60).map { item(it, true) }
        val text = FollowedList.build(live, (1..100).map { item(it) }, 1L)
        assertTrue(text.toByteArray().size <= FollowedList.MAX_BYTES)
        val json = JsonParser.parseString(text).asJsonObject
        assertEquals(60, json.getAsJsonArray("live").size())
        assertEquals(100, json.getAsJsonArray("followed").size())
    }

    @Test
    fun `the signature ignores the time`() {
        val live = listOf(item(1, true))
        assertEquals(FollowedList.signature(live, listOf(item(1))), FollowedList.signature(live, listOf(item(1))))
        assertFalse(FollowedList.signature(live, listOf(item(1))) == FollowedList.signature(emptyList(), listOf(item(1))))
    }
}
