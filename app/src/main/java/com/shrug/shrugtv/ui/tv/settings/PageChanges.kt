package com.shrug.shrugtv.ui.tv.settings

/**
 * Which settings differ from their defaults: a dot on pages that hold a changed value and
 * "Reset this page" at the end of a page. Only real values count, not actions or guides.
 */
object PageChanges {
    /** Kept when a page is reset: secrets and the app language (it restarts the app). */
    val KEEP = SettingsModel.SECRET_KEYS + "pref_app_language"

    /** The settings of [children]: categories are looked into, sub-pages only when [deep]. */
    fun leaves(children: List<Entry>, deep: Boolean): List<Entry> = children.flatMap {
        when (it) {
            is Entry.Category -> leaves(it.children, deep)
            is Entry.Page -> if (deep) leaves(it.children, true) else emptyList()
            else -> listOf(it)
        }
    }

    fun isChanged(e: Entry, z: SettingsAccess): Boolean = when {
        e.key == null || e.key in KEEP -> false
        e is Entry.Toggle -> z.bool(e.key, e.standard) != e.standard
        e is Entry.ListChoice -> z.string(e.key, e.standard) != e.standard
        e is Entry.TextField -> (z.string(e.key, e.standard) ?: "") != (e.standard ?: "")
        e is Entry.NumberField -> NumberInput.parse(z.string(e.key, e.standard.toString()) ?: "", e.min, e.max, e.standard) != e.standard
        e is Entry.ColorChoice -> (z.string(e.key, e.standard) ?: e.standard) != e.standard
        e is Entry.SkinChoice -> (z.string(e.key, com.shrug.shrugtv.ui.tv.theme.Skins.SHRUGTV.id)
            ?: com.shrug.shrugtv.ui.tv.theme.Skins.SHRUGTV.id) != com.shrug.shrugtv.ui.tv.theme.Skins.SHRUGTV.id
        else -> false
    }

    /** Keys on this page (not its sub-pages) that differ from the default. */
    fun changedKeys(page: Entry.Page, z: SettingsAccess): List<String> =
        leaves(page.children, deep = false).filter { isChanged(it, z) }.mapNotNull { it.key }

    /** Changed values on the page and all pages below it. */
    fun changedDeep(children: List<Entry>, z: SettingsAccess): Int = leaves(children, deep = true).count { isChanged(it, z) }
}
