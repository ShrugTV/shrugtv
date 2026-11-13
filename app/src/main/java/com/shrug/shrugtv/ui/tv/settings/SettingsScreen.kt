package com.shrug.shrugtv.ui.tv.settings

import androidx.compose.foundation.BorderStroke
import com.shrug.shrugtv.ui.tv.themeBackground
import com.shrug.shrugtv.ui.tv.TvStyle
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors

/**
 * Settings after the mockup (step 5): title, account and groups on the left,
 * the content of the selected group as a list on the right; sub-pages open on the right with
 * breadcrumbs. The content comes unchanged from settings.xml (SettingsModel).
 */
class SettingsAccess(
    val bool: (String, Boolean) -> Boolean,
    val string: (String, String?) -> String?,
    val setBool: (String, Boolean) -> Unit,
    val setString: (String, String) -> Unit,
    val action: (String) -> Unit,
    val active: (String?) -> Boolean,
    val extra: (String?) -> String?,
    /** Puts these keys back to their defaults. */
    val reset: (List<String>) -> Unit = {},
)

/** Left column: account at the top, info pages at the bottom; [changes] reads assets/changelog.md. */
data class Account(val name: String?, val logIn: () -> Unit,
                 val version: String = "", val changes: () -> List<Changelog.Version> = { emptyList() })

@Composable
fun SettingsScreen(groups: List<Entry.Page>, z: SettingsAccess, account: Account, version: Int) {
    var selected by remember { mutableStateOf(0) }
    val path = remember { mutableStateListOf<Entry.Page>() }
    LaunchedEffect(selected) { path.clear(); groups.getOrNull(selected)?.let { path.add(it) } }
    val firstGroup = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstGroup.requestFocus() } }
    // Back on a sub-page goes up one level instead of closing the settings.
    // After every level change, move focus into the right-hand list; otherwise it lands on the left
    // because the clicked row disappears (2026-10-21 in the emulator: focus jumped to "Account").
    val right = remember { FocusRequester() }
    var levelChange by remember { mutableStateOf(0) }
    LaunchedEffect(levelChange) { if (levelChange > 0) runCatching { right.requestFocus() } }
    androidx.activity.compose.BackHandler(enabled = path.size > 1) { path.removeAt(path.lastIndex); levelChange++ }

    // Pages with a preview (Appearance): the preview is pinned on the right and never scrolls.
    // While the focus is in the list the group column shrinks to a rail so list and preview both fit.
    val infoIndex = selected - groups.size
    val pinned = InfoPage.visible.getOrNull(infoIndex) == null &&
        path.lastOrNull()?.children?.any { it is Entry.Preview } == true
    var inList by remember { mutableStateOf(false) }
    // The setting in focus decides which preview scene shows
    var focusKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selected) { inList = false; focusKey = null }
    val narrow = pinned && inList
    val railWidth by animateDpAsState(if (narrow) 64.dp else 310.dp, label = "rail")

    Row(Modifier.fillMaxSize().themeBackground()) {
        Column(
            Modifier.width(railWidth).fillMaxHeight()
                .padding(start = if (narrow) 12.dp else 72.dp, top = 48.dp, end = if (narrow) 4.dp else 16.dp)
                .onFocusChanged { if (it.hasFocus) inList = false },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(if (narrow) "" else stringResource(R.string.pref_title_root_title), fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text,
                maxLines = 1, modifier = Modifier.padding(bottom = 14.dp))
            // focusRestorer: arrow left from the content returns to the selected entry, not to the
            // geometrically nearest one (2026-10-28 in the emulator: from "Changes" to "Application info")
            LazyColumn(Modifier.focusRestorer(), contentPadding = PaddingValues(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    GroupButton(stringResource(R.string.settings_account),
                        account.name?.let { stringResource(R.string.settings_signed_in, it) } ?: stringResource(R.string.settings_not_signed_in),
                        false, Modifier, compact = narrow, icon = R.drawable.ic_baseline_person_24px) { account.logIn() }
                }
                items(groups.size) { i ->
                    val g = groups[i]
                    GroupButton(g.title, null, i == selected,
                        (if (i == 0) Modifier.focusRequester(firstGroup) else Modifier).onFocusChanged { if (it.isFocused) selected = i },
                        compact = narrow, icon = g.icon, changed = PageChanges.changedDeep(g.children, z) > 0) {
                        selected = i
                    }
                }
                // Info pages: shown on the right like a group instead of separate WebView activities
                items(InfoPage.visible.size) { j ->
                    val i = groups.size + j
                    GroupButton(InfoPage.visible[j].title, null, i == selected,
                        Modifier.onFocusChanged { if (it.isFocused) selected = i }, compact = narrow, icon = R.drawable.ic_baseline_info_24px) {
                        selected = i
                        runCatching { right.requestFocus() }
                    }
                }
            }
        }

        val page = path.lastOrNull()
        val info = InfoPage.visible.getOrNull(selected - groups.size)
        Column(Modifier.fillMaxSize().padding(start = 24.dp, top = 48.dp, end = 60.dp)) {
            if (info != null) {
                Text(info.title, fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = TvColors.Text, modifier = Modifier.padding(bottom = 14.dp))
                InfoPageContent(info, account.version, account.changes, right)
            } else if (page != null) {
                Text(path.joinToString("  ›  ") { it.title }, fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 14.dp))
                Row(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalLivePreview provides pinned, LocalSettingsAccess provides z,
                        LocalFocusReport provides { focusKey = it }) {
                        LazyColumn(
                            Modifier.weight(1f).focusRequester(right).onFocusChanged { if (it.hasFocus) inList = true }
                                .clip(TvShapes.r(12.dp)).background(TvColors.Surface),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            rows(page.children, z, version) { path.add(it); levelChange++ }
                            val changed = PageChanges.changedKeys(page, z)
                            if (changed.isNotEmpty()) item(key = "reset#" + page.key) { ResetRow(changed.size) { z.reset(changed) } }
                        }
                        if (narrow) {
                            val preview = page.children.filterIsInstance<Entry.Preview>().first()
                            val own = when (page.key) {
                                "pref_chat_screen" -> PreviewScene.CHAT
                                "pref_stream_settings" -> PreviewScene.PLAYER
                                "pref_general_settings" -> PreviewScene.HOME
                                else -> PreviewScene.BOTH
                            }
                            SettingsPreview(sceneFor(focusKey, own), preview.title, Modifier.width(400.dp).padding(start = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

/** "Reset this page" with a question first; shown only when something on the page differs from the default. */
@Composable
private fun ResetRow(count: Int, reset: () -> Unit) {
    var ask by remember { mutableStateOf(false) }
    ListItem(
        selected = false, onClick = { ask = true }, scale = noZoom(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, contentColor = TvColors.TextMedium,
            focusedContainerColor = TvColors.SurfaceHigh, focusedContentColor = TvColors.Text),
        border = ListItemDefaults.border(focusedBorder = androidx.tv.material3.Border(TvStyle.focusStroke, shape = TvShapes.r(10.dp))),
        headlineContent = { Text(stringResource(R.string.settings_reset_page), fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(stringResource(R.string.settings_reset_page_count, count), fontFamily = Inter, fontSize = 10.sp, color = TvColors.TextMuted) },
    )
    if (ask) {
        val no = remember { FocusRequester() }
        Dialog(onDismissRequest = { ask = false }) {
            Column(Modifier.width(520.dp).clip(TvShapes.r(16.dp)).background(TvColors.SurfaceHigh).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.settings_reset_page_ask), fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
                Text(stringResource(R.string.settings_reset_page_text), fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp, color = TvColors.TextMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { ask = false }, modifier = Modifier.focusRequester(no),
                        colors = ButtonDefaults.colors(containerColor = TvColors.glass(0.14f), contentColor = TvColors.Text,
                            focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background)) {
                        Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
                    }
                    Button(onClick = { ask = false; reset() },
                        colors = ButtonDefaults.colors(containerColor = TvColors.glass(0.14f), contentColor = TvColors.Text,
                            focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background)) {
                        Text(stringResource(R.string.settings_reset_do), fontFamily = Inter, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { no.requestFocus() } }
    }
}

/** True on a page with a pinned preview: choices there apply while you move over them. */
val LocalLivePreview = compositionLocalOf { false }

/** A guide (`pref_tip`): not a setting, only text. Shown behind an (i) button instead of in the list. */
private fun isGuide(e: Entry) = e is Entry.Action && e.key == null && !e.selectable

private fun androidx.compose.foundation.lazy.LazyListScope.rows(
    children: List<Entry>, z: SettingsAccess, version: Int, open: (Entry.Page) -> Unit,
) {
    for (e in children) {
        when {
            e is Entry.Category -> {
                // The group's guide moves into an (i) button next to its title
                val guide = e.children.firstOrNull(::isGuide)
                item { CategoryHeader(e.title, guide?.text) }
                rows(e.children.filter { it !== guide }, z, version, open)
            }
            isGuide(e) -> item(key = "guide#" + e.hashCode()) { GuideRow(e.text) }
            else -> item(key = (e.key ?: e.title) + "#" + e.hashCode()) { EntryRow(e, z, version, open) }
        }
    }
}

/** No zoom on focus: the list clips its edges, a zoomed row lost its white frame and corners. */
@Composable
private fun noZoom() = ListItemDefaults.scale(scale = 1f, focusedScale = 1f, pressedScale = 1f, selectedScale = 1f,
    focusedSelectedScale = 1f, pressedSelectedScale = 1f)

@Composable
private fun CategoryHeader(title: String, guide: String?) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TvColors.AccentLight,
            modifier = Modifier.weight(1f))
        if (!guide.isNullOrBlank()) InfoButton { open = true }
    }
    if (open && guide != null) GuideDialog(title, guide) { open = false }
}

@Composable
private fun InfoButton(click: () -> Unit) {
    androidx.tv.material3.IconButton(
        onClick = click,
        modifier = Modifier.size(30.dp),
        scale = androidx.tv.material3.IconButtonDefaults.scale(focusedScale = 1f),
        colors = androidx.tv.material3.IconButtonDefaults.colors(
            containerColor = Color.Transparent, contentColor = TvColors.AccentLight,
            focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background,
        ),
    ) {
        Icon(painterResource(R.drawable.ic_baseline_info_24px), stringResource(R.string.settings_guide_show), Modifier.size(18.dp))
    }
}

/** A guide outside a group (e.g. on a sub-page): one short line, OK shows the whole text. */
@Composable
private fun GuideRow(text: String?) {
    if (text.isNullOrBlank()) return
    var open by remember { mutableStateOf(false) }
    ListItem(
        selected = false, onClick = { open = true }, scale = noZoom(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, contentColor = TvColors.TextMedium,
            focusedContainerColor = TvColors.SurfaceHigh, focusedContentColor = TvColors.Text),
        border = ListItemDefaults.border(focusedBorder = androidx.tv.material3.Border(TvStyle.focusStroke, shape = TvShapes.r(10.dp))),
        leadingContent = { Icon(painterResource(R.drawable.ic_baseline_info_24px), null, Modifier.size(16.dp)) },
        headlineContent = { Text(text, fontFamily = Inter, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
    if (open) GuideDialog(stringResource(R.string.settings_guide), text) { open = false }
}

@Composable
private fun GuideDialog(title: String, text: String, close: () -> Unit) {
    val ok = remember { FocusRequester() }
    Dialog(onDismissRequest = close) {
        Column(Modifier.width(560.dp).clip(TvShapes.r(16.dp)).background(TvColors.SurfaceHigh).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_baseline_info_24px), null, Modifier.size(20.dp), tint = TvColors.AccentLight)
                Text(title, fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
            }
            Text(text, fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp, color = TvColors.TextMedium)
            Button(onClick = close, modifier = Modifier.focusRequester(ok),
                colors = ButtonDefaults.colors(containerColor = TvColors.glass(0.14f), contentColor = TvColors.Text,
                    focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background)) {
                Text(stringResource(android.R.string.ok), fontFamily = Inter, fontWeight = FontWeight.Bold)
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { ok.requestFocus() } }
}

@Composable
private fun GroupButton(title: String, subtitle: String?, selected: Boolean, modifier: Modifier,
                        compact: Boolean = false, icon: Int = 0, changed: Boolean = false, click: () -> Unit) {
    ListItem(
        selected = selected,
        onClick = click,
        modifier = modifier,
        scale = noZoom(),
        headlineContent = {
            if (compact) {
                // Rail: the group's icon, or its first letter
                if (icon != 0) Icon(painterResource(icon), title, Modifier.size(20.dp))
                else Text(title.take(1), fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            } else Text((if (changed) "● " else "") + title, fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = if (compact) null else subtitle?.let { { Text(it, fontFamily = Inter, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent, contentColor = TvColors.TextMedium,
            selectedContainerColor = TvColors.Accent, selectedContentColor = TvColors.OnAccent,
            focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background,
            focusedSelectedContainerColor = TvColors.Ink, focusedSelectedContentColor = TvColors.Background,
        ),
    )
}

@Composable
private fun EntryRow(e: Entry, z: SettingsAccess, version: Int, open: (Entry.Page) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    // Descriptions take one line; the focused row shows them in full
    var focused by remember { mutableStateOf(false) }
    val report = LocalFocusReport.current
    val focus = Modifier.onFocusChanged { focused = it.isFocused; if (it.isFocused) report(e.key) }
    val scale = noZoom()
    val active = z.active(e.dependent)
    val colors = ListItemDefaults.colors(
        containerColor = Color.Transparent, contentColor = TvColors.Text,
        focusedContainerColor = TvColors.SurfaceHigh, focusedContentColor = TvColors.Text,
        disabledContainerColor = Color.Transparent, disabledContentColor = TvColors.TextMuted.copy(alpha = 0.5f),
    )
    val border = ListItemDefaults.border(focusedBorder = androidx.tv.material3.Border(TvStyle.focusStroke, shape = TvShapes.r(10.dp)))
    val header: @Composable () -> Unit = { Text(e.title, fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    fun subtitle(t: String?): (@Composable () -> Unit)? = t?.takeIf { it.isNotBlank() }?.let {
        { Text(it, fontFamily = Inter, fontSize = 10.sp, color = TvColors.TextMuted, maxLines = if (focused) 8 else 1, overflow = TextOverflow.Ellipsis) }
    }
    when (e) {
        is Entry.Page -> ListItem(selected = false, onClick = { open(e) }, enabled = active, colors = colors, border = border, modifier = focus, scale = scale,
            headlineContent = header, supportingContent = subtitle(e.text),
            leadingContent = if (e.icon != 0) ({ Icon(painterResource(e.icon), null, Modifier.size(18.dp)) }) else null,
            trailingContent = {
                val n = PageChanges.changedDeep(e.children, z)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (n > 0) Text("● $n", fontFamily = Inter, fontSize = 10.sp, color = TvColors.AccentLight)
                    Text("›", fontSize = 18.sp, color = TvColors.TextMuted)
                }
            })
        is Entry.Toggle -> {
            val on = z.bool(e.key, e.standard)
            ListItem(selected = false, onClick = { z.setBool(e.key, !on) }, enabled = active, colors = colors, border = border, modifier = focus, scale = scale,
                headlineContent = header, supportingContent = subtitle((if (on) e.textOn else e.textOff) ?: e.text),
                trailingContent = { Switch(checked = on, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = TvColors.Accent)) })
        }
        is Entry.ListChoice -> {
            val value = z.string(e.key, e.standard)
            val name = e.values.indexOf(value).takeIf { it >= 0 }?.let { e.names.getOrNull(it) } ?: value ?: ""
            val description = if (e.text == null || e.text.contains("%s")) null else e.text
            // On a page with a pinned preview a design choice applies while you move over it; Back restores it
            val live = LocalLivePreview.current && e.key.startsWith("pref_theme_")
            var original by remember { mutableStateOf<String?>(null) }
            ListItem(selected = false, onClick = { original = value; dialog = true }, enabled = active, colors = colors, border = border, modifier = focus, scale = scale,
                headlineContent = header, supportingContent = subtitle(description),
                trailingContent = { Text(name, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMedium, maxLines = 1) })
            if (dialog) ChoiceDialog(e, value, live,
                close = { if (live) z.setString(e.key, original ?: e.standard ?: "skin"); dialog = false },
                tryIt = { z.setString(e.key, it) }) { z.setString(e.key, it); dialog = false }
        }
        is Entry.TextField -> {
            val value = z.string(e.key, e.standard) ?: ""
            ListItem(selected = false, onClick = { dialog = true }, enabled = active, colors = colors, border = border, modifier = focus, scale = scale,
                headlineContent = header, supportingContent = subtitle(e.text),
                trailingContent = { Text(if (value.isEmpty()) "–" else if (e.key in SettingsModel.SECRET_KEYS) "••••••" else value, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMedium, maxLines = 1) })
            if (dialog) TextDialog(e, value, { dialog = false }) { z.setString(e.key, it); dialog = false }
        }
        is Entry.NumberField -> {
            val value = NumberInput.parse(z.string(e.key, e.standard.toString()) ?: "", e.min, e.max, e.standard).toString()
            ListItem(selected = false, onClick = { dialog = true }, enabled = active, colors = colors, border = border, modifier = focus, scale = scale,
                headlineContent = header, supportingContent = subtitle(e.text),
                trailingContent = { Text(value, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMedium, maxLines = 1) })
            if (dialog) NumberDialog(e, value, { dialog = false }) { z.setString(e.key, it); dialog = false }
        }
        is Entry.Action -> {
            val text = z.extra(e.key) ?: e.text
            if (!e.selectable || e.key == null) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(e.title, fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TvColors.TextMedium)
                    if (!text.isNullOrBlank()) Text(text, fontFamily = Inter, fontSize = 10.sp, color = TvColors.TextMuted)
                }
            } else {
                ListItem(selected = false, onClick = { z.action(e.key) }, enabled = active, colors = colors, border = border, modifier = focus, scale = scale,
                    headlineContent = header, supportingContent = subtitle(text))
            }
        }
        is Entry.ColorChoice -> ColorRow(e, z)
        is Entry.SkinChoice -> SkinRow(e, z)
        is Entry.Preview -> Unit // pinned beside the list by SettingsScreen
        is Entry.Category -> Unit
    }
}

/**
 * A dialog; on a page with a pinned preview ([live]) it sits on the left, does not dim the screen and so leaves
 * the preview in view.
 */
@Composable
fun SideDialog(live: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    if (!live) { Dialog(onDismissRequest = onDismiss) { content() }; return }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setDimAmount(0f)
        Box(Modifier.fillMaxSize().padding(start = 16.dp), contentAlignment = Alignment.CenterStart) { content() }
    }
}

@Composable
private fun ChoiceDialog(e: Entry.ListChoice, value: String?, live: Boolean, close: () -> Unit,
                         tryIt: (String) -> Unit, select: (String) -> Unit) {
    SideDialog(live, close) {
        Column(Modifier.width(420.dp).clip(TvShapes.r(16.dp)).background(TvColors.SurfaceHigh).padding(16.dp)) {
            Text(e.title, fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvColors.Text,
                modifier = Modifier.padding(bottom = 10.dp))
            val start = remember { FocusRequester() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(e.names.size) { i ->
                    val selected = e.values.getOrNull(i) == value
                    ListItem(
                        selected = selected, onClick = { e.values.getOrNull(i)?.let(select) },
                        modifier = (if (selected || (value !in e.values && i == 0)) Modifier.focusRequester(start) else Modifier)
                            .onFocusChanged { f -> if (live && f.isFocused) e.values.getOrNull(i)?.let(tryIt) },
                        scale = noZoom(),
                        headlineContent = { Text(e.names[i], fontFamily = Inter, fontSize = 13.sp) },
                        trailingContent = if (selected) ({ Text("✓", fontSize = 14.sp) }) else null,
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent, contentColor = TvColors.Text,
                            selectedContainerColor = TvColors.Accent, selectedContentColor = TvColors.OnAccent,
                            focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background,
                            focusedSelectedContainerColor = TvColors.Ink, focusedSelectedContentColor = TvColors.Background,
                        ),
                    )
                }
            }
            LaunchedEffect(Unit) { runCatching { start.requestFocus() } }
        }
    }
}

/**
 * A number. Left and Right change it in steps; OK on it opens the keypad for typing
 * (the keypad takes the remote keys while it is up, so it is not open from the start).
 */
@Composable
private fun NumberDialog(e: Entry.NumberField, value: String, close: () -> Unit, save: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    var typing by remember { mutableStateOf(false) }
    var valueFocused by remember { mutableStateOf(false) }
    val display = remember { FocusRequester() }
    val field = remember { FocusRequester() }
    val saveButton = remember { FocusRequester() }
    Dialog(onDismissRequest = close) {
        Column(Modifier.width(460.dp).clip(TvShapes.r(16.dp)).background(TvColors.SurfaceHigh).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(e.title, fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("◀", fontSize = 18.sp, color = TvColors.TextMedium)
                Box(Modifier.width(110.dp)
                    .border(if (valueFocused || typing) 2.dp else 1.dp, if (valueFocused || typing) TvColors.Ink else TvColors.TextMuted, TvShapes.r(8.dp))
                    .padding(12.dp)) {
                    if (typing) {
                        BasicTextField(text, { text = NumberInput.digits(it, e.max) }, singleLine = true, cursorBrush = SolidColor(TvColors.Text),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                text = NumberInput.parse(text, e.min, e.max, e.standard).toString()
                                typing = false
                            }),
                            textStyle = TextStyle(color = TvColors.Text, fontSize = 16.sp, fontFamily = Inter, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth().focusRequester(field))
                        LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
                    } else {
                        Text(text, fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TvColors.Text,
                            modifier = Modifier.fillMaxWidth()
                                .focusRequester(display)
                                // Down goes to Save, not to the nearest button
                                .focusProperties { down = saveButton }
                                .onFocusChanged { valueFocused = it.isFocused }
                                .onPreviewKeyEvent { ev ->
                                    val direction = when (ev.key) {
                                        androidx.compose.ui.input.key.Key.DirectionLeft -> -1
                                        androidx.compose.ui.input.key.Key.DirectionRight -> 1
                                        else -> 0
                                    }
                                    val ok = ev.key == androidx.compose.ui.input.key.Key.DirectionCenter || ev.key == androidx.compose.ui.input.key.Key.Enter
                                    when {
                                        direction != 0 -> {
                                            if (ev.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                                text = NumberInput.step(text, direction, e.step, e.min, e.max, e.standard)
                                            }
                                            true
                                        }
                                        ok -> {
                                            if (ev.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) typing = true
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .focusable())
                        LaunchedEffect(Unit) { runCatching { display.requestFocus() } }
                    }
                }
                Text("▶", fontSize = 18.sp, color = TvColors.TextMedium)
                Text("${e.min} – ${e.max}", fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMuted)
            }
            Text(stringResource(R.string.settings_number_hint, e.step), fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { save(NumberInput.parse(text, e.min, e.max, e.standard).toString()) },
                    modifier = Modifier.focusRequester(saveButton),
                    colors = ButtonDefaults.colors(containerColor = TvColors.Ink, contentColor = TvColors.Background)) {
                    Text(stringResource(R.string.settings_save), fontFamily = Inter, fontWeight = FontWeight.Bold)
                }
                Button(onClick = close, colors = ButtonDefaults.colors(containerColor = TvColors.glass(0.14f), contentColor = TvColors.Text)) {
                    Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
                }
            }
        }
    }
}

@Composable
private fun TextDialog(e: Entry.TextField, value: String, close: () -> Unit, save: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    val field = remember { FocusRequester() }
    Dialog(onDismissRequest = close) {
        Column(Modifier.width(460.dp).clip(TvShapes.r(16.dp)).background(TvColors.SurfaceHigh).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(e.dialogTitle ?: e.title, fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
            Box(Modifier.fillMaxWidth().border(1.dp, TvColors.TextMuted, TvShapes.r(8.dp)).padding(12.dp)) {
                BasicTextField(text, { text = it }, singleLine = true, cursorBrush = SolidColor(TvColors.Text),
                    visualTransformation = if (e.key in SettingsModel.SECRET_KEYS) androidx.compose.ui.text.input.PasswordVisualTransformation()
                        else androidx.compose.ui.text.input.VisualTransformation.None,
                    textStyle = TextStyle(color = TvColors.Text, fontSize = 14.sp, fontFamily = Inter),
                    modifier = Modifier.fillMaxWidth().focusRequester(field))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { save(text) }, colors = ButtonDefaults.colors(containerColor = TvColors.Ink, contentColor = TvColors.Background)) {
                    Text(stringResource(R.string.settings_save), fontFamily = Inter, fontWeight = FontWeight.Bold)
                }
                Button(onClick = close, colors = ButtonDefaults.colors(containerColor = TvColors.glass(0.14f), contentColor = TvColors.Text)) {
                    Text(stringResource(R.string.dialog_cancel), fontFamily = Inter)
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
}
