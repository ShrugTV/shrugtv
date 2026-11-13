package com.shrug.shrugtv.ui.tv.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.preference.PreferenceManager
import com.shrug.shrugtv.R
import com.shrug.shrugtv.activities.DebugActivity
import com.shrug.shrugtv.apihelpers.LiveStreamHistory
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.notification.NotificationSettings
import com.shrug.shrugtv.notification.OverlaySettings
import com.shrug.shrugtv.notification.ToastNotification
import com.shrug.shrugtv.ui.tv.dialogs.ClearHistoryActivity

internal object SettingsActions {

    private var versionClicks = 0
    private var buildTypeClicks = 0
    private var buildVariantClicks = 0

    @JvmStatic
    fun execute(a: Activity, key: String): Boolean {
        val ctx = a.applicationContext
        // Home screen row order: OK moves a row up one place
        if (key.startsWith(com.shrug.shrugtv.ui.tv.home.HomeRowOrder.MOVE_PREFIX)) {
            val p = PreferenceManager.getDefaultSharedPreferences(ctx)
            p.edit().putString(com.shrug.shrugtv.ui.tv.home.HomeRowOrder.PREF, com.shrug.shrugtv.ui.tv.home.HomeRowOrder.moveUp(
                com.shrug.shrugtv.ui.tv.home.HomeRowOrder.DEFAULT, p.getString(com.shrug.shrugtv.ui.tv.home.HomeRowOrder.PREF, null),
                key.removePrefix(com.shrug.shrugtv.ui.tv.home.HomeRowOrder.MOVE_PREFIX))).apply()
            return true
        }
        when (key) {
            com.shrug.shrugtv.ui.tv.home.HomeRowOrder.RESET -> {
                PreferenceManager.getDefaultSharedPreferences(ctx).edit().remove(com.shrug.shrugtv.ui.tv.home.HomeRowOrder.PREF).apply()
                hint(ctx, a.getString(R.string.settings_row_order_reset_done))
                return true
            }
            com.shrug.shrugtv.update.UpdateChecker.PREF_NOW -> {
                Thread {
                    val r = com.shrug.shrugtv.update.UpdateChecker.check(ctx, force = true)
                    val text = (when (r) {
                        is com.shrug.shrugtv.update.UpdateChecker.Result.Available -> a.getString(R.string.update_available, r.release.version, r.release.url)
                        com.shrug.shrugtv.update.UpdateChecker.Result.UpToDate -> a.getString(R.string.update_up_to_date)
                        com.shrug.shrugtv.update.UpdateChecker.Result.Failed -> a.getString(R.string.update_failed)
                    })
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (r is com.shrug.shrugtv.update.UpdateChecker.Result.Available) {
                            a.startActivity(com.shrug.shrugtv.update.UpdateActivity.intent(a, r.release))
                        } else {
                            hint(ctx, text, 8000L)
                        }
                    }
                }.start()
                return true
            }
            "pref_clear_caches" -> {
                clearCaches(ctx)
                hint(ctx, a.getString(R.string.settings_caches_cleared))
                return true
            }
            com.shrug.shrugtv.ui.tv.theme.ThemeSettings.ACTION_RESET -> {
                com.shrug.shrugtv.ui.tv.theme.ThemeSettings.resetOverrides(PreferenceManager.getDefaultSharedPreferences(ctx))
                return true
            }
            "pref_companion_pair" -> {
                a.startActivity(Intent(a, com.shrug.shrugtv.companion.CompanionQrActivity::class.java))
                return true
            }
            "pref_companion_devices" -> {
                a.startActivity(Intent(a, com.shrug.shrugtv.companion.CompanionDevicesActivity::class.java))
                return true
            }
            "pref_companion_disconnect" -> {
                com.shrug.shrugtv.companion.Companion.disconnectAll()
                hint(ctx, a.getString(R.string.settings_companion_disconnect))
                return true
            }
            "pref_mqtt_status" -> {
                hint(ctx, a.getString(R.string.settings_mqtt_status_value, com.shrug.shrugtv.ha.MqttBridge.status))
                return true
            }
            "pref_notification_test" -> {
                testNotification(a)
                return true
            }
            "pref_live_channel_history_clear_history" -> {
                LiveStreamHistory.clearLiveStreamHistory(ctx)
                hint(ctx, a.getString(R.string.settings_live_history_cleared))
                return true
            }
            "pref_clear_vod_history" -> {
                a.startActivity(Intent(a, ClearHistoryActivity::class.java))
                return true
            }
            "pref_request_draw_over_permission" -> {
                if (Build.VERSION.SDK_INT >= 26 && !Settings.canDrawOverlays(a)) {
                    a.startActivity(Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION"))
                } else {
                    hint(ctx, a.getString(R.string.settings_permission_granted))
                }
                return true
            }
            "pref_version_code" -> {
                if (++versionClicks == 6) {
                    versionClicks = 0
                    developerMode(a, true)
                }
                return true
            }
            "pref_buildType" -> {
                if (++buildTypeClicks == 6) {
                    buildTypeClicks = 0
                    developerMode(a, false)
                }
                return true
            }
            "pref_buildVariant" -> {
                if (++buildVariantClicks == 3) {
                    buildVariantClicks = 0
                    a.startActivity(Intent(a, DebugActivity::class.java))
                }
                return true
            }
            else -> return false
        }
    }

    /** Hook after a switch changed; nothing needs one at the moment. */
    @JvmStatic
    fun afterSwitch(ctx: Context, key: String, on: Boolean) = Unit

    /** Pictures (Glide, memory and disk) and the saved home screen; everything comes back when it is needed. */
    private fun clearCaches(ctx: Context) {
        com.bumptech.glide.Glide.get(ctx).clearMemory()
        Thread {
            try { com.bumptech.glide.Glide.get(ctx).clearDiskCache() } catch (e: Exception) { com.shrug.shrugtv.telemetry.CrashReporter.recordException(e) }
            java.io.File(ctx.cacheDir, "home.bin").delete()
        }.start()
    }

    private fun developerMode(a: Activity, on: Boolean) {
        val ctx = a.applicationContext
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean("pref_developer_mode", on).apply()
        hint(ctx, a.getString(if (on) R.string.settings_developer_mode_on else R.string.settings_developer_mode_off))
    }

    private fun hint(ctx: Context, text: String, ms: Long = 2000L) {
        ToastNotification(ctx, text, NotificationSettings(20.0f, 5, ms), 1)
    }

    private fun testNotification(a: Activity) {
        val ctx = a.applicationContext
        val p = PreferenceManager.getDefaultSharedPreferences(ctx) ?: return
        val type = p.getString("prefs_notification_type", "0")?.toIntOrNull() ?: 0
        val s = OverlaySettings(
            p.getString("prefs_notification_font_size", "20")?.toFloatOrNull() ?: 20.0f,
            p.getString("prefs_notification_position", "1")?.toIntOrNull() ?: 1,
            p.getString("pref_notification_duration", "3000")?.toLongOrNull() ?: 3000L,
            p.getString("pref_notification_animation", "1")?.toIntOrNull() ?: 1
        )
        val movie = Movie().apply {
            login = "TestMessage"
            avatarUrl = "android.resource://${a.packageName}/${R.drawable.channel_logo}"
            title = a.getString(R.string.test_detailed_title)
        }
        if (type == 1) {
            ToastNotification(ctx, movie, s)
        } else {
            ToastNotification(ctx, "TestMessage", s, 0)
        }
    }
}
