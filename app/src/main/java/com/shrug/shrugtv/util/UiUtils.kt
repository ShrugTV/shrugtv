package com.shrug.shrugtv.util

import android.content.Context
import android.util.DisplayMetrics
import com.shrug.shrugtv.database.AppDatabase
import com.shrug.shrugtv.model.Movie
import kotlin.math.roundToInt

/**
 * UI helpers for VOD resume positions and density conversion (replaces decompiled UiUtils.java,
 * ).
 */
object UiUtils {

    @JvmStatic
    fun applySavedVodPositions(context: Context, list: ArrayList<Movie>) {
        val all = AppDatabase.getInstance(context).vodInfoDao().all ?: return
        val byBroadcastId = all.associateBy { it.broadcastId }
        for (movie in list) {
            val vodInfo = byBroadcastId[movie.videoId] ?: continue
            movie.resumePositionText = vodInfo.positionText
            movie.setResumePositionSeconds(vodInfo.position)
        }
    }

    @JvmStatic
    fun dpToPx(dp: Float, context: Context): Int =
        (dp * context.resources.displayMetrics.density).roundToInt()

    @JvmStatic
    fun dpToPx(dp: Float, displayMetrics: DisplayMetrics): Int =
        (dp * displayMetrics.density).roundToInt()

    @JvmStatic
    fun dpToPx(dp: Int, context: Context): Int =
        (dp * context.resources.displayMetrics.density).roundToInt()

    @JvmStatic
    fun dpToPx(dp: Int, displayMetrics: DisplayMetrics): Int =
        (dp * displayMetrics.density).roundToInt()
}
