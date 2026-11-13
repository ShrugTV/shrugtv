package com.shrug.shrugtv.ui.tv.channel

import android.content.Intent
import com.shrug.shrugtv.ui.tv.themeBackground
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.google.gson.JsonObject
import com.shrug.shrugtv.R
import com.shrug.shrugtv.net.ResultCallback
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.events.AppEvent
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.home.HomeIcons
import com.shrug.shrugtv.ui.tv.home.NetImage
import com.shrug.shrugtv.ui.tv.components.FocusBorder
import com.shrug.shrugtv.ui.tv.components.focusEdgeLeft
import com.shrug.shrugtv.ui.tv.components.rowKeepsRight
import com.shrug.shrugtv.ui.tv.components.TvData
import com.shrug.shrugtv.ui.tv.components.LiveBadge
import com.shrug.shrugtv.ui.tv.components.VodCard
import com.shrug.shrugtv.ui.tv.components.Destinations
import com.shrug.shrugtv.ui.tv.components.glassButton
import com.shrug.shrugtv.ui.tv.components.whiteButton
import com.shrug.shrugtv.util.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import com.shrug.shrugtv.events.Events
import kotlin.coroutines.resume

/**
 * Channel page in Compose (step 4). Replaces ChannelDetailsActivity and
 * ChannelDetailsFragment (Leanback DetailsSupportFragment): banner, avatar, description, buttons
 * watch live/offline, follow (QR dialog FollowQrActivity), favorite; below the videos as rows
 * past broadcasts, highlights, uploads with paging. Expects the same extra as the old activity
 * (R.string.movie with the channel).
 */
class ChannelActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024, 1024)
        window.setBackgroundDrawable(null)
        val channel = intent.getSerializableExtra(getString(com.shrug.shrugtv.R.string.movie)) as? Movie
        if (channel == null) { finish(); return }
        setContent {
            TvHomeTheme {
                ChannelScreen(channel,
                    live = { Destinations.watch(this, it); finish() },
                    vod = { Destinations.open(this, it) },
                    contextMenu = { Destinations.contextMenu(this, it) })
            }
        }
    }
}

/** Result code of the follow QR dialog when the follow state may have changed (as in the old fragment). */
private const val FOLLOW_RESULT = 36797

/** One video row; [title] is a string resource, resolved in the composable (app language). */
private class ContentRow(@androidx.annotation.StringRes val title: Int, val kind: String, val type: Enums.RowType) {
    val videos = mutableStateListOf<Movie>()
    var cursor by mutableStateOf<String?>("")
    var loading by mutableStateOf(false)
    var done by mutableStateOf(false)
}

private suspend fun json(f: (ResultCallback<JsonObject>) -> Unit): JsonObject? = suspendCancellableCoroutine { c ->
    f(ResultCallback { e, j -> if (c.isActive) c.resume(if (e == null) j else null) })
}

private suspend fun more(ctx: android.content.Context, channelId: String, avatar: String?, z: ContentRow) {
    val c = z.cursor ?: return
    if (z.loading) return
    z.loading = true
    val j = if (z.kind == "clips") {
        json { HelixApi.getClipsByBroadcaster(ctx, channelId, c, it) }
    } else {
        json { HelixApi.getVideos(ctx, channelId, c, z.kind, it) }
    }
    val data = j?.get("data")
    if (data != null && data.isJsonArray) {
        // Saved positions come from Room, which refuses the main thread (crashed on 2026-10-22).
        val updated = withContext(Dispatchers.IO) {
            if (z.kind == "clips") {
                HelixParser.parseClips(data.asJsonArray, z.type, avatar ?: "")
            } else {
                HelixParser.parseVideos(data.asJsonArray, z.type, avatar ?: "").also { UiUtils.applySavedVodPositions(ctx, it) }
            }
        }
        z.videos.addAll(updated)
    }
    z.cursor = j?.let { runCatching { HelixParser.getPaginationCursor(it) }.getOrNull() }?.takeIf { it.isNotEmpty() }
    z.loading = false
    z.done = true
}

/** Channel offline: open the player anyway (rerun/offline image), like "OFFLINE" in the old fragment. */
private fun offlineStream(k: Movie) = Movie().apply {
    displayName = k.displayName
    login = k.login
    userId = k.userId
    itemType = Enums.ContentType.STREAM
    avatarUrl = k.avatarUrl
    setViewerCount("0")
}

@Composable
private fun ChannelScreen(channel: Movie, live: (Movie) -> Unit, vod: (Movie) -> Unit, contextMenu: (Movie) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val channelId = channel.userId.toString()
    var stream by remember { mutableStateOf<Movie?>(null) }
    var liveChecked by remember { mutableStateOf(false) }
    var follows by remember { mutableStateOf<Boolean?>(null) }
    var favorite by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val rows = remember {
        listOf(
            ContentRow(R.string.channel_past_broadcasts, "archive", Enums.RowType.VOD_CHANNEL_ARCHIVED),
            ContentRow(R.string.channel_highlights, "highlight", Enums.RowType.VOD_CHANNEL_HIGHLIGHTS),
            ContentRow(R.string.channel_uploads, "upload", Enums.RowType.VOD_CHANNEL_UPLOADED),
            ContentRow(R.string.channel_clips, "clips", Enums.RowType.VOD_CHANNEL_HIGHLIGHTS),
        )
    }

    suspend fun loadFollow() {
        if (!AppState.userProfileLoaded) return
        val data = json { HelixApi.getFollowRelation(ctx, channelId, it) }?.get("data")
        if (data != null && data.isJsonArray) follows = data.asJsonArray.size() > 0
    }
    val followDialog = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == FOLLOW_RESULT) scope.launch {
            val before = follows
            loadFollow()
            if (before != follows) Events.send(AppEvent(AppEvent.Type.FOLLOW_CHANNEL_CHANGED))
        }
    }

    LaunchedEffect(Unit) {
        TvData.isFavorite(ctx, channelId) { favorite = it }
        launch { loadFollow() }
        launch {
            val data = json { HelixApi.getStreamByUserId(ctx, channel.userId.toLong(), it) }?.get("data")
            stream = if (data != null && data.isJsonArray && data.asJsonArray.size() > 0)
                runCatching { HelixParser.parseStream(data.asJsonArray[0].asJsonObject, Enums.RowType.GAME) }.getOrNull() else null
            liveChecked = true
        }
        rows.forEach { z -> launch { more(ctx, channelId, channel.avatarUrl, z) } }
        runCatching { first.requestFocus() }
    }

    Box(Modifier.fillMaxSize().themeBackground()) {
        // banner at the top, fading into the background towards the bottom
        if (!channel.bannerUrl.isNullOrEmpty()) {
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                NetImage(channel.bannerUrl, Modifier.fillMaxSize(), width = 1920, height = 540)
                val background = TvColors.Background
                val gradient = remember(background) { bannerGradientImage(background) }
                androidx.compose.foundation.Image(
                    gradient,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 80.dp, top = 60.dp, end = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    NetImage(channel.avatarUrl, Modifier.size(96.dp).clip(CircleShape), width = 300, height = 300)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth(0.75f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(channel.displayName ?: channel.login ?: "", fontFamily = Inter, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
                            if (stream != null) LiveBadge()
                        }
                        val s = stream
                        val subline = when {
                            s != null -> listOfNotNull(s.gameName,
                                pluralStringResource(R.plurals.channel_viewers, s.viewerCount, s.viewerCount)).joinToString(" · ")
                            liveChecked -> stringResource(R.string.channel_offline)
                            else -> ""
                        }
                        if (subline.isNotEmpty()) Text(subline, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMedium)
                        val text = s?.title?.takeIf { it.isNotBlank() } ?: channel.description
                        if (!text.isNullOrBlank()) Text(text, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val s = stream
                    Button(onClick = { live(s ?: offlineStream(channel)) }, colors = whiteButton(), modifier = Modifier.focusRequester(first)) {
                        Icon(HomeIcons.Play, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (s != null) R.string.channel_watch_live else R.string.channel_open), fontFamily = Inter, fontWeight = FontWeight.Bold)
                    }
                    if (AppState.userProfileLoaded) {
                        Button(onClick = {
                            followDialog.launch(com.shrug.shrugtv.ui.tv.dialogs.FollowQrActivity.intent(ctx, channel))
                        }, colors = glassButton()) { Text(stringResource(if (follows == true) R.string.channel_unfollow else R.string.channel_follow), fontFamily = Inter) }
                    }
                    Button(onClick = {
                        val updated = !favorite
                        TvData.setFavorite(ctx, channelId, updated) {
                            favorite = updated
                            Events.send(AppEvent(AppEvent.Type.FAVORITE_CHANGED))
                        }
                    }, colors = if (favorite) whiteButton() else glassButton()) {
                        Text(stringResource(if (favorite) R.string.channel_favorite_on else R.string.channel_favorite_off), fontFamily = Inter)
                    }
                }
            }
            rows.forEach { z ->
                if (z.videos.isNotEmpty()) item(key = z.kind) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(z.title), fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
                        LazyRow(Modifier.focusRestorer().rowKeepsRight().focusEdgeLeft(), horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(start = FocusBorder, end = 48.dp)) {
                            itemsIndexed(z.videos) { i, m ->
                                if (i >= z.videos.size - 4 && z.cursor != null) LaunchedEffect(z.videos.size) { more(ctx, channelId, channel.avatarUrl, z) }
                                VodCard(m, click = { vod(m) }, longPress = { contextMenu(m) })
                            }
                        }
                    }
                }
            }
            if (rows.all { it.done && it.videos.isEmpty() }) item {
                Text(stringResource(R.string.channel_no_videos), fontFamily = Inter, fontSize = 13.sp, color = TvColors.TextMuted)
            }
        }
    }
}

private fun bannerGradientImage(background: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.ImageBitmap {
    val w = 16f
    val h = 65f
    val image = androidx.compose.ui.graphics.ImageBitmap(w.toInt(), h.toInt())
    val c = androidx.compose.ui.graphics.Canvas(image)
    val p = androidx.compose.ui.graphics.Paint()
    p.shader = androidx.compose.ui.graphics.LinearGradientShader(
        from = androidx.compose.ui.geometry.Offset(0f, 0f),
        to = androidx.compose.ui.geometry.Offset(0f, h),
        colors = listOf(background.copy(alpha = 0.35f), background),
    )
    c.drawRect(0f, 0f, w, h, p)
    return image
}
