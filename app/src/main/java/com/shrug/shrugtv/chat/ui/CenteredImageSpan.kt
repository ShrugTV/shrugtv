package com.shrug.shrugtv.chat.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan

/**
 * ImageSpan that centres the emote or badge vertically on the text line.
 */
class CenteredImageSpan(drawable: Drawable) : ImageSpan(drawable) {

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val d = drawable ?: return
        canvas.save()
        val fm = paint.fontMetricsInt
        val transY = (y + fm.descent - (fm.descent - fm.ascent) / 2) - (d.bounds.bottom - d.bounds.top) / 2
        canvas.translate(x, transY.toFloat())
        d.draw(canvas)
        canvas.restore()
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val d = drawable ?: return 0
        val bounds = d.bounds
        if (fm != null) {
            val pfm = paint.fontMetricsInt
            val mid = pfm.ascent + (pfm.descent - pfm.ascent) / 2
            val halfH = (bounds.bottom - bounds.top) / 2
            fm.ascent = mid - halfH
            fm.top = mid - halfH
            fm.bottom = mid + halfH
            fm.descent = mid + halfH
        }
        return bounds.right
    }
}
