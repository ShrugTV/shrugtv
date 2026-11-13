package com.shrug.shrugtv.ui.tv.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Time keys for images. */
class ImageTickTest {

    @Test
    fun livePreviewDetected() {
        assertTrue(ImageTick.isLivePreview("https://static-cdn.jtvnw.net/previews-ttv/live_user_lirik-800x450.jpg"))
        assertFalse(ImageTick.isLivePreview("https://static-cdn.jtvnw.net/jtv_user_pictures/abc-profile_image-70x70.png"))
        assertFalse(ImageTick.isLivePreview("https://static-cdn.jtvnw.net/ttv-boxart/33214-272x380.jpg"))
    }

    @Test
    fun liveChangesEveryMinute() {
        val t0 = 1_793_000_000_000L - 1_793_000_000_000L % ImageTick.LIVE_MS
        assertEquals(ImageTick.live(t0), ImageTick.live(t0 + ImageTick.LIVE_MS - 1))
        assertNotEquals(ImageTick.live(t0), ImageTick.live(t0 + ImageTick.LIVE_MS))
        assertEquals(ImageTick.tag(t0), ImageTick.tag(t0 + ImageTick.LIVE_MS))
    }
}
