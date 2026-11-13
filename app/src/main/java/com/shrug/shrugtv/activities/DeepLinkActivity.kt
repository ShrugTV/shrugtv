package com.shrug.shrugtv.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.shrug.shrugtv.util.AppLanguage

/**
 * The only entry from outside into the player ( item 4).
 *
 * Allowed: channel://com.shrug.shrugtv/startstream/<id>/<row> (launcher channels,
 * RecommendationWorker), shrugtv://stream/<login>, shrugtv://vod/<id>.
 */
class DeepLinkActivity : Activity() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        val target = targetFor(data)
        if (target != null) {
            startActivity(Intent(this, target).setAction(Intent.ACTION_VIEW).setData(data))
        } else {
            Log.w("DeepLink", "rejected: $data")
        }
        finish()
    }

    companion object {
        /** Which activity gets the link; null for anything that is not a known link. */
        @JvmStatic
        fun targetFor(data: Uri?): Class<*>? {
            if (data == null || data.scheme == null || data.host == null) {
                return null
            }
            val scheme = data.scheme
            val host = data.host
            if (scheme == "channel" && host == "com.shrug.shrugtv") {
                return PlayerActivity::class.java
            }
            if (scheme == "shrugtv" && host == "stream") {
                return PlayerActivity::class.java
            }
            if (scheme == "shrugtv" && host == "vod") {
                return PlayerActivity::class.java
            }
            return null
        }
    }
}
