package com.shrug.shrugtv.player.hls

import android.app.Application
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * A playlist response that breaks off (log of 2026-11-01: "Unexpected end of ZLIB input stream")
 * must end the load with an error the player can retry, not spin forever on the loader thread.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class PlaylistReadErrorTest {

    /** Delivers the start of a playlist, then fails on every further read, like a cut-off gzip stream. */
    private class BrokenStream : InputStream() {
        private val head = "#EXTM3U\n#EXT-X-TARGETDURATION:2\n".toByteArray()
        private var pos = 0
        var reads = 0
        override fun read(): Int {
            reads++
            if (pos < head.size) return head[pos++].toInt()
            throw EOFException("Unexpected end of ZLIB input stream")
        }
    }

    @Test(timeout = 5_000)
    fun `a broken response ends with an IOException`() {
        val stream = BrokenStream()
        var error: Throwable? = null
        try {
            TwitchHlsPlaylistParser().parse(Uri.parse("https://video-weaver.example/v1/playlist/x.m3u8"), stream)
        } catch (e: IOException) {
            error = e
        }
        assertTrue("IOException expected, got $error", error is IOException)
        assertTrue("no endless re-reading (${stream.reads} reads)", stream.reads < 10_000)
    }
}
