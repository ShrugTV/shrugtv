package com.shrug.shrugtv.ui.tv.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** "Changes" page from CHANGELOG.md. */
class ChangelogTest {

    @Test
    fun versionsSectionsItems() {
        val v = Changelog.read(
            """
            # Changelog

            Introduction with - dash

            ## Unreleased

            ### New
            - not released yet

            ## 1.2.0 – 2026-10-28

            ### New
            - First item,
              across two lines.
            - Second item

            ### Internal
            - repo only

            ### Fixed

            ## 1.1.0 – 2026-10-27

            ### Internal
            - nothing for the app
            """.trimIndent()
        )
        assertEquals(1, v.size)
        assertEquals("1.2.0", v[0].number)
        assertEquals("2026-10-28", v[0].date)
        assertEquals(listOf(Changelog.Section("New", listOf("First item, across two lines.", "Second item"))), v[0].sections)
    }

    @Test
    fun markup() {
        assertEquals(
            listOf(
                Changelog.Part("With ", Changelog.Kind.NORMAL),
                Changelog.Part("bold", Changelog.Kind.BOLD),
                Changelog.Part(" and ", Changelog.Kind.NORMAL),
                Changelog.Part("tools/x.py", Changelog.Kind.CODE),
                Changelog.Part(" and `open", Changelog.Kind.NORMAL),
            ),
            Changelog.parts("With **bold** and `tools/x.py` and `open"),
        )
    }
}
