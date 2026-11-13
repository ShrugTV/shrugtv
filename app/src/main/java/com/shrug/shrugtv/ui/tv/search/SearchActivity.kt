package com.shrug.shrugtv.ui.tv.search

import android.os.Bundle
import com.shrug.shrugtv.ui.tv.themeBackground
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.home.HomeIcons
import com.shrug.shrugtv.ui.tv.components.FocusBorder
import com.shrug.shrugtv.ui.tv.components.focusEdgeLeft
import com.shrug.shrugtv.ui.tv.components.rowKeepsRight
import com.shrug.shrugtv.ui.tv.components.TvData
import com.shrug.shrugtv.ui.tv.components.ContentCard
import com.shrug.shrugtv.ui.tv.components.Destinations
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Search in Compose (step 2): one input field, search after 0.6 s of quiet,
 * results as rows live, channels, categories. Replaces SearchActivity/SearchFragment (Leanback
 * SearchSupportFragment); the voice search of the old fragment is dropped.
 */
class SearchActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024, 1024)
        window.setBackgroundDrawable(null)
        setContent { TvHomeTheme { SearchScreen(open = { Destinations.open(this, it) }, contextMenu = { Destinations.contextMenu(this, it) }) } }
    }
}

/** One result row; [title] is a string resource, resolved in the composable (app language). */
private data class Hits(@androidx.annotation.StringRes val title: Int, val entries: List<Movie>)

private suspend fun search(ctx: android.content.Context, text: String): List<Hits> = coroutineScope {
    fun kind(f: (com.shrug.shrugtv.apihelpers.SearchCallback) -> Unit) = async {
        suspendCancellableCoroutine<List<Movie>> { c -> f(com.shrug.shrugtv.apihelpers.SearchCallback { l -> if (c.isActive) c.resume(l?.toList() ?: emptyList()) }) }
    }
    val live = kind { TvData.searchLive(ctx, text, it) }
    val channels = kind { TvData.searchChannels(ctx, text, it) }
    val games = kind { TvData.searchGames(ctx, text, it) }
    listOf(Hits(R.string.search_live, live.await()), Hits(R.string.search_channels, channels.await()),
        Hits(R.string.search_categories, games.await()))
        .filter { it.entries.isNotEmpty() }
}

@Composable
private fun SearchScreen(open: (Movie) -> Unit, contextMenu: (Movie) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var text by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<Hits>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var fieldFocus by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    // While the on-screen keyboard is open, it takes the arrow keys itself. "Search" on the keyboard
    // therefore closes it and jumps to the results; once it is closed (Back), arrow down leads there,
    // because a single-line text field in Compose does not pass it on.
    val results = remember { FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    fun toResults() { keyboard?.hide(); if (hits.isNotEmpty()) runCatching { results.requestFocus() } }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
    LaunchedEffect(text) {
        if (text.trim().length < 2) { hits = emptyList(); return@LaunchedEffect }
        delay(600)
        loading = true
        hits = runCatching { search(ctx, text.trim()) }.getOrDefault(emptyList())
        loading = false
    }

    Column(Modifier.fillMaxSize().themeBackground().padding(start = 80.dp, top = 40.dp, end = 48.dp)) {
        Row(
            Modifier.fillMaxWidth(0.6f)
                .border(2.dp, if (fieldFocus) TvColors.Focus else TvColors.TextMuted.copy(alpha = 0.4f), TvShapes.r(24.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(HomeIcons.Search, contentDescription = null, tint = TvColors.TextMuted, modifier = Modifier.size(16.dp))
            Box(Modifier.padding(start = 10.dp).fillMaxWidth()) {
                if (text.isEmpty()) Text(stringResource(R.string.search_hint), fontFamily = Inter, fontSize = 13.sp, color = TvColors.TextMuted)
                BasicTextField(
                    text, { text = it }, singleLine = true, cursorBrush = SolidColor(TvColors.Text),
                    textStyle = TextStyle(color = TvColors.Text, fontSize = 13.sp, fontFamily = Inter),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { toResults() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(field).onFocusChanged { fieldFocus = it.isFocused }
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown && hits.isNotEmpty()) { toResults(); true } else false
                        },
                )
            }
        }
        Text(
            when {
                loading -> stringResource(R.string.search_searching)
                text.trim().length >= 2 && hits.isEmpty() -> stringResource(R.string.search_no_results)
                else -> ""
            },
            fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted, modifier = Modifier.padding(top = 10.dp),
        )
        LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            items(hits.size) { i ->
                val t = hits[i]
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(t.title), fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
                    LazyRow((if (i == 0) Modifier.focusRequester(results) else Modifier).focusRestorer().rowKeepsRight().focusEdgeLeft(), horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(start = FocusBorder, end = 48.dp)) {
                        items(t.entries) { m -> ContentCard(m, click = { open(m) }, longPress = { contextMenu(m) }) }
                    }
                }
            }
        }
    }
}
