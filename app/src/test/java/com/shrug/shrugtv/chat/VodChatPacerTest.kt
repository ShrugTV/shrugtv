package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pacing of the chat of a recording. */
class VodChatPacerTest {

    private var clock = 0L
    private val pacer = VodChatPacer { clock }

    private fun msg(offset: Int, id: String = "id$offset"): ChatMessage =
        ChatMessage.Builder().text("m$offset").messageId(id).vodOffsetSeconds(offset.toDouble()).build()

    @Test
    fun `first tick asks for the page at the position`() {
        val step = pacer.tick(100.0)
        assertTrue(step.reset)
        assertEquals(100L, step.fetchOffset)
        assertTrue(step.due.isEmpty())
    }

    @Test
    fun `messages come out when the position passes their offset, in order`() {
        pacer.tick(100.0)
        pacer.onMessage(msg(104)); pacer.onMessage(msg(102)); pacer.onMessage(msg(110))
        assertEquals(emptyList<String>(), pacer.tick(101.0).due.map { it.text })
        assertEquals(listOf("m102"), pacer.tick(102.0).due.map { it.text })
        assertEquals(listOf("m104"), pacer.tick(105.0).due.map { it.text })
    }

    @Test
    fun `repeated ids from overlapping pages are ignored`() {
        pacer.tick(100.0)
        pacer.onMessage(msg(101, "a")); pacer.onMessage(msg(101, "a"))
        assertEquals(1, pacer.tick(102.0).due.size)
    }

    @Test
    fun `stale messages are skipped instead of shown in a burst`() {
        pacer.tick(100.0)
        pacer.onMessage(msg(80)); pacer.onMessage(msg(95)); pacer.onMessage(msg(99))
        assertEquals(listOf("m95", "m99"), pacer.tick(101.0).due.map { it.text })
    }

    @Test
    fun `next page is requested behind the last offset, at most every 3 seconds`() {
        assertEquals(100L, pacer.tick(100.0).fetchOffset)
        pacer.onMessage(msg(104)); pacer.onMessage(msg(112))
        clock += 1000
        assertNull("not within 3 s of the last request", pacer.tick(101.0).fetchOffset)
        clock += 3000
        // 112 is less than 20 s ahead of 102: continue right behind the last offset
        assertEquals(113L, pacer.tick(102.0).fetchOffset)
    }

    @Test
    fun `nothing is requested while enough is waiting`() {
        pacer.tick(100.0)
        pacer.onMessage(msg(140))
        clock += 4000
        assertNull(pacer.tick(101.0).fetchOffset)
    }

    @Test
    fun `a page that arrives after a jump does not move the fetch point`() {
        pacer.tick(15.0)
        pacer.tick(1820.0) // the jump, e.g. from a clip to its recording
        pacer.onMessage(msg(40)); pacer.onMessage(msg(60)) // late answer for the old position
        clock += 3000
        assertEquals(1821L, pacer.tick(1821.0).fetchOffset) // at the position, not behind the late messages (61)
    }

    @Test
    fun `a jump starts over`() {
        pacer.tick(100.0)
        pacer.onMessage(msg(150))
        clock += 4000
        val jump = pacer.tick(600.0)
        assertTrue(jump.reset)
        assertEquals(600L, jump.fetchOffset)
        // the old page is forgotten, and its id may come again
        pacer.onMessage(msg(601, "id150"))
        assertEquals(1, pacer.tick(602.0).due.size)
    }

    @Test
    fun `playback speed up to two times is not a jump`() {
        pacer.tick(100.0)
        assertFalse(pacer.tick(102.0).reset)
        assertFalse(pacer.tick(104.0).reset)
    }
}
