package com.shrug.shrugtv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualityMatchTest {

    @Test
    fun `quality names are parsed`() {
        assertEquals(1080 to 60, QualityMatch.parse("1080p60 (source)"))
        assertEquals(720 to 60, QualityMatch.parse("720p60"))
        assertEquals(480 to 30, QualityMatch.parse("480p"))
        assertEquals(160 to 30, QualityMatch.parse("160p30"))
        assertNull(QualityMatch.parse("Auto"))
        assertNull(QualityMatch.parse("audio_only"))
        assertNull(QualityMatch.parse(null))
    }

    @Test
    fun `audio only is recognised`() {
        assertEquals(true, QualityMatch.isAudioOnly("audio_only"))
        assertEquals(true, QualityMatch.isAudioOnly("Audio Only"))
        assertEquals(false, QualityMatch.isAudioOnly("720p60"))
        assertEquals(false, QualityMatch.isAudioOnly(null))
    }

    @Test
    fun `display names are readable`() {
        assertEquals("1080p60 (Source)", QualityMatch.displayName("1080p60 (source)", "Source", "Audio only"))
        assertEquals("Audio only", QualityMatch.displayName("audio_only", "Source", "Audio only"))
        assertEquals("720p60", QualityMatch.displayName("720p60", "Source", "Audio only"))
        assertEquals("Auto", QualityMatch.displayName("Auto", "Source", "Audio only"))
    }

    @Test
    fun `track with same height and closest frame rate wins`() {
        val tracks = listOf(1080 to 60f, 720 to 60f, 720 to 30f, 480 to -1f)
        assertEquals(1, QualityMatch.bestTrack(tracks, 720 to 60))
        assertEquals(2, QualityMatch.bestTrack(tracks, 720 to 30))
        assertEquals(3, QualityMatch.bestTrack(tracks, 480 to 30))
        assertEquals(-1, QualityMatch.bestTrack(tracks, 360 to 30))
    }
}
