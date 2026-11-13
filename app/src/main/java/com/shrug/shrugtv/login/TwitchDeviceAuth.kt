package com.shrug.shrugtv.login

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.net.Http
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.session.SessionStore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object TwitchDeviceAuth {

    private const val TAG = "TwitchDeviceAuth"
    const val SCOPES = "user:read:chat user:write:chat user:read:follows"
    private const val REFRESH_MARGIN_MS = 15 * 60 * 1000L
    private val FORM_MEDIA = "application/x-www-form-urlencoded; charset=utf-8".toMediaTypeOrNull()
    private var scheduler: ScheduledExecutorService? = null
    @Volatile
    private var appContext: Context? = null

    class DeviceCode(o: JsonObject) {
        val deviceCode: String = o.get("device_code").asString
        val userCode: String = o.get("user_code").asString
        val verificationUri: String = o.get("verification_uri").asString
        val intervalSec: Int = if (o.has("interval")) o.get("interval").asInt else 5
        val expiresAt: Long = System.currentTimeMillis() + (if (o.has("expires_in")) o.get("expires_in").asLong else 0L) * 1000L
    }

    enum class PollState { OK, PENDING, SLOW_DOWN, DENIED, EXPIRED, ERROR }

    private fun clientId(): String = BuildConfig.TWITCH_CLIENT_ID

    @JvmStatic
    fun requestCode(): DeviceCode? {
        val o = post(
            "https://id.twitch.tv/oauth2/device",
            "client_id=${enc(clientId())}&scopes=${enc(SCOPES)}"
        )
        if (o == null || !o.has("device_code")) {
            Log.w(TAG, "No device code: $o")
            return null
        }
        return DeviceCode(o)
    }

    @JvmStatic
    fun poll(context: Context, code: DeviceCode): PollState {
        if (System.currentTimeMillis() > code.expiresAt) {
            return PollState.EXPIRED
        }
        val o = post(
            "https://id.twitch.tv/oauth2/token",
            "client_id=${enc(clientId())}&scopes=${enc(SCOPES)}&device_code=${enc(code.deviceCode)}&grant_type=${enc("urn:ietf:params:oauth:grant-type:device_code")}"
        ) ?: return PollState.ERROR

        if (o.has("access_token")) {
            save(context, o)
            return PollState.OK
        }
        val message = if (o.has("message")) o.get("message").asString else ""
        if (message.contains("authorization_pending")) return PollState.PENDING
        if (message.contains("slow_down")) return PollState.SLOW_DOWN
        if (message.contains("denied")) return PollState.DENIED
        if (message.contains("invalid device code") || message.contains("expired")) return PollState.EXPIRED
        Log.w(TAG, "Unexpected response: $message")
        return PollState.ERROR
    }

    private fun save(context: Context, o: JsonObject) {
        val access = o.get("access_token").asString
        val expiresIn = if (o.has("expires_in")) o.get("expires_in").asLong else 0L
        val prefs = context.applicationContext.getSharedPreferences("User", 0)
        prefs.edit().apply {
            putString("TOKEN", access)
            if (o.has("refresh_token")) {
                putString("REFRESH_TOKEN", o.get("refresh_token").asString)
            }
            putLong("TOKEN_EXPIRES_AT", if (expiresIn > 0) System.currentTimeMillis() + expiresIn * 1000L else 0L)
            putBoolean("USER_LOGGED_OUT", false)
            apply()
        }
        AppState.accessToken = access
        SessionStore.updateToken(access)
        AppState.storedScopes = SCOPES.split(" ").map { it.trim() }
        Log.i(TAG, "Token saved, valid $expiresIn s")
    }

    @JvmStatic
    fun revoke(access: String?, refresh: String?) {
        for (token in listOfNotNull(access, refresh)) {
            if (token.isEmpty()) continue
            val body = "client_id=${enc(clientId())}&token=${enc(token)}".toRequestBody(FORM_MEDIA)
            val req = Request.Builder()
                .url("https://id.twitch.tv/oauth2/revoke")
                .post(body)
                .build()
            try {
                Http.client().newCall(req).execute().use { resp ->
                    Log.i(TAG, "Widerruf: HTTP ${resp.code}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Revoke failed: $e")
            }
        }
    }

    @Synchronized
    @JvmStatic
    fun refreshIfNeeded(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences("User", 0)
        val refresh = prefs.getString("REFRESH_TOKEN", "") ?: ""
        val expiresAt = prefs.getLong("TOKEN_EXPIRES_AT", 0)
        if (refresh.isEmpty() || expiresAt == 0L || (prefs.getString("TOKEN", "") ?: "").isEmpty()
            || expiresAt - System.currentTimeMillis() > REFRESH_MARGIN_MS
        ) {
            return false
        }
        return refreshNow(context.applicationContext, refresh)
    }

    @Synchronized
    @JvmStatic
    fun refreshOn401(failedAuthorizationHeader: String?): String? {
        val ctx = appContext ?: return null
        val current = AppState.accessToken
        val failedToken = if (failedAuthorizationHeader != null) {
            failedAuthorizationHeader.replaceFirst("^(?i)(Bearer|OAuth)\\s+".toRegex(), "").trim()
        } else {
            ""
        }
        if (!current.isNullOrEmpty() && current != failedToken) {
            return current
        }
        val prefs = ctx.getSharedPreferences("User", 0)
        val refresh = prefs.getString("REFRESH_TOKEN", "") ?: ""
        if (refresh.isEmpty()) {
            return null
        }
        return if (refreshNow(ctx, refresh)) AppState.accessToken else null
    }

    private fun refreshNow(context: Context, refresh: String): Boolean {
        val o = post(
            "https://id.twitch.tv/oauth2/token",
            "client_id=${enc(clientId())}&grant_type=refresh_token&refresh_token=${enc(refresh)}"
        )
        if (o != null && o.has("access_token")) {
            save(context, o)
            return true
        }
        val msg = if (o != null && o.has("message")) o.get("message").asString else "Netz"
        Log.w(TAG, "Refresh failed: $msg")
        return false
    }

    @JvmStatic
    fun startAutoRefresh(context: Context) {
        appContext = context.applicationContext
        synchronized(this) {
            if (scheduler == null) {
                val s = Executors.newSingleThreadScheduledExecutor()
                scheduler = s
                s.scheduleWithFixedDelay({
                    try {
                        appContext?.let { refreshIfNeeded(it) }
                    } catch (e: RuntimeException) {
                        Log.w(TAG, "Refresh", e)
                    }
                }, 0, 10, TimeUnit.MINUTES)
            } else {
                scheduler?.execute {
                    try {
                        appContext?.let { refreshIfNeeded(it) }
                    } catch (e: RuntimeException) {
                        Log.w(TAG, "Refresh", e)
                    }
                }
            }
        }
    }

    private fun enc(s: String): String {
        return try {
            URLEncoder.encode(s, "UTF-8")
        } catch (e: Exception) {
            s
        }
    }

    private fun post(url: String, form: String): JsonObject? {
        val body = form.toRequestBody(FORM_MEDIA)
        val req = Request.Builder()
            .url(url)
            .post(body)
            .build()
        return try {
            Http.client().newCall(req).execute().use { resp ->
                val text = resp.body?.string()
                if (text.isNullOrEmpty()) null else JsonParser().parse(text).asJsonObject
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP failed: $e")
            null
        }
    }
}
