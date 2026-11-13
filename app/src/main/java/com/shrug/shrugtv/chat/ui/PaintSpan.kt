package com.shrug.shrugtv.chat.ui

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.text.style.ReplacementSpan
import com.shrug.shrugtv.chat.seventv.SevenTvPaint
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a user name with its 7TV paint (gradient), like the 7TV extension. A ReplacementSpan
 * because the gradient needs the position and width of the name; names never wrap.
 */
class PaintSpan(private val paint: SevenTvPaint) : ReplacementSpan() {

    override fun getSize(p: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        if (fm != null) p.getFontMetricsInt(fm)
        return p.measureText(text, start, end).toInt()
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, p: Paint) {
        val width = p.measureText(text, start, end)
        val old = p.shader
        val oldColor = p.color
        if (paint.colors.size == 1) {
            p.color = paint.colors[0]
        } else {
            p.color = 0xFFFFFFFF.toInt()
            p.shader = shader(x, top.toFloat(), width, (bottom - top).toFloat())
        }
        canvas.drawText(text, start, end, x, y.toFloat(), p)
        p.shader = old
        p.color = oldColor
    }

    private fun shader(x: Float, y: Float, w: Float, h: Float): Shader {
        val mode = if (paint.repeat) Shader.TileMode.REPEAT else Shader.TileMode.CLAMP
        if (paint.radial) {
            return RadialGradient(x + w / 2, y + h / 2, maxOf(w, h) / 2, paint.colors, paint.positions, mode)
        }
        // CSS angle: 0deg points up, 90deg to the right
        val rad = Math.toRadians(paint.angle.toDouble())
        val dx = (sin(rad) * w / 2).toFloat()
        val dy = (-cos(rad) * h / 2).toFloat()
        val cx = x + w / 2
        val cy = y + h / 2
        return LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, paint.colors, paint.positions, mode)
    }
}

/** Hides text completely (the code of a zero-width emote that is drawn over the previous emote). */
class EmptySpan : ReplacementSpan() {
    override fun getSize(p: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?) = 0
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, p: Paint) = Unit
}
