package com.shrug.shrugtv.channels

/**
 * Android TV recommendation channel wrapper.
 */
data class RecommendationChannel(
    val channelId: Long,
    val isBrowsable: Boolean
) {
    val isValid: Boolean
        get() = channelId > -1 && isBrowsable
}
