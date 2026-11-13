package com.shrug.shrugtv.ui.tv.dialogs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.google.gson.JsonObject
import com.shrug.shrugtv.R
import com.shrug.shrugtv.api.FollowStatusChecker
import com.shrug.shrugtv.api.gql.GqlApi
import com.shrug.shrugtv.events.AppEvent
import com.shrug.shrugtv.events.LiveHistoryChannelRemovedEvent
import com.shrug.shrugtv.filter.FilterSettings
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.components.TvData
import com.shrug.shrugtv.ui.tv.components.Destinations
import com.shrug.shrugtv.events.Events

/**
 * Context menu (long press on a card, menu key in the player's channel switcher) in Compose
 * (step 5). Replaces ContextMenuActivity/ContextMenuFragment (Leanback
 * preference tree) with the same entries: follow, channel profile, favorite (followed channels
 * only), live history, filters. Expects the extra "current_item" like the old activity.
 */
class ContextMenuActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val m = intent.getSerializableExtra(EXTRA) as? Movie
        if (m == null) { finish(); return }
        setContent { TvHomeTheme { ContextMenuPanel(m, channel = { Destinations.channel(this, m); finish() }, close = { finish() }) } }
    }

    override fun onDestroy() {
        // Filters only take effect after reloading (like onDestroy in the old fragment)
        FilterSettings.load(applicationContext)
        super.onDestroy()
    }

    companion object {
        const val EXTRA = "current_item"

        fun open(c: Context, m: Movie) = c.startActivity(Intent(c, ContextMenuActivity::class.java).putExtra(EXTRA, m))
    }
}

/** Result code of the follow QR dialog when the follow state may have changed. */
private const val FOLLOW_RESULT = 36797

private class Entry(val title: String, val subtitle: String? = null, val switchValue: Boolean? = null, val click: () -> Unit)

@Composable
private fun ContextMenuPanel(m: Movie, channel: () -> Unit, close: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    // Resources of the activity, not of the application: only they follow the app language
    val res = androidx.compose.ui.platform.LocalContext.current.resources
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(ctx) }
    val game = m.itemType == Enums.ContentType.GAME
    val channelId = m.userId.toString()
    var follows by remember { mutableStateOf<Boolean?>(null) }
    var favorite by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    var filterOn by remember { mutableStateOf(prefs.getBoolean("pref_filtering_use_filtering", false)) }
    var reruns by remember { mutableStateOf(prefs.getBoolean("pref_filtering_reruns", false)) }
    var channelFiltered by remember { mutableStateOf(FilterSettings.isChannelFiltered(m.displayName)) }
    var categoryFiltered by remember { mutableStateOf(FilterSettings.isCategoryFiltered(m.gameName)) }
    var message by remember { mutableStateOf<String?>(null) }
    val first = remember { FocusRequester() }

    fun loadFollow(afterDialog: Boolean) {
        val before = follows
        val report = { updated: Boolean ->
            follows = updated
            if (afterDialog && before != updated) Events.send(AppEvent(
                if (game) AppEvent.Type.FOLLOW_GAME_CHANGED else AppEvent.Type.FOLLOW_CHANNEL_CHANGED))
        }
        if (game) TvData.gameFollowed(ctx, m.gameId) { report(it) }
        else if (AppState.userProfileLoaded) FollowStatusChecker.check(ctx, channelId) { f, _ -> report(f) }
    }
    val followDialog = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == FOLLOW_RESULT) loadFollow(true)
    }

    LaunchedEffect(Unit) {
        loadFollow(false)
        if (!game) {
            TvData.isFavorite(ctx, channelId) { favorite = it }
            GqlApi.getUserProfile(ctx, m.userId) { _, j: JsonObject? ->
                val u = runCatching { j?.getAsJsonObject("data")?.getAsJsonObject("user") }.getOrNull() ?: return@getUserProfile
                val followers = runCatching { u.getAsJsonObject("followers").get("totalCount").asInt }.getOrNull()
                val description = runCatching { u.get("description").takeIf { !it.isJsonNull }?.asString }.getOrNull()
                info = listOfNotNull(followers?.let { res.getQuantityString(R.plurals.dialog_menu_followers, it, it) }, description?.takeIf { it.isNotBlank() }).joinToString(" · ")
            }
        }
        runCatching { first.requestFocus() }
    }

    fun toggle(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    val added = stringResource(R.string.dialog_menu_history_added)
    val removed = stringResource(R.string.dialog_menu_history_removed)
    val followEntry = Entry(stringResource(when (follows) {
        true -> R.string.dialog_menu_unfollow; false -> R.string.dialog_menu_follow; null -> R.string.dialog_menu_follow_loading })) {
        followDialog.launch(FollowQrActivity.intent(ctx, m))
    }
    val entries = buildList {
        if (game) {
            add(followEntry)
        } else {
            add(followEntry)
            add(Entry(stringResource(R.string.dialog_menu_channel_profile), click = channel))
            if (follows == true) add(Entry(stringResource(if (favorite) R.string.dialog_menu_favorite_remove else R.string.dialog_menu_favorite_add), switchValue = favorite) {
                val updated = !favorite
                TvData.setFavorite(ctx, channelId, updated) {
                    favorite = updated
                    Events.send(AppEvent(AppEvent.Type.FAVORITE_CHANGED))
                }
            })
            add(Entry(stringResource(R.string.dialog_menu_history_add)) { TvData.history(ctx, m.userId, true); message = added })
            add(Entry(stringResource(R.string.dialog_menu_history_remove)) {
                TvData.history(ctx, m.userId, false)
                Events.send(LiveHistoryChannelRemovedEvent(m.userId))
                message = removed
            })
        }
        add(Entry(stringResource(R.string.dialog_menu_use_filters), switchValue = filterOn) { filterOn = !filterOn; toggle("pref_filtering_use_filtering", filterOn) })
        if (!game) add(Entry(stringResource(R.string.dialog_menu_hide_channel), m.displayName, switchValue = channelFiltered) {
            channelFiltered = !channelFiltered
            if (channelFiltered) FilterSettings.addChannel(ctx, m.displayName) else FilterSettings.removeChannel(ctx, m.displayName)
        })
        if (game || m.itemType == Enums.ContentType.STREAM || m.itemType == Enums.ContentType.VOD) {
            if (!m.gameName.isNullOrEmpty()) add(Entry(stringResource(R.string.dialog_menu_hide_category), m.gameName, switchValue = categoryFiltered) {
                categoryFiltered = !categoryFiltered
                if (categoryFiltered) FilterSettings.addCategory(ctx, m.gameName) else FilterSettings.removeCategory(ctx, m.gameName)
            })
        }
        if (m.itemType == Enums.ContentType.STREAM || m.itemType == Enums.ContentType.VOD) {
            add(Entry(stringResource(R.string.dialog_menu_hide_reruns), switchValue = reruns) { reruns = !reruns; toggle("pref_filtering_reruns", reruns) })
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.CenterEnd) {
        Column(Modifier.width(400.dp).fillMaxHeight().background(TvColors.Surface).padding(top = 40.dp, start = 24.dp, end = 24.dp)) {
            Text((if (game) m.gameName ?: m.title else m.displayName) ?: "", fontFamily = Inter, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = listOfNotNull(m.title?.takeIf { !game && it.isNotBlank() }, info?.takeIf { it.isNotBlank() }).joinToString("\n")
            if (subtitle.isNotEmpty()) Text(subtitle, fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted, maxLines = 4,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            message?.let { Text(it, fontFamily = Inter, fontSize = 11.sp, color = TvColors.AccentLight, modifier = Modifier.padding(top = 6.dp)) }
            LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(entries.size) { i ->
                    val e = entries[i]
                    ListItem(
                        selected = false,
                        onClick = e.click,
                        modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                        colors = ListItemDefaults.colors(focusedContainerColor = TvColors.Focus, focusedContentColor = Color.Black,
                            contentColor = TvColors.Text),
                        headlineContent = { Text(e.title, fontFamily = Inter, fontSize = 13.sp) },
                        supportingContent = e.subtitle?.let { { Text(it, fontFamily = Inter, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                        trailingContent = e.switchValue?.let { on -> { Switch(checked = on, onCheckedChange = null) } },
                    )
                }
            }
        }
    }
}
