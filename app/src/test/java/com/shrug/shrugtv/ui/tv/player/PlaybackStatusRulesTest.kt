package com.shrug.shrugtv.ui.tv.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** The error of a failed playback stays on the screen. */
class PlaybackStatusRulesTest {
    @Test
    fun `going idle after the failure keeps the error`() {
        assertEquals(PlaybackStatus.ERROR, PlaybackStatusRules.afterStateChange(PlaybackStatus.ERROR, PlaybackStatus.IDLE))
    }

    @Test
    fun `stopping to play after the failure keeps the error`() {
        assertEquals(PlaybackStatus.ERROR, PlaybackStatusRules.afterPlayingChange(PlaybackStatus.ERROR, false))
    }

    @Test
    fun `a new attempt replaces the error`() {
        assertEquals(PlaybackStatus.BUFFERING, PlaybackStatusRules.afterStateChange(PlaybackStatus.ERROR, PlaybackStatus.BUFFERING))
        assertEquals(PlaybackStatus.PLAYING, PlaybackStatusRules.afterStateChange(PlaybackStatus.ERROR, PlaybackStatus.PLAYING))
        assertEquals(PlaybackStatus.PLAYING, PlaybackStatusRules.afterPlayingChange(PlaybackStatus.ERROR, true))
    }

    @Test
    fun `without an error the status follows the player`() {
        assertEquals(PlaybackStatus.IDLE, PlaybackStatusRules.afterStateChange(PlaybackStatus.PLAYING, PlaybackStatus.IDLE))
        assertEquals(PlaybackStatus.PAUSED, PlaybackStatusRules.afterPlayingChange(PlaybackStatus.PLAYING, false))
        assertEquals(PlaybackStatus.ENDED, PlaybackStatusRules.afterStateChange(PlaybackStatus.PLAYING, PlaybackStatus.ENDED))
    }
}
