package com.michele.eurocoins.ui.components

import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.YearOption
import org.junit.Assert.assertEquals
import org.junit.Test

/** Testi di conferma del tasto "Remove": dicono cosa si perde, in modo esatto. */
class RemoveMessageTest {
    @Test
    fun commemorativeNamesTheSavedFinishes() {
        assertEquals("This removes Standard for Yleisradio 100 years.", commemorativeRemovalMessage("Yleisradio 100 years", listOf(CoinQuality.STANDARD)))
        assertEquals("This removes Standard and BU for X.", commemorativeRemovalMessage("X", listOf(CoinQuality.BU, CoinQuality.STANDARD)))
        assertEquals("This removes Standard, BU and Proof for X.", commemorativeRemovalMessage("X", CoinQuality.entries))
    }

    @Test
    fun regularWithOneYearNamesThatYear() {
        assertEquals("This removes 2025 of 2 euro for Croatia · Series 1.", regularRemovalMessage("2 euro", "Croatia · Series 1", listOf(YearOption(2025))))
    }

    @Test
    fun regularWithSeveralYearsCountsAndListsThemInOrder() {
        val years = listOf(YearOption(2025), YearOption(2023), YearOption(2024), YearOption(2023))
        assertEquals(
            "This removes all 3 years of 2 euro for Croatia · Series 1: 2023, 2024 and 2025.",
            regularRemovalMessage("2 euro", "Croatia · Series 1", years),
        )
    }

    @Test
    fun theGreekEfsVarietyIsCountedAsItsOwnEntry() {
        val years = listOf(YearOption(2002), YearOption(2002, "EFS"))
        assertEquals(
            "This removes all 2 years of 1 euro for Greece · Series 1: 2002 and 2002 EFS.",
            regularRemovalMessage("1 euro", "Greece · Series 1", years),
        )
    }

    @Test
    fun aLongListIsAbbreviatedInsteadOfBecomingAWall() {
        val years = (2010..2021).map { YearOption(it) }
        assertEquals(
            "This removes all 12 years of 5 cent for Italy · Series 1: 2010, 2011, 2012, 2013, 2014, 2015…",
            regularRemovalMessage("5 cent", "Italy · Series 1", years),
        )
    }
}
