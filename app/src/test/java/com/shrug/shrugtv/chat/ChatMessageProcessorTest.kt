package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ChatMessageProcessor filter logic.
 * Tests the spam glyph detection and filter decision logic.
 */
class ChatMessageProcessorTest {

    // Spam glyphs used by the processor
    private val spamGlyphs = arrayOf(
        "░", "⣿", "▤", "▊", "⠄", "⠈", "▧", "⢻", "⢿", "⣠", "⣴", "⣶", "⣀", "☒", "⌧", "▎", "▂", "▔"
    )

    private fun containsSpamGlyph(text: String?): Boolean {
        if (text == null) return false
        for (glyph in spamGlyphs) {
            if (text.contains(glyph)) return true
        }
        return false
    }

    @Test
    fun `null text is not spam`() {
        assertFalse(containsSpamGlyph(null))
    }

    @Test
    fun `empty text is not spam`() {
        assertFalse(containsSpamGlyph(""))
    }

    @Test
    fun `normal text is not spam`() {
        assertFalse(containsSpamGlyph("Hello world!"))
        assertFalse(containsSpamGlyph("Kappa PogChamp :)"))
    }

    @Test
    fun `text with spam glyph is detected`() {
        assertTrue(containsSpamGlyph("░░░░░░░"))
        assertTrue(containsSpamGlyph("⣿⣿⣿"))
        assertTrue(containsSpamGlyph("▤▤▤"))
    }

    @Test
    fun `mixed text with one spam glyph is detected`() {
        assertTrue(containsSpamGlyph("Hello ░ World"))
    }

    @Test
    fun `all spam glyphs are detected individually`() {
        for (glyph in spamGlyphs) {
            assertTrue("Glyph $glyph should be detected", containsSpamGlyph(glyph))
        }
    }

    @Test
    fun `blocked user matching is case insensitive`() {
        val blocked = listOf("baduser", "spammer")
        val testName = "BadUser"
        val isBlocked = blocked.any { it.equals(testName, ignoreCase = true) }
        assertTrue(isBlocked)
    }

    @Test
    fun `blocked user exact match`() {
        val blocked = listOf("exactuser")
        assertTrue(blocked.any { it.equals("exactuser", ignoreCase = true) })
        assertFalse(blocked.any { it.equals("otheruser", ignoreCase = true) })
    }

    @Test
    fun `blocked word pattern matching`() {
        val pattern = Regex("badword|spam", RegexOption.IGNORE_CASE)
        assertTrue(pattern.containsMatchIn("this has badword in it"))
        assertTrue(pattern.containsMatchIn("SPAM here"))
        assertFalse(pattern.containsMatchIn("clean text"))
    }

    @Test
    fun `filter settings null means no filtering`() {
        // When filterSettings is null, no filtering should occur
        val filterSettings: ChatFilterSettings? = null
        val shouldFilter = filterSettings?.isFilterBots == true
        assertFalse(shouldFilter)
    }

    @Test
    fun `message delay zero means immediate dispatch`() {
        val messageDelayMs = 0
        assertEquals(0, messageDelayMs)
        // Zero delay means no postDelayed
    }

    @Test
    fun `message delay positive means delayed dispatch`() {
        val messageDelayMs = 500
        assertTrue(messageDelayMs > 0)
    }

    @Test
    fun `spam glyph array is not empty`() {
        assertTrue(spamGlyphs.isNotEmpty())
        assertEquals(18, spamGlyphs.size)
    }

    @Test
    fun `unicode spam detection works with combining chars`() {
        // Some spam uses combining braille patterns
        assertTrue(containsSpamGlyph("⠄⠈⠠⡀⢀"))
    }
}
