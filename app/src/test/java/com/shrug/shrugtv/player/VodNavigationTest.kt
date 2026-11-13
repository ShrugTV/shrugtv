package com.shrug.shrugtv.player

import com.shrug.shrugtv.model.VodChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Categories on the timeline and stepped seeking. */
class VodNavigationTest {

    private fun chapter(name: String, startSec: Long, lengthSec: Long) = VodChapter(name, startSec * 1000, lengthSec * 1000, "")

    // 0-600 Just Chatting, 600-3600 Minecraft, 3600-4000 Just Chatting
    private val chapters = listOf(chapter("Just Chatting", 0, 600), chapter("Minecraft", 600, 3000), chapter("Just Chatting", 3600, 400))

    @Test
    fun `index at a position`() {
        assertEquals(0, VodChapters.indexAt(chapters, 0))
        assertEquals(0, VodChapters.indexAt(chapters, 599_999))
        assertEquals(1, VodChapters.indexAt(chapters, 600_000))
        assertEquals(2, VodChapters.indexAt(chapters, 3_900_000))
        assertEquals(-1, VodChapters.indexAt(listOf(chapter("late", 100, 50)), 10_000))
    }

    @Test
    fun `chapter ends at the next start, the last one by its length or the video end`() {
        assertEquals(600_000L, VodChapters.endMs(chapters, 0, 4_000_000))
        assertEquals(3_600_000L, VodChapters.endMs(chapters, 1, 4_000_000))
        assertEquals(4_000_000L, VodChapters.endMs(chapters, 2, 4_000_000))
        assertEquals(4_000_000L, VodChapters.endMs(chapters, 2, 9_000_000)) // 3600 + 400 s
        assertEquals(4_000_000L, VodChapters.endMs(listOf(VodChapter("x", 0, 0, "")), 0, 4_000_000))
    }

    @Test
    fun `next goes to the following chapter, nothing after the last`() {
        assertEquals(600_000L, VodChapters.next(chapters, 100_000)!!.positionMs)
        assertEquals(3_600_000L, VodChapters.next(chapters, 700_000)!!.positionMs)
        assertNull(VodChapters.next(chapters, 3_700_000))
        // within a second before a start (rounding after a jump) that start does not count as ahead
        assertEquals(3_600_000L, VodChapters.next(chapters, 599_500)!!.positionMs)
    }

    @Test
    fun `previous goes to the start of this chapter, within 5 s to the one before`() {
        assertEquals(600_000L, VodChapters.previous(chapters, 900_000)!!.positionMs)
        assertEquals(0L, VodChapters.previous(chapters, 603_000)!!.positionMs)
        assertEquals(0L, VodChapters.previous(chapters, 2_000)!!.positionMs)
        assertNull(VodChapters.previous(listOf(chapter("late", 100, 50)), 10_000))
    }

    private var clock = 0L
    private val stepper = SeekStepper { clock }

    @Test
    fun `steps grow with repeated presses and stop at the largest`() {
        val steps = (1..12).map { clock += 100; stepper.press(1).stepMs / 1000 }
        assertEquals(listOf(10L, 10, 10, 20, 30, 60, 120, 180, 300, 300, 300, 300), steps)
    }

    @Test
    fun `total is the signed sum, a pause or the other direction starts over`() {
        clock = 1000; assertEquals(10_000L, stepper.press(1).totalMs)
        clock += 200; assertEquals(20_000L, stepper.press(1).totalMs)
        clock += 200; assertEquals(-10_000L, stepper.press(-1).totalMs)
        clock += 1000; val after = stepper.press(-1)
        assertEquals(-10_000L, after.stepMs)
        assertEquals(-10_000L, after.totalMs)
    }

    @Test
    fun `hud shows the sum and the target`() {
        assertEquals("+30 s  →  00:20:30", SeekStepper.hudText(30_000, 1_230_000))
        assertEquals("+1:30  →  01:42:10", SeekStepper.hudText(90_000, 6_130_000))
        assertEquals("−2:05  →  00:10:00", SeekStepper.hudText(-125_000, 600_000))
    }
}
