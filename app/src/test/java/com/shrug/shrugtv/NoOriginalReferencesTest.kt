package com.shrug.shrugtv

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The original app is never named in this repo (Michael, 2026-11-13): not in code, docs,
 * scripts, resources, the changelog or file names. The pattern is assembled here so that this file does not
 * contain it itself. Git history is not scanned (it still holds the raw export, see docs/public_release.md).
 */
class NoOriginalReferencesTest {

    private val forbidden = Regex("s0" + "und", RegexOption.IGNORE_CASE)
    private val root = File("..").canonicalFile
    private val skippedDirs = setOf("build", ".git", ".gradle", ".idea", ".kotlin", "node_modules")
    private val textEndings = setOf("kt", "java", "xml", "md", "txt", "json", "py", "sh", "bat", "ps1", "gradle", "properties",
        "toml", "yml", "yaml", "js", "css", "html", "tsv", "cfg", "pro", "kts", "svg")

    private fun files(dir: File): Sequence<File> = dir.listFiles().orEmpty().asSequence().flatMap {
        when {
            it.isDirectory && it.name in skippedDirs -> emptySequence()
            it.isDirectory -> files(it)
            else -> sequenceOf(it)
        }
    }

    @Test
    fun `no tracked text file or file name mentions the original app`() {
        val hits = mutableListOf<String>()
        for (f in files(root)) {
            val rel = f.relativeTo(root).path.replace('\\', '/')
            if (forbidden.containsMatchIn(rel)) hits += "$rel (file name)"
            if (f.extension.lowercase() in textEndings && f.length() < 4_000_000) {
                val n = f.readLines().count { forbidden.containsMatchIn(it) }
                if (n > 0) hits += "$rel ($n lines)"
            }
        }
        assertTrue("the original app is named in:\n" + hits.joinToString("\n"), hits.isEmpty())
    }
}
