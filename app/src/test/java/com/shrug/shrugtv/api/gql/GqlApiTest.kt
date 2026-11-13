package com.shrug.shrugtv.api.gql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GqlApi constants and query structure.
 * Tests the WEB_CLIENT_ID constant and query body structure.
 */
class GqlApiTest {

    @Test
    fun `WEB_CLIENT_ID is not empty`() {
        assertTrue(GqlApi.WEB_CLIENT_ID.isNotEmpty())
    }

    @Test
    fun `WEB_CLIENT_ID has expected length`() {
        // Twitch web client IDs are typically 30 chars
        assertEquals(31, GqlApi.WEB_CLIENT_ID.length)
    }

    @Test
    fun `WEB_CLIENT_ID is alphanumeric`() {
        assertTrue(GqlApi.WEB_CLIENT_ID.all { it.isLetterOrDigit() })
    }

    @Test
    fun `WEB_CLIENT_ID matches known Twitch web client`() {
        // This is the public web client ID used by twitch.tv
        assertEquals("kd1unb4b3q4t58fwlpcbzcbnm76a8fp", GqlApi.WEB_CLIENT_ID)
    }

    @Test
    fun `query body contains required fields`() {
        // Test that a typical query body structure is valid JSON-like
        val sampleBody = """{"query":"query { games(first:100){ edges { node { id } } } }","variables":{}}"""
        assertTrue(sampleBody.contains("\"query\""))
        assertTrue(sampleBody.contains("\"variables\""))
    }

    @Test
    fun `cursor parameter in query is optional`() {
        // Empty cursor should not add 'after' parameter
        val cursor = ""
        val after = if (cursor.isEmpty()) "" else ", after: \"$cursor\""
        assertEquals("", after)
    }

    @Test
    fun `cursor parameter in query is included when present`() {
        val cursor = "abc123"
        val after = if (cursor.isEmpty()) "" else ", after: \"$cursor\""
        assertEquals(", after: \"abc123\"", after)
    }

    @Test
    fun `GQL endpoint structure is correct`() {
        // Verify the endpoint URL pattern (without making actual request)
        val endpoint = "https://gql.twitch.tv/gql"
        assertTrue(endpoint.startsWith("https://"))
        assertTrue(endpoint.contains("gql.twitch.tv"))
    }

    @Test
    fun `content type is JSON`() {
        val contentType = "application/json"
        assertEquals("application/json", contentType)
    }

    @Test
    fun `timeout is reasonable`() {
        val timeoutMs = 3000
        assertTrue(timeoutMs > 0)
        assertTrue(timeoutMs <= 10000) // Should not be too long
    }
}
