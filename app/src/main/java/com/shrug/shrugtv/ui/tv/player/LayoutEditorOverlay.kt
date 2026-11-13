package com.shrug.shrugtv.ui.tv.player

import android.view.KeyEvent
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.chat.ChatAppearance
import com.shrug.shrugtv.chat.ChatLayout
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors

/**
 * "Adjust chat" in the player (docs/chat_plan.md phase 3, CA12; replaces the static mock of
 * ). Up/down picks a row, left/right changes it; every change is visible at once
 * and saved to the same settings the settings screen shows. Back closes.
 */
@Composable
fun ChatAdjustOverlay(
    visible: Boolean,
    appearance: ChatAppearance,
    textSizeSp: Float,
    onChange: ((ChatAppearance) -> ChatAppearance) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(50)
            runCatching { firstRow.requestFocus() }
        }
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .clip(TvShapes.r(16.dp))
                    .background(TvColors.Panel)
                    .border(1.dp, TvColors.glass(0.15f), TvShapes.r(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_more_chat_position),
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                val layoutName = stringResource(
                    when (appearance.layout) {
                        ChatLayout.SIDE_BY_SIDE -> R.string.chat_layout_side_by_side
                        ChatLayout.OVERLAY -> R.string.chat_layout_overlay
                        ChatLayout.TRANSPARENT -> R.string.chat_layout_transparent
                    }
                )
                val width = appearance.widthDp(textSizeSp).toInt()
                val widthText = if (appearance.widthValue == 0) stringResource(R.string.chat_adjust_width_auto, width) else "$width dp"
                val rows = buildList {
                    add(AdjustRow(stringResource(R.string.settings_title_chat_layout), layoutName) { d -> onChange { it.stepLayout(d) } })
                    add(AdjustRow(stringResource(R.string.chat_adjust_width), widthText) { d -> onChange { it.stepWidth(d, textSizeSp) } })
                    add(AdjustRow(
                        stringResource(R.string.chat_adjust_position),
                        stringResource(if (appearance.onLeft) R.string.chat_adjust_left else R.string.chat_adjust_right)
                    ) { _ -> onChange { it.togglePosition() } })
                    if (appearance.layout == ChatLayout.OVERLAY) {
                        add(AdjustRow(stringResource(R.string.chat_adjust_background), "${appearance.opacityIndex * 10} %") { d ->
                            onChange { it.stepOpacity(d) }
                        })
                    }
                    // Sizes and height
                    add(AdjustRow(stringResource(R.string.chat_adjust_font), "${appearance.textSizeSp.toInt()} sp") { d -> onChange { it.stepFont(d) } })
                    add(AdjustRow(
                        stringResource(R.string.chat_adjust_emotes),
                        stringResource(if (appearance.emoteValue == "0") R.string.chat_adjust_emotes_normal else R.string.chat_adjust_emotes_bigger)
                    ) { d -> onChange { it.stepEmote(d) } })
                    add(AdjustRow(stringResource(R.string.chat_adjust_height), "${appearance.heightPercent} %") { d -> onChange { it.stepHeight(d) } })
                    if (appearance.heightPercent < 100) {
                        add(AdjustRow(
                            stringResource(R.string.chat_adjust_vertical),
                            stringResource(
                                when (appearance.vertical) {
                                    com.shrug.shrugtv.chat.ChatVertical.TOP -> R.string.chat_adjust_top
                                    com.shrug.shrugtv.chat.ChatVertical.MIDDLE -> R.string.chat_adjust_middle
                                    com.shrug.shrugtv.chat.ChatVertical.BOTTOM -> R.string.chat_adjust_bottom
                                }
                            )
                        ) { d -> onChange { it.stepVertical(d) } })
                    }
                }
                rows.forEachIndexed { index, row ->
                    AdjustRowView(
                        row = row,
                        isFirst = index == 0,
                        isLast = index == rows.lastIndex,
                        focusRequester = if (index == 0) firstRow else null,
                        onDismiss = onDismiss
                    )
                }
                Text(
                    text = stringResource(R.string.chat_adjust_hint),
                    fontFamily = Inter,
                    fontSize = 12.sp,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )
            }
        }
    }
}

private class AdjustRow(val label: String, val value: String, val onStep: (Int) -> Unit)

@Composable
private fun AdjustRowView(
    row: AdjustRow,
    isFirst: Boolean,
    isLast: Boolean,
    focusRequester: FocusRequester?,
    onDismiss: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(TvShapes.r(8.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onKeyEvent { e ->
                val n = e.nativeKeyEvent
                val down = n.action == KeyEvent.ACTION_DOWN
                when (n.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { if (down) row.onStep(-1); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { if (down) row.onStep(1); true }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { if (n.action == KeyEvent.ACTION_UP) row.onStep(1); true }
                    KeyEvent.KEYCODE_BACK -> { if (down) onDismiss(); true }
                    KeyEvent.KEYCODE_DPAD_UP -> isFirst
                    KeyEvent.KEYCODE_DPAD_DOWN -> isLast
                    else -> false
                }
            }
            .focusable(interactionSource = interaction)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        val color = if (focused) TvColors.Background else TvColors.Text
        Text(text = row.label, fontFamily = Inter, fontSize = 14.sp, color = color)
        Text(text = "◀  ${row.value}  ▶", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = color)
    }
}
