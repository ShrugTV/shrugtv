package com.shrug.shrugtv.ui.tv.player

import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shrug.shrugtv.chat.ChatMessage
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The chat panel keeps showing new lines after its text size changed. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class ChatPanelTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun line(i: Int) = ChatLine(i.toLong(), ChatMessage.Builder().text("message $i").messageId("m$i").displayName("u").build())

    private fun recyclers(v: View, out: MutableList<RecyclerView> = mutableListOf()): List<RecyclerView> {
        if (v is RecyclerView) out.add(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) recyclers(v.getChildAt(i), out)
        return out
    }

    @Test
    fun `lines added after a text size change reach the visible list`() {
        val lines = mutableStateOf(listOf(line(1), line(2)))
        val size = mutableStateOf(14f)
        compose.setContent {
            TvHomeTheme { ChatPanel(visible = true, lines = lines.value, textSizeSp = size.value) }
        }
        compose.waitForIdle()

        size.value = 18f
        compose.waitForIdle()
        lines.value = lines.value + line(3) + line(4)
        compose.waitForIdle()

        // The list diffs on a background thread and posts the result to the main thread
        var count = -1
        for (i in 0 until 60) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.waitForIdle()
            val lists = recyclers(compose.activity.window.decorView)
            assertEquals("exactly one list in the panel", 1, lists.size)
            assertNotNull(lists.single().adapter)
            count = lists.single().adapter!!.itemCount
            if (count == 4) break
            Thread.sleep(50)
        }
        assertEquals("the visible list has all four lines", 4, count)
    }
}
