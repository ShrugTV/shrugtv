package com.shrug.shrugtv.ui.tv.components

import androidx.compose.foundation.BorderStroke
import com.shrug.shrugtv.ui.tv.TvStyle
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.home.ImageCache
import com.shrug.shrugtv.ui.tv.home.NetImage
import androidx.compose.ui.layout.layout

/**
 * Shared building blocks of all Compose screens: cards for streams, VODs, channels
 * and games, LIVE badge, button colours, number format. Extracted from the home screen so that search,
 * category and channel page look the same. Sizes in dp are the mockup pixels halved.
 */

@Composable
fun whiteButton() = ButtonDefaults.colors(
    containerColor = TvColors.Ink, contentColor = TvColors.Background,
    focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background,
)

@Composable
fun glassButton() = ButtonDefaults.colors(
    containerColor = TvColors.glass(0.14f), contentColor = TvColors.Text,
    focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background,
)

@Composable
fun LiveBadge() {
    Text(
        stringResource(R.string.home_live_badge), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White,
        modifier = Modifier.clip(TvShapes.r(4.dp)).background(TvColors.Live).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Focus style "glow" of the theme: a soft light in the focus colour around the card. */
@Composable
fun focusGlow() = if (com.shrug.shrugtv.ui.tv.TvStyle.focusStyle == com.shrug.shrugtv.ui.tv.theme.FocusStyle.GLOW) {
    CardDefaults.glow(focusedGlow = androidx.tv.material3.Glow(TvColors.Focus.copy(alpha = 0.55f), 14.dp))
} else CardDefaults.glow()

@Composable
fun focusFrame() = CardDefaults.border(
    focusedBorder = Border(TvStyle.focusStroke, shape = TvShapes.r(10.dp)),
)

/**
 * Cards without a surface of their own: the image (or its placeholder in NetImage) covers the card
 * completely. The surface underneath was one more layer the weak Fire TV GPU painted every frame
 * (overdraw view).
 */
@Composable
fun withoutSurface() = CardDefaults.colors(
    containerColor = Color.Transparent, focusedContainerColor = Color.Transparent, pressedContainerColor = Color.Transparent,
)

/** The matching card for the content type. [focus] reports focus (home screen: hero), [longPress] = long press. */
@Composable
fun ContentCard(m: Movie, click: () -> Unit, focus: () -> Unit = {}, longPress: (() -> Unit)? = null, width: Dp = 160.dp) {
    when (m.itemType) {
        Enums.ContentType.STREAM -> StreamCard(m, click, focus, longPress, width)
        Enums.ContentType.CHANNEL -> ChannelCard(m, click, longPress)
        Enums.ContentType.GAME -> GameCard(m, click, longPress)
        else -> VodCard(m, click, longPress, width, focus)
    }
}

/**
 * Preview image for cards: deliberately the Twitch size 800x450, not smaller. From 1.3.0 to 1.6.0 this
 * was 440x248; Twitch generates sizes on request and does not deliver every one:
 * live_user_mydealz returned a permanent 302 to the grey 404_preview image for 440x248/320x180/640x360,
 * 200 for 800x450 (curl 2026-10-28). Coil decodes only at card size via
 * breite/hoehe in NetImage; this saves memory, not data volume.
 */
fun cardImageUrlFor(url: String?): String? = url

/**
 * Room on the left for the first card of a row: focused cards grow by 8 % (a good
 * 6 dp at 160 dp width) plus border; a LazyRow clips at its edge, but the first card sat flush against
 * it. [focusEdgeLeft] widens the row by [FocusBorder] to the left without moving anything in the
 * surrounding layout; it goes with contentPadding(start = FocusBorder) so the first card stays aligned
 * with the heading.
 */
/**
 * Arrow right at the end of a card row stays put. Without this, the focus search
 * left the row and jumped up into the hero buttons ("Go to channel", "Multiview"); with a single card
 * that happened on the first press (Fire TV and emulator, 2026-10-29). Apply to the LazyRow, after
 * focusRestorer so the row is a focus group.
 */
fun Modifier.rowKeepsRight(): Modifier = focusProperties {
    onExit = { if (requestedFocusDirection == androidx.compose.ui.focus.FocusDirection.Right) cancelFocusChange() }
}

val FocusBorder = 12.dp

fun Modifier.focusEdgeLeft(border: Dp = FocusBorder): Modifier = layout { measurable, c ->
    val px = border.roundToPx()
    val next = if (c.hasBoundedWidth) c.copy(minWidth = c.minWidth + px, maxWidth = c.maxWidth + px) else c
    val p = measurable.measure(next)
    layout((p.width - px).coerceAtLeast(0), p.height) { p.place(-px, 0) }
}

@Composable
fun WideCard(
    m: Movie, click: () -> Unit, focus: () -> Unit = {}, longPress: (() -> Unit)? = null, width: Dp = 160.dp,
    corner: @Composable () -> Unit, bottom: String, row2: String, bottom2: String = "", progress: Int = 0,
) {
    val clampedProgress = progress.coerceIn(0, 100)
    Column(Modifier.width(width)) {
        Card(
            onClick = click,
            onLongClick = longPress,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).onFocusChanged { if (it.isFocused) focus() },
            shape = CardDefaults.shape(TvShapes.r(10.dp)),
            scale = CardDefaults.scale(focusedScale = TvStyle.focusScale),
            border = focusFrame(),
            colors = withoutSurface(),
            glow = focusGlow(),
        ) {
            Box(Modifier.fillMaxSize()) {
                NetImage(cardImageUrlFor(m.cardImageUrl), Modifier.fillMaxSize(), width = 440, height = 248)
                Box(Modifier.padding(6.dp).align(Alignment.TopStart)) { corner() }
                if (bottom.isNotEmpty() || bottom2.isNotEmpty()) {
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(
                            start = 6.dp, end = 6.dp, top = 6.dp,
                            bottom = if (clampedProgress > 0) 8.dp else 6.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (t in listOf(bottom, bottom2)) if (t.isNotEmpty()) Text(
                            t, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                            modifier = Modifier.clip(TvShapes.r(3.dp))
                                .background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
                if (clampedProgress > 0) {
                    Box(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp)
                            .background(TvColors.glass(0.25f))
                    ) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(clampedProgress / 100f)
                                .background(TvColors.Accent)
                        )
                    }
                }
            }
        }
        Row(Modifier.padding(top = 7.dp, start = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            NetImage(m.avatarUrl, Modifier.size(20.dp).clip(CircleShape), width = 80, height = 80)
            Column {
                Text(m.displayName ?: "", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TvColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row2, fontSize = 8.sp, color = TvColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun StreamCard(m: Movie, click: () -> Unit, focus: () -> Unit = {}, longPress: (() -> Unit)? = null, width: Dp = 160.dp) {
    // Uptime next to the viewers; the images' minute tick redraws it too
    ImageCache.liveTick.longValue
    WideCard(m, click, focus, longPress, width, corner = { LiveBadge() }, bottom = viewers(m), row2 = m.gameName ?: "",
        bottom2 = durationShort(m))
}

@Composable
fun VodCard(m: Movie, click: () -> Unit, longPress: (() -> Unit)? = null, width: Dp = 160.dp, focus: () -> Unit = {}) {
    // A clip that was opened before is drawn dimmed, if the setting asks for it
    androidx.compose.foundation.layout.Box(Modifier.alpha(if (m.clipSeen) 0.4f else 1f)) {
        WideCard(m, click, focus = focus, longPress = longPress, width = width, corner = {}, bottom = m.durationText ?: "", row2 = m.title ?: "",
            bottom2 = if (m.isClip && m.viewCount > 0) number(m.viewCount) else "", progress = m.resumeProgressPercent)
    }
}

@Composable
fun ChannelCard(m: Movie, click: () -> Unit, longPress: (() -> Unit)? = null) {
    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick = click,
            onLongClick = longPress,
            modifier = Modifier.size(96.dp),
            shape = CardDefaults.shape(CircleShape),
            scale = CardDefaults.scale(focusedScale = TvStyle.focusScale),
            border = CardDefaults.border(focusedBorder = Border(TvStyle.focusStroke, shape = CircleShape)),
            colors = withoutSurface(),
            glow = focusGlow(),
        ) {
            NetImage(m.avatarUrl ?: m.cardImageUrl, Modifier.fillMaxSize(), width = 300, height = 300)
        }
        Text(m.displayName ?: "", fontSize = 10.sp, color = TvColors.TextMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun GameCard(m: Movie, click: () -> Unit, longPress: (() -> Unit)? = null) {
    Card(
        onClick = click,
        onLongClick = longPress,
        modifier = Modifier.width(96.dp).aspectRatio(272f / 380f),
        shape = CardDefaults.shape(TvShapes.r(10.dp)),
        scale = CardDefaults.scale(focusedScale = TvStyle.focusScale),
        border = focusFrame(),
        colors = withoutSurface(),
        glow = focusGlow(),
    ) {
        NetImage(m.boxArtUrl ?: m.cardImageUrl, Modifier.fillMaxSize(), width = 272, height = 380)
    }
}

/** Viewer count grouped by the current locale (32,750 / 32.750). */
fun viewers(m: Movie): String = number(m.viewerCount)

/**
 * Integer grouped by the current locale. Locale.getDefault() follows the app language, because
 * AppLanguage sets it together with the resources; before, always German.
 */
fun number(n: Int): String = java.text.NumberFormat.getIntegerInstance(java.util.Locale.getDefault()).format(n)

/** Uptime without the clock emoji that Movie.getUptimeText prepends. */
fun duration(m: Movie): String = (m.uptimeText ?: "").filter { it.isDigit() || it == ':' }

/** Uptime for cards: "3:51 h" instead of "03:51"; empty without start time. The "h" is the same in English and German. */
fun durationShort(m: Movie): String = durationShort(duration(m))

fun durationShort(hm: String): String = if (hm.isEmpty()) "" else hm.trimStart('0').let { if (it.startsWith(":")) "0$it" else it } + " h"
