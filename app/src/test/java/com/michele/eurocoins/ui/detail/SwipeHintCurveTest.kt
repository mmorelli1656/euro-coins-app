package com.michele.eurocoins.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeHintCurveTest {
    @Test
    fun startsAndEndsAtRest() {
        assertEquals(0f, swipeHintBump(0f), 1e-6f)
        assertEquals(0f, swipeHintBump(1f), 1e-6f)
    }

    @Test
    fun peaksAtTheMiddle() {
        assertEquals(1f, swipeHintBump(0.5f), 1e-6f)
    }

    @Test
    fun isSymmetricSoOutAndBackFeelTheSame() {
        for (t in listOf(0.1f, 0.25f, 0.4f)) assertEquals(swipeHintBump(t), swipeHintBump(1f - t), 1e-5f)
    }

    @Test
    fun risesWithoutAnyPauseOrDipOnTheWayOut() {
        var previous = swipeHintBump(0f)
        for (i in 1..50) {
            val value = swipeHintBump(i / 100f)
            assertTrue("t=${i / 100f}", value > previous)
            previous = value
        }
    }

    @Test
    fun startIsGentleNotAJump() {
        // Velocità quasi nulla alla partenza: nessuno scatto iniziale.
        assertTrue(swipeHintBump(0.01f) < 0.01f)
    }

    @Test
    fun neverGoesBelowRest() {
        for (i in -10..110) assertTrue(swipeHintBump(i / 100f) >= 0f)
    }
}
