package com.shrug.shrugtv.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Log tab: logcat lines and the removal of secrets (always, also in the live view). */
class AppLogTest {

    @Test
    fun `logcat epoch lines are parsed`() {
        val l = AppLog.parse("  1730412345.678  1234  5678 W EventSub: Subscription channel.chat.message failed: HTTP 429", 7)!!
        assertEquals(7L, l.seq)
        assertEquals(1_730_412_345_678L, l.timeMs)
        assertEquals('W', l.level)
        assertEquals("EventSub", l.tag)
        assertEquals(1234, l.pid)
        assertEquals(5678, l.tid)
        assertEquals("Subscription channel.chat.message failed: HTTP 429", l.msg)
        // Padded tags and messages with colons
        val m = AppLog.parse("1730412345.000001  10  11 D ChatEmoteManager    : load7tvChannelEmotes: 12", 1)!!
        assertEquals("ChatEmoteManager", m.tag)
        assertEquals("load7tvChannelEmotes: 12", m.msg)
        assertNull(AppLog.parse("--------- beginning of main", 1))
    }

    @Test
    fun `tokens and passwords never reach the browser`() {
        val r = AppLog.redact("Authorization: Bearer abcdefghijklmnop12345 refresh_token=xyz123secret password: hunter2 /p/AbCdEfGhIjKlMnOpQrStUv")
        assertFalse(r, r.contains("abcdefghijklmnop12345"))
        assertFalse(r.contains("xyz123secret"))
        assertFalse(r.contains("hunter2"))
        assertFalse(r.contains("AbCdEfGhIjKlMnOpQrStUv"))
        assertTrue(r.contains("Bearer ***"))
        // Normal diagnostics stay readable
        assertEquals("HTTP 429 code: 7 broadcaster=123", AppLog.redact("HTTP 429 code: 7 broadcaster=123"))
    }
}
