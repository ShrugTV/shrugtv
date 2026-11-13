package com.shrug.shrugtv.ui.tv.player

import com.shrug.shrugtv.chat.ChatMessage
import com.shrug.shrugtv.eventSub.ChatEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chat lines of the player: cap, dedup, moderation (chat plan A1, 2.2, 2.3). */
class ChatViewModelTest {

    private fun msg(i: Int, login: String = "u$i", notice: Boolean = false): ChatMessage =
        ChatMessage.Builder().text("m$i").messageId("id$i").displayName(login).chatterLogin(login).notice(notice).build()

    @Test
    fun `list is capped and ids keep growing`() {
        val vm = ChatViewModel()
        for (i in 0 until 250) vm.addMessage(msg(i))
        val lines = vm.lines.value
        assertEquals(ChatViewModel.MAX_MESSAGES, lines.size)
        assertEquals("m249", lines.last().message.text)
        assertTrue(lines.zipWithNext().all { (a, b) -> b.id > a.id })
    }

    @Test
    fun `duplicate message ids are dropped`() {
        val vm = ChatViewModel()
        vm.addMessage(msg(1))
        vm.addMessage(msg(1))
        assertEquals(1, vm.lines.value.size)
    }

    @Test
    fun `delete marks one message`() {
        val vm = ChatViewModel()
        vm.addMessage(msg(1)); vm.addMessage(msg(2))
        vm.deleteMessage("id2")
        assertFalse(vm.lines.value[0].deleted)
        assertTrue(vm.lines.value[1].deleted)
    }

    @Test
    fun `timeout marks all messages of the user, case-insensitive`() {
        val vm = ChatViewModel()
        vm.addMessage(msg(1, "troll")); vm.addMessage(msg(2, "nice")); vm.addMessage(msg(3, "troll"))
        vm.deleteUserMessages("Troll")
        assertEquals(listOf(true, false, true), vm.lines.value.map { it.deleted })
    }

    @Test
    fun `clear keeps notices`() {
        val vm = ChatViewModel()
        vm.addMessage(msg(1)); vm.addMessage(msg(2, notice = true))
        vm.deleteAll()
        assertEquals(listOf(true, false), vm.lines.value.map { it.deleted })
    }

    @Test
    fun `room settings are kept until clear`() {
        val vm = ChatViewModel()
        vm.setRoomSettings(ChatEvent.RoomSettings(false, null, 30, false, false))
        assertEquals(30, vm.roomSettings.value!!.slowSeconds)
        vm.clear()
        assertNull(vm.roomSettings.value)
    }

    private fun said(i: Int, text: String) =
        ChatMessage.Builder().text(text).messageId("s$i").displayName("u$i").chatterLogin("u$i").build()

    @Test
    fun `identical texts merge into one line with a counter while the window runs`() {
        var t = 0L
        val vm = ChatViewModel().apply { now = { t }; mergeWindowMs = 10_000L }
        vm.addMessage(said(1, "!100"))
        t = 2_000L; vm.addMessage(said(2, "!100 "))
        t = 3_000L; vm.addMessage(said(3, "hallo"))
        t = 9_000L; vm.addMessage(said(4, "!100"))
        val lines = vm.lines.value
        assertEquals(2, lines.size)
        assertEquals(3, lines[0].repeat)
        assertEquals("hallo", lines[1].message.text)
        // the window counts from the last repetition: 9 s + 10 s
        t = 18_500L; vm.addMessage(said(5, "!100"))
        assertEquals(2, vm.lines.value.size)
        assertEquals(4, vm.lines.value[0].repeat)
        t = 40_000L; vm.addMessage(said(6, "!100"))
        assertEquals(3, vm.lines.value.size)
    }

    @Test
    fun `merging is off by default and ignores deleted lines`() {
        val t = 0L
        val off = ChatViewModel().apply { now = { t } }
        off.addMessage(said(1, "!100")); off.addMessage(said(2, "!100"))
        assertEquals(2, off.lines.value.size)
        val vm = ChatViewModel().apply { now = { t }; mergeWindowMs = 10_000L }
        vm.addMessage(said(3, "spam")); vm.deleteMessage("s3"); vm.addMessage(said(4, "spam"))
        assertEquals(2, vm.lines.value.size)
        assertNull(ChatViewModel.mergeKey("   "))
        assertEquals("a b", ChatViewModel.mergeKey(" A   b "))
        // invisible characters chat tools add to repeat a message
        assertEquals("!stuhl", ChatViewModel.mergeKey("!stuhl \uDB40\uDC00"))
        assertEquals("!stuhl", ChatViewModel.mergeKey("!stuhl\u034F"))
        assertEquals("!stuhl", ChatViewModel.mergeKey("!st\u200Buhl"))
    }
}
