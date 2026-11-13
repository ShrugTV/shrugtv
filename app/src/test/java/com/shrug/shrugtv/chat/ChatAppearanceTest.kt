package com.shrug.shrugtv.chat

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatAppearanceTest {

    private val start = ChatAppearance(ChatLayout.SIDE_BY_SIDE, widthValue = 0, opacityIndex = 6, onLeft = false)

    @Test
    fun `layout cycles through all three`() {
        assertEquals(ChatLayout.OVERLAY, start.stepLayout(1).layout)
        assertEquals(ChatLayout.TRANSPARENT, start.stepLayout(-1).layout)
        assertEquals(ChatLayout.SIDE_BY_SIDE, start.stepLayout(3).layout)
    }

    @Test
    fun `width and opacity stay in range`() {
        // Auto is 224 dp at 14 sp: right goes wider, left stays Auto
        assertEquals(225, start.stepWidth(1, 14f).widthValue)
        assertEquals(0, start.stepWidth(-1, 14f).widthValue)
        val w300 = start.copy(widthValue = 300)
        assertEquals(300, w300.stepWidth(1, 14f).widthValue)
        assertEquals(275, w300.stepWidth(-1, 14f).widthValue)
        assertEquals(0, start.copy(widthValue = 100).stepWidth(-1, 14f).widthValue)
        assertEquals(9, start.stepOpacity(10).opacityIndex)
        assertEquals(0, start.stepOpacity(-10).opacityIndex)
        assertEquals(0.6f, start.backgroundAlpha, 0.001f)
        assertEquals(224f, start.widthDp(14f), 0f)
    }

    @Test
    fun `saved values read back the same`() {
        val prefs = MapPrefs()
        val changed = start.stepLayout(1).copy(widthValue = 175).stepOpacity(-2).togglePosition()
        changed.save(prefs)
        assertEquals(changed, ChatAppearance.from(prefs))
        assertEquals("overlay", prefs.getString("pref_chat_layout", null))
        assertEquals("175", prefs.getString("pref_chat_width", null))
        assertTrue(prefs.getString("pref_chat_position", null) in listOf("4", "5", "6"))
    }

    /** Minimal in-memory SharedPreferences (strings only). */
    private class MapPrefs : SharedPreferences {
        val map = HashMap<String, String?>()
        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String, defValue: String?): String? = if (map.containsKey(key)) map[key] else defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int) = defValue
        override fun getLong(key: String, defValue: Long) = defValue
        override fun getFloat(key: String, defValue: Float) = defValue
        override fun getBoolean(key: String, defValue: Boolean) = defValue
        override fun contains(key: String) = map.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String, value: String?) = apply { map[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = this
            override fun putLong(key: String, value: Long) = this
            override fun putFloat(key: String, value: Float) = this
            override fun putBoolean(key: String, value: Boolean) = this
            override fun remove(key: String) = apply { map.remove(key) }
            override fun clear() = apply { map.clear() }
            override fun commit() = true
            override fun apply() {}
        }
    }

    @Test
    fun `text size steps through 10 to 22 sp and stops at the ends`() {
        assertEquals(14f, start.textSizeSp)
        assertEquals(15f, start.stepFont(1).textSizeSp)
        var a = start
        repeat(20) { a = a.stepFont(1) }
        assertEquals(22f, a.textSizeSp)
        repeat(20) { a = a.stepFont(-1) }
        assertEquals(10f, a.textSizeSp)
    }

    @Test
    fun `height steps down from full and placement only moves within top to bottom`() {
        assertEquals(100, start.heightPercent)
        assertEquals(75, start.stepHeight(-1).heightPercent)
        assertEquals(100, start.stepHeight(1).heightPercent)
        var a = start
        repeat(9) { a = a.stepHeight(-1) }
        assertEquals(33, a.heightPercent)
        assertEquals(ChatVertical.MIDDLE, start.stepVertical(-1).vertical)
        assertEquals(ChatVertical.BOTTOM, start.stepVertical(1).vertical)
    }

    @Test
    fun `emote and font changes mean the chat has to render anew, height does not`() {
        assertTrue(start.stepFont(1).sizesDiffer(start))
        assertTrue(start.stepEmote(-1).sizesDiffer(start))
        assertFalse(start.stepHeight(-1).sizesDiffer(start))
    }

    @Test
    fun `font sizes 18 to 22 are no longer 14`() {
        assertEquals(18f, ChatPrefs.textSizeSp("8"))
        assertEquals(22f, ChatPrefs.textSizeSp("12"))
        assertEquals(17f, ChatPrefs.textSizeSp("7"))
        assertEquals(10f, ChatPrefs.textSizeSp("6"))
    }
}
