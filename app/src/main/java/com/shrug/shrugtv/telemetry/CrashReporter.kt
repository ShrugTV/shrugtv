package com.shrug.shrugtv.telemetry

import android.content.Context
import android.os.Build
import android.util.Log
import com.shrug.shrugtv.BuildConfig
import java.io.File
import java.time.Instant

/**
 * Local error and crash logging ( stage 0.7).
 * Crashes: caught exceptions go to Logcat, unhandled
 * Abstuerze werden zusaetzlich unter `files/last_crash.txt` abgelegt.
 */
object CrashReporter {
    private const val TAG = "ShrugTV"
    private const val CRASH_FILE = "last_crash.txt"

    @JvmStatic
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val report = buildString {
                    appendLine("Time: ${Instant.now()}")
                    appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    appendLine(Log.getStackTraceString(throwable))
                }
                File(appContext.filesDir, CRASH_FILE).writeText(report)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    @JvmStatic
    fun lastCrashReport(context: Context): String? =
        runCatching {
            val f = File(context.applicationContext.filesDir, CRASH_FILE)
            if (f.isFile) f.readText() else null
        }.getOrNull()

    @JvmStatic
    fun log(message: String?) {
        if (message != null) Log.d(TAG, message)
    }

    @JvmStatic
    fun recordException(exc: Exception?) {
        if (exc == null || exc is InterruptedException) return
        Log.w(TAG, "Caught exception", exc)
    }

    @JvmStatic
    fun setCustomKey(key: String?, value: Int) {
    }

    @JvmStatic
    fun setCustomKey(key: String?, value: String?) {
    }

    @JvmStatic
    fun setUserId(userId: String?) {
    }
}
