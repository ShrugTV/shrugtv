package com.shrug.shrugtv.ui.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ui.tv.theme.BackgroundStyle
import com.shrug.shrugtv.ui.tv.theme.FocusStyle
import com.shrug.shrugtv.ui.tv.theme.FontChoice
import com.shrug.shrugtv.ui.tv.theme.ShrugTheme
import com.shrug.shrugtv.ui.tv.theme.Skins
import com.shrug.shrugtv.ui.tv.theme.ThemeSettings

/**
 * Design language of the new UI (variant A "Hero", chosen by Michael on 2026-10-20;
 * mockup https://claude.ai/artifact/Jd8wCZ34CzvHHe4wAzS7pY).
 *
 * Since  to #226 colours, corners, focus, glass, font and text size come from the theme in effect
 * ([ThemeSettings]: skin plus overrides), so a change in the settings shows at once in every screen. The default skin
 * keeps the original look. Font Inter (res/font, SIL OFL 1.1, docs/lizenzen/Inter-OFL.txt) in four weights.
 */
val LocalShrugTheme = staticCompositionLocalOf { Skins.SHRUGTV }

/** The bundled Inter; screens use [Inter], which follows the font of the theme. */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.INTER -> InterFamily
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONO -> FontFamily.Monospace
}

/** Font of the theme in effect; the name stays from the time it was always Inter. */
val Inter: FontFamily @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.style.font.family()

/** Colour tokens of the theme in effect; read them inside composables. */
object TvColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.surface
    val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.surfaceHigh
    val Text: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.text
    val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.textMuted
    val TextMedium: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.textMedium
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.accent
    val AccentLight: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.accentLight
    val AccentSoft: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.accentSoft
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.onAccent
    val Live: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.live
    val Focus: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.focus
    val Placeholder: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.placeholder
    val ChatSide: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.chatSide
    /** The "white" of the skin: focused fills, lines and glass (dark on light skins). */
    val Ink: Color @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.colors.ink

    /** Menus and dialogs: surface, nearly opaque (was 0xF017171C). */
    val Panel: Color @Composable @ReadOnlyComposable get() = Surface.copy(alpha = 0xF0 / 255f)

    /** A glass fill: ink at [alpha], scaled by the glass setting of the theme. */
    @Composable @ReadOnlyComposable
    fun glass(alpha: Float): Color = Ink.copy(alpha = (alpha * LocalShrugTheme.current.style.glass).coerceIn(0f, 1f))
}

/** Corners of the theme in effect. */
object TvShapes {
    /** A rounded rectangle with the designed radius [r], scaled by the corner setting. */
    @Composable @ReadOnlyComposable
    fun r(r: Dp): RoundedCornerShape = RoundedCornerShape(r * LocalShrugTheme.current.style.cornerScale)
}

/** Focus of the theme in effect. */
object TvStyle {
    val focusScale: Float @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.style.focusScale
    val focusStyle: FocusStyle @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.style.focusStyle
    val borderWidth: Dp @Composable @ReadOnlyComposable get() =
        if (LocalShrugTheme.current.style.focusStyle == FocusStyle.SCALE) 0.dp else LocalShrugTheme.current.style.borderWidth.dp
    /** The focus frame of rows and buttons. */
    val focusStroke: BorderStroke @Composable @ReadOnlyComposable get() = BorderStroke(borderWidth, TvColors.Focus)
    val animations: Boolean @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.style.animations
    val heroDim: Float @Composable @ReadOnlyComposable get() = LocalShrugTheme.current.style.heroDim
}

/** Page background as the theme wants it: plain, a soft gradient or a hint of the accent. */
@Composable
fun Modifier.themeBackground(): Modifier {
    val t = LocalShrugTheme.current
    val c = t.colors
    return when (t.style.background) {
        BackgroundStyle.SOLID -> background(c.background)
        BackgroundStyle.GRADIENT -> background(Brush.verticalGradient(listOf(c.surface, c.background)))
        BackgroundStyle.TINT -> background(c.background).background(
            Brush.radialGradient(listOf(c.accent.copy(alpha = 0.10f), Color.Transparent), radius = 1600f)
        )
    }
}

private fun ShrugTheme.colorScheme() = (if (colors.background.luminanceIsLight()) lightColorScheme(
    primary = colors.accent, onPrimary = colors.onAccent, background = colors.background, onBackground = colors.text,
    surface = colors.surface, onSurface = colors.text, surfaceVariant = colors.surfaceHigh, onSurfaceVariant = colors.textMuted,
    border = colors.focus,
) else darkColorScheme(
    primary = colors.accent, onPrimary = colors.onAccent, background = colors.background, onBackground = colors.text,
    surface = colors.surface, onSurface = colors.text, surfaceVariant = colors.surfaceHigh, onSurfaceVariant = colors.textMuted,
    border = colors.focus,
))

private fun Color.luminanceIsLight() = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.5f

private fun typography(family: FontFamily) = Typography().run {
    fun TextStyle.f() = copy(fontFamily = family)
    copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f(), titleMedium = titleMedium.f(), titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

@Composable
fun TvHomeTheme(content: @Composable () -> Unit) {
    val theme by ThemeSettings.theme.collectAsState()
    val scheme = remember(theme) { theme.colorScheme() }
    val type = remember(theme.style.font) { typography(theme.style.font.family()) }
    val density = LocalDensity.current
    // Text size of the theme on top of the system's font scale
    val scaled = remember(density, theme.style.textScale) { Density(density.density, density.fontScale * theme.style.textScale) }
    CompositionLocalProvider(LocalShrugTheme provides theme, LocalDensity provides scaled) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}

/**
 * The player sits on video: on a light skin it keeps the dark colours of the default skin, with the accent, corners,
 * font and focus of the theme in effect.
 */
@Composable
fun VideoTheme(content: @Composable () -> Unit) {
    val t = LocalShrugTheme.current
    if (!t.colors.background.luminanceIsLight()) return content()
    val dark = remember(t) {
        Skins.SHRUGTV.withAccent(t.colors.accent).copy(id = t.id, style = t.style)
    }
    CompositionLocalProvider(LocalShrugTheme provides dark) { content() }
}
