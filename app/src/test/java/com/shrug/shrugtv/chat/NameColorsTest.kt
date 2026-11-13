package com.shrug.shrugtv.chat

import com.shrug.shrugtv.chat.ui.ChatColors
import com.shrug.shrugtv.chat.ui.NameColorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Name colours made readable by contrast. */
class NameColorsTest {
    private val darkPanel = 0xFF18181B.toInt()
    private val lightPanel = 0xFFF2F2F5.toInt()

    @Test
    fun `dark blue and dark red reach 4_5 to 1 on a dark panel`() {
        for (c in listOf(0xFF0000FF, 0xFF8B0000, 0xFF000080, 0xFF2E0854).map { it.toInt() }) {
            assertTrue("start is below 4.5", ChatColors.contrast(c, darkPanel) < 4.5f)
            assertTrue("result reaches 4.5", ChatColors.contrast(ChatColors.readableOn(c, darkPanel), darkPanel) >= 4.5f)
        }
    }

    @Test
    fun `a readable colour stays exactly as it is`() {
        val pink = 0xFFFFC0CB.toInt()
        assertEquals(pink, ChatColors.readableOn(pink, darkPanel))
    }

    @Test
    fun `the colour moves only as far as needed and keeps its hue`() {
        val out = ChatColors.readableOn(0xFF0000FF.toInt(), darkPanel)
        assertTrue("still blue", (out and 0xFF) > ((out shr 16) and 0xFF) && (out and 0xFF) > ((out shr 8) and 0xFF))
        assertTrue("not just white", ChatColors.contrast(out, darkPanel) < 7f)
    }

    @Test
    fun `on a light panel a pale colour is darkened`() {
        val yellow = 0xFFFFFF00.toInt()
        assertTrue(ChatColors.contrast(yellow, lightPanel) < 4.5f)
        assertTrue(ChatColors.contrast(ChatColors.readableOn(yellow, lightPanel), lightPanel) >= 4.5f)
    }

    @Test
    fun `contrast of black on white is 21`() {
        assertEquals(21f, ChatColors.contrast(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01f)
    }

    @Test
    fun `the mode falls back to readable`() {
        assertEquals(NameColorMode.READABLE, NameColorMode.of(null))
        assertEquals(NameColorMode.READABLE, NameColorMode.of("nonsense"))
        assertEquals(NameColorMode.RAW, NameColorMode.of("raw"))
        assertEquals(NameColorMode.ACCENT, NameColorMode.of("accent"))
    }
}
