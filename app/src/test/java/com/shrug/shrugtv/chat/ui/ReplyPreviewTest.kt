package com.shrug.shrugtv.chat.ui

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shrug.shrugtv.chat.ChatExtras
import com.shrug.shrugtv.chat.ChatMessage
import com.shrug.shrugtv.chat.ChatPrefs
import com.shrug.shrugtv.chat.ChatReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference

/** The line above a reply names the answered message long enough to read it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class ReplyPreviewTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun render(body: String, author: String = "Alice", chars: Int = ChatPrefs.REPLY_CHARS_DEFAULT): String {
        val msg = ChatMessage.Builder().text("yes").displayName("Bob")
            .reply(ChatReply("p1", author.lowercase(), author, body)).build()
        return ChatMessageRenderer(WeakReference(ctx), 20, 28, false, extras = ChatExtras(replyChars = chars))
            .render(msg).formatted!!.toString().lineSequence().first()
    }

    @Test
    fun `a message of normal length is shown whole`() {
        val body = "I think the boss needs a different strategy on the third phase"
        assertEquals("↪ @Alice: $body", render(body))
    }

    @Test
    fun `a long message is cut with an ellipsis at about two lines`() {
        val line = render("x".repeat(300))
        assertTrue(line.endsWith("…"))
        assertTrue("two lines of the panel, not one short stub: ${line.length}", line.length in 60..95)
    }

    @Test
    fun `a long name leaves at least 10 characters of the message`() {
        assertEquals(10, ChatMessageRenderer.replyBodyRoom("N".repeat(100)))
        assertEquals(73, ChatMessageRenderer.replyBodyRoom("Alice"))
    }

    @Test
    fun `the setting decides where the message is cut`() {
        val body = "y".repeat(200)
        val short = render(body, chars = 40)
        val long = render(body, chars = 200)
        assertTrue(short.endsWith("…"))
        assertTrue("shorter with 40 than with 200: ${short.length} / ${long.length}", short.length < long.length)
        assertEquals("↪ @Alice: ".length + (40 - "Alice".length), short.length)
        assertEquals("↪ @Alice: ".length + (200 - "Alice".length), long.length)
    }
}
