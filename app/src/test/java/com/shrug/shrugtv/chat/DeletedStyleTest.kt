package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Settings value of "Deleted messages". */
class DeletedStyleTest {
    @Test
    fun `values map to styles, unknown or missing means struck through`() {
        assertEquals(DeletedStyle.STRIKE, DeletedStyle.of("strike"))
        assertEquals(DeletedStyle.GREY, DeletedStyle.of("grey"))
        assertEquals(DeletedStyle.HIDE, DeletedStyle.of("hide"))
        assertEquals(DeletedStyle.NORMAL, DeletedStyle.of("normal"))
        assertEquals(DeletedStyle.STRIKE, DeletedStyle.of(null))
        assertEquals(DeletedStyle.STRIKE, DeletedStyle.of("2"))
    }
}
