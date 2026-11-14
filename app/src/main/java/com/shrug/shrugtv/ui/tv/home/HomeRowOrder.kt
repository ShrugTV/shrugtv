package com.shrug.shrugtv.ui.tv.home

/**
 * The order of the home screen rows: `pref_row_order` holds the row ids, comma separated.
 * Unknown ids are ignored and rows that are not in it keep their built-in order behind the saved ones, so a
 * new row never disappears.
 */
object HomeRowOrder {
    const val PREF = "pref_row_order"
    const val MOVE_PREFIX = "pref_row_move_"
    const val RESET = "pref_row_order_reset"

    /** [defaults] (the built-in order) sorted by [saved]. */
    fun order(defaults: List<String>, saved: String?): List<String> {
        val chosen = saved.orEmpty().split(',').map { it.trim() }.filter { it in defaults }.distinct()
        return chosen + defaults.filter { it !in chosen }
    }

    /** [id] one place up; the first one goes to the end. Returns the new value of the preference. */
    fun moveUp(defaults: List<String>, saved: String?, id: String): String {
        val list = order(defaults, saved).toMutableList()
        val i = list.indexOf(id)
        if (i < 0) return list.joinToString(",")
        list.removeAt(i)
        list.add(if (i == 0) list.size else i - 1, id)
        return list.joinToString(",")
    }

    /** 1-based place of [id]. */
    fun place(defaults: List<String>, saved: String?, id: String): Int = order(defaults, saved).indexOf(id) + 1

    /** The built-in order, one place for the home screen and the settings. */
    val DEFAULT = listOf(
        "followed_live", "live_history", "continue_watching", "followed_channels", "clips",
        "followed_categories", "past_broadcasts", "top_categories", "top_streams",
    )
}
