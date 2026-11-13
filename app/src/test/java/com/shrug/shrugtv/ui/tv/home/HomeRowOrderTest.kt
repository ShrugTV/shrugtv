package com.shrug.shrugtv.ui.tv.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRowOrderTest {
    private val d = listOf("a", "b", "c", "d")

    @Test fun `nothing saved keeps the built-in order`() = assertEquals(d, HomeRowOrder.order(d, null))

    @Test fun `saved order wins, unknown ids drop out and missing rows follow`() =
        assertEquals(listOf("c", "a", "b", "d"), HomeRowOrder.order(d, "c,x,a"))

    @Test fun `move up swaps with the one above`() = assertEquals("a,c,b,d", HomeRowOrder.moveUp(d, null, "c"))

    @Test fun `the first row moves to the end`() = assertEquals("b,c,d,a", HomeRowOrder.moveUp(d, null, "a"))

    @Test fun `place is one based`() = assertEquals(3, HomeRowOrder.place(d, null, "c"))
}
