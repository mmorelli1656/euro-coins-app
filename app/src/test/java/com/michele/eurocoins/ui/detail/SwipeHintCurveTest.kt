package com.michele.eurocoins.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeHintCurveTest {
    private val samples = (0..2000).map { it / 2000f }

    @Test
    fun startsAndEndsAtRest() {
        assertEquals(0f, swipeHintBounce(0f), 1e-6f)
        assertEquals(0f, swipeHintBounce(1f), 1e-6f)
    }

    @Test
    fun neverGoesBelowRestNorAboveTheFirstPeak() {
        for (t in samples) {
            val v = swipeHintBounce(t)
            assertTrue("t=$t v=$v", v >= 0f)
            assertTrue("t=$t v=$v", v <= 1.0001f)
        }
    }

    @Test
    fun hasExactlyThreeBouncesThatShrink() {
        val peaks = mutableListOf<Float>()
        for (i in 1 until samples.size - 1) {
            val before = swipeHintBounce(samples[i - 1])
            val here = swipeHintBounce(samples[i])
            val after = swipeHintBounce(samples[i + 1])
            if (here > before && here >= after && here > 0.01f) peaks += here
        }
        assertEquals(3, peaks.size)
        assertTrue("peaks=$peaks", peaks[0] > peaks[1] && peaks[1] > peaks[2])
        assertEquals(1f, peaks[0], 0.01f)
        assertEquals(0.62f * 0.62f, peaks[1], 0.01f)
        assertEquals(0.62f * 0.62f * 0.62f * 0.62f, peaks[2], 0.01f)
    }

    @Test
    fun startIsGentleNotAJump() {
        // Velocità quasi nulla alla partenza: nessuno scatto iniziale.
        assertTrue(swipeHintBounce(0.01f) < 0.01f)
    }

    @Test
    fun noJumpBetweenConsecutiveSamples() {
        // Continua: tra due campioni vicini il valore non salta mai (nessuna pausa-e-ripartenza a scatti).
        for (i in 1 until samples.size) {
            val step = kotlin.math.abs(swipeHintBounce(samples[i]) - swipeHintBounce(samples[i - 1]))
            assertTrue("t=${samples[i]} step=$step", step < 0.01f)
        }
    }
}
