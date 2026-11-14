package com.shrug.shrugtv.companion

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.preference.PreferenceManager
import com.google.gson.JsonObject
import com.shrug.shrugtv.ui.tv.theme.ThemeSettings

/**
 * The colours of the TV's current skin for the phone page: the page paints itself in them and
 * follows live when the skin or a colour changes. Switch "TV skin on the phone page" (`pref_companion_skin`, default on).
 */
object SkinApi {
    const val PREF = "pref_companion_skin"

    private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

    fun enabled(context: Context) = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF, true)

    fun theme(context: Context): JsonObject = JsonObject().apply {
        val on = enabled(context)
        addProperty("on", on)
        if (!on) return@apply
        val t = ThemeSettings.theme.value
        val c = t.colors
        addProperty("id", t.id)
        addProperty("dark", c.background.luminance() < 0.5f)
        add("colors", JsonObject().apply {
            addProperty("background", hex(c.background))
            addProperty("surface", hex(c.surface))
            addProperty("surfaceHigh", hex(c.surfaceHigh))
            addProperty("text", hex(c.text))
            addProperty("textMuted", hex(c.textMuted))
            addProperty("accent", hex(c.accent))
            addProperty("accentLight", hex(c.accentLight))
            addProperty("onAccent", hex(c.onAccent))
            addProperty("live", hex(c.live))
        })
    }

    /** The manifest of the installed page; the colours of the skin when it is on, the defaults otherwise. */
    fun manifest(context: Context): String {
        val colors = theme(context).getAsJsonObject("colors")
        val bg = colors?.get("background")?.asString ?: "#0B0B0E"
        return JsonObject().apply {
            addProperty("name", "ShrugTV")
            addProperty("short_name", "ShrugTV")
            addProperty("description", "Remote, chat and settings for ShrugTV")
            addProperty("id", "/")
            addProperty("start_url", "/")
            addProperty("scope", "/")
            addProperty("display", "standalone")
            addProperty("orientation", "portrait")
            addProperty("background_color", bg)
            addProperty("theme_color", bg)
            add("icons", com.google.gson.JsonArray().apply {
                add(JsonObject().apply { addProperty("src", "/favicon.png"); addProperty("sizes", "192x192"); addProperty("type", "image/png"); addProperty("purpose", "any") })
            })
        }.toString()
    }
}
