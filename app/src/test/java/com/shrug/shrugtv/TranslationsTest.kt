package com.shrug.shrugtv

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * The translations: whatever a language has must fit English: no unknown names, the
 * same %-placeholders, the same number of list items. A missing text is allowed (English shows instead); a
 * broken placeholder is not, it crashes `getString` or shows a wrong number.
 */
class TranslationsTest {

    private val res = File("src/main/res")
    private val placeholder = Regex("""%(?:\d+\$)?,?(?:\.\d+)?[sdfx%]|\\n""")
    private val languages = listOf("de", "es", "fr", "pt", "it", "pl", "tr", "ru", "ja", "ko", "zh")

    private class Texts(
        val strings: MutableMap<String, String> = linkedMapOf(),
        val arrays: MutableMap<String, List<String>> = linkedMapOf(),
        val plurals: MutableMap<String, Map<String, String>> = linkedMapOf(),
    )

    private fun load(dir: String): Texts {
        val out = Texts()
        val folder = File(res, dir)
        for (f in folder.listFiles { x -> x.extension == "xml" }.orEmpty().sorted()) {
            val doc = try { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f) } catch (e: Exception) { continue }
            val kids = doc.documentElement.childNodes
            for (i in 0 until kids.length) {
                val e = kids.item(i) as? Element ?: continue
                val name = e.getAttribute("name")
                if (e.getAttribute("translatable") == "false") continue
                when (e.tagName) {
                    "string" -> out.strings[name] = e.textContent
                    "string-array" -> out.arrays[name] = items(e)
                    "plurals" -> out.plurals[name] = (0 until e.childNodes.length).mapNotNull { e.childNodes.item(it) as? Element }
                        .associate { it.getAttribute("quantity") to it.textContent }
                }
            }
        }
        return out
    }

    private fun items(e: Element) = (0 until e.childNodes.length).mapNotNull { e.childNodes.item(it) as? Element }.map { it.textContent }

    private fun marks(s: String) = placeholder.findAll(s).map { it.value }.sorted().toList()

    @Test
    fun `every translation fits the English texts`() {
        val en = load("values")
        val problems = mutableListOf<String>()
        for (lang in languages) {
            if (!File(res, "values-$lang").exists()) continue
            val t = load("values-$lang")
            for ((k, v) in t.strings) {
                val src = en.strings[k]
                if (src == null) problems += "$lang: unknown string $k"
                else if (marks(src) != marks(v)) problems += "$lang: placeholders of $k differ: ${marks(src)} vs ${marks(v)}"
            }
            for ((k, v) in t.arrays) {
                val src = en.arrays[k]
                if (src == null) problems += "$lang: unknown array $k"
                else if (src.size != v.size) problems += "$lang: array $k has ${v.size} items, English ${src.size}"
            }
            for ((k, v) in t.plurals) {
                val src = en.plurals[k]
                if (src == null) { problems += "$lang: unknown plurals $k"; continue }
                if ("other" !in v) problems += "$lang: plurals $k lack 'other'"
                val ref = marks(src["other"] ?: src.values.first())
                for ((q, text) in v) if (q != "one" && marks(text) != ref) problems += "$lang: placeholders of $k/$q differ"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `the language list has a name and a code for every language`() {
        val names = load("values").arrays["app_language_names"]
        val codes = File(res, "values/language_shrugtv.xml").readText()
            .substringAfter("app_language_values").let { Regex("<item>([^<]+)</item>").findAll(it).map { m -> m.groupValues[1] }.toList() }
        assertTrue("names ${names?.size} vs codes ${codes.size}", names != null && names.size == codes.size)
        for (lang in languages) assertTrue("$lang missing in the language list", lang in codes)
        for (lang in languages) {
            val own = load("values-$lang").arrays["app_language_names"] ?: continue
            assertTrue("$lang: the language list has ${own.size} names, English ${names!!.size}", own.size == names.size)
        }
    }
}
