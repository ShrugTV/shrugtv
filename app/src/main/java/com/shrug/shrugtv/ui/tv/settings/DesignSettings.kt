package com.shrug.shrugtv.ui.tv.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.LocalShrugTheme
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvShapes
import com.shrug.shrugtv.ui.tv.TvStyle
import com.shrug.shrugtv.ui.tv.theme.ShrugTheme
import com.shrug.shrugtv.ui.tv.theme.Skins
import com.shrug.shrugtv.ui.tv.theme.ThemeSettings
import com.shrug.shrugtv.ui.tv.themeBackground

/*
 * Entries of the "Appearance" page: live preview, skin tiles and the colour picker.
 * Everything else on the page is an ordinary list; all of it writes `pref_theme_*`, ThemeSettings turns that into
 * the theme and every screen follows at once.
 */

/** Colours offered in the picker: the accents of the skins, bright and muted tones, greys. */
val PickerColors: List<Long> = listOf(
    0xFF9147FF, 0xFF7B3FE4, 0xFFBF94FF, 0xFFFF79C6, 0xFFFF6B9D, 0xFFB3005F,
    0xFFDB002C, 0xFFE91916, 0xFFFF5A5F, 0xFFDB5800, 0xFFFF7A45, 0xFFFFB020,
    0xFFFECF01, 0xFFFFD400, 0xFFF1FA8C, 0xFF0BDB00, 0xFF50FA7B, 0xFF6BCB77,
    0xFF00B8A9, 0xFF1FC8B8, 0xFF88C0D0, 0xFF005FDB, 0xFF4D96FF, 0xFF8BE9FD,
    0xFFFFFFFF, 0xFFEFEFF1, 0xFFADADB8, 0xFF3B4252, 0xFF17171C, 0xFF000000,
)

/** The colour a `pref_theme_*` key has in [t] right now. */
fun colorOf(t: ShrugTheme, key: String): Color = when (key) {
    ThemeSettings.PREF_BACKGROUND -> t.colors.background
    ThemeSettings.PREF_SURFACE -> t.colors.surface
    ThemeSettings.PREF_TEXT -> t.colors.text
    ThemeSettings.PREF_TEXT_MUTED -> t.colors.textMuted
    ThemeSettings.PREF_LIVE -> t.colors.live
    ThemeSettings.PREF_FOCUS -> t.colors.focus
    else -> t.colors.accent
}

fun skinName(id: String): Int = when (id) {
    "oled" -> R.string.skin_oled
    "twitch" -> R.string.skin_twitch
    "nord" -> R.string.skin_nord
    "dracula" -> R.string.skin_dracula
    "sunset" -> R.string.skin_sunset
    "ocean" -> R.string.skin_ocean
    "retro" -> R.string.skin_retro
    "light" -> R.string.skin_light
    "contrast" -> R.string.skin_contrast
    else -> if (com.shrug.shrugtv.ui.tv.theme.CustomSkins.isCustom(id)) R.string.skin_custom else R.string.skin_shrugtv
}

@Composable
private fun rowColors() = ListItemDefaults.colors(
    containerColor = Color.Transparent, contentColor = TvColors.Text,
    focusedContainerColor = TvColors.SurfaceHigh, focusedContentColor = TvColors.Text,
)

@Composable
private fun rowBorder() = ListItemDefaults.border(focusedBorder = androidx.tv.material3.Border(TvStyle.focusStroke, shape = TvShapes.r(10.dp)))

@Composable
private fun noZoom() = ListItemDefaults.scale(scale = 1f, focusedScale = 1f, pressedScale = 1f, selectedScale = 1f,
    focusedSelectedScale = 1f, pressedSelectedScale = 1f)

@Composable
private fun Swatch(c: Color, size: Int = 22) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(c).border(1.dp, TvColors.glass(0.35f), CircleShape))
}

// ------------------------------------------------------------------ colour

@Composable
fun ColorRow(e: Entry.ColorChoice, z: SettingsAccess) {
    var open by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val own = z.string(e.key, e.standard)?.takeIf { it != ThemeSettings.FROM_SKIN && ShrugTheme.parseHex(it) != null }
    val current = colorOf(LocalShrugTheme.current, e.key)
    var original by remember { mutableStateOf(ThemeSettings.FROM_SKIN) }
    val report = LocalFocusReport.current
    ListItem(
        selected = false, onClick = { original = z.string(e.key, e.standard) ?: ThemeSettings.FROM_SKIN; open = true }, colors = rowColors(), border = rowBorder(), scale = noZoom(),
        modifier = Modifier.onFocusChanged { focused = it.isFocused; if (it.isFocused) report(e.key) },
        headlineContent = { Text(e.title, fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
        supportingContent = e.text?.takeIf { it.isNotBlank() }?.let {
            { Text(it, fontFamily = Inter, fontSize = 10.sp, color = TvColors.TextMuted, maxLines = if (focused) 6 else 1) }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (own != null) "● " + own.uppercase() else stringResource(R.string.theme_from_skin),
                    fontFamily = Inter, fontSize = 12.sp, color = if (own != null) TvColors.AccentLight else TvColors.TextMedium)
                Swatch(current)
            }
        },
    )
    // Live: the colour applies while you move over it, Back restores the old value
    if (open) ColorDialog(e.title, current, own != null, LocalLivePreview.current,
        close = { z.setString(e.key, original); open = false }, tryIt = { z.setString(e.key, it) }) { v -> z.setString(e.key, v); open = false }
}

/**
 * Colour picker for the remote: 30 colours, or fine-tuning with three rows (hue, saturation, brightness) that
 * Left/Right change, or typing the code. "From the skin" removes the own colour.
 */
@Composable
private fun ColorDialog(title: String, current: Color, hasOwn: Boolean, live: Boolean, close: () -> Unit,
                        tryIt: (String) -> Unit, save: (String) -> Unit) {
    var mode by remember { mutableStateOf(0) } // 0 colours, 1 fine-tuning, 2 code
    val first = remember { FocusRequester() }
    SideDialog(live, close) {
        Column(Modifier.width(480.dp).clip(TvShapes.r(16.dp)).background(TvColors.SurfaceHigh).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Swatch(current, 26)
                Text(title, fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
            }
            when (mode) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PickerColors.chunked(6).forEachIndexed { r, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEachIndexed { i, v ->
                                    val c = Color(v)
                                    val isCurrent = c.toArgb() == current.toArgb()
                                    Surface(
                                        onClick = { save(ShrugTheme.toHex(c)) },
                                        modifier = Modifier.size(width = 62.dp, height = 38.dp)
                                            .onFocusChanged { f -> if (live && f.isFocused) tryIt(ShrugTheme.toHex(c)) }
                                            .then(if (r == 0 && i == 0) Modifier.focusRequester(first) else Modifier),
                                        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(TvShapes.r(8.dp)),
                                        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(containerColor = c, focusedContainerColor = c),
                                        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
                                            border = if (isCurrent) androidx.tv.material3.Border(BorderStroke(2.dp, TvColors.Ink.copy(alpha = 0.6f)), shape = TvShapes.r(8.dp)) else androidx.tv.material3.Border.None,
                                            focusedBorder = androidx.tv.material3.Border(BorderStroke(3.dp, TvColors.Focus), shape = TvShapes.r(8.dp))),
                                        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                                    ) {
                                        if (isCurrent) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("✓", fontSize = 16.sp, color = ShrugTheme.onColor(c))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> FineTune(current, first, if (live) tryIt else { _ -> }, save)
                else -> HexInput(current, first, if (live) tryIt else { _ -> }, save)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogButton(stringResource(R.string.theme_picker_colors), mode == 0) { mode = 0 }
                DialogButton(stringResource(R.string.theme_picker_fine), mode == 1) { mode = 1 }
                DialogButton(stringResource(R.string.theme_picker_code), mode == 2) { mode = 2 }
                if (hasOwn) DialogButton(stringResource(R.string.theme_from_skin), false) { save(ThemeSettings.FROM_SKIN) }
            }
        }
    }
    LaunchedEffect(mode) { runCatching { first.requestFocus() } }
}

@Composable
private fun DialogButton(text: String, active: Boolean, click: () -> Unit) {
    Button(onClick = click, colors = ButtonDefaults.colors(
        containerColor = if (active) TvColors.Accent else TvColors.glass(0.14f), contentColor = if (active) TvColors.OnAccent else TvColors.Text,
        focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background)) {
        Text(text, fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Hue, saturation and brightness, each a row that Left/Right change in steps (held keys repeat). */
@Composable
private fun FineTune(start: Color, first: FocusRequester, tryIt: (String) -> Unit, save: (String) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(start.toArgb(), it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    val c = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    LaunchedEffect(h, s, v) { if (c.toArgb() != start.toArgb() || h != hsv[0]) tryIt(ShrugTheme.toHex(c)) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(width = 120.dp, height = 56.dp).clip(TvShapes.r(10.dp)).background(c))
            Text(ShrugTheme.toHex(c), fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
        }
        SliderRow(stringResource(R.string.theme_picker_hue), "${h.toInt()}°", h / 360f,
            Brush.horizontalGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) }),
            Modifier.focusRequester(first)) { d -> h = ((h + d * 6f) % 360f + 360f) % 360f }
        SliderRow(stringResource(R.string.theme_picker_saturation), "${(s * 100).toInt()} %", s,
            Brush.horizontalGradient(listOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 0f, v))), Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, v))))),
            Modifier) { d -> s = (s + d * 0.05f).coerceIn(0f, 1f) }
        SliderRow(stringResource(R.string.theme_picker_brightness), "${(v * 100).toInt()} %", v,
            Brush.horizontalGradient(listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, 1f))))),
            Modifier) { d -> v = (v + d * 0.05f).coerceIn(0f, 1f) }
        Button(onClick = { save(ShrugTheme.toHex(c)) }, colors = ButtonDefaults.colors(containerColor = TvColors.Ink, contentColor = TvColors.Background)) {
            Text(stringResource(R.string.settings_save), fontFamily = Inter, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SliderRow(label: String, value: String, fraction: Float, track: Brush, modifier: Modifier, step: (Int) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().clip(TvShapes.r(8.dp))
            .background(if (focused) TvColors.glass(0.10f) else Color.Transparent)
            .border(if (focused) TvStyle.focusStroke else BorderStroke(0.dp, Color.Transparent), TvShapes.r(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { ev ->
                val d = when (ev.key) { Key.DirectionLeft -> -1; Key.DirectionRight -> 1; else -> 0 }
                if (d != 0 && ev.type == KeyEventType.KeyDown) step(d)
                d != 0
            }
            .focusable()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMedium, modifier = Modifier.width(96.dp))
        Box(Modifier.weight(1f).height(14.dp).clip(CircleShape).background(track)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0.001f, 1f))) {
                Box(Modifier.align(Alignment.CenterEnd).size(14.dp).clip(CircleShape).background(Color.White).border(2.dp, Color.Black, CircleShape))
            }
        }
        Text(value, fontFamily = Inter, fontSize = 12.sp, color = TvColors.Text, modifier = Modifier.width(52.dp))
    }
}

@Composable
private fun HexInput(start: Color, first: FocusRequester, tryIt: (String) -> Unit, save: (String) -> Unit) {
    var text by remember { mutableStateOf(ShrugTheme.toHex(start)) }
    val parsed = ShrugTheme.parseHex(text)
    LaunchedEffect(parsed) { if (parsed != null && parsed.toArgb() != start.toArgb()) tryIt(ShrugTheme.toHex(parsed)) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(TvShapes.r(8.dp)).background(parsed ?: Color.Transparent).border(1.dp, TvColors.TextMuted, TvShapes.r(8.dp)))
        Box(Modifier.width(200.dp).border(1.dp, TvColors.TextMuted, TvShapes.r(8.dp)).padding(12.dp)) {
            BasicTextField(text, { text = it.take(9) }, singleLine = true, cursorBrush = SolidColor(TvColors.Text),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { parsed?.let { save(ShrugTheme.toHex(it)) } }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                textStyle = TextStyle(color = TvColors.Text, fontSize = 15.sp, fontFamily = Inter),
                modifier = Modifier.fillMaxWidth().focusRequester(first))
        }
        Button(onClick = { parsed?.let { save(ShrugTheme.toHex(it)) } }, enabled = parsed != null,
            colors = ButtonDefaults.colors(containerColor = TvColors.Ink, contentColor = TvColors.Background)) {
            Text(stringResource(R.string.settings_save), fontFamily = Inter, fontWeight = FontWeight.Bold)
        }
    }
    Text(stringResource(R.string.theme_picker_code_hint), fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted)
}

// ------------------------------------------------------------------ skins

/** All skins as tiles, each drawn in its own colours; OK picks it and clears own changes. */
@Composable
fun SkinRow(e: Entry.SkinChoice, z: SettingsAccess) {
    val current = z.string(e.key, Skins.SHRUGTV.id) ?: Skins.SHRUGTV.id
    // Own skins come after the ready-made ones, drawn in their own colours
    val custom = com.shrug.shrugtv.ui.tv.theme.CustomSkins.parse(z.string(ThemeSettings.PREF_CUSTOM, null))
    val tiles = Skins.all.map { it to stringResource(skinName(it.id)) } +
        custom.map { com.shrug.shrugtv.ui.tv.theme.CustomSkins.theme(it) to it.name }
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(e.title, fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TvColors.Text, modifier = Modifier.padding(start = 8.dp))
        e.text?.let { Text(it, fontFamily = Inter, fontSize = 10.sp, lineHeight = 14.sp, color = TvColors.TextMuted, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
            items(tiles.size) { i ->
                val (skin, name) = tiles[i]
                SkinTile(skin, name, skin.id == current) {
                    z.action(ThemeSettings.ACTION_RESET)
                    z.setString(e.key, skin.id)
                }
            }
        }
    }
}

@Composable
private fun SkinTile(skin: ShrugTheme, name: String, selected: Boolean, click: () -> Unit) {
    val c = skin.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = click,
            modifier = Modifier.size(width = 132.dp, height = 78.dp),
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(TvShapes.r(10.dp)),
            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(containerColor = c.background, focusedContainerColor = c.background),
            border = androidx.tv.material3.ClickableSurfaceDefaults.border(
                border = if (selected) androidx.tv.material3.Border(BorderStroke(2.dp, TvColors.Accent), shape = TvShapes.r(10.dp)) else androidx.tv.material3.Border.None,
                focusedBorder = androidx.tv.material3.Border(TvStyle.focusStroke, shape = TvShapes.r(10.dp))),
            scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = TvStyle.focusScale),
        ) {
            // A tiny home screen in the skin's colours: a card, a line of text and the accent
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(width = 44.dp, height = 6.dp).clip(CircleShape).background(c.text))
                Box(Modifier.size(width = 30.dp, height = 4.dp).clip(CircleShape).background(c.textMuted))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    val r = (6 * skin.style.cornerScale).dp
                    Box(Modifier.size(width = 34.dp, height = 22.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(r)).background(c.surface)
                        .border(1.5.dp, c.focus, androidx.compose.foundation.shape.RoundedCornerShape(r)))
                    Box(Modifier.size(width = 34.dp, height = 22.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(r)).background(c.surface))
                    Box(Modifier.size(width = 14.dp, height = 22.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(r)).background(c.accent))
                }
            }
        }
        Text((if (selected) "● " else "") + name, fontFamily = Inter, fontSize = 11.sp, maxLines = 1,
            color = if (selected) TvColors.AccentLight else TvColors.TextMedium, modifier = Modifier.padding(top = 6.dp))
    }
}
