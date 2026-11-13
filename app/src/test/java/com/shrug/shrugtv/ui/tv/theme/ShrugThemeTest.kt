package com.shrug.shrugtv.ui.tv.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShrugThemeTest {
    private fun prefs(vararg pairs: Pair<String, String>): (String, String?) -> String? {
        val m = mapOf(*pairs)
        return { k, d -> m[k] ?: d }
    }

    @Test
    fun withoutPreferencesTheDefaultLookStays() {
        assertEquals(Skins.SHRUGTV, ThemeSettings.resolve(prefs()))
    }

    @Test
    fun accentOverrideChangesAccentAndDerivedColours() {
        val t = ThemeSettings.resolve(prefs(ThemeSettings.PREF_ACCENT to "#005FDB"))
        assertEquals(Color(0xFF005FDB), t.colors.accent)
        assertEquals(Color.White, t.colors.onAccent)
        assertEquals(Skins.SHRUGTV.colors.background, t.colors.background)
    }

    @Test
    fun lightAccentGetsDarkText() {
        val t = Skins.SHRUGTV.withAccent(Color(0xFFFECF01))
        assertEquals(Color(0xFF111111), t.colors.onAccent)
    }

    @Test
    fun fromSkinAndGarbageMeanNoOverride() {
        assertEquals(Skins.SHRUGTV, ThemeSettings.resolve(prefs(ThemeSettings.PREF_ACCENT to ThemeSettings.FROM_SKIN)))
        assertEquals(Skins.SHRUGTV, ThemeSettings.resolve(prefs(ThemeSettings.PREF_ACCENT to "#12")))
        assertEquals(Skins.SHRUGTV, ThemeSettings.resolve(prefs(ThemeSettings.PREF_SKIN to "unknown")))
    }

    @Test
    fun hexRoundTrip() {
        assertEquals("#9147FF", ShrugTheme.toHex(ShrugTheme.parseHex("#9147ff")!!))
    }

    @Test
    fun oldAccentIsMigratedUnlessDefaultOrAlreadySet() {
        assertEquals("#005FDB", ThemeSettings.migratedAccent("1", hasNewValue = false))
        assertNull(ThemeSettings.migratedAccent("2", hasNewValue = false))
        assertNull(ThemeSettings.migratedAccent("1", hasNewValue = true))
        assertNull(ThemeSettings.migratedAccent(null, hasNewValue = false))
    }

    @Test
    fun everySkinIsReadable() {
        for (t in Skins.all) {
            val c = t.colors
            assertTrue("${t.id} text", ShrugTheme.contrast(c.text, c.background) >= 4.5f)
            assertTrue("${t.id} text on surface", ShrugTheme.contrast(c.text, c.surface) >= 4.5f)
            assertTrue("${t.id} muted", ShrugTheme.contrast(c.textMuted, c.surface) >= 4.5f)
            assertTrue("${t.id} focus", ShrugTheme.contrast(c.focus, c.surface) >= 3f)
            assertTrue("${t.id} on accent", ShrugTheme.contrast(c.onAccent, c.accent) >= 4.5f)
        }
    }

    @Test
    fun skinIdsAreUnique() {
        assertEquals(Skins.all.size, Skins.all.map { it.id }.toSet().size)
    }

    @Test
    fun styleOverridesApplyOnTopOfTheSkin() {
        val t = ThemeSettings.resolve(prefs(
            ThemeSettings.PREF_SKIN to "nord", ThemeSettings.PREF_CORNERS to "0", ThemeSettings.PREF_FOCUS_STYLE to "glow",
            ThemeSettings.PREF_FONT to "mono", ThemeSettings.PREF_TEXT_SCALE to "1.2", ThemeSettings.PREF_ANIMATIONS to "off",
            ThemeSettings.PREF_BACKGROUND to "#101010",
        ))
        assertEquals("nord", t.id)
        assertEquals(0f, t.style.cornerScale)
        assertEquals(FocusStyle.GLOW, t.style.focusStyle)
        assertEquals(FontChoice.MONO, t.style.font)
        assertEquals(1.2f, t.style.textScale)
        assertEquals(false, t.style.animations)
        assertEquals(Color(0xFF101010), t.colors.background)
        // untouched values stay the skin's
        assertEquals(Skins.NORD.colors.accent, t.colors.accent)
        assertEquals(Skins.NORD.style.background, t.style.background)
    }

    @Test
    fun outOfRangeValuesAreIgnored() {
        val t = ThemeSettings.resolve(prefs(ThemeSettings.PREF_TEXT_SCALE to "9", ThemeSettings.PREF_FOCUS_STYLE to "wobble"))
        assertEquals(Skins.SHRUGTV.style, t.style)
    }

    @Test
    fun overridesAreCounted() {
        assertEquals(0, ThemeSettings.overrideCount(prefs(ThemeSettings.PREF_ACCENT to "skin")))
        assertEquals(2, ThemeSettings.overrideCount(prefs(ThemeSettings.PREF_ACCENT to "#FFFFFF", ThemeSettings.PREF_CORNERS to "0")))
    }
}
