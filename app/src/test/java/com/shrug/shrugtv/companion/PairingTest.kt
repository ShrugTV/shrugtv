package com.shrug.shrugtv.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingTest {
    private var now = 1_000_000L
    private val p = Pairing(now = { now })

    @Test
    fun `token works once and only before it expires`() {
        val o = p.newOffer()
        assertEquals(32, o.token.length)
        assertEquals(6, o.pin.length)
        val phone = p.redeemToken(o.token, "UA")
        assertNotNull(phone)
        assertTrue(p.isValid(phone!!.sessionId))
        assertNull("second use", p.redeemToken(o.token, "UA"))

        val o2 = p.newOffer()
        now += Pairing.OFFER_MS + 1
        assertNull("expired", p.redeemToken(o2.token, "UA"))
    }

    @Test
    fun `wrong PINs end the offer`() {
        val o = p.newOffer()
        repeat(Pairing.MAX_PIN_TRIES) { assertNull(p.redeemPin("000000", "UA")) }
        assertNull("offer gone after too many tries", p.redeemPin(o.pin, "UA"))
    }

    @Test
    fun `right PIN pairs`() {
        val o = p.newOffer()
        assertNotNull(p.redeemPin(" ${o.pin} ", "UA"))
    }

    @Test
    fun `max three phones, oldest goes first, disconnect clears`() {
        val ids = (1..4).map { p.redeemToken(p.newOffer().token, "UA$it")!!.sessionId }
        assertFalse(p.isValid(ids[0]))
        assertTrue(ids.drop(1).all { p.isValid(it) })
        assertEquals(3, p.phones().size)
        p.disconnectAll()
        assertTrue(ids.none { p.isValid(it) })
        assertFalse(p.isValid(null))
    }

    @Test
    fun `only private addresses`() {
        for (ip in listOf("192.168.0.5", "10.1.2.3", "172.16.0.1", "172.31.255.255", "127.0.0.1", "::1", "fe80::1", "fd00::5", "::ffff:192.168.1.2")) {
            assertTrue(ip, CompanionServer.isPrivateAddress(ip))
        }
        for (ip in listOf("8.8.8.8", "172.32.0.1", "100.64.1.1", "2001:db8::1", "", null)) {
            assertFalse(ip.toString(), CompanionServer.isPrivateAddress(ip))
        }
    }
}
