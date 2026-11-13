package com.shrug.shrugtv.update

import android.content.Context
import androidx.preference.PreferenceManager
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.net.Http
import okhttp3.Request

/**
 * Asks the release API of the public repository for the newest release. At most once a day, with
 * ETag, without a login, and only when the switch "Check for updates" is on (default off) or the user taps "Check now".
 * Nothing is downloaded or installed: the app only says that a version exists and where to get it.
 */
object UpdateChecker {

    const val PREF_ENABLED = "pref_update_check"
    const val PREF_NOW = "pref_update_check_now"
    private const val DAY_MS = 24L * 60 * 60 * 1000

    sealed class Result {
        data class Available(val release: UpdateCheck.Release) : Result()
        object UpToDate : Result()
        object Failed : Result()
    }

    private fun store(ctx: Context) = ctx.getSharedPreferences("update_check", Context.MODE_PRIVATE)

    fun enabled(ctx: Context): Boolean =
        BuildConfig.UPDATE_CHECK && PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_ENABLED, false)

    /** Blocking; call from a background thread. [force] ignores the daily limit (the "Check now" action). */
    fun check(ctx: Context, force: Boolean): Result {
        if (!BuildConfig.UPDATE_CHECK || BuildConfig.UPDATE_SOURCE.isEmpty()) return Result.UpToDate
        val s = store(ctx)
        val now = System.currentTimeMillis()
        val cached = s.getString("release_json", null)
        if (!force && cached != null && now - s.getLong("last_check", 0L) < DAY_MS) return result(ctx, cached)
        val req = Request.Builder().url(BuildConfig.UPDATE_SOURCE).header("Accept", "application/json")
            .apply { s.getString("etag", null)?.takeIf { cached != null }?.let { header("If-None-Match", it) } }.build()
        return try {
            Http.client().newCall(req).execute().use { resp ->
                when {
                    resp.code == 304 && cached != null -> {
                        s.edit().putLong("last_check", now).apply()
                        result(ctx, cached)
                    }
                    resp.isSuccessful -> {
                        val body = resp.body?.string().orEmpty()
                        s.edit().putString("release_json", body).putString("etag", resp.header("ETag")).putLong("last_check", now).apply()
                        result(ctx, body)
                    }
                    else -> Result.Failed // 403/429 rate limit or any other answer: try again later, no crash
                }
            }
        } catch (e: Exception) {
            Result.Failed
        }
    }

    private fun result(ctx: Context, json: String): Result {
        val skipped = store(ctx).getString("skipped", null)
        return UpdateCheck.available(BuildConfig.VERSION_NAME, json, skipped)?.let { Result.Available(it) } ?: Result.UpToDate
    }

    /** True once per found version, so the start-up notice does not repeat on every launch. */
    fun markNotified(ctx: Context, version: String): Boolean {
        val s = store(ctx)
        if (s.getString("notified", null) == version) return false
        s.edit().putString("notified", version).apply()
        return true
    }
}
