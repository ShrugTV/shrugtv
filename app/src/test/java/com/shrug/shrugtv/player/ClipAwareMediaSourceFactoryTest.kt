package com.shrug.shrugtv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipAwareMediaSourceFactoryTest {
    @Test
    fun `clip addresses are files, playlists are not`() {
        assertTrue(ClipAwareMediaSourceFactory.isFile("/nauth/b5ec1f44/landscape/h264/1080/index.mp4"))
        assertTrue(ClipAwareMediaSourceFactory.isFile("/clip/UPPER.MP4"))
        assertFalse(ClipAwareMediaSourceFactory.isFile("/api/channel/hls/forsen.m3u8"))
        assertFalse(ClipAwareMediaSourceFactory.isFile("/vod/12345/index-dvr.m3u8"))
        assertFalse(ClipAwareMediaSourceFactory.isFile(null))
        assertFalse(ClipAwareMediaSourceFactory.isFile("/mp4/playlist"))
    }
}
