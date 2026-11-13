package com.shrug.shrugtv.ui.tv.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The advanced level: every key must exist, and hiding must not leave empty containers. */
class AdvancedKeysTest {

    private val xml = File("src/main/res/xml/settings.xml").readText()

    @Test
    fun `every advanced key exists in settings xml`() {
        val keys = Regex("""android:key="([^"]+)"""").findAll(xml).map { it.groupValues[1] }.toSet()
        val missing = SettingsModel.ADVANCED_KEYS - keys
        assertTrue("advanced keys missing from settings.xml: $missing", missing.isEmpty())
    }

    private fun toggle(key: String) = Entry.Toggle(key, key, null, false, null, null, null)
    private fun tip() = Entry.Action(null, "tip", "x", false, null)

    private val tree = Entry.Page(null, "root", null, 0, listOf(
        Entry.Page("pref_general_settings", "Home", null, 0, listOf(
            Entry.Category("A", listOf(tip(), toggle("pref_splash_min_ms"))),
            Entry.Category("B", listOf(toggle("pref_x"), toggle("pref_player_rest_after"))),
        )),
        Entry.Page("pref_advanced", "Advanced", null, 0, listOf(
            Entry.Page("pref_developer", "Developer", null, 0, listOf(toggle("pref_dev_show_errors"))),
        )),
    ))

    @Test
    fun `advanced off hides the keys and the categories they leave empty`() {
        val groups = SettingsModel.visible(tree, developer = false, advanced = false)
        assertEquals(listOf("pref_general_settings"), groups.map { it.key })
        val categories = groups[0].children.filterIsInstance<Entry.Category>()
        assertEquals(listOf("B"), categories.map { it.title })
        assertEquals(listOf("pref_x"), categories[0].children.map { it.key })
    }

    @Test
    fun `advanced on shows everything except the developer page`() {
        val groups = SettingsModel.visible(tree, developer = false, advanced = true)
        // The group "Advanced" only holds the developer page, so it stays out while developer mode is off
        assertEquals(listOf("pref_general_settings"), groups.map { it.key })
        assertEquals(2, groups[0].children.filterIsInstance<Entry.Category>().size)
    }

    @Test
    fun `developer mode shows the group Advanced even with advanced off`() {
        val groups = SettingsModel.visible(tree, developer = true, advanced = false)
        assertEquals(listOf("pref_general_settings", "pref_advanced"), groups.map { it.key })
    }
}
