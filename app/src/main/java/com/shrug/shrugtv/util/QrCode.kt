package com.shrug.shrugtv.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import java.util.EnumMap

object QrCode {
    /** Black on white, square with edge length size, border border modules. */
    @JvmStatic
    @Throws(WriterException::class)
    fun generate(text: String, size: Int, border: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = border
        val m = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val b = m.width
        val h = m.height
        val pixel = IntArray(b * h)
        for (y in 0 until h) {
            val row = y * b
            for (x in 0 until b) {
                pixel[row + x] = if (m[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val image = Bitmap.createBitmap(b, h, Bitmap.Config.ARGB_8888)
        image.setPixels(pixel, 0, b, 0, 0, b, h)
        return image
    }
}
