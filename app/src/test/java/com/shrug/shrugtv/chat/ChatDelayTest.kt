package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Chat delay matching the picture. */
class ChatDelayTest {
    @Test
    fun `automatic delay follows the live offset minus the viewer lead`() {
        val d = ChatDelay(auto = true, fixedMs = 0) { 7_500L }
        assertEquals(5_500L, d.delayFor(1_000L))
    }

    @Test
    fun `fixed delay is used as it is, zero switches it off`() {
        assertEquals(3_000L, ChatDelay(auto = false, fixedMs = 3_000) { 9_000L }.delayFor(0L))
        assertEquals(0L, ChatDelay(auto = false, fixedMs = 0) { 9_000L }.delayFor(0L))
    }

    @Test
    fun `without a usable offset the last value stays, at first no delay`() {
        var offset: Long? = null
        val d = ChatDelay(auto = true, fixedMs = 0) { offset }
        assertEquals(0L, d.delayFor(0L))
        offset = 6_000L
        assertEquals(4_000L, d.delayFor(10_000L))
        offset = null
        assertEquals(4_000L, d.delayFor(20_000L))
    }

    @Test
    fun `a shrinking latency never reorders messages`() {
        var offset = 10_000L
        val d = ChatDelay(auto = true, fixedMs = 0) { offset }
        val first = 0L + d.delayFor(0L)
        offset = 2_000L
        val second = 10L + d.delayFor(10L)
        assert(second >= first) { "second due $second before first $first" }
    }

    @Test
    fun `implausible offsets are ignored`() {
        assertNull(ChatDelay.sane(Long.MIN_VALUE + 1))
        assertNull(ChatDelay.sane(-5L))
        assertNull(ChatDelay.sane(120_000L))
        assertEquals(7_000L, ChatDelay.sane(7_000L))
    }
}
