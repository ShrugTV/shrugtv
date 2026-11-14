package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The source of a clip for its chat: the recording, the start second and the channel. */
class ClipSourceTest {

    @Test
    fun `recording, start second and channel are read`() {
        val i = ClipSource.parseText("""{"data":{"clip":{"videoOffsetSeconds":1820,"video":{"id":"2924941995"},"broadcaster":{"id":"22484632","login":"forsen","displayName":"forsen"}}}}""")!!
        assertEquals(2924941995L, i.videoId)
        assertEquals(1820, i.offsetSeconds)
        assertEquals(22484632, i.broadcasterId)
        assertEquals("forsen", i.login)
    }

    @Test
    fun `the title of the clip is read too`() {
        val i = ClipSource.parseText("""{"data":{"clip":{"title":"Forsen's take","videoOffsetSeconds":5,"video":{"id":"7"},"broadcaster":{"id":"1","login":"x","displayName":"X"}}}}""")!!
        assertEquals("Forsen's take", i.title)
    }

    @Test
    fun `a clip whose recording is gone has no chat`() {
        assertNull(ClipSource.parseText("""{"data":{"clip":{"videoOffsetSeconds":null,"video":null,"broadcaster":{"id":"1","login":"x","displayName":"X"}}}}"""))
        assertNull(ClipSource.parseText("""{"data":{"clip":{"videoOffsetSeconds":12,"video":{"id":"0"}}}}"""))
    }

    @Test
    fun `broken or empty answers give nothing`() {
        assertNull(ClipSource.parseText("""{"data":{"clip":null}}"""))
        assertNull(ClipSource.parseText("""{"errors":[{"message":"failed integrity check"}]}"""))
        assertNull(ClipSource.parse(null))
    }

    @Test
    fun `a missing offset is no source`() {
        assertNull(ClipSource.parseText("""{"data":{"clip":{"video":{"id":"5"}}}}"""))
    }
}
