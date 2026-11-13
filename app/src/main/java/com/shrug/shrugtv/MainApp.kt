package com.shrug.shrugtv

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.shrug.shrugtv.channels.RecommendationWorker
import com.shrug.shrugtv.database.AppDatabase
import com.shrug.shrugtv.database.VodInfo
import com.shrug.shrugtv.filter.FilterSettings
import com.shrug.shrugtv.login.TwitchDeviceAuth
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.notification.LiveNotificationTicker
import com.shrug.shrugtv.player.PlaybackUrlResolver
import com.shrug.shrugtv.remotesettings.RemoteSettings
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import com.shrug.shrugtv.util.AppLanguage
import com.shrug.shrugtv.util.Threads
import com.shrug.shrugtv.util.TimeUtil
import java.util.concurrent.TimeUnit

class MainApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    private var sessionHeartbeatRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        @JvmStatic
        fun compareByLastAccessDesc(vodInfo: VodInfo, vodInfo2: VodInfo): Int {
            return vodInfo2.lastAccessDateTime.compareTo(vodInfo.lastAccessDateTime)
        }

        @JvmStatic
        fun loadPreviewQualities(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context) ?: return
            val i = prefs.getString("pref_stream_preview_quality", "0")?.toIntOrNull() ?: 0
            val previewQuality = Enums.PreviewQuality.values().getOrElse(i) { Enums.PreviewQuality.values()[0] }
            AppState.streamPreviewResolution = Enums.previewResolution(previewQuality)
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        com.shrug.shrugtv.ui.tv.theme.ThemeSettings.sync(this)
        loadUserPreferences(this)
        com.shrug.shrugtv.session.SessionStore.loadFromPrefs(this)
        AppState.loadAccessToken(applicationContext)
        if (AppState.accessToken.isNotEmpty() && AppState.userId.isEmpty()) {
            com.shrug.shrugtv.session.UserSession.loadUserProfile(this, object : com.shrug.shrugtv.session.UserSession.ProfileCallback {
                override fun onError(str: String) {}
                override fun onUnauthorized() {}
                override fun onEmptyResponse() {}
                override fun onRequestFailed() {}
            })
        }
        TwitchDeviceAuth.startAutoRefresh(applicationContext)
        ensureRemoteSettingsLoaded()
        loadPreviewQualities(this)
        FilterSettings.load(this)
        migrateResetToken()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                AppState.returnedFromBackground = true
            }
        })

        LiveNotificationTicker.init(this)
        // Home Assistant via MQTT (off unless set up in the settings)
        com.shrug.shrugtv.ha.MqttBridge.init(this)
        // Phone companion (off unless switched on) and its remote keys
        com.shrug.shrugtv.companion.KeyInjector.init(this)
        com.shrug.shrugtv.companion.Companion.init(this)
        LiveNotificationTicker.get()?.let { ticker ->
            ProcessLifecycleOwner.get().lifecycle.addObserver(ticker)
        }

        createNotificationChannel()
        startSessionHeartbeat()
        cleanupVodHistoryAsync()
        scheduleRecommendationWorker()
        initRemoteConfig()
        checkForUpdate()

        if (Build.VERSION.SDK_INT >= 24) {
            Log.i("Start", "Application ready after ${SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()} ms since process start")
        }
    }

    /** Public edition, only with the switch on: at most once a day, one notice per found version. */
    private fun checkForUpdate() {
        if (!com.shrug.shrugtv.update.UpdateChecker.enabled(this)) return
        Threads.doInBackground {
            val r = com.shrug.shrugtv.update.UpdateChecker.check(this, force = false)
            if (r is com.shrug.shrugtv.update.UpdateChecker.Result.Available && com.shrug.shrugtv.update.UpdateChecker.markNotified(this, r.release.version)) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    com.shrug.shrugtv.notification.ToastNotification(this, getString(R.string.update_available, r.release.version, r.release.url),
                        com.shrug.shrugtv.notification.NotificationSettings(20.0f, 5, 8000L), 1)
                }
            }
        }
    }

    private fun loadUserPreferences(context: Context) {
        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (defaultPrefs != null) {
            AppState.favoritesInFrontOfFollowed = defaultPrefs.getBoolean("pref_favorites_in_front_of_followed", true)
            Threads.doInBackground {
                loadFavoriteChannels()
            }
            val proxyIdx = defaultPrefs.getString("pref_proxy_server_name", "0")?.toIntOrNull() ?: 0
            val proxyServer = Enums.ProxyServer.values().getOrElse(proxyIdx) { Enums.ProxyServer.values()[0] }
            PlaybackUrlResolver.setProxyServer(proxyServer)
            PlaybackUrlResolver.setProxyUrl(defaultPrefs.getString("pref_proxy_url", "") ?: "")
        }
        val userPrefs = getSharedPreferences("User", 0)
        if (userPrefs != null) {
            AppState.userLoggedOut = userPrefs.getBoolean("USER_LOGGED_OUT", false)
        }
    }

    private fun loadFavoriteChannels() {
        AppState.favoriteChannelIds = AppDatabase.getInstance(this).favoriteChannelDao().allChannelIds
    }

    private fun ensureRemoteSettingsLoaded() {
        if (AppState.remoteSettings == null) {
            RemoteSettings.load(this)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(
                NotificationChannel("ShrugTVNotification", "ShrugTV", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun startSessionHeartbeat() {
        if (AppState.appStartTime == null) {
            AppState.appStartTime = TimeUtil.now()
        }
        val runnable = object : Runnable {
            override fun run() {
                AppState.appStartTime = TimeUtil.now()
                mainHandler.postDelayed(this, 300_000L)
            }
        }
        sessionHeartbeatRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun cleanupVodHistoryAsync() {
        Threads.doInBackground {
            cleanupVodHistory(200)
            // Watch statistics older than 25 months
            try {
                com.shrug.shrugtv.stats.WatchStatsStore.prune(applicationContext, java.time.LocalDate.now())
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
    }

    private fun cleanupVodHistory(maxItems: Int) {
        try {
            val db = AppDatabase.getInstance(applicationContext) ?: return
            val history = db.vodInfoDao().history
            if (history.size > maxItems) {
                history.sortByDescending { it.lastAccessDateTime }
                val toDelete = history.subList(maxItems, history.size)
                for (item in toDelete) {
                    db.vodInfoDao().delete(item)
                }
            }
            val all = db.vodInfoDao().all
            if (all.size > maxItems) {
                all.sortByDescending { it.id }
                val toDelete = all.subList(maxItems, all.size)
                for (item in toDelete) {
                    db.vodInfoDao().delete(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CrashReporter.recordException(e)
        }
    }

    private fun scheduleRecommendationWorker() {
        if (Build.VERSION.SDK_INT >= 26) {
            Threads.doInBackground {
                try {
                    val request = PeriodicWorkRequest.Builder(
                        RecommendationWorker::class.java,
                        30L,
                        TimeUnit.MINUTES
                    ).build()
                    WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                        "Recommendations",
                        ExistingPeriodicWorkPolicy.KEEP,
                        request
                    )
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                }
            }
        }
    }

    private fun migrateResetToken() {
        if (AppState.isTurbo) {
            getSharedPreferences("User", 0).edit().apply {
                putString("TOKEN", "")
                putBoolean("IS_TURBO", false)
                commit()
            }
        }
        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val userPrefs = getSharedPreferences("User", 0)
        if (defaultPrefs == null || userPrefs == null || !defaultPrefs.getBoolean("reset_token_beta_113", true)) {
            return
        }
        AppState.accessToken = ""
        userPrefs.edit().apply {
            putString("TOKEN", "")
            apply()
        }
        defaultPrefs.edit().apply {
            putBoolean("reset_token_beta_113", false)
            apply()
        }
    }

    private fun initRemoteConfig() {
        // Frueher Firebase Remote Config; im privaten Nachbau entfernt.
    }
}
