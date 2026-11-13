package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAnimationTest {
    @Test
    fun `values map to animations, unknown or missing means slide`() {
        assertEquals(ChatAnimation.OFF, ChatAnimation.of("off"))
        assertEquals(ChatAnimation.FADE, ChatAnimation.of("fade"))
        assertEquals(ChatAnimation.SLOW, ChatAnimation.of("slow"))
        assertEquals(ChatAnimation.SLIDE, ChatAnimation.of(null))
        assertEquals(ChatAnimation.SLIDE, ChatAnimation.of("wobble"))
    }

    @Test
    fun `only the sliding ones move the old lines`() {
        assertEquals(0L, ChatAnimation.FADE.moveMs)
        assertTrue(ChatAnimation.SLIDE.moveMs > 0 && ChatAnimation.SLOW.moveMs > ChatAnimation.SLIDE.moveMs)
        assertEquals(0L, ChatAnimation.OFF.addMs)
    }
}
