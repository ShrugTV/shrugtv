package com.shrug.shrugtv.ui.tv.home

import android.content.SharedPreferences

/**
 * Rules of the start splash, without Compose so they run in unit tests.
 *
 * The splash covers the home screen on a cold start until its first content is there, at least [Settings.minMs]
 * (so it does not just flicker) and at most [MAX_MS] (offline or a slow network must not hold the app).
 */
object SplashRules {
    const val PREF_STYLE = "pref_splash_style"
    const val PREF_GREETING = "pref_splash_greeting"
    const val PREF_MIN_MS = "pref_splash_min_ms"

    const val MAX_MS = 5_000L

    enum class Style(val value: String) {
        /** Logo, name, greeting and a glow in the accent colour (default). */
        FULL("full"),
        /** Only the logo, as in the start window. */
        LOGO("logo"),
        /** No splash in the app; only the start window of the system stays. */
        OFF("off");

        companion object {
            fun of(value: String?): Style = entries.firstOrNull { it.value == value } ?: FULL
        }
    }

    data class Settings(val style: Style = Style.FULL, val greeting: Boolean = true, val minMs: Long = 600L)

    fun read(p: SharedPreferences) = Settings(
        style = Style.of(p.getString(PREF_STYLE, null)),
        greeting = p.getBoolean(PREF_GREETING, true),
        minMs = (p.getString(PREF_MIN_MS, null)?.toLongOrNull() ?: 600L).coerceIn(0L, MAX_MS),
    )

    /** Hide now? [ready] = the home screen has rows or finished loading. */
    fun hide(elapsedMs: Long, ready: Boolean, minMs: Long): Boolean =
        elapsedMs >= MAX_MS || (ready && elapsedMs >= minMs)

    enum class Greeting { MORNING, DAY, EVENING, NIGHT }

    fun greeting(hour: Int): Greeting = when (hour) {
        in 5..10 -> Greeting.MORNING
        in 11..17 -> Greeting.DAY
        in 18..22 -> Greeting.EVENING
        else -> Greeting.NIGHT
    }

    private var shown = false

    /** True once per process: the splash belongs to a cold start, not to rotating or coming back. */
    @Synchronized
    fun claim(freshInstance: Boolean): Boolean {
        if (!freshInstance || shown) return false
        shown = true
        return true
    }
}
