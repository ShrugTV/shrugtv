package com.shrug.shrugtv.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lines of the chat history service (IRC with IRCv3 tags). */
class IrcMessageTest {

    @Test
    fun `PRIVMSG with tags is parsed`() {
        val m = IrcMessage.parse("@badge-info=subscriber/14;badges=moderator/1,subscriber/12;color=#FF4500;display-name=Some\\sUser;" +
            "emotes=25:0-4;id=abc-1;user-id=42;historical=1 :someuser!someuser@someuser.tmi.twitch.tv PRIVMSG #papaplatte :Kappa hello : world")!!
        assertEquals("PRIVMSG", m.command)
        assertEquals(listOf("#papaplatte"), m.params)
        assertEquals("Kappa hello : world", m.text)
        assertEquals("someuser", m.nick)
        assertEquals("Some User", m.tag("display-name"))
        assertEquals("42", m.tag("user-id"))
        assertEquals(listOf("moderator" to "1", "subscriber" to "12"), m.badges)
        assertFalse(m.isAction)
        assertNull(m.tag("missing"))
    }

    @Test
    fun `actions and escapes`() {
        val m = IrcMessage.parse(":a!a@a PRIVMSG #c :\u0001ACTION waves\u0001")!!
        assertTrue(m.isAction)
        assertEquals("waves", m.text)
        assertEquals("a;b c\\d", IrcMessage.unescape("a\\:b\\sc\\\\d"))
        assertNull(IrcMessage.parse(""))
        assertEquals("PING", IrcMessage.parse("PING :tmi.twitch.tv")!!.command)
    }
}
