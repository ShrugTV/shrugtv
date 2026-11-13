package com.shrug.shrugtv.session

import android.content.Context
import androidx.preference.PreferenceManager
import com.shrug.shrugtv.R
import com.shrug.shrugtv.chat.KnownBots
import com.shrug.shrugtv.filter.FilterSettings
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.notification.LiveNotificationTicker
import com.shrug.shrugtv.player.PlaybackUrlResolver
import com.shrug.shrugtv.telemetry.CrashReporter

object GlobalSettings {

    @JvmStatic
    fun load(ctx: Context) {
        PreferenceManager.setDefaultValues(ctx, R.xml.settings, false)
        AppState.languageFlags = AppState.buildLanguageFlags()
        KnownBots.init()
        val user = ctx.getSharedPreferences("User", 0)
        AppState.accessToken = user.getString("TOKEN", "") ?: ""
        AppState.loginVersionName = user.getString("VersionName", "") ?: ""
        AppState.loginVersionCode = user.getInt("VersionCode", 0)

        val p = PreferenceManager.getDefaultSharedPreferences(ctx) ?: return
        if ("small" == p.getString("prefs_video_card_size", "normal")) {
            AppState.previewImageWidth = 560
            AppState.previewImageHeight = 315
        } else {
            AppState.previewImageWidth = 800
            AppState.previewImageHeight = 450
        }
        AppState.showLanguageRow = p.getBoolean("pref_show_language_row", false)
        AppState.showLanguageRowGames = p.getBoolean("pref_show_language_row_games", false)
        AppState.languageRowAsPrimary = p.getBoolean("pref_language_row_as_primary", false)
        AppState.showTopGames = p.getBoolean("pref_show_top_games", true)
        AppState.showTopStreamers = p.getBoolean("pref_show_top_streamers", true)
        AppState.showFollowed = p.getBoolean("pref_show_Followed", true)
        AppState.showLiveStreamHistory = p.getBoolean("pref_show_LiveStreamHistory", true)
        AppState.showFollowedGames = p.getBoolean("pref_show_FollowedGames", true)
        AppState.showFollowedChannels = p.getBoolean("pref_show_FollowedChannels", true)
        AppState.showPastBroadcasts = p.getBoolean("pref_show_PastBroadcasts", true)
        AppState.showVodHistory = p.getBoolean("pref_show_VodHistory", false)
        AppState.autoRefreshEnabled = p.getBoolean("pref_autorefresh_parent", false)
        AppState.autoRefreshMinutes = p.getString("prefs_autorefresh_minutes", "30")?.toIntOrNull() ?: 30
        AppState.showDevErrors = p.getBoolean("pref_dev_show_errors", false)

        val proxyIdx = p.getString("pref_proxy_server_name", "0")?.toIntOrNull() ?: 0
        val proxyServer = Enums.ProxyServer.values().getOrElse(proxyIdx) { Enums.ProxyServer.values()[0] }
        PlaybackUrlResolver.setProxyServer(proxyServer)
        PlaybackUrlResolver.setProxyUrl(p.getString("pref_proxy_url", "") ?: "")

        val language = p.getString("prefs_languagerow", "") ?: ""
        if (language.isNotEmpty()) {
            val langName = AppState.buildLanguageNames()[language]
            AppState.streamLanguageName = langName ?: ""
            AppState.streamLanguageCode = language
        }
        AppState.favoritesInFrontOfFollowed = p.getBoolean("pref_favorites_in_front_of_followed", true)
        SessionStore.loadFromPrefs(ctx)
        AppSettings.sync(ctx)
        FilterSettings.load(ctx)
        notificationService(p)
    }

    private fun notificationService(p: android.content.SharedPreferences) {
        try {
            // The live card in the player needs the ticker too
            val on = p.getBoolean("pref_notification_on", false) || p.getBoolean("pref_notification_player_card", true)
            val ticker = LiveNotificationTicker.get()
            if (ticker != null) {
                if (on) {
                    ticker.startTicker()
                } else {
                    ticker.stopTicker()
                }
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }
}
