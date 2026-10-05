package com.michele.eurocoins.ui.regular

import kotlin.math.max

/**
 * Il disco della moneta dentro una foto con lo SFONDO SCURO: centro e raggio in pixel.
 *
 * Quasi tutte le foto della BCE hanno lo sfondo bianco e appoggiano bene sulla card bianca del
 * dettaglio; `France_1euro_2022.jpg` (1 euro della serie francese 2022, e per eredità della 2024)
 * è l'unica delle 295 con lo sfondo NERO (angoli a 0,0,0, misurato il 2026-10-05): sulla card bianca
 * compariva come un quadrato nero attorno alla moneta, che nell'elenco non si vede solo perché la
 * miniatura è ritagliata a cerchio. Il rilevamento è sui quattro angoli, quindi le foto a sfondo
 * bianco (e quelle ritagliate a filo, dove la moneta tocca i bordi ma gli angoli restano bianchi)
 * non sono toccate.
 */
internal data class CoinDisc(val cx: Float, val cy: Float, val radius: Float)

/** Sotto questa luminanza (0-255) un pixel è sfondo scuro. Il bordo della moneta più scuro è ~65. */
private const val DARK_LUMINANCE = 30

private fun luminance(argb: Int): Float {
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

private fun isDark(argb: Int): Boolean = ((argb ushr 24) and 0xFF) > 200 && luminance(argb) < DARK_LUMINANCE

/** `true` se tutti e quattro gli angoli sono opachi e scuri. */
internal fun hasDarkBackground(pixels: IntArray, width: Int, height: Int): Boolean {
    if (width < 2 || height < 2) return false
    return isDark(pixels[0]) &&
        isDark(pixels[width - 1]) &&
        isDark(pixels[(height - 1) * width]) &&
        isDark(pixels[height * width - 1])
}

/**
 * Il disco della moneta in una foto a sfondo scuro: il rettangolo dei pixel NON scuri ha come
 * diametro il lato più lungo (la moneta è tonda, e il bordo in alto e in basso può essere più scuro
 * di quello a sinistra e a destra: nella foto francese 537 px in larghezza e 527 in altezza) e come
 * centro il suo centro. Null se la foto non ha lo sfondo scuro o non c'è nulla di chiaro.
 */
internal fun findCoinDisc(pixels: IntArray, width: Int, height: Int): CoinDisc? {
    if (!hasDarkBackground(pixels, width, height)) return null
    var minX = width
    var maxX = -1
    var minY = height
    var maxY = -1
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            if (!isDark(pixels[row + x])) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    if (maxX < minX || maxY < minY) return null
    val diameter = max(maxX - minX + 1, maxY - minY + 1)
    return CoinDisc(
        cx = (minX + maxX + 1) / 2f,
        cy = (minY + maxY + 1) / 2f,
        radius = diameter / 2f,
    )
}
