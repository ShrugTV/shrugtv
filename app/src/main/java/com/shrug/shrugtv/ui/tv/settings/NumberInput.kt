package com.shrug.shrugtv.ui.tv.settings

/** What the number dialog of the settings does with typed digits and Left/Right. */
object NumberInput {
    /** Only digits, at most as many as the largest value has. */
    @JvmStatic
    fun digits(typed: String, max: Int): String = typed.filter { it.isDigit() }.take(max.toString().length)

    /** The text after Left ([direction] -1) or Right (+1): one [step], kept inside [min] and [max]. */
    @JvmStatic
    fun step(text: String, direction: Int, step: Int, min: Int, max: Int, standard: Int): String {
        val current = text.toIntOrNull() ?: standard
        return (current + direction * step).coerceIn(min, max).toString()
    }

    /** The value to save: the typed number inside the limits, the default if nothing usable was typed. */
    @JvmStatic
    fun parse(text: String, min: Int, max: Int, standard: Int): Int = (text.toIntOrNull() ?: standard).coerceIn(min, max)
}
