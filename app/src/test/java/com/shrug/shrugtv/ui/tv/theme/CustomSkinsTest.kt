package com.shrug.shrugtv.ui.tv.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSkinsTest {
    private fun prefs(vararg pairs: Pair<String, String>): (String, String?) -> String? {
        val m = mapOf(*pairs)
        return { k, d -> m[k] ?: d }
    }

    private val mine = CustomSkin("custom_a1", "Mein Abend", "nord", mapOf(
        ThemeSettings.PREF_ACCENT to "#FF7A45", ThemeSettings.PREF_CORNERS to "0",
    ))

    @Test
    fun listSurvivesJson() {
        assertEquals(listOf(mine), CustomSkins.parse(CustomSkins.toJson(listOf(mine))))
    }

    @Test
    fun brokenOrForeignJsonGivesNothing() {
        assertTrue(CustomSkins.parse("not json").isEmpty())
        assertTrue(CustomSkins.parse("""[{"id":"evil","name":"x","base":"nord"}]""").isEmpty())
    }

    @Test
    fun invalidValuesAreDropped() {
        val json = """[{"id":"custom_b","name":"B","base":"nowhere","values":{"pref_theme_accent":"#12","pref_theme_font":"mono","pref_other":"1"}}]"""
        val s = CustomSkins.parse(json).single()
        assertEquals(Skins.SHRUGTV.id, s.base)
        assertEquals(mapOf(ThemeSettings.PREF_FONT to "mono"), s.values)
    }

    @Test
    fun ownSkinResolvesWithOwnChangesOnTop() {
        val read = prefs(
            ThemeSettings.PREF_SKIN to "custom_a1",
            ThemeSettings.PREF_CUSTOM to CustomSkins.toJson(listOf(mine)),
            ThemeSettings.PREF_CORNERS to "2.4",
        )
        val t = ThemeSettings.resolve(read)
        assertEquals("custom_a1", t.id)
        assertEquals(Color(0xFFFF7A45), t.colors.accent)
        assertEquals(Skins.NORD.colors.background, t.colors.background)
        assertEquals(2.4f, t.style.cornerScale)
    }

    @Test
    fun deletedOwnSkinFallsBackToTheDefault() {
        val t = ThemeSettings.resolve(prefs(ThemeSettings.PREF_SKIN to "custom_gone"))
        assertEquals(Skins.SHRUGTV.colors, t.colors)
    }

    @Test
    fun exportAndImportGiveTheSameDesign() {
        val read = prefs(ThemeSettings.PREF_SKIN to "dracula", ThemeSettings.PREF_ACCENT to "#00B8A9", ThemeSettings.PREF_GLASS to "0")
        val text = CustomSkins.export(read, "Teal Dracula")
        val back = CustomSkins.fromExport(text, "custom_x")!!
        assertEquals("Teal Dracula", back.name)
        assertEquals("dracula", back.base)
        assertEquals(mapOf(ThemeSettings.PREF_ACCENT to "#00B8A9", ThemeSettings.PREF_GLASS to "0"), back.values)
        assertEquals(ThemeSettings.resolve(read).colors, CustomSkins.theme(back).colors)
    }

    @Test
    fun importRefusesOtherFiles() {
        assertNull(CustomSkins.fromExport("""{"name":"x"}""", "custom_y"))
        assertNull(CustomSkins.fromExport("[]", "custom_y"))
    }

    @Test
    fun validityFollowsTheResolver() {
        assertTrue(CustomSkins.valid(ThemeSettings.PREF_TEXT_SCALE, "1.2"))
        assertFalse(CustomSkins.valid(ThemeSettings.PREF_TEXT_SCALE, "3"))
        assertFalse(CustomSkins.valid(ThemeSettings.PREF_ACCENT, ThemeSettings.FROM_SKIN))
        assertTrue(CustomSkins.valid(ThemeSettings.PREF_FOCUS_STYLE, "glow"))
    }
}
