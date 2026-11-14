package com.shrug.shrugtv.ui.tv.components

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.shrug.shrugtv.R
import com.shrug.shrugtv.activities.PlayerActivity
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie

/**
 * Where a click leads, the same for all Compose screens: stream → player, VOD →
 * VOD player, channel → channel page, game → category. Channel page and category are both Compose since
 * 2026-10-22; [channelTarget] and [categoryTarget] remain as the single place for them.
 */
object Destinations {
    private const val TAG = "Ziele"

    var channelTarget: Class<out Activity> = com.shrug.shrugtv.ui.tv.channel.ChannelActivity::class.java
    var categoryTarget: Class<out Activity> = com.shrug.shrugtv.ui.tv.category.CategoryActivity::class.java

    private fun movieExtra(a: Activity) = a.getString(R.string.movie)

    fun open(a: Activity, m: Movie) {
        when (m.itemType) {
            Enums.ContentType.STREAM -> watch(a, m)
            Enums.ContentType.CHANNEL -> channel(a, m)
            Enums.ContentType.GAME -> category(a, m)
            else -> {
                if (m.isClip) com.shrug.shrugtv.clips.ClipsSeen.mark(a.applicationContext, m.clipSlug)
                a.startActivity(Intent(a, PlayerActivity::class.java).putExtra(movieExtra(a), m).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            }
        }
    }

    fun watch(a: Activity, m: Movie, multiview: Movie? = null) {
        val i = Intent(a, PlayerActivity::class.java).putExtra(movieExtra(a), m).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        if (multiview != null) i.putExtra(PlayerActivity.EXTRA_MULTIVIEW, multiview)
        a.startActivity(i)
    }

    /** Long press on a card: context menu (follow, favourite, filter …). */
    fun contextMenu(a: Activity, m: Movie) = com.shrug.shrugtv.ui.tv.dialogs.ContextMenuActivity.open(a, m)

    fun channel(a: Activity, m: Movie) {
        a.startActivity(Intent(a, channelTarget).putExtra(movieExtra(a), m))
    }

    fun category(a: Activity, m: Movie) {
        val i = Intent(a, categoryTarget).putExtra("game", m).putExtra("activity-title", "${m.title} (${m.viewerCountText})")
        try {
            i.putExtra("background-url", java.net.URI(m.boxArtUrl))
        } catch (e: Exception) {
            Log.w(TAG, "Box-Art", e)
        }
        a.startActivity(i)
    }
}
