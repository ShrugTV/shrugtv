package com.shrug.shrugtv

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Code, comments, scripts and documentation are English (Michael, 2026-11-13, ; the project language
 * since ). A line that holds two words that only exist in German, or an umlaut in a comment, fails
 * the build. Not scanned: the translations (`values-*`), the changelog history, the phone page dictionary (it holds the
 * German texts) and test data that needs real umlauts.
 */
class EnglishOnlyTest {

    private val root = File("..").canonicalFile
    private val skippedDirs = setOf("build", ".git", ".gradle", ".idea", ".kotlin", "node_modules", "__pycache__")
    private val textEndings = setOf("kt", "java", "xml", "md", "json", "py", "sh", "bat", "ps1", "gradle", "properties", "toml",
        "yml", "yaml", "js", "css", "html", "pro", "kts", "txt")
    private val skippedFiles = setOf(
        "CHANGELOG.md", "app/src/main/assets/changelog.md", "app/src/main/assets/companion/app.js",
        "tools/i18n.py", "app/src/main/res/values/language_shrugtv.xml", "app/src/main/res/values/arrays.xml",
        "app/src/main/java/com/shrug/shrugtv/session/AppState.kt",
        "app/src/test/java/com/shrug/shrugtv/chat/ChatHighlightsTest.kt", "app/src/test/java/com/shrug/shrugtv/chat/ChatPrefsTest.kt",
        "app/src/test/java/com/shrug/shrugtv/ha/HaDiscoveryTest.kt", "app/src/main/java/com/shrug/shrugtv/ha/HaDiscovery.kt",
        "app/src/test/java/com/shrug/shrugtv/EnglishOnlyTest.kt",
    )

    /** Words that exist in German only (no English meaning, no common identifier). */
    private val german = setOf(
        "der", "dem", "des", "und", "oder", "nicht", "wird", "werden", "fuer", "ueber", "eine", "einen", "einer", "ist", "sind",
        "auf", "aus", "bei", "dass", "wenn", "auch", "noch", "beim", "von", "kein", "keine", "sich", "wurde", "muss", "soll",
        "gibt", "aber", "jetzt", "jede", "jeder", "statt", "ohne", "damit", "weil", "haben", "hat", "neu", "neue", "zum", "zur",
        "dann", "nur", "als", "wie", "zeile", "zeilen", "fehler", "datei", "dateien", "ersten", "erste", "gleich",
        "koennen", "kann", "wurden", "sowie", "sonst", "dafuer", "dazu", "daher", "deshalb", "dort", "hier", "oben", "unten",
        "links", "rechts", "immer", "nie", "schon", "wieder", "alles", "viele", "mehr", "wenig", "zuerst", "danach", "davor",
        "bevor", "waehrend", "sobald", "solange", "gerade", "bereits", "eigene", "eigenen", "nichts", "etwas", "gemeinsam",
        "ausser", "gegen", "unter", "zwischen", "durch", "vor", "nach", "jeden", "jedem", "diese", "dieser", "dieses", "diesen",
        "ihre", "ihren", "seine", "seinen", "sein", "sie", "wir", "ich", "du", "ihr", "man", "bis", "ab", "um", "zu", "im", "am",
        "vom", "beim", "ins", "ans", "fest", "grob", "fein", "alte", "alten", "alter", "neuen", "kleine", "grosse", "ganze",
        "taste", "tasten", "knopf", "leiste", "fenster", "bild", "bilder", "kanal", "kanaele", "wert", "werte", "zahl",
        "anzahl", "stand", "seit", "bleibt", "bleiben", "steht", "stehen", "gilt", "geht", "kommt", "macht", "gibt", "nimmt",
        "zeigt", "laeuft", "laden", "speichern", "starten", "beenden", "oeffnen", "schliessen", "pruefen", "fehlt", "fehlen",
    )
    private val alsoEnglish = setOf("die", "man", "hat", "sie", "fest", "grob", "fein", "bis", "ab", "um", "zu", "im", "am", "ins", "ans", "stand",
        "sein", "wir", "ich", "du", "ihr", "bild", "ein", "gibt", "dann", "laden", "kann", "nach", "vor", "mit", "durch", "gegen", "unter", "links", "wert", "werte", "zahl", "laeuft")
    private val wordRx = Regex("[A-Za-zÄÖÜäöüß]+")
    private val commentStart = Regex("""^\s*(//|\*|/\*|#|<!--|--)""")

    private fun files(dir: File): Sequence<File> = dir.listFiles().orEmpty().asSequence().flatMap {
        when {
            it.isDirectory && (it.name in skippedDirs || Regex("values-[a-z]{2}(-r[A-Z]{2})?").matches(it.name)) -> emptySequence()
            it.isDirectory -> files(it)
            else -> sequenceOf(it)
        }
    }

    @Test
    fun `no German lines in code, scripts and docs`() {
        val hits = mutableListOf<String>()
        for (f in files(root)) {
            val rel = f.relativeTo(root).path.replace('\\', '/')
            if (rel in skippedFiles || f.extension.lowercase() !in textEndings || f.length() > 2_000_000) continue
            f.readLines().forEachIndexed { i, line ->
                val words = wordRx.findAll(line).map { it.value.lowercase() }.toSet()
                val hitWords = words.filter { it in german && it !in alsoEnglish }
                val umlautInComment = commentStart.containsMatchIn(line) && line.any { it in "äöüÄÖÜß" }
                if (hitWords.size >= 2 || umlautInComment) hits += "$rel:${i + 1}: ${line.trim().take(100)}"
            }
        }
        assertTrue("German text left (${hits.size} lines):\n" + hits.take(60).joinToString("\n"), hits.isEmpty())
    }
}
