package com.shrug.shrugtv.chat.ui

import android.app.Application
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shrug.shrugtv.chat.ChatExtras
import com.shrug.shrugtv.chat.ChatMessage
import com.shrug.shrugtv.chat.emote.Emote
import com.shrug.shrugtv.chat.seventv.SevenTvPaint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference

/** Zero-width emotes over the emote before them, 7TV paints (7TV features). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class ZeroWidthRenderTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun image() = ShapeDrawable().apply { intrinsicWidth = 28; intrinsicHeight = 28 }

    private fun emote(code: String, pos: String, zw: Boolean) =
        Emote.Builder().code(code).provider(Emote.Provider.SEVEN_TV).zeroWidth(zw).positions(listOf(pos)).build().apply { drawable = image() }

    private fun render(extras: ChatExtras, paint: SevenTvPaint? = null): ChatMessage {
        // "Kappa RainTime hi": RainTime (6-13) is an overlay of Kappa (0-4)
        val msg = ChatMessage.Builder().text("Kappa RainTime hi").displayName("Tester")
            .emotes(listOf(emote("Kappa", "0-4", false), emote("RainTime", "6-13", true)))
            .paint(paint).build()
        return ChatMessageRenderer(WeakReference(ctx), 20, 28, false, extras = extras).render(msg)
    }

    @Test
    fun `an overlay is stacked on the emote before it and its code hidden`() {
        val f = render(ChatExtras()).formatted!!
        val images = f.getSpans(0, f.length, CenteredImageSpan::class.java)
        assertEquals(1, images.size)
        assertTrue(images[0].drawable is StackedDrawable)
        assertEquals(1, f.getSpans(0, f.length, EmptySpan::class.java).size)
    }

    @Test
    fun `switched off, both emotes stand next to each other`() {
        val f = render(ChatExtras(zeroWidth = false)).formatted!!
        assertEquals(2, f.getSpans(0, f.length, CenteredImageSpan::class.java).size)
        assertEquals(0, f.getSpans(0, f.length, EmptySpan::class.java).size)
    }

    @Test
    fun `a 7TV paint draws the name`() {
        val paint = SevenTvPaint("p", intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt()), floatArrayOf(0f, 1f))
        val f = render(ChatExtras(), paint).formatted!!
        assertEquals(1, f.getSpans(0, f.length, PaintSpan::class.java).size)
    }
}
