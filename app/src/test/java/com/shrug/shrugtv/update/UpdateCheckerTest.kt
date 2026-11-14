package com.shrug.shrugtv.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The network way of the update check against a mock server: answer, daily limit, ETag, rate
 * limit, broken and missing answers. The source is not fixed: GitHub and compatible hosts send the same fields.
 */
@RunWith(RobolectricTestRunner::class)
class UpdateCheckerTest {

    private lateinit var server: MockWebServer
    private lateinit var ctx: Context
    private val release = """{"tag_name":"v2.2.0","html_url":"https://example.test/r","body":"Notes","draft":false,"prerelease":false}"""

    @Before
    fun start() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx.getSharedPreferences("update_check", Context.MODE_PRIVATE).edit().clear().commit()
        server = MockWebServer().apply { start() }
    }

    @After
    fun stop() = server.shutdown()

    private fun url() = server.url("/releases/latest").toString()

    private fun check(force: Boolean = false, current: String = "2.1.0") = UpdateChecker.check(ctx, force, url(), current)

    @Test
    fun `newer release is reported and asked once a day`() {
        server.enqueue(MockResponse().setBody(release).setHeader("ETag", "\"abc\""))
        val r = check()
        assertTrue(r is UpdateChecker.Result.Available)
        assertEquals("2.2.0", (r as UpdateChecker.Result.Available).release.version)
        // second check within the day: answered from the stored text, no request
        assertTrue(check() is UpdateChecker.Result.Available)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `forced check sends the etag and takes 304 as unchanged`() {
        server.enqueue(MockResponse().setBody(release).setHeader("ETag", "\"abc\""))
        check()
        server.enqueue(MockResponse().setResponseCode(304))
        val r = check(force = true)
        assertTrue(r is UpdateChecker.Result.Available)
        server.takeRequest()
        assertEquals("\"abc\"", server.takeRequest().getHeader("If-None-Match"))
    }

    @Test
    fun `same version is up to date`() {
        server.enqueue(MockResponse().setBody(release))
        assertEquals(UpdateChecker.Result.UpToDate, check(current = "2.2.0"))
    }

    @Test
    fun `rate limit and server errors fail quietly`() {
        for (code in listOf(403, 429, 500)) {
            server.enqueue(MockResponse().setResponseCode(code).setBody("""{"message":"API rate limit exceeded"}"""))
            assertEquals("code $code", UpdateChecker.Result.Failed, check(force = true))
        }
    }

    @Test
    fun `broken answer is no update`() {
        server.enqueue(MockResponse().setBody("<html>not json</html>"))
        assertEquals(UpdateChecker.Result.UpToDate, check(force = true))
    }

    @Test
    fun `unreachable server fails quietly`() {
        val dead = server.url("/x").toString()
        server.shutdown()
        assertEquals(UpdateChecker.Result.Failed, UpdateChecker.check(ctx, true, dead, "2.1.0"))
    }

    @Test
    fun `empty source means no check`() {
        assertEquals(UpdateChecker.Result.UpToDate, UpdateChecker.check(ctx, true, "", "2.1.0"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `skipped version is not announced again`() {
        server.enqueue(MockResponse().setBody(release))
        check()
        UpdateChecker.skip(ctx, "2.2.0")
        assertEquals(UpdateChecker.Result.UpToDate, check())
    }
}
