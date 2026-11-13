package com.shrug.shrugtv.startup

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.google.gson.JsonArray
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie

/**
 * "Open the last channel on start": while the setting is on, the app remembers the login of the live
 * stream it plays; on a cold start it asks Twitch whether that channel is live and then opens the player.
 */
object StartResume {
    const val PREF_ENABLED = "pref_start_last_channel"
    const val PREF_LAST_LOGIN = "pref_start_last_login"

    /** Once per process: a recreated home screen (rotation, theme change) must not open the player again. */
    @Volatile
    private var done = false

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, false)

    /** A Twitch login: 1 to 25 letters, digits or underscores. Anything else is never queried. */
    fun validLogin(login: String?): String? =
        login?.trim()?.lowercase()?.takeIf { it.length in 1..25 && it.all { c -> c in 'a'..'z' || c in '0'..'9' || c == '_' } }

    /** Remembers a live stream; the recording of a live stream and VODs do not count. Does nothing while the setting is off. */
    fun remember(context: Context, movie: Movie) {
        if (movie.itemType != Enums.ContentType.STREAM || movie.archiveOfLive || movie.videoId > 0L) return
        val login = validLogin(movie.login) ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!prefs.getBoolean(PREF_ENABLED, false)) return
        if (prefs.getString(PREF_LAST_LOGIN, null) != login) prefs.edit().putString(PREF_LAST_LOGIN, login).apply()
    }

    /** Only the launcher start of a fresh process opens the player, not a return or a recreation. */
    fun shouldResume(enabled: Boolean, alreadyDone: Boolean, freshInstance: Boolean, action: String?, savedLogin: String?): Boolean =
        enabled && !alreadyDone && freshInstance && action == Intent.ACTION_MAIN && validLogin(savedLogin) != null

    /** The login to try now, or null; marks this process' start as handled either way. */
    fun claim(context: Context, freshInstance: Boolean, action: String?): String? {
        if (done) return null
        done = true
        val saved = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_LAST_LOGIN, null)
        return validLogin(saved).takeIf { shouldResume(isEnabled(context), false, freshInstance, action, saved) }
    }

    /** The channel's running stream, or null when it is offline or Twitch does not answer. Blocks: call off the main thread. */
    fun liveStream(context: Context, login: String): Movie? {
        val response = HelixApi.getStreamsByQuery(context, "user_login=$login") ?: return null
        if (response.get("error") != null) return null
        val data = response.get("data") as? JsonArray ?: return null
        return HelixParser.parseStreams(context, Enums.RowType.LIVE_CHANNEL_HISTORY, data).firstOrNull()
    }
}
