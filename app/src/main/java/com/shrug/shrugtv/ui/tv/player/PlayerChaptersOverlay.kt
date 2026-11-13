package com.shrug.shrugtv.ui.tv.player

import android.view.KeyEvent
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.VodChapter
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.home.NetImage
import com.shrug.shrugtv.util.TimeUtil

/**
 * Chapter selection overlay for VODs (slice 4c).
 *
 * Replaces `include_vod_chapters.xml` and `VodChapterAdapter.kt`:
 * - Horizontal film strip with all VOD chapters.
 * - Thumbnail, chapter name, start time and duration.
 * - D-pad operable with a focus frame.
 */
@Composable
fun PlayerChaptersOverlay(
    visible: Boolean,
    chapters: List<VodChapter>,
    currentPositionMs: Long,
    onSelectChapter: (VodChapter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chaptersFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(50)
            runCatching { chaptersFocusRequester.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.5f to Color(0x99000000),
                        0.75f to TvColors.Background.copy(alpha = 0xDD / 255f),
                        1.0f to TvColors.Background.copy(alpha = 0xF8 / 255f)
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.player_panel_chapters),
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TvColors.Text
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.player_chapters_count, chapters.size),
                        fontFamily = Inter,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = TvColors.TextMuted
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        val isCurrentChapter = currentPositionMs >= chapter.positionMs &&
                                (index == chapters.size - 1 || currentPositionMs < chapters[index + 1].positionMs)
                        val shouldFocus = isCurrentChapter || (index == 0 && chapters.none { currentPositionMs >= it.positionMs })
                        val cardModifier = if (shouldFocus) Modifier.focusRequester(chaptersFocusRequester) else Modifier

                        ChapterCard(
                            chapter = chapter,
                            isCurrent = isCurrentChapter,
                            onClick = { onSelectChapter(chapter) },
                            modifier = cardModifier
                        )
                    }
                }
            }
        }
    }
}

/** Same size as the box art that GQL delivers (`boxArtURL(width:245, height:342)`). */
private const val CHAPTER_IMAGE_WIDTH = 245
private const val CHAPTER_IMAGE_HEIGHT = 342
private val CHAPTER_CARD_WIDTH = 116.dp

@Composable
private fun ChapterCard(
    chapter: VodChapter,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = modifier
            .width(CHAPTER_CARD_WIDTH)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        onClick()
                    }
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The category pictures are portrait (245 x 342): show them whole
                .aspectRatio(CHAPTER_IMAGE_WIDTH.toFloat() / CHAPTER_IMAGE_HEIGHT)
                .testTag("chapter_image")
                .clip(TvShapes.r(8.dp))
                .background(TvColors.Surface)
                .border(
                    width = if (isFocused) 2.dp else if (isCurrent) 1.5.dp else 0.dp,
                    color = if (isFocused) TvColors.Focus else if (isCurrent) TvColors.Accent else Color.Transparent,
                    shape = TvShapes.r(8.dp)
                )
        ) {
            NetImage(
                url = chapter.imageUrl,
                modifier = Modifier.fillMaxSize(),
                width = CHAPTER_IMAGE_WIDTH,
                height = CHAPTER_IMAGE_HEIGHT
            )

            // Startzeitpunkt-Pille
            val startText = TimeUtil.hms(chapter.positionMs / 1000)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(TvShapes.r(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = startText,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = chapter.name,
            fontFamily = Inter,
            fontWeight = if (isFocused || isCurrent) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (isFocused) TvColors.Focus else if (isCurrent) TvColors.Accent else TvColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = chapter.durationText,
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = TvColors.TextMuted,
            maxLines = 1
        )
    }
}
