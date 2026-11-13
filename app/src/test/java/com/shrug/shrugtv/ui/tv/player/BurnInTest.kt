package com.shrug.shrugtv.ui.tv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BurnInTest {
    @Test
    fun `drift stays within the amplitude and really moves`() {
        val spots = (0 until 100).map { BurnIn.drift(it * 15_000L, 8) }
        assertTrue(spots.all { kotlin.math.abs(it.first) <= 8 && kotlin.math.abs(it.second) <= 8 })
        assertTrue(spots.map { it.first }.toSet().size > 5)
        assertTrue(spots.map { it.second }.toSet().size > 5)
        assertEquals(0 to 0, BurnIn.drift(0, 8))
    }

    @Test
    fun `rest spot stays inside the area and differs from step to step`() {
        val spots = (0L until 50L).map { BurnIn.restSpot(it, 1280, 720, 420, 190) }
        assertTrue(spots.all { it.first in 0..860 && it.second in 0..530 })
        for (i in 1 until spots.size) assertTrue("step $i", spots[i] != spots[i - 1])
        // consecutive places are well apart, not a jitter
        assertTrue(spots.zipWithNext().all { (a, b) -> kotlin.math.abs(a.first - b.first) > 100 })
    }

    @Test
    fun `rest spot in a too small area is the corner`() {
        assertEquals(0 to 0, BurnIn.restSpot(7, 300, 100, 420, 190))
    }
}
