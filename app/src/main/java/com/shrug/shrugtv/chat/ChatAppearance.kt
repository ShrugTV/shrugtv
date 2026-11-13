package com.shrug.shrugtv.chat

import android.content.SharedPreferences

/**
 * Chat look that "Adjust chat" in the player changes live (docs/chat_plan.md phase 3, CA12;
 * ). The values are the entry values of the settings, so the settings screen
 * shows the same: `pref_chat_layout`, `pref_chat_width`, `pref_chat_position`,
 * `pref_chat_background_opacity`, `prefs_chat_font_size`, `pref_chat_emote_size`, `pref_chat_height`,
 * `pref_chat_vertical`.
 */
data class ChatAppearance(
    val layout: ChatLayout,
    /** Entry of `value_chat_width`: 0 = scales with the font, otherwise dp. */
    val widthValue: Int,
    /** Entry of `value_chat_background_opacity`: 0..9 = 0..90 %. */
    val opacityIndex: Int,
    val onLeft: Boolean,
    /** Entry of `value_chat_font_size` ("2" = 14 sp). */
    val fontValue: Int = 2,
    /** Entry of `value_emote_size`: "0" normal, "1" bigger, otherwise a fixed pixel height. */
    val emoteValue: String = "1",
    /** Share of the screen height, one of [HEIGHTS]. */
    val heightPercent: Int = 100,
    /** Where a chat lower than the screen sits. */
    val vertical: ChatVertical = ChatVertical.BOTTOM
) {
    val textSizeSp: Float get() = ChatPrefs.textSizeSp(fontValue.toString())

    fun widthDp(textSp: Float = textSizeSp): Float = ChatPrefs.widthDp(widthValue.toString(), textSp)

    val backgroundAlpha: Float get() = opacityIndex.coerceIn(0, 9) / 10f

    val heightFraction: Float get() = heightPercent.coerceIn(HEIGHTS.first(), 100) / 100f

    fun stepLayout(delta: Int): ChatAppearance {
        val all = ChatLayout.values()
        return copy(layout = all[Math.floorMod(layout.ordinal + delta, all.size)])
    }

    /**
     * Right = wider, left = narrower. "Auto" sits at its real width between the fixed widths, so
     * the first step from it goes to the next wider or narrower fixed width; left of 100 dp is Auto.
     */
    fun stepWidth(delta: Int, textSp: Float = textSizeSp): ChatAppearance {
        val fixed = WIDTHS.filter { it > 0 }
        val current = widthDp(textSp)
        val next = if (delta > 0) {
            fixed.firstOrNull { it > current } ?: fixed.last()
        } else {
            if (widthValue == 0) return this
            fixed.lastOrNull { it < current } ?: 0
        }
        return copy(widthValue = next)
    }

    fun stepOpacity(delta: Int): ChatAppearance = copy(opacityIndex = (opacityIndex + delta).coerceIn(0, 9))

    fun togglePosition(): ChatAppearance = copy(onLeft = !onLeft)

    /** Right = bigger text, from 10 to 22 sp. */
    fun stepFont(delta: Int): ChatAppearance {
        val i = FONT_VALUES.indexOf(fontValue).takeIf { it >= 0 } ?: FONT_VALUES.indexOf(2)
        return copy(fontValue = FONT_VALUES[(i + delta).coerceIn(0, FONT_VALUES.lastIndex)])
    }

    /** Right = bigger emotes: normal, bigger; a fixed pixel size from the settings counts as bigger. */
    fun stepEmote(delta: Int): ChatAppearance = copy(emoteValue = if (delta > 0) "1" else "0")

    /** Right = taller. */
    fun stepHeight(delta: Int): ChatAppearance {
        val i = HEIGHTS.indexOfFirst { it >= heightPercent }.takeIf { it >= 0 } ?: HEIGHTS.lastIndex
        return copy(heightPercent = HEIGHTS[(i + delta).coerceIn(0, HEIGHTS.lastIndex)])
    }

    /** Right = further down: top, middle, bottom. */
    fun stepVertical(delta: Int): ChatAppearance {
        val all = ChatVertical.values()
        return copy(vertical = all[(vertical.ordinal + delta).coerceIn(0, all.lastIndex)])
    }

    /** Text or emote size differ: the chat has to render its messages anew. */
    fun sizesDiffer(other: ChatAppearance): Boolean = fontValue != other.fontValue || emoteValue != other.emoteValue

    fun save(prefs: SharedPreferences) {
        prefs.edit()
            .putString("pref_chat_layout", layout.value)
            .putString("pref_chat_width", widthValue.toString())
            .putString("pref_chat_background_opacity", opacityIndex.toString())
            // value_chat_position: 2 = right bottom, 5 = left middle
            .putString("pref_chat_position", if (onLeft) "5" else "2")
            .putString("prefs_chat_font_size", fontValue.toString())
            .putString("pref_chat_emote_size", emoteValue)
            .putString("pref_chat_height", heightPercent.toString())
            .putString("pref_chat_vertical", vertical.value)
            .apply()
    }

    companion object {
        /** `value_chat_width` in order; 0 (auto) first. */
        @JvmField
        val WIDTHS = listOf(0, 100, 150, 175, 200, 225, 250, 275, 300)

        /** `value_chat_font_size` from small to big (10 … 22 sp). */
        @JvmField
        val FONT_VALUES = listOf(6, 5, 4, 3, 2, 1, 0, 7, 8, 9, 10, 11, 12)

        /** `value_chat_height` in percent, from low to full height. */
        @JvmField
        val HEIGHTS = listOf(33, 50, 66, 75, 100)

        @JvmStatic
        fun from(prefs: SharedPreferences): ChatAppearance = ChatAppearance(
            layout = ChatLayout.of(prefs.getString("pref_chat_layout", null)),
            widthValue = prefs.getString("pref_chat_width", "0")?.toIntOrNull() ?: 0,
            opacityIndex = (prefs.getString("pref_chat_background_opacity", "6")?.toIntOrNull() ?: 6).coerceIn(0, 9),
            onLeft = (prefs.getString("pref_chat_position", "2")?.toIntOrNull() ?: 2) in 4..6,
            fontValue = prefs.getString("prefs_chat_font_size", "2")?.toIntOrNull()?.takeIf { it in FONT_VALUES } ?: 2,
            emoteValue = prefs.getString("pref_chat_emote_size", "1") ?: "1",
            heightPercent = prefs.getString("pref_chat_height", "100")?.toIntOrNull()?.takeIf { it in HEIGHTS } ?: 100,
            vertical = ChatVertical.of(prefs.getString("pref_chat_vertical", null))
        )
    }
}

/** Vertical place of a chat lower than the screen. */
enum class ChatVertical(val value: String) {
    TOP("top"), MIDDLE("middle"), BOTTOM("bottom");

    companion object {
        @JvmStatic
        fun of(value: String?): ChatVertical = values().firstOrNull { it.value == value } ?: BOTTOM
    }
}
