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
}
