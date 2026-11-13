package com.shrug.shrugtv.notification

/**
 * Overlay and Toast animation presets.
 */
enum class OverlayAnimation {
    NONE,
    FADE_IN,
    ZOOM_IN,
    SLIDE_IN,
    SLIDE_UP
}

/**
 * Settings configuration for in-app toast and live notifications.
 */
open class NotificationSettings(
    val textSize: Float = 20.0f,
    val position: Int = 1,
    val durationMs: Long = 3000L
)

/**
 * Extended notification settings including animation style.
 */
class OverlaySettings(
    textSize: Float = 20.0f,
    position: Int = 1,
    durationMs: Long = 3000L,
    val animation: Int = 1
) : NotificationSettings(textSize, position, durationMs)
