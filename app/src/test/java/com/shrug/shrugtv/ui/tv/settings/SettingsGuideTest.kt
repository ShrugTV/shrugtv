package com.shrug.shrugtv.ui.tv.settings

import android.app.Application
import android.content.Context
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Guides sit behind an (i) button, not in the list (2026-11-01: they took too much room). */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w960dp-h540dp-land-xhdpi")
class SettingsGuideTest {

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun press(key: Key, times: Int = 1) = repeat(times) {
        compose.onRoot().performKeyInput { keyDown(key) }
        compose.mainClock.advanceTimeBy(60)
        compose.onRoot().performKeyInput { keyUp(key) }
        compose.mainClock.advanceTimeBy(150)
        compose.waitForIdle()
    }


    @Test
    fun `the MQTT guide opens from the info button, not in the list`() {
        val standards = mutableMapOf<String, Any?>()
        val root = SettingsModel.load(ctx, R.xml.settings, standards)
        // MQTT sits under "Connections" since ; the test shows it as a group of its own
        val connections = root.children.filterIsInstance<Entry.Page>().first { it.key == "pref_connections" }
        val groups = listOf(connections.children.filterIsInstance<Entry.Page>().first { it.key == "pref_mqtt_screen" })
        val mqtt = 0
        val access = SettingsAccess(
            bool = { _, d -> d }, string = { _, d -> d }, setBool = { _, _ -> }, setString = { _, _ -> },
            action = { }, active = { true }, extra = { null },
        )
        compose.setContent {
            val im = LocalInputModeManager.current
            SideEffect { im.requestInputMode(InputMode.Keyboard) }
            TvHomeTheme { SettingsScreen(groups, access, Account(null, {}), 0) }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.onAllNodesWithText(groups[mqtt].title).onFirst().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        val guide = ctx.getString(R.string.settings_mqtt_tip)
        compose.onNodeWithText(guide).assertDoesNotExist()
        // Right: into the list, onto the (i) of the first group, down to a setting and back
        press(Key.DirectionRight)
        // Where Right lands depends on the height of the menu (one more item since "Appearance"), so go to the top
        // of the list first; otherwise the first group and its (i) scroll out of view
        press(Key.DirectionUp, 8)
        press(Key.DirectionDown, 2)
        // Open the first group's guide with its (i); the dialog shows the whole text
        // TV buttons react to keys and semantics, not to touch
        compose.onAllNodes(androidx.compose.ui.test.hasClickAction() and (
            androidx.compose.ui.test.hasContentDescription(ctx.getString(R.string.settings_guide_show))))[0]
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.onNodeWithText(guide).assertExists()
    }
}
