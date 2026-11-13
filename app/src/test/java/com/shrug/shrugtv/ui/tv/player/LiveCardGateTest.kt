package com.shrug.shrugtv.ui.tv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** "X is live" card in the player. */
class LiveCardGateTest {
    @Test
    fun `first card for another channel is shown`() {
        assertTrue(LiveCardGate.shouldShow(nowMs = 5_000, lastShownMs = 0, channelId = 7, currentChannelId = 3, otherCardUp = false))
    }

    @Test
    fun `not for the channel that is playing or without an id`() {
        assertFalse(LiveCardGate.shouldShow(5_000, 0, 3, 3, false))
        assertFalse(LiveCardGate.shouldShow(5_000, 0, 0, 3, false))
    }

    @Test
    fun `not while another card is up`() {
        assertFalse(LiveCardGate.shouldShow(5_000, 0, 7, 3, true))
    }

    @Test
    fun `at most one card per two minutes`() {
        assertFalse(LiveCardGate.shouldShow(100_000, 1_000, 7, 3, false))
        assertTrue(LiveCardGate.shouldShow(121_000, 1_000, 7, 3, false))
    }
}
