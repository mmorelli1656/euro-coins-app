package com.michele.eurocoins.data.pro

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
}
