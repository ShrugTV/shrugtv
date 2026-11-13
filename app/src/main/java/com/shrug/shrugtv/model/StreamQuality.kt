package com.shrug.shrugtv.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class StreamQuality(
    val name: String?,
    val url: String?
) : java.io.Serializable
