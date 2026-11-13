package com.shrug.shrugtv.ui.tv.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** Stepping through the channel lists with up and down. */
class SwitcherNavTest {
    @Test
    fun `up goes to the next list and wraps after the last`() {
        assertEquals(1, SwitcherNav.step(0, 1, 6))
        assertEquals(5, SwitcherNav.step(4, 1, 6))
        assertEquals(0, SwitcherNav.step(5, 1, 6))
    }

    @Test
    fun `down goes back and closes on the first list`() {
        assertEquals(2, SwitcherNav.step(3, -1, 6))
        assertEquals(0, SwitcherNav.step(1, -1, 6))
        assertEquals(SwitcherNav.CLOSE, SwitcherNav.step(0, -1, 6))
    }

    @Test
    fun `skipping empty lists keeps the direction, wraps both ways and never closes`() {
        assertEquals(2, SwitcherNav.skip(1, 1, 6))
        assertEquals(0, SwitcherNav.skip(5, 1, 6))
        assertEquals(5, SwitcherNav.skip(0, -1, 6))
    }

    @Test
    fun `no lists means close`() {
        assertEquals(SwitcherNav.CLOSE, SwitcherNav.step(0, 1, 0))
    }

    @Test
    fun `only the lists that are switched on are shown, in their order`() {
        val on = setOf(ChannelSwitcherTab.TOP_STREAMS, ChannelSwitcherTab.FOLLOWED)
        assertEquals(listOf(ChannelSwitcherTab.FOLLOWED, ChannelSwitcherTab.TOP_STREAMS), SwitcherNav.enabled { it in on })
    }

    @Test
    fun `all lists off leaves Followed`() {
        assertEquals(listOf(ChannelSwitcherTab.FOLLOWED), SwitcherNav.enabled { false })
    }

    @Test
    fun `every list has its own setting key`() {
        assertEquals(ChannelSwitcherTab.values().size, ChannelSwitcherTab.values().map { it.key }.toSet().size)
    }
}
