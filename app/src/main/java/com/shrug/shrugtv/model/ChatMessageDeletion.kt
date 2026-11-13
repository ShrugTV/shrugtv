package com.shrug.shrugtv.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessageDeletion(
    val channel: String?,
    val targetMessageId: String?
)
