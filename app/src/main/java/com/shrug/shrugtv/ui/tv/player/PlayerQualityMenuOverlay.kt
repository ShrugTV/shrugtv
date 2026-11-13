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
import com.shrug.shrugtv.model.StreamQuality
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors

/**
 * Quality selection menu as a modern glass panel (slice 4a).
 *
 * Ersetzt `include_video_quality.xml` und `VideoQualityAdapter.java`:
 * - Vertical list of all available HLS qualities / variants
 * - Active quality marked
 * - Switching is seamless through TrackSelectionOverride without a stream restart
 */
@Composable
fun PlayerQualityMenuOverlay(
    visible: Boolean,
    qualities: List<StreamQuality>,
    currentQuality: String,
    onSelectQuality: (StreamQuality) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qualityFocusRequester = remember { FocusRequester() }
    val displayQualities: List<StreamQuality> = remember(qualities, currentQuality) {
        if (qualities.isEmpty()) {
            listOf(StreamQuality(name = currentQuality.ifEmpty { "Auto" }, url = null))
        } else {
            qualities
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(50)
            runCatching { qualityFocusRequester.requestFocus() }
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
                    .width(260.dp)
                    .clip(TvShapes.r(16.dp))
                    .background(TvColors.Panel)
                    .border(1.dp, TvColors.glass(0.15f), TvShapes.r(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_panel_quality),
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                displayQualities.forEachIndexed { index, quality ->
                    val isSelected = quality.name.equals(currentQuality, ignoreCase = true)
                    val shouldFocus = isSelected || (index == 0 && displayQualities.none { it.name.equals(currentQuality, ignoreCase = true) })
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    val itemModifier = if (shouldFocus) Modifier.focusRequester(qualityFocusRequester) else Modifier

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TvShapes.r(8.dp))
                            .background(if (isFocused) Color.White else Color.Transparent)
                            .then(itemModifier)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP && index == 0) return@onKeyEvent true
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                                    onDismiss()
                                    true
                                } else if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                        onSelectQuality(quality)
                                    }
                                    true
                                } else false
                            }
                            .clickable(interactionSource = interactionSource, indication = null) {
                                onSelectQuality(quality)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isFocused) TvColors.Background else TvColors.Accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(16.dp))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = com.shrug.shrugtv.player.QualityMatch.displayName(quality.name, stringResource(R.string.player_quality_source), stringResource(R.string.player_quality_audio_only)),
                                fontFamily = Inter,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isFocused) TvColors.Background else if (isSelected) TvColors.AccentLight else TvColors.Text
                            )
                        }
                    }
                }
            }
        }
    }
