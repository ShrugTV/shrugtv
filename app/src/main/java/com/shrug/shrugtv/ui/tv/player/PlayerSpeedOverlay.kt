package com.shrug.shrugtv.ui.tv.player

import android.view.KeyEvent
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.PlaybackSpeed
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors

val AVAILABLE_PLAYBACK_SPEEDS = listOf(
    PlaybackSpeed("0.5x", 0.5f),
    PlaybackSpeed("0.75x", 0.75f),
    PlaybackSpeed("1.0x (Normal)", 1.0f),
    PlaybackSpeed("1.25x", 1.25f),
    PlaybackSpeed("1.5x", 1.5f),
    PlaybackSpeed("2.0x", 2.0f)
)

/**
 * Playback speed menu for VODs (slice 4c).
 *
 * Ersetzt `include_playback_speed.xml` und `PlaybackSpeedAdapter.java`:
 * - Glass panel for choosing 0.5x to 2.0x.
 * - Aktives Tempo markiert.
 * - D-Pad bedienbar.
 */
@Composable
fun PlayerSpeedOverlay(
    visible: Boolean,
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(50)
            runCatching { speedFocusRequester.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 48.dp, bottom = 120.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .clip(TvShapes.r(16.dp))
                    .background(TvColors.Panel)
                    .border(1.dp, TvColors.glass(0.15f), TvShapes.r(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_panel_speed),
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                AVAILABLE_PLAYBACK_SPEEDS.forEachIndexed { index, item ->
                    val isSelected = Math.abs(item.speed - currentSpeed) < 0.01f
                    val shouldFocus = isSelected || (index == 2 && AVAILABLE_PLAYBACK_SPEEDS.none { Math.abs(it.speed - currentSpeed) < 0.01f })
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    val itemModifier = if (shouldFocus) Modifier.focusRequester(speedFocusRequester) else Modifier

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TvShapes.r(8.dp))
                            .background(if (isFocused) Color.White else Color.Transparent)
                            .then(itemModifier)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                                    onDismiss()
                                    true
                                } else if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                        onSelectSpeed(item.speed)
                                    }
                                    true
                                } else false
                            }
                            .clickable(interactionSource = interactionSource, indication = null) {
                                onSelectSpeed(item.speed)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                            Text(
                                text = item.label,
                                fontFamily = Inter,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = if (isFocused) TvColors.Background else if (isSelected) TvColors.Accent else TvColors.Text,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.exo_ic_check),
                                    contentDescription = "Aktiv",
                                    tint = if (isFocused) TvColors.Background else TvColors.Accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
