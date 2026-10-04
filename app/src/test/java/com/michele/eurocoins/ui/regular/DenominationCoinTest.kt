package com.michele.eurocoins.ui.regular

import com.michele.eurocoins.data.REGULAR_DENOMINATIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DenominationCoinTest {

    @Test
    fun everyDenominationHasACoin() {
        for (taglio in REGULAR_DENOMINATIONS) assertNotNull("manca la moneta di $taglio", coinSpecFor(taglio))
        assertNull(coinSpecFor("3 euro"))
    }

    @Test
    fun theTwoEuroIsFullSizeAndTheOneCentIsAboutTwoThirds() {
        assertEquals(1f, coinScaleFor("2 euro")!!, 0.0001f)
        assertEquals(0.63f, coinScaleFor("1 cent")!!, 0.005f)
        assertTrue(REGULAR_DENOMINATIONS.all { coinScaleFor(it)!! <= 1f })
    }

    @Test
    fun sizesAreRealNotInValueOrder() {
        // Le monete vere non crescono col valore: la 5 cent è più grande della 10 cent, la 50 cent più della 1 euro.
        assertTrue(coinScaleFor("5 cent")!! > coinScaleFor("10 cent")!!)
        assertTrue(coinScaleFor("50 cent")!! > coinScaleFor("1 euro")!!)
    }

    @Test
    fun metalsMatchTheRealCoins() {
        assertEquals(CoinMetal.COPPER, coinSpecFor("2 cent")!!.metal)
        assertEquals(CoinMetal.NORDIC_GOLD, coinSpecFor("20 cent")!!.metal)
        assertEquals(CoinMetal.BIMETAL_GOLD_RING, coinSpecFor("1 euro")!!.metal)
        assertEquals(CoinMetal.BIMETAL_SILVER_RING, coinSpecFor("2 euro")!!.metal)
    }
}
