package com.michele.eurocoins.ui.regular

import android.graphics.Bitmap
import android.graphics.Rect
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Le foto EC delle serie divisionali non sono ritagliate in modo uniforme a filo moneta: a
 * differenza delle foto BCE delle commemorative (già a filo, vedi `PHOTO_ZOOM` in
 * `HomeScreen.kt`), qui il margine trasparente attorno alla moneta varia da file a file — misurato
 * da 0 (moneta che tocca i bordi) a oltre il 10% per lato su alcuni file. Senza questo passaggio,
 * `ContentScale.Crop` riempie comunque il riquadro ma CON quel margine incluso: la moneta appare
 * più piccola del cerchio che la contiene, con un alone del colore di sfondo intorno — e la
 * dimensione apparente varia da moneta a moneta invece di essere uniforme.
 *
 * Ritaglia il bitmap al rettangolo dei pixel non trasparenti e non quasi-bianchi, prima che Coil
 * applichi `contentScale`. Non risolve la risoluzione di partenza (alcuni file sono anche solo
 * 62×62 o 100×100 px, contro i 270×270 delle foto BCE): quello resta un limite della fonte.
 */
object RegularIssueImageTrim : Transformation() {
    override val cacheKey: String = "RegularIssueImageTrim"

    private const val ALPHA_THRESHOLD = 32
    private const val WHITE_THRESHOLD = 245

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val bounds = opaqueBounds(input) ?: return input
        if (bounds.width() == input.width && bounds.height() == input.height) return input
        return Bitmap.createBitmap(input, bounds.left, bounds.top, bounds.width(), bounds.height())
    }

    private fun isBackgroundPixel(argb: Int): Boolean {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha < ALPHA_THRESHOLD) return true
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return r >= WHITE_THRESHOLD && g >= WHITE_THRESHOLD && b >= WHITE_THRESHOLD
    }

    private fun opaqueBounds(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                if (!isBackgroundPixel(pixels[rowOffset + x])) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return null
        return Rect(minX, minY, maxX + 1, maxY + 1)
    }
}
