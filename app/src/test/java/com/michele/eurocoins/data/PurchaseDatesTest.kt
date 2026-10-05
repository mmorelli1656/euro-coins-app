package com.michele.eurocoins.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun theCommemorativeDetailSaysOneDateOrHowManyDiffer() {
        assertNull(purchaseLine(emptyList()))
        assertNull(purchaseLine(listOf(null, null)))
        assertEquals("Bought 12 Mar 2026", purchaseLine(listOf(march12, march12, null)))
        assertEquals("Bought on 2 different dates", purchaseLine(listOf(march12, june3)))
        assertEquals(listOf("Bought 12 Mar 2026"), commemorativePurchaseLines(listOf(item(date = march12), item(CoinQuality.BU, march12))))
        assertEquals(emptyList<String>(), commemorativePurchaseLines(listOf(item())))
    }

    @Test
    fun aSingleRegularYearGetsOneLine() {
        val items = listOf(regular(2002, date = march12), regular(2002, CoinQuality.BU, date = march12))
        assertEquals(listOf("Bought 12 Mar 2026"), regularPurchaseLines(items))
    }

    @Test
    fun severalRegularYearsGetOneLineEachWhereThereIsADate() {
        val items = listOf(
            regular(2003, date = june3),
            regular(2002, date = march12),
            regular(2004), // senza data: nessuna riga
            regular(2002, variety = "EFS", date = march12),
        )
        assertEquals(
            listOf("2002 · Bought 12 Mar 2026", "2002 EFS · Bought 12 Mar 2026", "2003 · Bought 3 Jun 2026"),
            regularPurchaseLines(items),
        )
    }

    @Test
    fun noRegularDateMeansNoLine() {
        assertEquals(emptyList<String>(), regularPurchaseLines(listOf(regular(2002), regular(2003))))
    }
}
