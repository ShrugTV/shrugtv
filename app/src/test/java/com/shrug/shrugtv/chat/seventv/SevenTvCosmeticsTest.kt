package com.shrug.shrugtv.chat.seventv

import com.google.gson.JsonParser
import com.shrug.shrugtv.chat.emote.EmoteManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 7TV paints, badges, personal emote sets and zero-width flags (taken over from the 7TV extension). */
class SevenTvCosmeticsTest {

    private fun json(s: String) = JsonParser.parseString(s).asJsonObject

    @Test
    fun `7TV RGBA colours become Android ARGB`() {
        assertEquals(0xFFFF0000.toInt(), SevenTvCosmetics.argb(0xFF0000FFL))
        // Negative packed ints as 7TV sends them (-16776961 = 0xFF0000FF)
        assertEquals(0xFFFF0000.toInt(), SevenTvCosmetics.argb(-16776961L))
        assertEquals(0x8000FF00.toInt(), SevenTvCosmetics.argb(0x00FF0080L))
    }

    @Test
    fun `paints, badges and entitlements map to Twitch users`() {
        val c = SevenTvCosmetics()
        c.onCosmetic(json("""{"id":"p1","kind":"PAINT","data":{"id":"p1","name":"Sunset","function":"LINEAR_GRADIENT","angle":45,"repeat":false,
            "stops":[{"at":0,"color":-16776961},{"at":1,"color":16711935}]}}"""))
        c.onCosmetic(json("""{"id":"b1","kind":"BADGE","data":{"id":"b1","host":{"url":"//cdn.7tv.app/badge/b1","files":[{"name":"1x.webp","format":"WEBP"},{"name":"2x.webp","format":"WEBP"}]}}}"""))
        val user = """"user":{"id":"7tvuser","connections":[{"platform":"YOUTUBE","id":"yt"},{"platform":"TWITCH","id":"123"}]}"""
        assertNull(c.onEntitlement(json("""{"kind":"PAINT","ref_id":"p1",$user}"""), true))
        c.onEntitlement(json("""{"kind":"BADGE","ref_id":"b1",$user}"""), true)
        assertEquals("123" to "set9", c.onEntitlement(json("""{"kind":"EMOTE_SET","ref_id":"set9",$user}"""), true))

        val paint = c.paintOf("123")!!
        assertEquals(45f, paint.angle)
        assertFalse(paint.radial)
        assertArrayEquals(intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()), paint.colors)
        assertEquals("https://cdn.7tv.app/badge/b1/2x.webp", c.badgeUrlOf("123"))
        assertEquals("set9", c.emoteSetOf("123"))
        assertNull(c.paintOf("999"))

        c.onEntitlement(json("""{"kind":"PAINT","ref_id":"p1",$user}"""), false)
        assertNull(c.paintOf("123"))
    }

    @Test
    fun `single colour and image paints fall back to a solid colour`() {
        val solid = SevenTvCosmetics.parsePaint("x", json("""{"function":"URL","image_url":"https://x","color":-16776961,"stops":[]}"""))!!
        assertEquals(1, solid.colors.size)
        assertEquals(0xFFFF0000.toInt(), solid.colors[0])
        assertNull(SevenTvCosmetics.parsePaint("y", json("""{"function":"URL"}""")))
    }

    @Test
    fun `zero-width flags of 7TV emotes are read`() {
        assertTrue(EmoteManager.isSevenTvZeroWidth(json("""{"flags":1,"data":{}}""")))
        assertTrue(EmoteManager.isSevenTvZeroWidth(json("""{"flags":0,"data":{"flags":256}}""")))
        assertFalse(EmoteManager.isSevenTvZeroWidth(json("""{"flags":0,"data":{"flags":0}}""")))
        val e = EmoteManager.parseSevenTvEmote(json("""{"name":"RainTime","flags":1,"data":{"animated":true,
            "host":{"url":"//cdn.7tv.app/emote/abc","files":[{"name":"2x.webp","format":"WEBP"}]}}}"""))!!
        assertTrue(e.isZeroWidth)
        assertEquals("https://cdn.7tv.app/emote/abc/2x.webp", e.url)
    }
}
