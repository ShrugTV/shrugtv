package com.shrug.shrugtv.ui.tv.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The theme in effect, read from the preferences and kept current: skin `pref_skin`
 * plus single overrides `pref_theme_<token>`, where the value [FROM_SKIN] (or nothing) takes the skin's value. A change
 * shows at once, `TvHomeTheme` collects [theme].
 */
object ThemeSettings {
    const val PREF_SKIN = "pref_skin"
    const val PREF_ACCENT = "pref_theme_accent"
    const val PREF_BACKGROUND = "pref_theme_background"
    const val PREF_SURFACE = "pref_theme_surface"
    const val PREF_TEXT = "pref_theme_text"
    const val PREF_TEXT_MUTED = "pref_theme_text_muted"
    const val PREF_LIVE = "pref_theme_live"
    const val PREF_FOCUS = "pref_theme_focus"
    const val PREF_CORNERS = "pref_theme_corners"
    const val PREF_FOCUS_STYLE = "pref_theme_focus_style"
    const val PREF_FOCUS_SCALE = "pref_theme_focus_scale"
    const val PREF_BORDER = "pref_theme_border"
    const val PREF_GLASS = "pref_theme_glass"
    const val PREF_HERO_DIM = "pref_theme_hero_dim"
    const val PREF_BACKGROUND_STYLE = "pref_theme_background_style"
    const val PREF_FONT = "pref_theme_font"
    const val PREF_TEXT_SCALE = "pref_theme_text_scale"
    const val PREF_ANIMATIONS = "pref_theme_animations"
    /** Own skins as JSON ([CustomSkins]). */
    const val PREF_CUSTOM = "pref_custom_skins"
    /** Action: every override back to the skin. */
    const val ACTION_RESET = "pref_theme_reset"

    /** List value for "take it from the skin". */
    const val FROM_SKIN = "skin"

    /** All override keys; picking a skin or the reset action clears them. */
    val OVERRIDES = listOf(
        PREF_ACCENT, PREF_BACKGROUND, PREF_SURFACE, PREF_TEXT, PREF_TEXT_MUTED, PREF_LIVE, PREF_FOCUS,
        PREF_CORNERS, PREF_FOCUS_STYLE, PREF_FOCUS_SCALE, PREF_BORDER, PREF_GLASS, PREF_HERO_DIM,
        PREF_BACKGROUND_STYLE, PREF_FONT, PREF_TEXT_SCALE, PREF_ANIMATIONS,
    )

    private const val OLD_ACCENT = "prefs_accent_color"
    private const val OLD_THEME = "prefs_theme_color"

    /** Accents of the old list `prefs_accent_color` (index → colour); 2 (purple) was the default and stays the skin's. */
    private val OLD_ACCENTS = mapOf(0 to "#DB002C", 1 to "#005FDB", 3 to "#0BDB00", 4 to "#B3005F", 5 to "#DB5800", 6 to "#FECF01")

    private val _theme = MutableStateFlow(Skins.SHRUGTV)
    val theme: StateFlow<ShrugTheme> = _theme.asStateFlow()

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    @JvmStatic
    fun sync(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        migrate(prefs)
        _theme.value = resolve(prefs::getString)
        if (listener == null) {
            val l = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                if (key == null || key == PREF_SKIN || key == PREF_CUSTOM || key.startsWith("pref_theme_")) _theme.value = resolve(sp::getString)
            }
            prefs.registerOnSharedPreferenceChangeListener(l)
            listener = l
        }
    }

    /**
     * Skin plus overrides; [read] is `SharedPreferences.getString`, so tests need no Android. An own skin
     * is its base skin with its values; own changes still go on top.
     */
    fun resolve(read: (String, String?) -> String?): ShrugTheme {
        val skinId = read(PREF_SKIN, null)
        if (CustomSkins.isCustom(skinId)) {
            val custom = CustomSkins.parse(read(PREF_CUSTOM, null)).firstOrNull { it.id == skinId }
            if (custom != null) {
                val layered: (String, String?) -> String? = { k, d ->
                    if (k == PREF_SKIN) custom.base
                    else read(k, null)?.takeIf { it.isNotBlank() && it != FROM_SKIN } ?: custom.values[k] ?: d
                }
                return resolvePlain(layered).copy(id = custom.id)
            }
        }
        return resolvePlain(read)
    }

    private fun resolvePlain(read: (String, String?) -> String?): ShrugTheme {
        fun own(key: String): String? = read(key, null)?.takeIf { it.isNotBlank() && it != FROM_SKIN }
        fun color(key: String) = ShrugTheme.parseHex(own(key))
        fun number(key: String, range: ClosedFloatingPointRange<Float>) = own(key)?.toFloatOrNull()?.takeIf { it in range }

        var t = Skins.byId(read(PREF_SKIN, null))
        color(PREF_BACKGROUND)?.let { t = t.withBackground(it) }
        color(PREF_SURFACE)?.let { t = t.withSurface(it) }
        color(PREF_TEXT)?.let { t = t.withText(it) }
        color(PREF_TEXT_MUTED)?.let { t = t.withTextMuted(it) }
        color(PREF_ACCENT)?.let { t = t.withAccent(it) }
        color(PREF_LIVE)?.let { t = t.copy(colors = t.colors.copy(live = it)) }
        color(PREF_FOCUS)?.let { t = t.copy(colors = t.colors.copy(focus = it)) }

        val s = t.style
        t = t.copy(style = s.copy(
            cornerScale = number(PREF_CORNERS, 0f..3f) ?: s.cornerScale,
            focusStyle = own(PREF_FOCUS_STYLE)?.let { v -> FocusStyle.entries.firstOrNull { it.name.equals(v, true) } } ?: s.focusStyle,
            focusScale = number(PREF_FOCUS_SCALE, 1f..1.2f) ?: s.focusScale,
            borderWidth = number(PREF_BORDER, 0f..6f) ?: s.borderWidth,
            glass = number(PREF_GLASS, 0f..2f) ?: s.glass,
            heroDim = number(PREF_HERO_DIM, 0.4f..1.2f) ?: s.heroDim,
            background = own(PREF_BACKGROUND_STYLE)?.let { v -> BackgroundStyle.entries.firstOrNull { it.name.equals(v, true) } } ?: s.background,
            font = own(PREF_FONT)?.let { v -> FontChoice.entries.firstOrNull { it.name.equals(v, true) } } ?: s.font,
            textScale = number(PREF_TEXT_SCALE, 0.8f..1.4f) ?: s.textScale,
            animations = when (own(PREF_ANIMATIONS)) { "on" -> true; "off" -> false; else -> s.animations },
        ))
        return t
    }

    /** Number of overrides set (shown on the reset row). */
    fun overrideCount(read: (String, String?) -> String?): Int =
        OVERRIDES.count { read(it, null).let { v -> !v.isNullOrBlank() && v != FROM_SKIN } }

    /** Every override back to the skin. */
    @JvmStatic
    fun resetOverrides(prefs: SharedPreferences) {
        prefs.edit().apply { OVERRIDES.forEach { remove(it) } }.apply()
    }

    /** A new skin starts clean: its own colours and shapes, no leftovers of the previous one. */
    @JvmStatic
    fun pickSkin(prefs: SharedPreferences, id: String) {
        prefs.edit().apply {
            OVERRIDES.forEach { remove(it) }
            putString(PREF_SKIN, if (CustomSkins.isCustom(id)) id else Skins.byId(id).id)
        }.apply()
    }

    /**
     * Once: the old accent list becomes an accent override, the old theme list (it never reached the new UI) goes.
     * Returns the override written, if any.
     */
    fun migrate(prefs: SharedPreferences): String? {
        if (!prefs.contains(OLD_ACCENT) && !prefs.contains(OLD_THEME)) return null
        val hex = migratedAccent(prefs.getString(OLD_ACCENT, null), prefs.contains(PREF_ACCENT))
        prefs.edit().apply {
            if (hex != null) putString(PREF_ACCENT, hex)
            remove(OLD_ACCENT)
            remove(OLD_THEME)
        }.apply()
        return hex
    }

    /** The override for an old accent index, null when it was the default or a new value is already set. */
    fun migratedAccent(oldIndex: String?, hasNewValue: Boolean): String? =
        if (hasNewValue) null else OLD_ACCENTS[oldIndex?.toIntOrNull()]
}
