package com.shrug.shrugtv.ui.tv.category

import android.os.Bundle
import com.shrug.shrugtv.ui.tv.themeBackground
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.google.gson.JsonObject
import com.shrug.shrugtv.R
import com.shrug.shrugtv.net.ResultCallback
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.home.NetImage
import com.shrug.shrugtv.ui.tv.components.StreamCard
import com.shrug.shrugtv.ui.tv.components.VodCard
import com.shrug.shrugtv.ui.tv.components.Destinations
import com.shrug.shrugtv.ui.tv.components.glassButton
import com.shrug.shrugtv.ui.tv.components.whiteButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Category (streams and clips of a game) in Compose (step 3).
 * Replaces StreamCategoryActivity/StreamCategoryFragment: header with box art, grid of live streams
 * or popular clips with paging, optionally only the streams in the configured language.
 * Expects the same extras as the old activity ("game").
 */
class CategoryActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024, 1024)
        window.setBackgroundDrawable(null)
        val game = intent.getSerializableExtra("game") as? Movie
        if (game == null) { finish(); return }
        setContent {
            TvHomeTheme {
                CategoryScreen(game,
                    open = { Destinations.open(this, it) },
                    contextMenu = { Destinations.contextMenu(this, it) },
                    follow = { startActivity(com.shrug.shrugtv.ui.tv.dialogs.FollowQrActivity.intent(this, game)) })
            }
        }
    }
}

private enum class CategoryTab { ALL, LANGUAGE, CLIPS }

private class Page(val streams: List<Movie>, val cursor: String?)

/**
 * One page of streams or clips including avatars, entirely in the background (
 * ). Before, the response arrived via Ion callback on the main thread and
 * parseStreams fetched the avatars there, blocking; the page froze on "Loading ..." (Fire TV: once 45-90 s).
 */
private suspend fun load(ctx: android.content.Context, gameId: Long, cursor: String, tab: CategoryTab): Page {
    val t0 = android.os.SystemClock.elapsedRealtime()
    val json = suspendCancellableCoroutine<JsonObject?> { c ->
        val cb = ResultCallback<JsonObject> { e, j -> if (c.isActive) c.resume(if (e == null) j else null) }
        when (tab) {
            CategoryTab.LANGUAGE -> HelixApi.getStreamsByGameAndLanguage(ctx, gameId, cursor, cb)
            CategoryTab.ALL -> HelixApi.getStreamsByGame(ctx, gameId, cursor, cb)
            CategoryTab.CLIPS -> HelixApi.getClipsByGame(ctx, gameId, cursor, cb)
        }
    }
    val t1 = android.os.SystemClock.elapsedRealtime()
    return withContext(Dispatchers.IO) {
        val data = json?.get("data")
        val list = if (data != null && data.isJsonArray) {
            when (tab) {
                CategoryTab.CLIPS -> HelixParser.parseClips(ctx, data.asJsonArray, Enums.RowType.GAME, "")
                CategoryTab.LANGUAGE -> HelixParser.parseStreams(ctx, Enums.RowType.GAME_LANGUAGE, data.asJsonArray)
                CategoryTab.ALL -> HelixParser.parseStreams(ctx, Enums.RowType.GAME, data.asJsonArray)
            }
        } else arrayListOf()
        val next = json?.let { runCatching { HelixParser.getPaginationCursor(it) }.getOrNull() }
        android.util.Log.i("Category", "${list.size} entries ($tab) after ${t1 - t0} ms, with avatars after ${android.os.SystemClock.elapsedRealtime() - t0} ms")
        Page(list, next?.takeIf { it.isNotEmpty() })
    }
}

@Composable
private fun CategoryScreen(game: Movie, open: (Movie) -> Unit, contextMenu: (Movie) -> Unit, follow: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val languagePossible = AppState.showLanguageRowGames && !AppState.streamLanguageCode.isNullOrEmpty()
    var tab by remember {
        mutableStateOf(if (languagePossible && AppState.languageRowAsPrimary) CategoryTab.LANGUAGE else CategoryTab.ALL)
    }
    val streams = remember(tab) { mutableStateListOf<Movie>() }
    var cursor by remember(tab) { mutableStateOf<String?>("") }
    var loading by remember(tab) { mutableStateOf(false) }
    val firstCard = remember { FocusRequester() }

    suspend fun more() {
        val c = cursor ?: return
        if (loading) return
        loading = true
        val s = load(ctx, game.gameId, c, tab)
        if (tab == CategoryTab.CLIPS) {
            streams.addAll(s.streams.filter { updated -> streams.none { it.clipSlug == updated.clipSlug } })
        } else {
            streams.addAll(s.streams.filter { updated -> streams.none { it.login == updated.login } })
        }
        cursor = s.cursor
        loading = false
    }
    LaunchedEffect(tab) { more() }

    Column(Modifier.fillMaxSize().themeBackground().padding(start = 80.dp, top = 36.dp, end = 48.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NetImage(game.boxArtUrl ?: game.cardImageUrl, Modifier.width(54.dp).aspectRatio(272f / 380f).clip(TvShapes.r(8.dp)), width = 272, height = 380)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title ?: game.gameName ?: "", fontFamily = Inter, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Search hits carry no viewer count; then the line is left out instead of "0 viewers".
                if (game.viewerCount > 0) Text(pluralStringResource(R.plurals.category_viewers, game.viewerCount, game.viewerCount), fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Button(onClick = follow, colors = glassButton()) { Text(stringResource(R.string.category_follow), fontFamily = Inter) }
                    Button(onClick = { tab = CategoryTab.ALL }, colors = if (tab == CategoryTab.ALL) whiteButton() else glassButton()) {
                        Text(stringResource(R.string.category_all_languages), fontFamily = Inter)
                    }
                    if (languagePossible) {
                        Button(onClick = { tab = CategoryTab.LANGUAGE }, colors = if (tab == CategoryTab.LANGUAGE) whiteButton() else glassButton()) {
                            Text(AppState.streamLanguageName ?: AppState.streamLanguageCode, fontFamily = Inter)
                        }
                    }
                    Button(onClick = { tab = CategoryTab.CLIPS }, colors = if (tab == CategoryTab.CLIPS) whiteButton() else glassButton()) {
                        Text(stringResource(R.string.category_clips), fontFamily = Inter)
                    }
                }
            }
        }
        Box(Modifier.padding(top = 18.dp)) {
            if (streams.isEmpty()) {
                val emptyRes = when {
                    loading -> R.string.category_loading
                    tab == CategoryTab.CLIPS -> R.string.category_clips_empty
                    else -> R.string.category_empty
                }
                Text(stringResource(emptyRes), fontFamily = Inter, fontSize = 13.sp, color = TvColors.TextMuted)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
            ) {
                itemsIndexed(streams, key = { _, m -> if (m.isClip) m.clipSlug else (m.login ?: m.hashCode()) }) { i, m ->
                    if (i >= streams.size - 5) LaunchedEffect(streams.size) { more() }
                    // Only set focus once the first card is really in the grid (before, it landed on "Follow").
                    if (i == 0) LaunchedEffect(tab) { runCatching { firstCard.requestFocus() } }
                    Box(if (i == 0) Modifier.focusRequester(firstCard) else Modifier) {
                        if (m.isClip) {
                            VodCard(m, click = { open(m) }, longPress = { contextMenu(m) }, width = 150.dp)
                        } else {
                            StreamCard(m, click = { open(m) }, longPress = { contextMenu(m) }, width = 150.dp)
                        }
                    }
                }
            }
        }
    }
}
