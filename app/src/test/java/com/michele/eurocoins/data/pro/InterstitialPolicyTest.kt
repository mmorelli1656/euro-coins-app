package com.michele.eurocoins.data.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialPolicyTest {
    @Test
    fun needsBothEnoughCoinsAndEnoughTime() {
        assertTrue(isInterstitialDue(INTERSTITIAL_EVERY_COINS, INTERSTITIAL_MIN_GAP_MS))
        assertTrue(isInterstitialDue(INTERSTITIAL_EVERY_COINS + 5, INTERSTITIAL_MIN_GAP_MS * 4))
    }

    @Test
    fun fastScrollerIsNotInterruptedBeforeTheGap() {
        assertFalse(isInterstitialDue(INTERSTITIAL_EVERY_COINS * 3, INTERSTITIAL_MIN_GAP_MS - 1))
    }

    @Test
    fun slowBrowserIsNotInterruptedBeforeEnoughCoins() {
        assertFalse(isInterstitialDue(INTERSTITIAL_EVERY_COINS - 1, INTERSTITIAL_MIN_GAP_MS * 10))
    }

    @Test
    fun startOfSessionIsQuiet() {
        assertFalse(isInterstitialDue(0, 0))
    }

    @Test
    fun oneSeriesOfEightCoinsIsEnoughToTrigger() {
        // Una serie Regular Issues ha 8 monete: la soglia deve stare sotto, o chi ne guarda una sola non vede mai l'annuncio.
        assertTrue(INTERSTITIAL_EVERY_COINS < 8)
    }

    @Test
    fun elapsedTimeIsPlainDifference() {
        assertEquals(5_000L, elapsedSince(1_000L, 6_000L))
    }

    @Test
    fun clockSetBackwardsRestartsTheGapInsteadOfBlockingAds() {
        // L'orario del telefono portato indietro: l'ultimo annuncio sembra nel futuro, il tempo trascorso vale 0.
        assertEquals(0L, elapsedSince(10_000L, 4_000L))
        assertFalse(isInterstitialDue(INTERSTITIAL_EVERY_COINS, elapsedSince(10_000L, 4_000L)))
    }
}
