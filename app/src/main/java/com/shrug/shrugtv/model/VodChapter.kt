package com.shrug.shrugtv.model

import androidx.compose.runtime.Immutable
import com.shrug.shrugtv.util.TimeUtil

@Immutable
data class VodChapter(
    val name: String,
    val positionMs: Long,
    val durationMs: Long,
    val imageUrl: String
) {
    val durationText: String
        get() = TimeUtil.hms(durationMs / 1000)
}
