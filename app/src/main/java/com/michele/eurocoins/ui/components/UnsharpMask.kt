package com.michele.eurocoins.ui.components

/**
 * Unsharp mask su pixel ARGB: `out = in + amount · (in − sfocata)` su ciascun canale colore, con la
 * sfocata = due passate del kernel gaussiano 3×3 (separabile, [1 2 1] per [1 2 1] / 16: sigma ~1,2
 * px). L'alfa resta com'è. Logica pura, senza Android, per poterla testare in JVM; la usa
 * [ThumbnailSharpen]. Valori di `amount` sensati: 0.6-1.0; oltre ~1.2 compare un alone sui bordi.
 */
internal fun unsharpMask(argb: IntArray, width: Int, height: Int, amount: Float): IntArray {
    require(argb.size == width * height) { "argb.size deve essere width * height" }
    val out = IntArray(argb.size)
    val channel = FloatArray(argb.size)
    for (a in argb.indices) out[a] = argb[a] and ALPHA_MASK
    for (shift in intArrayOf(16, 8, 0)) {
        for (i in argb.indices) channel[i] = ((argb[i] ushr shift) and 0xFF).toFloat()
        val blurred = blur3x3(blur3x3(channel, width, height), width, height)
        for (i in argb.indices) {
            val v = channel[i] + amount * (channel[i] - blurred[i])
            out[i] = out[i] or (v.coerceIn(0f, 255f).toInt() shl shift)
        }
    }
    return out
}

private const val ALPHA_MASK = 0xFF shl 24

/** Una passata del kernel [1 2 1]/4 in orizzontale e poi in verticale, bordi replicati. */
private fun blur3x3(src: FloatArray, width: Int, height: Int): FloatArray {
    val tmp = FloatArray(src.size)
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            val l = src[row + maxOf(x - 1, 0)]
            val c = src[row + x]
            val r = src[row + minOf(x + 1, width - 1)]
            tmp[row + x] = (l + 2f * c + r) / 4f
        }
    }
    val dst = FloatArray(src.size)
    for (y in 0 until height) {
        val up = maxOf(y - 1, 0) * width
        val mid = y * width
        val down = minOf(y + 1, height - 1) * width
        for (x in 0 until width) {
            dst[mid + x] = (tmp[up + x] + 2f * tmp[mid + x] + tmp[down + x]) / 4f
        }
    }
    return dst
}
