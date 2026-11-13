package com.shrug.shrugtv.api.helix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for HelixApi URL building and parameter helpers.
 * Tests the pure functions that don't require network or Android context.
 */
class HelixApiTest {

    @Test
    fun `afterParam returns empty string for empty cursor`() {
        assertEquals("", HelixApi.afterParam(""))
    }

    @Test
    fun `afterParam returns encoded cursor parameter`() {
        assertEquals("&after=abc123", HelixApi.afterParam("abc123"))
    }

    @Test
    fun `afterParam handles special characters in cursor`() {
        val cursor = "abc+def/ghi="
        val result = HelixApi.afterParam(cursor)
        assertTrue(result.startsWith("&after="))
        assertTrue(result.contains(cursor))
    }

    @Test
    fun `first100Param returns correct parameter`() {
        assertEquals("&first=100", HelixApi.first100Param())
    }

    @Test
    fun `first50Param returns correct parameter`() {
        assertEquals("&first=50", HelixApi.first50Param())
    }

    @Test
    fun `afterParam with whitespace cursor`() {
        assertEquals("&after= ", HelixApi.afterParam(" "))
    }

    @Test
    fun `combined params build valid query string suffix`() {
        val cursor = "xyz789"
        val combined = HelixApi.first100Param() + HelixApi.afterParam(cursor)
        assertEquals("&first=100&after=xyz789", combined)
    }

    @Test
    fun `empty cursor with first param`() {
        val combined = HelixApi.first100Param() + HelixApi.afterParam("")
        assertEquals("&first=100", combined)
    }

    @Test
    fun `multiple after calls are independent`() {
        val a = HelixApi.afterParam("a")
        val b = HelixApi.afterParam("b")
        assertEquals("&after=a", a)
        assertEquals("&after=b", b)
    }

    @Test
    fun `first params are constants`() {
        // Verify they don't change between calls
        assertEquals(HelixApi.first100Param(), HelixApi.first100Param())
        assertEquals(HelixApi.first50Param(), HelixApi.first50Param())
    }
}
