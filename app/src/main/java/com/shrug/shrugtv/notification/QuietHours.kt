package com.shrug.shrugtv.notification

/**
 * Quiet hours for "X is live" notices: between [from] and [to] (full hours, 0..23)
 * nothing pops up. A window that crosses midnight (22 to 7) works; from == to means the whole day is quiet.
 */
object QuietHours {
    const val PREF_ON = "pref_notification_quiet"
    const val PREF_FROM = "pref_notification_quiet_from"
    const val PREF_TO = "pref_notification_quiet_to"

    fun isQuiet(enabled: Boolean, from: Int, to: Int, hour: Int): Boolean {
        if (!enabled) return false
        val f = from.coerceIn(0, 23)
        val t = to.coerceIn(0, 23)
        return when {
            f == t -> true
            f < t -> hour in f until t
            else -> hour >= f || hour < t
        }
    }
}
