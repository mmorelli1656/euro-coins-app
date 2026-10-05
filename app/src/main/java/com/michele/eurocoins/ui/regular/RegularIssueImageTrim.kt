package com.michele.eurocoins.ui.regular

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
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
    // Cambia con il comportamento: le trasformate vecchie in memoria non devono sopravvivere.
    override val cacheKey: String = "RegularIssueImageTrim:dark-background"

    private const val ALPHA_THRESHOLD = 32
    private const val WHITE_THRESHOLD = 245

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Sfondo scuro (solo France_1euro_2022): si tiene il disco della moneta e il resto diventa
        // trasparente, altrimenti sulla card bianca del dettaglio si vede un quadrato nero.
        val pixels = IntArray(input.width * input.height)
        input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        findCoinDisc(pixels, input.width, input.height)?.let { return coinOnTransparent(input, it) }
        val bounds = opaqueBounds(pixels, input.width, input.height) ?: return input
        if (bounds.width() == input.width && bounds.height() == input.height) return input
        return Bitmap.createBitmap(input, bounds.left, bounds.top, bounds.width(), bounds.height())
    }

    /**
     * Ritaglia il quadrato del disco e lo ridisegna come cerchio con il bordo anti-aliasing su fondo
     * trasparente. Il raggio perde 1,5 px: il bordo del jpeg verso il nero sfuma e lascerebbe un
     * filo scuro attorno alla moneta.
     */
    private fun coinOnTransparent(input: Bitmap, disc: CoinDisc): Bitmap {
        val side = (disc.radius * 2).toInt().coerceIn(1, minOf(input.width, input.height))
        val left = (disc.cx - side / 2f).toInt().coerceIn(0, input.width - side)
        val top = (disc.cy - side / 2f).toInt().coerceIn(0, input.height - side)
        val square = Bitmap.createBitmap(input, left, top, side, side)
        val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        Canvas(output).drawCircle(side / 2f, side / 2f, side / 2f - 1.5f, paint)
        return output
    }

    private fun isBackgroundPixel(argb: Int): Boolean {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha < ALPHA_THRESHOLD) return true
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return r >= WHITE_THRESHOLD && g >= WHITE_THRESHOLD && b >= WHITE_THRESHOLD
    }

    private fun opaqueBounds(pixels: IntArray, width: Int, height: Int): Rect? {
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
