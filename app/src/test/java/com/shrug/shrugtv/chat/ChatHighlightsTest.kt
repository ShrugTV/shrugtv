package com.shrug.shrugtv.chat

import com.shrug.shrugtv.chat.ui.ChatColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHighlightsTest {

    @Test
    fun `mentions by login or display name, with or without at`() {
        assertTrue(ChatHighlights.mentions("hi @michael!", "michael", "Michael"))
        assertTrue(ChatHighlights.mentions("MICHAEL look", "michael", "Michael"))
        assertTrue(ChatHighlights.mentions("@Mïchael", "x", "Mïchael"))
        assertFalse(ChatHighlights.mentions("michaelson is here", "michael", "Michael"))
        assertFalse(ChatHighlights.mentions("anything", null, ""))
    }

    @Test
    fun `highlight words and users`() {
        val h = ChatHighlights(listOf("giveaway", "Größe"), listOf("@Friend", "buddy"))
        assertEquals(listOf(6..13), h.wordRanges("big   GIVEAWAY now"))
        assertEquals(1, h.wordRanges("GRÖSSE größe").size)
        assertTrue(h.isHighlightedUser("friend", null))
        assertTrue(h.isHighlightedUser(null, "Buddy"))
        assertFalse(h.isHighlightedUser("someone", "Someone"))
        assertTrue(ChatHighlights.NONE.isEmpty)
    }

    @Test
    fun `dark user colours are lightened, light ones stay`() {
        val blue = ChatColors.readable(0xFF0000FF.toInt())
        assertTrue("lighter than pure blue", (blue and 0xFF) > 0xFF / 2 || ((blue shr 8) and 0xFF) > 0x40)
        val light = 0xFFFFC0CB.toInt()
        assertEquals(light, ChatColors.readable(light))
    }

    @Test
    fun `colour parsing and stable fallback`() {
        assertEquals(0xFF1E90FF.toInt(), ChatColors.parse("#1E90FF"))
        assertNull(ChatColors.parse("#000000"))
        assertNull(ChatColors.parse(""))
        assertNull(ChatColors.parse("#12"))
        assertEquals(ChatColors.fallback("Alice"), ChatColors.fallback("alice"))
    }

    @Test
    fun `roles are highlighted only when switched on, colours have a default`() {
        val h = ChatHighlights(emptyList(), emptyList(), setOf(ChatHighlights.Role.MOD), ChatHighlights.colorOf("green"))
        assertTrue(h.isHighlightedRole(broadcaster = false, mod = true, vip = false))
        assertFalse(h.isHighlightedRole(broadcaster = true, mod = false, vip = true))
        assertFalse(ChatHighlights.NONE.isHighlightedRole(true, true, true))
        assertEquals(0xFF3DDC84.toInt(), h.color)
        assertEquals(ChatHighlights.DEFAULT_COLOR, ChatHighlights.colorOf("nonsense"))
    }
}
