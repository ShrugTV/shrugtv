package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chat settings that used to never reach the chat (docs/bug_review.md B22, chat plan C0.4). */
class ChatPrefsTest {

    @Test
    fun `font size values map to sp`() {
        assertEquals(14f, ChatPrefs.textSizeSp("2"), 0f)
        assertEquals(10f, ChatPrefs.textSizeSp("6"), 0f)
        assertEquals(16f, ChatPrefs.textSizeSp("0"), 0f)
        assertEquals(17f, ChatPrefs.textSizeSp("7"), 0f)
        assertEquals(14f, ChatPrefs.textSizeSp(null), 0f)
        assertEquals(14f, ChatPrefs.textSizeSp("x"), 0f)
    }

    @Test
    fun `reply length is a number inside its limits, else the default`() {
        assertEquals(78, ChatPrefs.replyCharsOf(null))
        assertEquals(78, ChatPrefs.replyCharsOf("abc"))
        assertEquals(120, ChatPrefs.replyCharsOf(" 120 "))
        assertEquals(20, ChatPrefs.replyCharsOf("3"))
        assertEquals(300, ChatPrefs.replyCharsOf("9999"))
    }

    @Test
    fun `emote size is never zero`() {
        // 14 sp at density 2 = 28 px
        assertEquals(50, ChatPrefs.emoteSizePx("1", 28f, 2f))
        assertEquals(39, ChatPrefs.emoteSizePx("0", 28f, 2f))
        assertEquals(40, ChatPrefs.emoteSizePx("40", 28f, 2f))
        assertEquals(60, ChatPrefs.emoteSizePx("40", 28f, 3f))
        assertTrue(ChatPrefs.emoteSizePx(null, 0f, 0f) > 0)
    }

    @Test
    fun `comma list is trimmed and empty entries dropped`() {
        assertEquals(listOf("alice", "Bob"), ChatPrefs.splitList(" alice, ,Bob ,"))
        assertEquals(emptyList<String>(), ChatPrefs.splitList(""))
        assertEquals(emptyList<String>(), ChatPrefs.splitList(null))
    }

    @Test
    fun `blocked words match whole words case-insensitively`() {
        val settings = ChatFilterSettings.Builder()
            .blockedWordsRegex(ChatPrefs.wordsRegex(listOf("spoiler", "c++", "größe")))
            .build()
        val p = settings.compiledPattern!!
        assertTrue(p.matcher("no SPOILER please").find())
        assertTrue(p.matcher("I like c++ a lot").find())
        assertTrue(p.matcher("Größe zählt").find())
        assertFalse(p.matcher("spoilers are fine").find())
        assertFalse(p.matcher("nothing here").find())
    }

    @Test
    fun `no words means no pattern`() {
        assertNull(ChatPrefs.wordsRegex(emptyList()))
    }

    @Test
    fun `chat width scales with the font when auto`() {
        assertEquals(224f, ChatPrefs.widthDp("0", 14f), 0f)
        assertEquals(256f, ChatPrefs.widthDp("0", 16f), 0f)
        assertEquals(300f, ChatPrefs.widthDp("300", 14f), 0f)
        assertEquals(100f, ChatPrefs.widthDp("20", 14f), 0f)
    }

    @Test
    fun `layout defaults to side by side`() {
        assertEquals(ChatLayout.SIDE_BY_SIDE, ChatLayout.of(null))
        assertEquals(ChatLayout.SIDE_BY_SIDE, ChatLayout.of("unknown"))
        assertEquals(ChatLayout.OVERLAY, ChatLayout.of("overlay"))
        assertEquals(ChatLayout.TRANSPARENT, ChatLayout.of("transparent"))
    }
}
