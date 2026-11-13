package com.shrug.shrugtv.ui.tv.player

import android.app.Application
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shrug.shrugtv.chat.ChatAppearance
import com.shrug.shrugtv.chat.ChatLayout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** "Adjust chat" with the remote (CA12): up/down picks a row, left/right changes it, Back closes. */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class ChatAdjustKeyTest {

    @get:Rule
    val compose = createComposeRule()

    private val appearance = mutableStateOf(ChatAppearance(ChatLayout.SIDE_BY_SIDE, 0, 6, false))
    private var dismissed = 0

    private fun show() {
        compose.setContent {
            val im = LocalInputModeManager.current
            SideEffect { im.requestInputMode(InputMode.Keyboard) }
            ChatAdjustOverlay(
                visible = true,
                appearance = appearance.value,
                textSizeSp = 14f,
                onChange = { change -> appearance.value = change(appearance.value) },
                onDismiss = { dismissed++ }
            )
        }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key) }
        compose.mainClock.advanceTimeBy(60)
        compose.onRoot().performKeyInput { keyUp(key) }
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
    }

    @Test
    fun `right on the first row changes the layout`() {
        show()
        press(Key.DirectionRight)
        assertEquals(ChatLayout.OVERLAY, appearance.value.layout)
    }

    @Test
    fun `down then right widens the chat`() {
        show()
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        assertEquals(225, appearance.value.widthValue)
        assertEquals(ChatLayout.SIDE_BY_SIDE, appearance.value.layout)
    }

    @Test
    fun `Back closes`() {
        show()
        press(Key.Back)
        assertEquals(1, dismissed)
    }
}
