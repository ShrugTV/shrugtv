package com.shrug.shrugtv.remotesettings

import android.content.Context
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.session.AppState

/**
 * Fixed parameters for the GQL query of the playback token.
 */
class RemoteSettings {

    companion object {
        private const val PLATFORM = "android"
        private const val PLAYER_BACKEND = "mediaplayer"
        private const val PLAYER_TYPE = "mobile"

        @JvmStatic
        fun load(context: Context) {
            AppState.remoteSettings = RemoteSettings()
        }

        private fun adTestEnabled(): Boolean {
            if (!BuildConfig.DEBUG) {
                return false
            }
            return try {
                val value = Class.forName("android.os.SystemProperties")
                    .getMethod("get", String::class.java).invoke(null, "debug.ttvh.adtest")
                "1" == value
            } catch (e: Exception) {
                false
            }
        }
    }

    val platform: String
        get() = if (adTestEnabled()) "web" else PLATFORM

    val playerBackend: String
        get() = PLAYER_BACKEND

    val playerType: String
        get() = if (adTestEnabled()) "site" else PLAYER_TYPE

    /** No foreign token any more; callers ask anonymously. */
    val streamToken: String?
        get() = null

    val isLoadedFromServer: Boolean
        get() = false
}
