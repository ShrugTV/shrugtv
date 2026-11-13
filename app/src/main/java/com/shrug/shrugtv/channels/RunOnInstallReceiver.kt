package com.shrug.shrugtv.channels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules periodic launcher recommendations on app install / update.
 */
class RunOnInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= 26) {
            val request = PeriodicWorkRequestBuilder<RecommendationWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "Recommendations",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
