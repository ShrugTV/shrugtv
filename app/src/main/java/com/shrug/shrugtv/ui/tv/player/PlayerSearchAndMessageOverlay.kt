package com.shrug.shrugtv.ui.tv.player

import androidx.compose.animation.AnimatedVisibility
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.apihelpers.Search
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.components.ContentCard
import com.shrug.shrugtv.ui.tv.components.glassButton
import com.shrug.shrugtv.ui.tv.components.whiteButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Search and send message in the player in the modern glass style (slice 4a).
 *
 * 1. Search as a round bar at the top centre with a horizontal result row below.
 * 2. Send message as a glass bar at the chat with input field, emote button and send.
 */
@Composable
fun PlayerSearchOverlay(
    visible: Boolean,
    onSelectStream: (Movie) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(50)
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        val context = LocalContext.current
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }

        LaunchedEffect(query) {
            val q = query.trim()
            if (q.length >= 2) {
                searching = true
                results = withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                            Search.getLiveStreams(context, q) { list ->
                                if (continuation.isActive) {
                                    continuation.resumeWith(Result.success(list?.toList() ?: emptyList()))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                searching = false
            } else {
                results = emptyList()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvColors.Background.copy(alpha = 0xCC / 255f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp)
            ) {
                // Round search bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .width(520.dp)
                        .clip(TvShapes.r(26.dp))
                        .background(TvColors.Surface.copy(alpha = 0xEE / 255f))
                        .border(1.5.dp, TvColors.glass(0.25f), TvShapes.r(26.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.magnify),
                        contentDescription = stringResource(R.string.player_btn_search),
                        tint = TvColors.TextMuted,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = Inter,
                            fontSize = 16.sp,
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(TvColors.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {}),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    fontFamily = Inter,
                                    fontSize = 15.sp,
                                    color = Color(0xFF8C8C99)
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (query.isNotEmpty()) {
                        Button(
                            onClick = { query = "" },
                            colors = glassButton(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", fontSize = 12.sp, color = TvColors.TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Result row
                if (results.isNotEmpty()) {
                    Text(
                        text = "${results.size} Treffer",
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TvColors.TextMuted,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 48.dp, bottom = 12.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        items(results, key = { it.playbackId ?: "${it.userId}_${it.login}_${it.gameId}" }) { item ->
                            ContentCard(
                                m = item,
                                click = { onSelectStream(item) },
                                width = 180.dp
                            )
                        }
                    }
                } else if (searching) {
                    Text(
                        text = stringResource(R.string.home_loading),
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        color = TvColors.TextMuted,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Glass bar for sending a message in the player.
 */
@Composable
fun PlayerSendMessageOverlay(
    visible: Boolean,
    channelName: String,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messageFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(50)
            runCatching { messageFocusRequester.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        var messageText by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(540.dp)
                    .clip(TvShapes.r(26.dp))
                    .background(TvColors.Panel)
                    .border(1.5.dp, TvColors.glass(0.2f), TvShapes.r(26.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.send_message),
                    contentDescription = null,
                    tint = TvColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Inter,
                        fontSize = 15.sp,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(TvColors.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText.trim())
                            messageText = ""
                        }
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(messageFocusRequester),
                    decorationBox = { innerTextField ->
                        if (messageText.isEmpty()) {
                            Text(
                                text = "Nachricht an $channelName ...",
                                fontFamily = Inter,
                                fontSize = 14.sp,
                                color = Color(0xFF8C8C99)
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText.trim())
                            messageText = ""
                        }
                    },
                    colors = whiteButton(),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = stringResource(R.string.input_send),
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
