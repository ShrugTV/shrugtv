package com.shrug.shrugtv.model

import androidx.compose.runtime.Immutable

@Immutable
data class RaidNotification(
    val channel: String?,
    val targetDisplayName: String?,
    val targetUserId: String?
)
