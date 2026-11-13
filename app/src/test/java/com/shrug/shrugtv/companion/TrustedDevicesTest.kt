package com.shrug.shrugtv.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Remembered companion devices: key in the cookie, only its hash on the TV. */
class TrustedDevicesTest {

    private class MemoryStore : TrustedDevices.Store {
        var json: String? = null
        override fun load() = json
        override fun save(json: String) { this.json = json }
    }

    private var clock = 1_000_000L

    @Test
    fun `a paired device is found again, also after a restart`() {
        val store = MemoryStore()
        val devices = TrustedDevices(store, { clock })
        val (device, key) = devices.add("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit Version/17.0 Mobile Safari/604.1", "192.168.0.23")
        assertEquals("Safari · iPhone", device.name)
        assertFalse("the key itself is never stored", store.json!!.contains(key))
        // App restart: a new instance reads the stored list
        clock += 3_600_000
        val again = TrustedDevices(store, { clock })
        val found = again.find(key, "192.168.0.24")
        assertNotNull(found)
        assertEquals(device.id, found!!.id)
        assertEquals(clock, found.lastSeen)
        assertNull(again.find("0".repeat(64), null))
        assertNull(again.find(null, null))
    }

    @Test
    fun `removing a device ends its access, the list is capped`() {
        val devices = TrustedDevices(MemoryStore(), { clock })
        val (first, key) = devices.add("Mozilla/5.0 (Windows NT 10.0) Chrome/130.0 Safari/537.36", null)
        assertTrue(devices.remove(first.id))
        assertNull(devices.find(key, null))
        repeat(TrustedDevices.MAX_DEVICES + 3) { clock += 1_000; devices.add("Firefox/130.0 (X11; Linux x86_64)", null) }
        assertEquals(TrustedDevices.MAX_DEVICES, devices.list().size)
        devices.clear()
        assertTrue(devices.list().isEmpty())
    }

    @Test
    fun `device names from user agents`() {
        assertEquals("Chrome · Windows", TrustedDevices.deviceName("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"))
        assertEquals("Edge · Windows", TrustedDevices.deviceName("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/130.0 Safari/537.36 Edg/130.0"))
        assertEquals("Chrome · Android (Pixel 8)", TrustedDevices.deviceName("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/130.0 Mobile Safari/537.36"))
        assertEquals("Chrome · Android", TrustedDevices.deviceName("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 Chrome/130.0 Mobile Safari/537.36"))
        assertEquals("Firefox · Mac", TrustedDevices.deviceName("Mozilla/5.0 (Macintosh; Intel Mac OS X 14.0; rv:130.0) Gecko/20100101 Firefox/130.0"))
        assertEquals("Browser", TrustedDevices.deviceName(""))
    }
}
