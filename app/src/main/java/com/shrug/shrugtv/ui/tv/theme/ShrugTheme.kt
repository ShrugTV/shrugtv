package com.shrug.shrugtv.ui.tv.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * Colour tokens of the UI. Every screen reads them through `TvColors`, so a skin or a single
 * override changes the whole app at once.
 */
@Immutable
data class ThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val text: Color,
    val textMuted: Color,
    val textMedium: Color,
    val accent: Color,
    /** Lighter (on light skins darker) accent for text and icons on surfaces. */
    val accentLight: Color,
    /** Muted accent for thin borders. */
    val accentSoft: Color,
    /** Text and icons on an accent fill. */
    val onAccent: Color,
    val live: Color,
    val focus: Color,
    /** Image placeholder while a picture loads. */
    val placeholder: Color,
    /** Chat background beside the video. */
    val chatSide: Color,
    /** The "white" of the skin: focused fills, glass and lines (dark on light skins). */
    val ink: Color = Color.White,
)

/** How focus shows on cards and rows. */
enum class FocusStyle { BORDER, GLOW, SCALE }

/** The page background behind home screen and settings. */
enum class BackgroundStyle { SOLID, GRADIENT, TINT }

/** Font of the whole UI; only fonts that ship with the app or the system, no download. */
enum class FontChoice { INTER, SANS, SERIF, MONO }

/** Shapes, focus, surfaces, font and motion. */
@Immutable
data class ThemeStyle(
    /** Factor on every corner radius: 0 square, 1 as designed, 2.4 very round. */
    val cornerScale: Float = 1f,
    val focusStyle: FocusStyle = FocusStyle.BORDER,
    val focusScale: Float = 1.08f,
    /** Width of the focus border in dp. */
    val borderWidth: Float = 2f,
    /** Factor on the semi-transparent glass fills: 0 none, 1 as designed. */
    val glass: Float = 1f,
    /** Factor on the darkening behind the hero text. */
    val heroDim: Float = 1f,
    val background: BackgroundStyle = BackgroundStyle.SOLID,
    val font: FontChoice = FontChoice.INTER,
    /** Factor on all text sizes. */
    val textScale: Float = 1f,
    val animations: Boolean = true,
)

/** A complete look: a skin, possibly with single values changed. */
@Immutable
data class ShrugTheme(val id: String, val colors: ThemeColors, val style: ThemeStyle = ThemeStyle()) {

    /** The same theme with another accent; the derived accent colours follow it. */
    fun withAccent(accent: Color): ShrugTheme = copy(colors = colors.copy(
        accent = accent,
        accentLight = lerp(accent, colors.text, 0.4f),
        accentSoft = lerp(accent, colors.textMuted, 0.3f),
        onAccent = onColor(accent),
    ))

    /** Another background; the chat beside the video follows it. */
    fun withBackground(c: Color) = copy(colors = colors.copy(background = c, chatSide = lerp(c, colors.surface, 0.25f)))

    /** Another surface; the raised surface and the image placeholder follow it. */
    fun withSurface(c: Color) = copy(colors = colors.copy(surface = c, surfaceHigh = lerp(c, colors.text, 0.04f),
        placeholder = lerp(c, colors.text, 0.07f)))

    fun withText(c: Color) = copy(colors = colors.copy(text = c, textMedium = lerp(colors.textMuted, c, 0.6f)))

    fun withTextMuted(c: Color) = copy(colors = colors.copy(textMuted = c, textMedium = lerp(c, colors.text, 0.6f)))

    companion object {
        /** White or dark text on [fill], whichever reads better. */
        fun onColor(fill: Color): Color {
            val dark = Color(0xFF111111)
            return if (contrast(Color.White, fill) >= contrast(dark, fill)) Color.White else dark
        }

        /** "#RRGGBB" or "#AARRGGBB" → colour, null if it is no colour. */
        fun parseHex(text: String?): Color? {
            val t = text?.trim()?.removePrefix("#") ?: return null
            if (t.length != 6 && t.length != 8) return null
            val v = t.toLongOrNull(16) ?: return null
            return Color(if (t.length == 6) 0xFF000000L or v else v)
        }

        fun toHex(c: Color): String = "#%06X".format(c.toArgb() and 0xFFFFFF)

        /** WCAG contrast ratio of two colours (1 to 21). */
        fun contrast(a: Color, b: Color): Float {
            val la = a.luminance() + 0.05f
            val lb = b.luminance() + 0.05f
            return maxOf(la, lb) / minOf(la, lb)
        }

        /** A skin from its few base colours; the rest is derived. */
        fun of(
            id: String, background: Long, surface: Long, text: Long, muted: Long, accent: Long, live: Long, focus: Long,
            ink: Color = Color.White, style: ThemeStyle = ThemeStyle(),
        ): ShrugTheme {
            val bg = Color(background)
            val sf = Color(surface)
            val tx = Color(text)
            val mu = Color(muted)
            val ac = Color(accent)
            return ShrugTheme(id, ThemeColors(
                background = bg, surface = sf, surfaceHigh = lerp(sf, tx, 0.04f), text = tx, textMuted = mu,
                textMedium = lerp(mu, tx, 0.6f), accent = ac, accentLight = lerp(ac, tx, 0.4f), accentSoft = lerp(ac, mu, 0.3f),
                onAccent = onColor(ac), live = Color(live), focus = Color(focus), placeholder = lerp(sf, tx, 0.07f),
                chatSide = lerp(bg, sf, 0.25f), ink = ink,
            ), style)
        }
    }
}

/** Ready-made skins. The first is the default and keeps the original look. */
object Skins {
    /** Today's look (variant A "Hero"), unchanged. */
    val SHRUGTV = ShrugTheme(
        id = "shrugtv",
        colors = ThemeColors(
            background = Color(0xFF0B0B0E),
            surface = Color(0xFF17171C),
            surfaceHigh = Color(0xFF1F1F25),
            text = Color(0xFFEFEFF1),
            textMuted = Color(0xFFADADB8),
            textMedium = Color(0xFFDEDEE3),
            accent = Color(0xFF9147FF),
            accentLight = Color(0xFFBF94FF),
            accentSoft = Color(0xFF9F7AEA),
            onAccent = Color.White,
            live = Color(0xFFE91916),
            focus = Color(0xFFFFFFFF),
            placeholder = Color(0xFF26262E),
            chatSide = Color(0xFF0E0E10),
        ),
    )

    val OLED = ShrugTheme.of("oled", 0xFF000000, 0xFF0E0E0E, 0xFFF2F2F2, 0xFFA6A6A6, 0xFF9147FF, 0xFFE91916, 0xFFFFFFFF,
        style = ThemeStyle(glass = 0.5f, heroDim = 1.1f))
    val TWITCH = ShrugTheme.of("twitch", 0xFF0E0E10, 0xFF18181B, 0xFFEFEFF1, 0xFFADADB8, 0xFF9147FF, 0xFFEB0400, 0xFFFFFFFF,
        style = ThemeStyle(cornerScale = 0.4f, focusScale = 1.04f))
    val NORD = ShrugTheme.of("nord", 0xFF2E3440, 0xFF3B4252, 0xFFECEFF4, 0xFFB8C0CE, 0xFF88C0D0, 0xFFBF616A, 0xFFECEFF4,
        style = ThemeStyle(cornerScale = 1.6f, font = FontChoice.SANS, background = BackgroundStyle.GRADIENT))
    val DRACULA = ShrugTheme.of("dracula", 0xFF21222C, 0xFF282A36, 0xFFF8F8F2, 0xFFB4B8D4, 0xFFFF79C6, 0xFFFF5555, 0xFF50FA7B,
        style = ThemeStyle(focusStyle = FocusStyle.GLOW))
    val SUNSET = ShrugTheme.of("sunset", 0xFF1A0F0C, 0xFF2A1712, 0xFFFFF1E6, 0xFFD1B2A2, 0xFFFF7A45, 0xFFFF3B30, 0xFFFFD1B3,
        style = ThemeStyle(cornerScale = 1.6f, background = BackgroundStyle.TINT))
    val OCEAN = ShrugTheme.of("ocean", 0xFF08161C, 0xFF0F2630, 0xFFE6F7FA, 0xFF9CC0C8, 0xFF1FC8B8, 0xFFFF5A5F, 0xFF7FF0E6,
        style = ThemeStyle(focusStyle = FocusStyle.GLOW, background = BackgroundStyle.GRADIENT))
    val RETRO = ShrugTheme.of("retro", 0xFF000000, 0xFF0A140A, 0xFF33FF66, 0xFF2BC452, 0xFF33FF66, 0xFFFF3333, 0xFF33FF66,
        ink = Color(0xFF33FF66),
        style = ThemeStyle(cornerScale = 0f, font = FontChoice.MONO, glass = 0.6f, focusScale = 1.03f))
    val LIGHT = ShrugTheme.of("light", 0xFFF2F2F5, 0xFFFFFFFF, 0xFF141418, 0xFF55555F, 0xFF7B3FE4, 0xFFD3120F, 0xFF141418,
        ink = Color(0xFF141418), style = ThemeStyle(glass = 0.7f, heroDim = 1f))
    val CONTRAST = ShrugTheme.of("contrast", 0xFF000000, 0xFF101010, 0xFFFFFFFF, 0xFFE0E0E0, 0xFFFFD400, 0xFFFF4040, 0xFFFFD400,
        style = ThemeStyle(borderWidth = 4f, focusScale = 1.1f, glass = 0.6f, textScale = 1.1f))

    val all: List<ShrugTheme> = listOf(SHRUGTV, OLED, TWITCH, NORD, DRACULA, SUNSET, OCEAN, RETRO, LIGHT, CONTRAST)

    fun byId(id: String?): ShrugTheme = all.firstOrNull { it.id == id } ?: SHRUGTV
}
