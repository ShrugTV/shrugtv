package com.shrug.shrugtv.session

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Typed access to the existing SharedPreferences with unchanged `pref_*` keys
 * and default values (stage 0.3).
 */
@Immutable
data class HomeRowSettings(
    val showFollowed: Boolean = true,
    val showLiveHistory: Boolean = true,
    val showVodHistory: Boolean = false,
    val showFollowedChannels: Boolean = true,
    val showFollowedGames: Boolean = true,
    val showPastBroadcasts: Boolean = true,
    val showTopGames: Boolean = true,
    val showTopStreamers: Boolean = true,
    val showLanguageRow: Boolean = false,
    val showLanguageRowGames: Boolean = false,
    val languageRowAsPrimary: Boolean = false,
    val streamLanguageCode: String = "",
    val favoritesInFrontOfFollowed: Boolean = true,
)

object AppSettings {
    private val _homeRows = MutableStateFlow(HomeRowSettings())
    val homeRows: StateFlow<HomeRowSettings> = _homeRows.asStateFlow()

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    @JvmStatic
    fun sync(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        _homeRows.value = readHomeRows(prefs)
        if (listener == null) {
            val l = SharedPreferences.OnSharedPreferenceChangeListener { sp, _ ->
                _homeRows.value = readHomeRows(sp)
            }
            prefs.registerOnSharedPreferenceChangeListener(l)
            listener = l
        }
    }

    private fun readHomeRows(p: SharedPreferences): HomeRowSettings = HomeRowSettings(
        showFollowed = p.getBoolean("pref_show_Followed", true),
        showLiveHistory = p.getBoolean("pref_show_LiveStreamHistory", true),
        showVodHistory = p.getBoolean("pref_show_VodHistory", false),
        showFollowedChannels = p.getBoolean("pref_show_FollowedChannels", true),
        showFollowedGames = p.getBoolean("pref_show_FollowedGames", true),
        showPastBroadcasts = p.getBoolean("pref_show_PastBroadcasts", true),
        showTopGames = p.getBoolean("pref_show_top_games", true),
        showTopStreamers = p.getBoolean("pref_show_top_streamers", true),
        showLanguageRow = p.getBoolean("pref_show_language_row", false),
        showLanguageRowGames = p.getBoolean("pref_show_language_row_games", false),
        languageRowAsPrimary = p.getBoolean("pref_language_row_as_primary", false),
        streamLanguageCode = p.getString("prefs_languagerow", "") ?: "",
        favoritesInFrontOfFollowed = p.getBoolean("pref_favorites_in_front_of_followed", true),
    )

    @JvmStatic
    fun getIntString(prefs: SharedPreferences, key: String, defaultVal: Int): Int =
        prefs.getString(key, null)?.toIntOrNull() ?: defaultVal
}
