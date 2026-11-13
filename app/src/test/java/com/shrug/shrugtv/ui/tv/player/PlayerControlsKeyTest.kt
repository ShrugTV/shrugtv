package com.shrug.shrugtv.ui.tv.player

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shrug.shrugtv.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Remote control in the player control bar, on the JVM with Robolectric (docs/bug_review.md
 * B28-B31). Plain Application: MainApp starts WorkManager, timers and the token refresh.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class PlayerControlsKeyTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val qualityClicks = mutableListOf<Unit>()
    private val chatClicks = mutableListOf<Unit>()
    private val chatLayoutClicks = mutableListOf<Unit>()
    private val seekSteps = mutableListOf<Int>()
    private val moreActions = mutableListOf<PlayerMoreAction>()
    private var dismissed = 0
    private val focusIndex = mutableStateOf(0)
    private val moreMenu = mutableStateOf(false)

    /** [width]: the bar's width; null = the Robolectric default (320 dp), where only a few buttons fit and "More" takes the rest. */
    private fun showBar(startIndex: Int, width: Dp? = null, vod: Boolean = false, compact: Boolean = false, height: Dp? = null) {
        focusIndex.value = startIndex
        compose.setContent {
            // A TV is in keyboard (non-touch) mode; in touch mode clickable menu entries are not
            // focusable, which is the Robolectric default
            val inputModeManager = LocalInputModeManager.current
            SideEffect { inputModeManager.requestInputMode(InputMode.Keyboard) }
            Box((if (width != null) Modifier.requiredWidth(width) else Modifier).let { if (height != null) it.requiredHeight(height) else it }) {
                PlayerControlsOverlay(
                    visible = true,
                    movie = null,
                    currentQuality = "Auto",
                    isFollowed = false,
                    isVod = vod,
                    compact = compact,
                    vodDurationMs = if (vod) 600_000L else 0L,
                    onSeekStep = { seekSteps.add(it) },
                    onQualityClick = { qualityClicks.add(Unit) },
                    onChatClick = { chatClicks.add(Unit) },
                    onChatLayoutClick = { chatLayoutClicks.add(Unit) },
                    onChannelClick = {},
                    onFollowClick = {},
                    onSearchClick = {},
                    onMoreAction = { moreActions.add(it) },
                    onDismiss = { dismissed++ },
                    buttonFocusIndex = focusIndex,
                    moreMenuState = moreMenu
                )
            }
        }
        // The bar requests focus for its button row after 350 ms
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }

    /** A real press: key down, held for [holdMs], key up. */
    private fun press(key: Key, holdMs: Long = 120) {
        compose.onRoot().performKeyInput { keyDown(key) }
        compose.mainClock.advanceTimeBy(holdMs)
        compose.onRoot().performKeyInput { keyUp(key) }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
    }

    private val menuEntry get() = context.getString(R.string.player_more_reload_emotes)

    @Test
    fun `one OK press on More opens the menu and selects nothing`() {
        showBar(startIndex = Int.MAX_VALUE) // clamped to "More", the last button
        press(Key.DirectionCenter)
        compose.onNodeWithText(menuEntry).assertExists()
        assertTrue("no menu entry may be selected by the same press", moreActions.isEmpty())
    }

    @Test
    fun `button acts on key-up, once`() {
        showBar(startIndex = 0) // "Quality"
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(120)
        assertEquals("not on key-down", 0, qualityClicks.size)
        compose.onRoot().performKeyInput { keyUp(Key.DirectionCenter) }
        compose.waitForIdle()
        assertEquals(1, qualityClicks.size)
    }

    @Test
    fun `Back in the More menu closes only the menu`() {
        showBar(startIndex = Int.MAX_VALUE)
        press(Key.DirectionCenter)
        compose.onNodeWithText(menuEntry).assertExists()
        press(Key.Back)
        compose.onNodeWithText(menuEntry).assertDoesNotExist()
        assertEquals("the bar itself stays", 0, dismissed)
    }

    @Test
    fun `focus stays in the More menu at its end`() {
        showBar(startIndex = Int.MAX_VALUE)
        press(Key.DirectionCenter)
        repeat(12) { press(Key.DirectionDown, holdMs = 30) }
        compose.onNodeWithText(menuEntry).assertExists()
        assertTrue(moreMenu.value)
    }

    @Test
    fun `OK triggers the highlighted button, not the first one`() {
        // The round buttons used to be focusable themselves and the row handed its focus to the
        // first one: whatever was highlighted, OK opened "Quality" (B32)
        showBar(startIndex = 0, width = 1200.dp) // wide: no "More" in the way
        press(Key.DirectionRight) // Quality -> Chat
        press(Key.DirectionCenter)
        assertEquals(1, chatClicks.size)
        assertEquals(0, qualityClicks.size)
    }

    @Test
    fun `wide bar shows every entry and has no More button`() {
        showBar(startIndex = 0, width = 1200.dp)
        compose.onNodeWithText(menuEntry).assertExists() // "Reload emotes" is a button of the bar itself
        compose.onNodeWithText(context.getString(R.string.player_more)).assertDoesNotExist()
    }

    @Test
    fun `narrow bar keeps the first entries and bundles the rest in More`() {
        showBar(startIndex = 0)
        compose.onNodeWithText("Auto").assertExists() // the Quality button is labelled with the current quality
        compose.onNodeWithText(context.getString(R.string.player_more)).assertExists()
        compose.onNodeWithText(menuEntry).assertDoesNotExist() // inside the closed menu
    }

    @Test
    fun `chat layout button follows the chat button and acts once`() {
        showBar(startIndex = 0, width = 1200.dp)
        press(Key.DirectionRight) // Quality -> Chat
        press(Key.DirectionRight) // Chat -> Chat layout
        press(Key.DirectionCenter)
        assertEquals(1, chatLayoutClicks.size)
        assertEquals(0, chatClicks.size)
    }

    @Test
    fun `up takes the focus to the timeline of a recording, left and right seek there, down comes back`() {
        showBar(startIndex = 0, width = 1200.dp, vod = true)
        press(Key.DirectionUp)
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        press(Key.DirectionLeft)
        assertEquals(listOf(1, 1, -1), seekSteps)
        press(Key.DirectionDown)
        // back on the buttons: right moves the highlight again and seeks nothing
        press(Key.DirectionRight)
        assertEquals(3, seekSteps.size)
    }

    @Test
    fun `live streams have no timeline to focus`() {
        showBar(startIndex = 0, width = 1200.dp)
        press(Key.DirectionUp)
        press(Key.DirectionRight)
        assertEquals(0, seekSteps.size)
    }

    private val chatLayoutName get() = context.getString(R.string.player_btn_chat_layout)

    @Test
    fun `compact bar fits more buttons than the labelled one at the same width`() {
        showBar(startIndex = 0, width = 640.dp, vod = true, compact = false)
        compose.onNodeWithContentDescription(chatLayoutName, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `compact bar shows the chat layout button where the labelled bar needs More`() {
        showBar(startIndex = 0, width = 640.dp, vod = true, compact = true)
        compose.onNodeWithContentDescription(chatLayoutName, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the last entry of a tall More menu can be reached and is not cut off`() {
        // Narrow bar, low screen: more entries than fit above the bar
        showBar(startIndex = Int.MAX_VALUE, width = 320.dp, compact = false, height = 330.dp)
        press(Key.DirectionCenter)
        repeat(12) { press(Key.DirectionDown) }
        compose.onNodeWithText(context.getString(R.string.player_more_stats)).assertIsDisplayed()
    }
}
