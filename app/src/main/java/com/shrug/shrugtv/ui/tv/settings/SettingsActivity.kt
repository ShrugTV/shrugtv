package com.shrug.shrugtv.ui.tv.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceManager
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.R
import com.shrug.shrugtv.activities.LoginActivity
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.ui.tv.TvHomeTheme

/**
 * Settings in Compose for TV (step 5). Reads res/xml/settings.xml
 * (see CLAUDE.md for the one file) and writes the same SharedPreferences as the former
 * Leanback SettingsActivity, removed since 2026-10-22, as well as the group
 * "Updates" with its update function.
 *
 * Hidden: "Extra1" (empty placeholders from the original) and the developer page while developer
 * mode is off.
 */
class SettingsActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    private lateinit var prefs: SharedPreferences
    private val standards = mutableMapOf<String, Any?>()
    private var version by mutableIntStateOf(0)

    /** Changes from the browser (companion settings page) show up here at once. */
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024, 1024)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        val root = SettingsModel.load(this, R.xml.settings, standards)
        val access = SettingsAccess(
            bool = { k, d -> version; prefs.getBoolean(k, d) },
            string = { k, d -> version; prefs.getString(k, d) },
            setBool = { k, v ->
                prefs.edit().putBoolean(k, v).apply()
                SettingsActions.afterSwitch(applicationContext, k, v)
                version++
            },
            setString = { k, v ->
                prefs.edit().putString(k, v).apply()
                version++
                // App language changed: restart so every screen switches
                if (k == com.shrug.shrugtv.util.AppLanguage.PREF_KEY) com.shrug.shrugtv.util.AppLanguage.apply(this)
            },
            action = { k -> SettingsActions.execute(this, k); version++ },
            active = { dep -> version; dep == null || prefs.getBoolean(dep, standards[dep] as? Boolean ?: false) },
            extra = { k -> version; extra(k) },
            reset = { keys -> prefs.edit().apply { keys.forEach { remove(it) } }.apply(); version++ },
        )
        setContent {
            TvHomeTheme {
                val groups = remember(version) { visibleGroups(root) }
                val account = Account(
                    name = AppState.userDisplayName.takeIf { AppState.userProfileLoaded && it.isNotEmpty() },
                    logIn = {
                        val target = if (AppState.userProfileLoaded) com.shrug.shrugtv.ui.tv.dialogs.LogoutActivity::class.java else LoginActivity::class.java
                        startActivity(Intent(this, target))
                    },
                    version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    changes = ::changes,
                )
                SettingsScreen(groups, access, account, version)
            }
        }
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        version++ // back from sign-in or sign-out
    }

    private fun visibleGroups(root: Entry.Page): List<Entry.Page> =
        SettingsModel.visible(root, prefs.getBoolean("pref_developer_mode", false), prefs.getBoolean("pref_show_advanced", false))

    /** Page "Changes" from assets/changelog.md (tools/changelog_app.py). */
    private fun changes(): List<Changelog.Version> = runCatching {
        assets.open("changelog.md").bufferedReader(Charsets.UTF_8).use { Changelog.read(it.readText()) }
    }.getOrElse { emptyList() }

    /** Runtime values that the old SettingsFragment used as summary. */
    private fun extra(key: String?): String? = if (key != null && key.startsWith(com.shrug.shrugtv.ui.tv.home.HomeRowOrder.MOVE_PREFIX)) {
        // Where a home row stands now
        val h = com.shrug.shrugtv.ui.tv.home.HomeRowOrder
        getString(R.string.settings_row_place, h.place(h.DEFAULT, prefs.getString(h.PREF, null), key.removePrefix(h.MOVE_PREFIX)))
    } else when (key) {
        "pref_version_name" -> BuildConfig.VERSION_NAME
        "pref_version_code" -> BuildConfig.VERSION_CODE.toString()
        "pref_user_id" -> AppState.hashedUserId
        "pref_buildType" -> BuildConfig.BUILD_TYPE
        com.shrug.shrugtv.ui.tv.theme.ThemeSettings.ACTION_RESET -> {
            val n = com.shrug.shrugtv.ui.tv.theme.ThemeSettings.overrideCount(prefs::getString)
            if (n == 0) getString(R.string.theme_reset_none) else resources.getQuantityString(R.plurals.theme_reset_count, n, n)
        }
        else -> null
    }
}
