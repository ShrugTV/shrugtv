package com.shrug.shrugtv.ui.tv.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.chat.ChatHighlights
import com.shrug.shrugtv.chat.ChatPrefs
import com.shrug.shrugtv.chat.ui.ChatColors
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvShapes
import com.shrug.shrugtv.ui.tv.TvStyle
import com.shrug.shrugtv.ui.tv.theme.FocusStyle
import com.shrug.shrugtv.ui.tv.themeBackground

/*
 * The preview beside the settings: a sample stream picture drawn in code (no image
 * asset, no real streamer), a home screen and a player with a chat, all in the theme in effect and reading the
 * chat and player preferences, so every change shows at once. Which scene shows follows the focused setting.
 */

/** What the preview shows. */
enum class PreviewScene { BOTH, HOME, PLAYER, CHAT }

/** The settings access of the screen, so the preview reads the same values the list shows. */
val LocalSettingsAccess = compositionLocalOf<SettingsAccess?> { null }

/** Every row reports its key when it takes focus; the screen picks the preview scene from it. */
val LocalFocusReport = compositionLocalOf<(String?) -> Unit> { {} }

/** The scene that shows a setting best; [page] is the page's own scene when the key says nothing. */
fun sceneFor(key: String?, page: PreviewScene): PreviewScene = when {
    key == null -> page
    key.startsWith("pref_chat") || key.startsWith("prefs_chat") ||
        key in setOf("pref_show_chat", "pref_show_sub_messages", "pref_show_prime", "pref_show_giftedsubs", "pref_show_timestamp", "pref_join_chat") ||
        key.startsWith("pref_filter") -> PreviewScene.CHAT
    key.startsWith("pref_player_") || key.startsWith("pref_clock_") || key == "pref_low_latency" -> PreviewScene.PLAYER
    key == "prefs_video_card_size" || key.startsWith("pref_splash") -> PreviewScene.HOME
    else -> page
}

/** The preview column: one scene big, or home and player stacked. */
@Composable
fun SettingsPreview(scene: PreviewScene, title: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TvColors.AccentLight,
            modifier = Modifier.padding(start = 4.dp))
        when (scene) {
            PreviewScene.BOTH -> {
                HomePanel(Modifier.fillMaxWidth().height(196.dp))
                PlayerPanel(withChat = true, Modifier.fillMaxWidth().height(196.dp))
            }
            PreviewScene.HOME -> HomePanel(Modifier.fillMaxWidth().height(300.dp), rows = 2)
            PreviewScene.PLAYER -> PlayerPanel(withChat = false, Modifier.fillMaxWidth().height(300.dp))
            PreviewScene.CHAT -> PlayerPanel(withChat = true, Modifier.fillMaxWidth().height(380.dp))
        }
        Text(stringResource(R.string.theme_preview_line), fontFamily = Inter, fontSize = 9.sp, lineHeight = 12.sp,
            color = TvColors.TextMuted, modifier = Modifier.padding(start = 4.dp))
    }
}

// ------------------------------------------------------------------ the sample picture

/**
 * A stream frame drawn in code: sky, sun, two hills and a small figure. [variant] shifts the colours so the cards
 * of the home scene do not look the same.
 */
@Composable
fun SampleFrame(modifier: Modifier, variant: Int = 0) {
    val skies = listOf(
        Color(0xFF2B1B5A) to Color(0xFFFF8A5B), Color(0xFF0B3A5B) to Color(0xFF5BD0C0),
        Color(0xFF3B1F3B) to Color(0xFFE8618C), Color(0xFF1B3B2B) to Color(0xFFD9E36B),
    )
    val (top, bottom) = skies[variant.mod(skies.size)]
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        drawCircle(Color(0xFFFFE9A8), radius = h * 0.13f, center = Offset(w * 0.72f, h * 0.38f))
        val far = Path().apply {
            moveTo(0f, h * 0.72f); lineTo(w * 0.22f, h * 0.5f); lineTo(w * 0.45f, h * 0.68f); lineTo(w * 0.66f, h * 0.46f)
            lineTo(w, h * 0.7f); lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(far, top.copy(alpha = 0.75f).compositeOver(Color.Black, 0.35f))
        val near = Path().apply {
            moveTo(0f, h * 0.84f); lineTo(w * 0.3f, h * 0.7f); lineTo(w * 0.62f, h * 0.86f); lineTo(w, h * 0.74f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(near, Color(0xFF14121C))
        // The figure: head and body on the near hill
        val cx = w * 0.3f
        drawCircle(Color(0xFFFFD9B0), radius = h * 0.045f, center = Offset(cx, h * 0.6f))
        drawRect(Color(0xFF7C5CFF), topLeft = Offset(cx - h * 0.04f, h * 0.645f), size = androidx.compose.ui.geometry.Size(h * 0.08f, h * 0.11f))
    }
}

private fun Color.compositeOver(background: Color, amount: Float): Color =
    Color(red * (1 - amount) + background.red * amount, green * (1 - amount) + background.green * amount,
        blue * (1 - amount) + background.blue * amount, 1f)

// ------------------------------------------------------------------ home

@Composable
private fun HomePanel(modifier: Modifier, rows: Int = 1) {
    val z = LocalSettingsAccess.current
    val small = z?.string("prefs_video_card_size", "normal") == "small"
    val cardW = if (small) 62.dp else 78.dp
    val cardH = if (small) 35.dp else 44.dp
    Box(modifier.clip(TvShapes.r(10.dp)).themeBackground()) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Hero: the sample picture, dimmed towards the left by the hero-dim setting, with channel and title
            Box(Modifier.fillMaxWidth().height(70.dp).clip(TvShapes.r(8.dp))) {
                SampleFrame(Modifier.fillMaxSize(), 0)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                    listOf(TvColors.Background.copy(alpha = (0.92f * TvStyle.heroDim).coerceIn(0f, 1f)), Color.Transparent))))
                Column(Modifier.padding(8.dp).align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LiveBadge()
                    Text(stringResource(R.string.preview_channel), fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
                    Text(stringResource(R.string.preview_title), fontFamily = Inter, fontSize = 8.sp, color = TvColors.TextMedium, maxLines = 1)
                }
            }
            for (r in 0 until rows) {
                Text(stringResource(R.string.preview_row), fontFamily = Inter, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until 4) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            PreviewCard(cardW, cardH, i + r * 2, focused = r == 0 && i == 1)
                            Text(listOf("luna", "mrfox", "pixelpete", "quietone")[(i + r * 2) % 4], fontFamily = Inter, fontSize = 7.sp,
                                color = if (r == 0 && i == 1) TvColors.Text else TvColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(cardW))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Text(stringResource(R.string.home_live_badge), fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White,
        modifier = Modifier.clip(TvShapes.r(3.dp)).background(TvColors.Live).padding(horizontal = 3.dp, vertical = 1.dp))
}

@Composable
private fun PreviewCard(w: Dp, h: Dp, variant: Int, focused: Boolean) {
    val shape = TvShapes.r(8.dp)
    val glow = focused && TvStyle.focusStyle == FocusStyle.GLOW
    Box(
        Modifier.scale(if (focused) TvStyle.focusScale else 1f)
            .then(if (glow) Modifier.background(Brush.radialGradient(listOf(TvColors.Focus.copy(alpha = 0.45f), Color.Transparent)), shape) else Modifier)
            .padding(if (glow) 2.dp else 0.dp)
    ) {
        Box(
            Modifier.size(w, h).clip(shape).background(TvColors.Placeholder)
                .then(if (focused && TvStyle.borderWidth > 0.dp) Modifier.border(TvStyle.focusStroke, shape) else Modifier)
        ) {
            SampleFrame(Modifier.fillMaxSize(), variant + 1)
            Box(Modifier.padding(3.dp)) { LiveBadge() }
            Text("${(variant + 1) * 1130}", fontFamily = Inter, fontSize = 6.sp, color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).clip(TvShapes.r(3.dp))
                    .background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 3.dp))
        }
    }
}

// ------------------------------------------------------------------ player

@Composable
private fun PlayerPanel(withChat: Boolean, modifier: Modifier) {
    val z = LocalSettingsAccess.current
    val layout = z?.string("pref_chat_layout", "side_by_side") ?: "side_by_side"
    val chatOn = withChat && (z?.bool("pref_join_chat", true) ?: true) && (z?.bool("pref_show_chat", true) ?: true)
    val side = layout == "side_by_side"
    Box(modifier.clip(TvShapes.r(10.dp)).background(Color.Black)) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                SampleFrame(Modifier.fillMaxSize(), 0)
                PlayerInfo(z)
                PlayerBar(z, Modifier.align(Alignment.BottomCenter))
                if (chatOn && !side) {
                    ChatMock(z, overlay = layout == "overlay", Modifier.align(Alignment.CenterEnd).width(130.dp).fillMaxHeight(0.78f).padding(end = 6.dp))
                }
            }
            if (chatOn && side) ChatMock(z, overlay = false, Modifier.width(130.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun PlayerInfo(z: SettingsAccess?) {
    if (z?.bool("pref_player_info_corner", true) == false) return
    Row(Modifier.padding(8.dp).clip(TvShapes.r(10.dp)).background(TvColors.glass(0.16f).compositeOver(Color.Black, 0.55f)).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(TvColors.Accent))
        Column {
            Text(stringResource(R.string.preview_channel), fontFamily = Inter, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
            Text(stringResource(R.string.preview_viewers), fontFamily = Inter, fontSize = 6.sp, color = TvColors.TextMedium)
        }
        LiveBadge()
    }
}

@Composable
private fun PlayerBar(z: SettingsAccess?, modifier: Modifier) {
    Column(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
        .padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(TvColors.glass(0.25f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(0.62f).background(TvColors.Accent))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(width = 34.dp, height = 14.dp).clip(TvShapes.r(10.dp)).background(TvColors.glass(0.14f)), contentAlignment = Alignment.Center) {
                Text("720p60", fontFamily = Inter, fontSize = 6.sp, color = TvColors.Text)
            }
            Box(Modifier.size(14.dp).clip(CircleShape).background(TvColors.Ink), contentAlignment = Alignment.Center) {
                Text("▶", fontSize = 6.sp, color = TvColors.Background)
            }
            Box(Modifier.size(14.dp).clip(CircleShape).background(TvColors.glass(0.14f)))
            Box(Modifier.size(14.dp).clip(CircleShape).background(TvColors.glass(0.14f)))
            Box(Modifier.weight(1f))
            if (z?.bool("pref_clock_showclock", true) != false) {
                val twelve = z?.string("pref_clock_time_format", "H:mm")?.contains("a") == true
                Text(if (twelve) "8:15 PM" else "20:15", fontFamily = Inter, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = TvColors.Text)
            }
        }
    }
}

// ------------------------------------------------------------------ chat

private data class SampleMessage(val user: String, val color: Int, val role: Int, val text: Int, val emote: Boolean = false)

/** role: 0 none, 1 mod, 2 VIP, 3 broadcaster, 4 subscriber */
private val sampleMessages = listOf(
    SampleMessage("luna", 0xFF1E90FF.toInt(), 4, R.string.preview_msg_1),
    SampleMessage("mrfox", 0xFFFF7A45.toInt(), 0, R.string.preview_msg_2, emote = true),
    SampleMessage("shrugmod", 0xFF2E8B57.toInt(), 1, R.string.preview_msg_3),
    SampleMessage("pixelpete", 0xFF00007F.toInt(), 0, R.string.preview_msg_4),
    SampleMessage("quietone", 0xFFB3005F.toInt(), 2, R.string.preview_msg_5, emote = true),
    SampleMessage("streamer", 0xFFFF4040.toInt(), 3, R.string.preview_msg_6),
)

/** The chat in the look the chat settings describe; a simplified drawing, the numbers come from the real preferences. */
@Composable
private fun ChatMock(z: SettingsAccess?, overlay: Boolean, modifier: Modifier) {
    val fontSp = ChatPrefs.textSizeSp(z?.string("prefs_chat_font_size", "2")) * 0.5f
    val opacity = ((z?.string("pref_chat_background_opacity", "6")?.toIntOrNull() ?: 6).coerceIn(0, 9)) / 10f
    val alternate = z?.bool("pref_chat_alternate", false) == true
    val separators = z?.bool("pref_chat_separators", false) == true
    val compact = z?.bool("pref_chat_compact", false) == true
    val badges = z?.bool("pref_chat_show_badges", true) != false
    val timestamps = z?.bool("pref_show_timestamp", false) == true
    val nameMode = z?.string("pref_chat_name_colors", "readable") ?: "readable"
    val deleted = z?.string("pref_chat_deleted_style", "strike") ?: "strike"
    val emoteH = if ((z?.string("pref_chat_emote_size", "1") ?: "1") == "0") fontSp.dp * 1.1f else fontSp.dp * 1.6f
    val hlColor = Color(ChatHighlights.colorOf(z?.string("pref_chat_highlight_color", null)))
    val panel = if (overlay) TvColors.ChatSide.copy(alpha = opacity) else TvColors.ChatSide
    val panelArgb = TvColors.ChatSide.toArgb()
    Column(modifier.clip(TvShapes.r(6.dp)).background(panel).padding(vertical = 3.dp), verticalArrangement = Arrangement.Bottom) {
        sampleMessages.forEachIndexed { i, m ->
            val highlighted = when (m.role) {
                1 -> z?.bool("pref_chat_highlight_mods", false) == true
                2 -> z?.bool("pref_chat_highlight_vips", false) == true
                3 -> z?.bool("pref_chat_highlight_broadcaster", false) == true
                else -> false
            }
            // The fourth message stands for a deleted one
            val isDeleted = i == 3
            if (isDeleted && deleted == "hide") return@forEachIndexed
            val name = when (nameMode) {
                "accent" -> TvColors.Accent
                "readable" -> Color(ChatColors.readableOn(m.color, panelArgb))
                else -> Color(m.color)
            }
            Column(Modifier.fillMaxWidth()
                .background(when {
                    highlighted -> hlColor.copy(alpha = 0.28f)
                    alternate && i % 2 == 1 -> TvColors.glass(0.07f)
                    else -> Color.Transparent
                })
                .padding(horizontal = 4.dp, vertical = if (compact) 1.dp else 3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (timestamps) Text("20:1${i}", fontFamily = Inter, fontSize = (fontSp * 0.85f).sp, color = TvColors.TextMuted)
                    if (badges && m.role != 0) {
                        Box(Modifier.size((fontSp * 0.9f).dp).clip(TvShapes.r(2.dp)).background(when (m.role) {
                            1 -> Color(0xFF00AD03); 2 -> Color(0xFFE005B9); 3 -> Color(0xFFE91916); else -> TvColors.Accent
                        }))
                    }
                    Text(m.user + ":", fontFamily = Inter, fontSize = fontSp.sp, fontWeight = FontWeight.Bold, color = name, maxLines = 1)
                    if (m.emote) Box(Modifier.height(emoteH).width(emoteH).clip(TvShapes.r(3.dp)).background(TvColors.Accent.copy(alpha = 0.8f)))
                }
                Text(stringResource(m.text), fontFamily = Inter, fontSize = fontSp.sp, lineHeight = (fontSp * 1.25f).sp,
                    color = if (isDeleted && deleted == "grey") TvColors.TextMuted else TvColors.Text,
                    textDecoration = if (isDeleted && deleted == "strike") TextDecoration.LineThrough else null,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (separators && i < sampleMessages.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(TvColors.glass(0.18f)))
        }
    }
}
