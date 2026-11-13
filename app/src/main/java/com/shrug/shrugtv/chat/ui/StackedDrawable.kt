package com.shrug.shrugtv.chat.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable

/**
 * An emote with zero-width emotes drawn over it (7TV/BTTV overlays such as hats or rain), like the
 * 7TV extension stacks them. Animated layers keep animating: start/stop and redraws are passed on.
 */
class StackedDrawable(private val layers: List<Drawable>) : Drawable(), Animatable, Drawable.Callback {

    init {
        layers.forEach { it.callback = this }
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        // The base keeps its bounds; overlays are centred on it with their own aspect ratio
        layers.forEachIndexed { i, d ->
            if (i == 0) {
                d.bounds = bounds
            } else {
                val h = bounds.height()
                val w = if (d.intrinsicHeight > 0) d.intrinsicWidth * h / d.intrinsicHeight else bounds.width()
                val left = bounds.left + (bounds.width() - w) / 2
                d.setBounds(left, bounds.top, left + w, bounds.bottom)
            }
        }
    }

    override fun getIntrinsicWidth() = layers.first().bounds.width().takeIf { it > 0 } ?: layers.first().intrinsicWidth
    override fun getIntrinsicHeight() = layers.first().bounds.height().takeIf { it > 0 } ?: layers.first().intrinsicHeight

    override fun draw(canvas: Canvas) = layers.forEach { it.draw(canvas) }
    override fun setAlpha(alpha: Int) = layers.forEach { it.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) = layers.forEach { it.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun start() = layers.forEach { (it as? Animatable)?.start() }
    override fun stop() = layers.forEach { (it as? Animatable)?.stop() }
    override fun isRunning() = layers.any { (it as? Animatable)?.isRunning == true }

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()
    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = scheduleSelf(what, `when`)
    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)
}
