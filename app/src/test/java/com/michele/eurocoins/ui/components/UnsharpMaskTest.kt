package com.michele.eurocoins.ui.components

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsharpMaskTest {
    private fun gray(v: Int, alpha: Int = 255) = (alpha shl 24) or (v shl 16) or (v shl 8) or v
    private fun red(argb: Int) = (argb ushr 16) and 0xFF

    @Test
    fun `un'immagine uniforme non cambia`() {
        val img = IntArray(10 * 10) { gray(180) }
        assertArrayEquals(img, unsharpMask(img, 10, 10, 0.9f))
    }

    @Test
    fun `con intensita zero i pixel restano uguali`() {
        val img = IntArray(8 * 8) { gray(if (it % 8 < 4) 40 else 220) }
        assertArrayEquals(img, unsharpMask(img, 8, 8, 0f))
    }

    @Test
    fun `un bordo netto si accentua da entrambi i lati e lontano dal bordo non cambia`() {
        val w = 16
        val h = 4
        // Colonne 0-7 scure (60), 8-15 chiare (200).
        val img = IntArray(w * h) { gray(if (it % w < 8) 60 else 200) }
        val out = unsharpMask(img, w, h, 0.9f)
        val row = 1
        assertTrue("il lato scuro accanto al bordo si scurisce", red(out[row * w + 7]) < 60)
        assertTrue("il lato chiaro accanto al bordo si schiarisce", red(out[row * w + 8]) > 200)
        assertEquals("lontano dal bordo scuro", 60, red(out[row * w + 1]))
        assertEquals("lontano dal bordo chiaro", 200, red(out[row * w + 14]))
    }

    @Test
    fun `il risultato resta nel campo 0-255`() {
        val img = IntArray(8 * 8) { gray(if (it % 2 == 0) 0 else 255) }
        val out = unsharpMask(img, 8, 8, 2f)
        assertTrue(out.all { red(it) in 0..255 })
    }

    @Test
    fun `l'alfa non viene toccata`() {
        val img = IntArray(6 * 6) { gray(if (it % 6 < 3) 30 else 230, alpha = if (it < 12) 0 else 255) }
        val out = unsharpMask(img, 6, 6, 0.9f)
        for (i in img.indices) assertEquals((img[i] ushr 24), (out[i] ushr 24))
    }
}
