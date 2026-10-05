package com.michele.eurocoins.ui.regular

import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinDiscDetectionTest {

    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val gold = 0xFFD8C27A.toInt()

    /** Foto sintetica: sfondo [bg], un disco pieno [coin] di raggio [r] centrato in ([cx], [cy]). */
    private fun photo(size: Int, bg: Int, coin: Int, cx: Float, cy: Float, r: Float): IntArray =
        IntArray(size * size) { i ->
            val x = i % size
            val y = i / size
            if (hypot(x + 0.5f - cx, y + 0.5f - cy) <= r) coin else bg
        }

    @Test
    fun blackBackgroundIsDetectedAndTheDiscIsFound() {
        val p = photo(540, black, gold, 270f, 270f, 268f)
        assertTrue(hasDarkBackground(p, 540, 540))
        val disc = findCoinDisc(p, 540, 540)
        assertNotNull(disc)
        assertEquals(270f, disc!!.cx, 1f)
        assertEquals(270f, disc.cy, 1f)
        assertEquals(268f, disc.radius, 1.5f)
    }

    @Test
    fun anOffCenterDiscWithMarginsKeepsItsOwnCenterAndRadius() {
        val p = photo(300, black, gold, 120f, 150f, 80f)
        val disc = findCoinDisc(p, 300, 300)!!
        assertEquals(120f, disc.cx, 1f)
        assertEquals(150f, disc.cy, 1f)
        assertEquals(80f, disc.radius, 1.5f)
    }

    @Test
    fun whiteBackgroundPhotosAreNotTouched() {
        val p = photo(540, white, gold, 270f, 270f, 268f)
        assertFalse(hasDarkBackground(p, 540, 540))
        assertNull(findCoinDisc(p, 540, 540))
    }

    @Test
    fun aCoinThatTouchesTheEdgesOnAWhiteFrameIsNotTakenForDark() {
        // foto ritagliata a filo (San Marino 2017...): la moneta tocca i bordi, gli angoli restano chiari
        val p = photo(245, white, 0xFF202020.toInt(), 122.5f, 122.5f, 122f)
        assertFalse(hasDarkBackground(p, 245, 245))
    }

    @Test
    fun oneDarkCornerIsNotEnough() {
        val p = photo(100, white, gold, 50f, 50f, 40f)
        p[0] = black
        assertFalse(hasDarkBackground(p, 100, 100))
    }

    @Test
    fun anAllBlackImageHasNoDisc() {
        assertNull(findCoinDisc(IntArray(50 * 50) { black }, 50, 50))
    }

    @Test
    fun aTransparentCornerIsNotDarkBackground() {
        // gli angoli trasparenti (png delle foto EC) restano gestiti dal ritaglio per alpha, non da qui
        val p = IntArray(40 * 40) { 0x00000000 }
        assertFalse(hasDarkBackground(p, 40, 40))
    }

    @Test
    fun theRealFrenchPhotoMeasurements() {
        // misurato nel browser su France_1euro_2022.jpg: bordo chiaro da x 1 a 537 e da y 6 a 532 (lum >= 30):
        // un disco di ~537 px di diametro centrato in ~(269, 269) su 540x540
        val p = photo(540, black, gold, 269.5f, 269f, 268.5f)
        for (y in 0 until 5) for (x in 0 until 540) p[y * 540 + x] = black // il bordo alto, piu' scuro
        val disc = findCoinDisc(p, 540, 540)!!
        assertEquals(537f, disc.radius * 2, 3f)
        assertEquals(269.5f, disc.cx, 1.5f)
    }
}
