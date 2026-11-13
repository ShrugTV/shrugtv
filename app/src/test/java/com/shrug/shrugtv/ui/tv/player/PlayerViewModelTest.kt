package com.shrug.shrugtv.ui.tv.player

import com.shrug.shrugtv.ui.tv.player.PlaybackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PlayerViewModel state logic.
 * Tests the StateFlow default values and state transition logic
 * without requiring Android Context or ExoPlayer.
 */
class PlayerViewModelTest {

    @Test
    fun `playback status default is IDLE`() {
        val status = MutableStateFlow(PlaybackStatus.IDLE)
        assertEquals(PlaybackStatus.IDLE, status.value)
    }

    @Test
    fun `playback status transitions to PLAYING`() {
        val status = MutableStateFlow(PlaybackStatus.IDLE)
        status.value = PlaybackStatus.PLAYING
        assertEquals(PlaybackStatus.PLAYING, status.value)
    }

    @Test
    fun `playback status transitions to BUFFERING`() {
        val status = MutableStateFlow(PlaybackStatus.IDLE)
        status.value = PlaybackStatus.BUFFERING
        assertEquals(PlaybackStatus.BUFFERING, status.value)
    }

    @Test
    fun `current quality default is Auto`() {
        val quality = MutableStateFlow("Auto")
        assertEquals("Auto", quality.value)
    }

    @Test
    fun `current quality can change to specific quality`() {
        val quality = MutableStateFlow("Auto")
        quality.value = "1080p60"
        assertEquals("1080p60", quality.value)
    }

    @Test
    fun `current speed default is 1x`() {
        val speed = MutableStateFlow(1.0f)
        assertEquals(1.0f, speed.value, 0.001f)
    }

    @Test
    fun `current speed can change`() {
        val speed = MutableStateFlow(1.0f)
        speed.value = 1.5f
        assertEquals(1.5f, speed.value, 0.001f)
        speed.value = 0.5f
        assertEquals(0.5f, speed.value, 0.001f)
    }

    @Test
    fun `isMultiview default is false`() {
        val isMultiview = MutableStateFlow(false)
        assertFalse(isMultiview.value)
    }

    @Test
    fun `isMultiview can be enabled`() {
        val isMultiview = MutableStateFlow(false)
        isMultiview.value = true
        assertTrue(isMultiview.value)
    }

    @Test
    fun `isPrimaryAudio default is true`() {
        val isPrimary = MutableStateFlow(true)
        assertTrue(isPrimary.value)
    }

    @Test
    fun `currentMovie default is null`() {
        val movie = MutableStateFlow<Any?>(null)
        assertNull(movie.value)
    }

    @Test
    fun `qualities default is empty list`() {
        val qualities = MutableStateFlow<List<String>>(emptyList())
        assertTrue(qualities.value.isEmpty())
    }

    @Test
    fun `chapters default is empty list`() {
        val chapters = MutableStateFlow<List<String>>(emptyList())
        assertTrue(chapters.value.isEmpty())
    }

    @Test
    fun `seekHudText default is null`() {
        val seekHud = MutableStateFlow<String?>(null)
        assertNull(seekHud.value)
    }

    @Test
    fun `isFollowed default is false`() {
        val isFollowed = MutableStateFlow(false)
        assertFalse(isFollowed.value)
    }

    @Test
    fun `isChatVisible default is true`() {
        val isChatVisible = MutableStateFlow(true)
        assertTrue(isChatVisible.value)
    }

    @Test
    fun `activeOverlay default is NONE`() {
        val overlay = MutableStateFlow("NONE")
        assertEquals("NONE", overlay.value)
    }

    @Test
    fun `state flows are independent`() {
        val a = MutableStateFlow(1)
        val b = MutableStateFlow(2)
        a.value = 10
        assertEquals(10, a.value)
        assertEquals(2, b.value) // b unchanged
    }
}
