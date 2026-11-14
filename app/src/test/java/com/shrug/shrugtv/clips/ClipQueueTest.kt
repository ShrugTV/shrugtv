package com.shrug.shrugtv.clips

import com.shrug.shrugtv.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The clips that play one after the other. */
class ClipQueueTest {
    private fun clip(slug: String) = Movie().apply { setClipSlug(slug) }
    private val list = listOf("a", "b", "c", "d").map(::clip)

    @Test
    fun `the next clip follows in the list`() {
        assertEquals("b", ClipQueue.nextIn(list, "a", emptySet())!!.clipSlug)
        assertEquals("d", ClipQueue.nextIn(list, "c", emptySet())!!.clipSlug)
    }

    @Test
    fun `opened clips are skipped`() {
        assertEquals("d", ClipQueue.nextIn(list, "a", setOf("b", "c"))!!.clipSlug)
        assertNull(ClipQueue.nextIn(list, "c", setOf("d")))
    }

    @Test
    fun `neighbours ignore what was opened and stop at the ends`() {
        assertEquals("b", ClipQueue.neighborIn(list, "c", -1)!!.clipSlug)
        assertEquals("d", ClipQueue.neighborIn(list, "c", 1)!!.clipSlug)
        assertNull(ClipQueue.neighborIn(list, "a", -1))
        assertNull(ClipQueue.neighborIn(list, "d", 1))
        assertNull(ClipQueue.neighborIn(list, "zzz", 1))
    }

    @Test
    fun `place in the queue, none when alone`() {
        ClipQueue.set(list)
        assertEquals(3 to 4, ClipQueue.position(list[2]))
        ClipQueue.set(listOf(list[0]))
        assertNull(ClipQueue.position(list[0]))
        ClipQueue.clear()
    }

    @Test
    fun `end of the list and unknown clips have no next one`() {
        assertNull(ClipQueue.nextIn(list, "d", emptySet()))
        assertNull(ClipQueue.nextIn(list, "zzz", emptySet()))
        assertNull(ClipQueue.nextIn(emptyList(), "a", emptySet()))
    }

    @Test
    fun `the queue keeps only clips and forgets on clear`() {
        ClipQueue.set(list + Movie())
        assertEquals("b", ClipQueue.next(list[0], emptySet())!!.clipSlug)
        ClipQueue.clear()
        assertNull(ClipQueue.next(list[0], emptySet()))
    }
}
