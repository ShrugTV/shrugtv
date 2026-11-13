package com.shrug.shrugtv.chat.emote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The memory of recently seen channels. */
class ChannelCacheTest {
    private var clock = 0L
    private fun cache(max: Int = 3, ttl: Long = 1000) = ChannelCache<List<String>>(max, ttl) { clock }

    private fun ChannelCache<List<String>>.put(key: String, vararg v: String) = update(key, { emptyList() }) { v.toList() }

    @Test
    fun `a stored value comes back, an unknown channel does not`() {
        val c = cache()
        c.put("a", "x")
        assertEquals(listOf("x"), c.get("a"))
        assertNull(c.get("b"))
    }

    @Test
    fun `the channel used longest ago goes first`() {
        val c = cache(max = 3)
        c.put("a", "1"); c.put("b", "2"); c.put("c", "3")
        c.get("a") // a was used just now, b is the oldest
        c.put("d", "4")
        assertEquals(3, c.size)
        assertNull(c.get("b"))
        assertEquals(listOf("1"), c.get("a"))
        assertEquals(listOf("3"), c.get("c"))
        assertEquals(listOf("4"), c.get("d"))
    }

    @Test
    fun `an entry older than the time to live is gone`() {
        val c = cache(ttl = 1000)
        c.put("a", "x")
        clock = 1000
        assertEquals(listOf("x"), c.get("a"))
        clock = 1001
        assertNull(c.get("a"))
        assertEquals(0, c.size)
    }

    @Test
    fun `updating adds to what is there and renews the age`() {
        val c = ChannelCache<List<String>>(3, 1000) { clock }
        c.update("a", { emptyList() }) { it + "bttv" }
        clock = 900
        c.update("a", { emptyList() }) { it + "ffz" }
        clock = 1500 // 1500 after the first part, 600 after the second
        assertEquals(listOf("bttv", "ffz"), c.get("a"))
    }

    @Test
    fun `an expired entry is not built upon`() {
        val c = ChannelCache<List<String>>(3, 1000) { clock }
        c.update("a", { emptyList() }) { it + "old" }
        clock = 5000
        c.update("a", { emptyList() }) { it + "new" }
        assertEquals(listOf("new"), c.get("a"))
    }

    @Test
    fun `clear empties it`() {
        val c = cache()
        c.put("a", "x")
        c.clear()
        assertNull(c.get("a"))
    }
}
