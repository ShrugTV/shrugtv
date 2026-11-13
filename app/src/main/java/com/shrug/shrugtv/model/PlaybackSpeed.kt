package com.shrug.shrugtv.model

import androidx.compose.runtime.Immutable

@Immutable
data class PlaybackSpeed(
    val label: String,
    val speed: Float
)
