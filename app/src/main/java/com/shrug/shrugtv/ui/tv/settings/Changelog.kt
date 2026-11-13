package com.shrug.shrugtv.ui.tv.settings

/**
 * Reads assets/changelog.md, which tools/changelog_app.py generates from CHANGELOG.md
 *: "## X.Y.Z – date", below it "### Section" and "- item" with
 * indented continuation lines. Sections without items and "Internal" are dropped, as is
 * everything before the first version heading.
 */
object Changelog {

    data class Version(val number: String, val date: String?, val sections: List<Section>)
    data class Section(val title: String, val bullets: List<String>)

    /** A piece of text with `code` or **bold** markup. */
    data class Part(val text: String, val kind: Kind)
    enum class Kind { NORMAL, BOLD, CODE }

    private val versionHeader = Regex("""^(\d+\.\d+\.\d+)\s*(?:[–-]\s*(.+))?$""")

    fun read(text: String): List<Version> {
        val versions = mutableListOf<Version>()
        var number: String? = null
        var date: String? = null
        var sections = mutableListOf<Section>()
        var section: String? = null
        var bullets = mutableListOf<String>()
        var bullet: String? = null

        fun closeBullet() { bullet?.let { bullets.add(it) }; bullet = null }
        fun closeSection() {
            closeBullet()
            val t = section
            if (t != null && t != "Internal" && bullets.isNotEmpty()) sections.add(Section(t, bullets))
            bullets = mutableListOf()
            section = null
        }
        fun closeVersion() {
            closeSection()
            number?.let { if (sections.isNotEmpty()) versions.add(Version(it, date, sections)) }
            sections = mutableListOf()
            number = null
        }

        for (z in text.lines()) {
            when {
                z.startsWith("## ") -> {
                    closeVersion()
                    versionHeader.find(z.substring(3).trim())?.let { number = it.groupValues[1]; date = it.groupValues[2].ifBlank { null } }
                }
                z.startsWith("### ") -> { closeSection(); section = z.substring(4).trim() }
                z.startsWith("- ") -> { closeBullet(); if (number != null && section != null) bullet = z.substring(2).trim() }
                z.startsWith("  ") && bullet != null -> bullet += " " + z.trim()
                else -> closeBullet()
            }
        }
        closeVersion()
        return versions
    }

    /** Splits `code` and **bold**; an opening mark without a closing one stays as text. */
    fun parts(text: String): List<Part> {
        val out = mutableListOf<Part>()
        val normal = StringBuilder()
        var i = 0
        fun flushNormal() { if (normal.isNotEmpty()) { out.add(Part(normal.toString(), Kind.NORMAL)); normal.clear() } }
        while (i < text.length) {
            val (mark, kind) = when {
                text.startsWith("**", i) -> "**" to Kind.BOLD
                text[i] == '`' -> "`" to Kind.CODE
                else -> null to Kind.NORMAL
            }
            val end = mark?.let { text.indexOf(it, i + it.length) } ?: -1
            if (mark != null && end > i + mark.length) {
                flushNormal()
                out.add(Part(text.substring(i + mark.length, end), kind))
                i = end + mark.length
            } else {
                normal.append(text[i]); i++
            }
        }
        flushNormal()
        return out
    }
}
