package com.shrug.shrugtv.net

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutionException

/** Error bodies were handed to callers as normal results (docs/bug_review.md B17). */
class HttpStatusTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `json request fails with the status on 403`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"Forbidden","status":403,"message":"missing scope"}"""))
        try {
            Http.load(server.url("/x").toString()).asJsonObject().get()
            fail("403 must not be a result")
        } catch (e: ExecutionException) {
            val status = Http.StatusException.of(e)
            assertNotNull(status)
            assertEquals(403, status!!.code)
            assertEquals(true, status.body.contains("missing scope"))
        }
    }

    @Test
    fun `json request returns the body on 200`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":[{"id":"1"}]}"""))
        val json = Http.load(server.url("/x").toString()).asJsonObject().get()
        assertEquals("1", json.getAsJsonArray("data")[0].asJsonObject.get("id").asString)
    }

    @Test
    fun `raw string keeps the body of an error (restricted VOD playlists)`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""[{"error":"vod_manifest_restricted"}]"""))
        val text = Http.load(server.url("/vod.m3u8").toString()).asString().get()
        assertEquals(true, text.contains("vod_manifest_restricted"))
    }
}
