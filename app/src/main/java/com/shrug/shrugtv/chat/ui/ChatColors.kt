package com.shrug.shrugtv.chat.ui

/**
 * Readable user colours on the dark chat panel (docs/chat_plan.md phase 3). Pure ARGB math without
 * android.graphics.Color, so it runs in JVM tests.
 */
object ChatColors {

    /** Minimum HSL lightness on the dark panel; Twitch's #0000FF has 0.5 and is hard to read. */
    private const val MIN_LIGHTNESS = 0.62f

    /** WCAG AA for normal text. */
    const val MIN_CONTRAST = 4.5f

    /** Fallback colours when a user has none: picked by name, so the same user keeps one colour. */
    private val FALLBACK = intArrayOf(
        0xFFFF7F50.toInt(), 0xFF9ACD32.toInt(), 0xFFFF6347.toInt(), 0xFF3CB371.toInt(), 0xFFDAA520.toInt(),
        0xFFD2691E.toInt(), 0xFF5F9EA0.toInt(), 0xFF1E90FF.toInt(), 0xFFFF69B4.toInt(), 0xFF9F7AEA.toInt(),
        0xFF00FF7F.toInt(), 0xFF40C4FF.toInt(), 0xFFFFB74D.toInt(), 0xFFAED581.toInt(), 0xFFF06292.toInt()
    )

    @JvmStatic
    fun fallback(name: String): Int = FALLBACK[Math.floorMod(name.lowercase().hashCode(), FALLBACK.size)]

    /** Parses "#RRGGBB"; null for empty, black, white or invalid. */
    @JvmStatic
    fun parse(hex: String?): Int? {
        val h = hex?.trim()?.removePrefix("#") ?: return null
        if (h.length != 6) return null
        val rgb = h.toIntOrNull(16) ?: return null
        if (rgb == 0x000000 || rgb == 0xFFFFFF) return null
        return (0xFF shl 24) or rgb
    }

    /** [argb] with its HSL lightness raised to at least [MIN_LIGHTNESS]; hue and saturation stay. */
    @JvmStatic
    fun readable(argb: Int): Int {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        if (l >= MIN_LIGHTNESS) return argb
        val d = max - min
        val s = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2f * l - 1f))
        val h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d).mod(6f))
            max == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        return hsl(h, s, MIN_LIGHTNESS)
    }

    /** WCAG relative luminance of an ARGB colour (0 to 1). */
    @JvmStatic
    fun luminance(argb: Int): Float {
        fun lin(c: Int): Float {
            val v = c / 255f
            return if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }
        return 0.2126f * lin((argb shr 16) and 0xFF) + 0.7152f * lin((argb shr 8) and 0xFF) + 0.0722f * lin(argb and 0xFF)
    }

    /** WCAG contrast ratio of two colours (1 to 21). */
    @JvmStatic
    fun contrast(a: Int, b: Int): Float {
        val la = luminance(a) + 0.05f
        val lb = luminance(b) + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }

    /**
     * [argb] changed as little as needed (hue and saturation stay, only the HSL lightness moves) until it reaches
     * [minContrast] against [background]: lighter on a dark background, darker on a light one
     *. A colour that is readable already comes back unchanged.
     */
    @JvmStatic
    fun readableOn(argb: Int, background: Int, minContrast: Float = MIN_CONTRAST): Int {
        if (contrast(argb, background) >= minContrast) return argb
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        val d = max - min
        val s = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2f * l - 1f))
        val h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d).mod(6f))
            max == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        val lighten = luminance(background) < 0.5f
        // The end of the range is always readable (white or black); search the lightness closest to the original
        var good = if (lighten) 1f else 0f
        var bad = l
        repeat(14) {
            val mid = (good + bad) / 2f
            if (contrast(hsl(h, s, mid), background) >= minContrast) good = mid else bad = mid
        }
        return hsl(h, s, good)
    }

    private fun hsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f).mod(2f) - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}

/** `pref_chat_name_colors`: how user names are coloured in the chat. */
enum class NameColorMode(val value: String) {
    /** The colour the user picked, even if it is hard to read. */
    RAW("raw"),
    /** The user's colour, lightened (or darkened on a light theme) until the contrast is at least 4.5:1. */
    READABLE("readable"),
    /** One colour for all names: the accent colour of the theme. */
    ACCENT("accent");

    companion object {
        @JvmStatic
        fun of(value: String?): NameColorMode = values().firstOrNull { it.value == value } ?: READABLE
    }
}
