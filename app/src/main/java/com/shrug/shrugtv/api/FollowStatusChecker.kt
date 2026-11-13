package com.shrug.shrugtv.api

import android.content.Context
import com.google.gson.JsonObject
import com.shrug.shrugtv.R
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.net.ResultCallback
import com.shrug.shrugtv.telemetry.CrashReporter

/**
 * Checks whether the current user follows a broadcaster via Helix (replaces decompiled
 * FollowStatusChecker.java).
 */
object FollowStatusChecker {

    fun interface Callback {
        fun onFollowStatus(follows: Boolean, iconRes: Int)
    }

    @JvmStatic
    fun check(context: Context?, broadcasterId: String, callback: Callback) {
        HelixApi.getFollowRelation(context, broadcasterId, ResultCallback { _, json ->
            onResponse(callback, json)
        })
    }

    private fun onResponse(callback: Callback, jsonObject: JsonObject?) {
        if (jsonObject == null) return
        try {
            val data = jsonObject.getAsJsonArray("data") ?: return
            if (data.size() > 0) {
                callback.onFollowStatus(true, R.drawable.ic_follow_fill)
            } else {
                callback.onFollowStatus(false, R.drawable.ic_follow_outline)
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }
}
