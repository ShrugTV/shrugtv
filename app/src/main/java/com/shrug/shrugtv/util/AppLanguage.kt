package com.shrug.shrugtv.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import java.util.Locale

/**
 * App language: English by default, German and ten more available (es, fr, pt, it, pl,
 * tr, ru, ja, ko, zh; texts via tools/i18n.py). The setting "pref_app_language" holds "system" (follow the device)
 * or one of these codes.
 *
 * Fire OS 8 is Android 11; the per-app language API (LocaleManager) only exists from Android 13, and
 * AppCompatDelegate only covers AppCompatActivity. So every activity wraps its base context here
 * (attachBaseContext), and the Application does the same for app-level strings.
 */
object AppLanguage {
    const val PREF_KEY = "pref_app_language"
    private const val SYSTEM = "system"

    /** Language tag chosen in the settings, or null to follow the device. */
    @JvmStatic
    fun chosenTag(context: Context): String? {
        val value = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_KEY, SYSTEM) ?: SYSTEM
        return if (value == SYSTEM) null else value
    }

    /** Context whose resources use the chosen language; unchanged when following the device. */
    @JvmStatic
    fun wrap(base: Context): Context {
        val tag = chosenTag(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /**
     * After a change: restart the app on the home screen so every screen switches, not just the
     * settings (recreate() would leave the home screen behind it in the old language).
     */
    @JvmStatic
    fun apply(activity: Activity) {
        val start = activity.packageManager.getLeanbackLaunchIntentForPackage(activity.packageName)
            ?: activity.packageManager.getLaunchIntentForPackage(activity.packageName)
            ?: return activity.recreate()
        start.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(start)
        activity.finish()
    }
}
