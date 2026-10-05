package com.michele.eurocoins.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseDatesTest {

    private val march12 = LocalDate.of(2026, 3, 12).toEpochDay()
    private val june3 = LocalDate.of(2026, 6, 3).toEpochDay()

    private fun item(quality: CoinQuality = CoinQuality.STANDARD, date: Long? = null) =
        CollectionItem(coinKey = "k", quality = quality, purchasedOn = date, anno = 2025, paese = "Andorra", tema = "T", addedAt = 1L)

    private fun regular(anno: Int, quality: CoinQuality = CoinQuality.STANDARD, variety: String = "", date: Long? = null) =
        RegularCollectionItem(seriesKey = "Belgio|1", taglio = "1 cent", anno = anno, quality = quality, variety = variety, purchasedOn = date, paese = "Belgio", addedAt = 1L)

    @Test
    fun theDateIsShownInEnglishWithoutLeadingZero() {
        assertEquals("12 Mar 2026", formatPurchaseDate(march12))
        assertEquals("3 Jun 2026", formatPurchaseDate(june3))
        assertEquals("1 Jan 1999", formatPurchaseDate(LocalDate.of(1999, 1, 1).toEpochDay()))
    }

    @Test
    fun thePickerMillisRoundTripToTheSameDay() {
        // Il selettore di Material lavora in millisecondi UTC a mezzanotte: stesso giorno in ogni fuso.
        assertEquals(march12, pickerMillisToEpochDay(epochDayToPickerMillis(march12)))
        // Anche un orario qualunque dello stesso giorno ricade in quel giorno.
        assertEquals(march12, pickerMillisToEpochDay(epochDayToPickerMillis(march12) + 86_399_999L))
        // Prima dell'epoca Unix (non serve alle monete, ma il floor deve reggere).
        assertEquals(-1L, pickerMillisToEpochDay(-1L))
    }

    @Test
    fun onlyDaysFrom1999UpToTodayAreSelectable() {
        val today = LocalDate.of(2026, 10, 5)
        assertTrue(isSelectablePurchaseDay(today.toEpochDay(), today))
        assertTrue(isSelectablePurchaseDay(LocalDate.of(1999, 1, 1).toEpochDay(), today))
        assertFalse(isSelectablePurchaseDay(today.toEpochDay() + 1, today)) // niente date future
        assertFalse(isSelectablePurchaseDay(LocalDate.of(1998, 12, 31).toEpochDay(), today)) // prima dell'euro
    }

    @Test
    fun eachFinishHasItsOwnDateLabel() {
        // Standard e BU della stessa moneta comprate in giorni diversi: ognuna dice il suo, nessuna "eredita".
        val items = listOf(item(CoinQuality.STANDARD, march12), item(CoinQuality.BU, june3), item(CoinQuality.PROOF))
        assertEquals(
            listOf("12 Mar 2026", "3 Jun 2026", "No date"),
            items.map { purchaseDateLabel(it.purchasedOn) },
        )
    }

    @Test
    fun theSecondLineAppearsOnlyIfSomeFinishHasADate() {
        assertFalse(anyPurchaseDate(emptyList()))
        assertFalse(anyPurchaseDate(listOf(null, null)))
        assertTrue(anyPurchaseDate(listOf(null, march12)))
        // Regular: la stessa finitura in due annate ha una data per annata.
        val items = listOf(regular(2002, date = march12), regular(2003), regular(2002, CoinQuality.BU, date = june3))
        assertEquals(
            listOf("12 Mar 2026", "No date", "3 Jun 2026"),
            items.map { purchaseDateLabel(it.purchasedOn) },
        )
    }
}
