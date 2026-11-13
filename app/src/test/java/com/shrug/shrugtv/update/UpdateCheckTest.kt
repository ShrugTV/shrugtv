package com.shrug.shrugtv.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {

    private fun answer(tag: String, draft: Boolean = false, pre: Boolean = false) =
        """{"tag_name":"$tag","html_url":"https://example.test/r/$tag","body":"Notes","draft":$draft,"prerelease":$pre}"""

    @Test
    fun `newer release is an update`() {
        assertTrue(UpdateCheck.isNewer("2.0.0", "v2.1.0"))
        assertTrue(UpdateCheck.isNewer("2.0.9", "v2.1.0"))
        assertTrue(UpdateCheck.isNewer("1.9.9", "2.0.0"))
        assertTrue(UpdateCheck.isNewer("2.0.0", "v10.0.0"))
    }

    @Test
    fun `same or older release is no update`() {
        assertFalse(UpdateCheck.isNewer("2.1.0", "v2.1.0"))
        assertFalse(UpdateCheck.isNewer("2.1.0", "v2.0.5"))
    }

    @Test
    fun `development state counts as its base version`() {
        assertFalse(UpdateCheck.isNewer("2.0.0-5-gabc123", "v2.0.0"))
        assertTrue(UpdateCheck.isNewer("2.0.0-5-gabc123", "v2.0.1"))
    }

    @Test
    fun `pre-release tags and odd names are never an update`() {
        assertFalse(UpdateCheck.isNewer("2.0.0", "v2.1.0-rc1"))
        assertFalse(UpdateCheck.isNewer("2.0.0", "nightly"))
        assertFalse(UpdateCheck.isNewer("unknown", "v2.1.0"))
    }

    @Test
    fun `release answer is parsed`() {
        val r = UpdateCheck.parseRelease(answer("v2.1.0"))
        assertNotNull(r)
        assertEquals("2.1.0", r!!.version)
        assertEquals("https://example.test/r/v2.1.0", r.url)
        assertEquals("Notes", r.notes)
    }

    @Test
    fun `drafts, pre-releases and broken answers are ignored`() {
        assertNull(UpdateCheck.parseRelease(answer("v2.1.0", draft = true)))
        assertNull(UpdateCheck.parseRelease(answer("v2.1.0", pre = true)))
        assertNull(UpdateCheck.parseRelease("not json"))
        assertNull(UpdateCheck.parseRelease("""{"message":"API rate limit exceeded"}"""))
        assertNull(UpdateCheck.parseRelease(""))
    }

    @Test
    fun `available respects skipped version`() {
        assertEquals("2.1.0", UpdateCheck.available("2.0.0", answer("v2.1.0"), null)!!.version)
        assertNull(UpdateCheck.available("2.0.0", answer("v2.1.0"), "2.1.0"))
        assertNull(UpdateCheck.available("2.1.0", answer("v2.1.0"), null))
    }

    @Test
    fun `apk address and checksum come from the release`() {
        val sha = "a".repeat(64)
        val json = """{"tag_name":"v2.2.0","html_url":"https://example.test/r","body":"Notes\n\nSHA-256 of `shrugtv-2.2.0.apk`: `$sha`",
            "assets":[{"name":"shrugtv.apk","browser_download_url":"https://example.test/latest.apk"},
                      {"name":"shrugtv-2.2.0.apk","browser_download_url":"https://example.test/2.2.0.apk"}]}"""
        val r = UpdateCheck.parseRelease(json)!!
        assertEquals("https://example.test/2.2.0.apk", r.apkUrl)
        assertEquals(sha, r.sha256)
    }

    @Test
    fun `release without checksum or file has none`() {
        val r = UpdateCheck.parseRelease(answer("v2.2.0"))!!
        assertEquals("", r.apkUrl)
        assertEquals("", r.sha256)
        assertEquals("", UpdateCheck.checksumIn("SHA-256 of `x`: `abc`"))
    }

    @Test
    fun `release notes are shortened for the dialog`() {
        val notes = "### New\n- First **bold** change with `code`\n- Second\n\nSHA-256 of x: abc"
        assertEquals("New\n• First bold change with code\n• Second", UpdateCheck.shortNotes(notes))
        assertEquals("", UpdateCheck.shortNotes(""))
        assertTrue(UpdateCheck.shortNotes((1..20).joinToString("\n") { "line $it" }).endsWith("…"))
    }
}
