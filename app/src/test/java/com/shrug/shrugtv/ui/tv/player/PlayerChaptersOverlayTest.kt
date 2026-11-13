package com.shrug.shrugtv.ui.tv.player

import android.app.Application
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shrug.shrugtv.model.VodChapter
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The category list shows the portrait category pictures whole. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class PlayerChaptersOverlayTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `chapter pictures are portrait like the box art, not 16 to 9`() {
        val chapters = listOf(
            VodChapter("Just Chatting", 0L, 600_000L, ""),
            VodChapter("World of Warcraft", 600_000L, 1_200_000L, "")
        )
        compose.setContent {
            TvHomeTheme {
                PlayerChaptersOverlay(visible = true, chapters = chapters, currentPositionMs = 0L, onSelectChapter = {}, onDismiss = {})
            }
        }
        compose.waitForIdle()
        val image = compose.onAllNodesWithTag("chapter_image", useUnmergedTree = true).onFirst()
        val box = image.getBoundsInRoot()
        val ratio = (box.right - box.left).value / (box.bottom - box.top).value
        assertEquals("width to height of the picture", 245f / 342f, ratio, 0.02f)
    }
}
