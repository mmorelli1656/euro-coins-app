package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegularSeriesDenominationsTest {

    private val all: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun of(paese: String, ordine: Int): List<SeriesDenomination> {
        val forCountry = all.filter { it.paese == paese }
        return denominationsOf(forCountry, forCountry.first { it.ordineCronologico == ordine })
    }

    /** taglio -> ordine della serie di origine, come lo vede l'utente. */
    private fun origins(list: List<SeriesDenomination>) = list.associate { it.image.taglio to it.series.ordineCronologico }

    @Test
    fun franceSecondSeriesKeepsTheFullSet() {
        val d = of("Francia", 2)
        assertEquals(8, d.size)
        // 1 e 2 euro nuovi (2022); i 10-20-50 cent "seminatore" valgono fino al 2023, cioè dentro la
        // serie 2022; 1-2-5 cent mai cambiati
        assertEquals(
            mapOf(
                "2 euro" to 2, "1 euro" to 2,
                "50 cent" to 1, "20 cent" to 1, "10 cent" to 1,
                "5 cent" to 1, "2 cent" to 1, "1 cent" to 1,
            ),
            origins(d),
        )
        assertEquals(setOf("2 euro", "1 euro"), d.filter { !it.inherited }.map { it.image.taglio }.toSet())
    }

    @Test
    fun franceThirdSeriesTakesEachDenominationFromItsLatestDesign() {
        val d = of("Francia", 3)
        assertEquals(8, d.size)
        assertEquals(
            mapOf(
                "50 cent" to 3, "20 cent" to 3, "10 cent" to 3,
                "2 euro" to 2, "1 euro" to 2, // il disegno 2022 NON è finito
                "5 cent" to 1, "2 cent" to 1, "1 cent" to 1,
            ),
            origins(d),
        )
    }

    @Test
    fun spainThirdSeriesInheritsFromTheModifiedSeries() {
        val d = of("Spagna", 3)
        assertEquals(8, d.size)
        assertEquals(setOf(3), d.filter { !it.inherited }.map { it.series.ordineCronologico }.toSet())
        // dalla serie 2 (2010), non dalla 1 (2002): è la più recente
        assertEquals(setOf(2), d.filter { it.inherited }.map { it.series.ordineCronologico }.toSet())
    }

    @Test
    fun seriesWithoutOwnImagesInheritsNothing() {
        // Vaticano 2026: i tagli cambiano tutti, e non c'è ancora una foto
        assertTrue(of("Città del Vaticano", 6).isEmpty())
    }

    @Test
    fun countriesThatAlreadyListEightDenominationsAreUntouched() {
        for (series in all) {
            val d = denominationsOf(all.filter { it.paese == series.paese }, series)
            if (series.immagini.isEmpty()) continue
            assertEquals("${series.paese} #${series.ordineCronologico}", 8, d.size)
            assertEquals(8, d.map { it.image.taglio }.toSet().size)
            if (series.immagini.size == 8) assertTrue(d.none { it.inherited })
        }
    }
}

class RegularSeriesWindowTest {

    private val all: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun denom(paese: String, ordine: Int, taglio: String): SeriesDenomination {
        val forCountry = all.filter { it.paese == paese }
        return denominationsOf(forCountry, forCountry.first { it.ordineCronologico == ordine }).first { it.image.taglio == taglio }
    }

    @Test
    fun franceFiveCentIsSplitIntoNonOverlappingWindows() {
        val s1 = denom("Francia", 1, "5 cent").image
        val s2 = denom("Francia", 2, "5 cent").image
        val s3 = denom("Francia", 3, "5 cent").image
        assertEquals(1999 to 2021, s1.annoInizio to s1.annoFine)
        assertEquals(2022 to 2023, s2.annoInizio to s2.annoFine) // non dal 1999 della serie 1
        assertEquals(2024 to null, s3.annoInizio to s3.annoFine)
        assertTrue(s2.tirature.all { it.anno in 2022..2023 })
        assertTrue(s3.tirature.all { it.anno >= 2024 })
        assertTrue(s1.tirature.all { it.anno <= 2021 })
    }

    @Test
    fun franceTwoEuroOfTheSecondSeriesStopsWhereTheThirdBegins() {
        val s2 = denom("Francia", 2, "2 euro")
        val s3 = denom("Francia", 3, "2 euro")
        assertEquals(false, s2.inherited)
        assertEquals(2022 to 2023, s2.image.annoInizio to s2.image.annoFine)
        assertEquals(true, s3.inherited)
        assertEquals(2024 to null, s3.image.annoInizio to s3.image.annoFine)
        assertEquals(2, s3.series.ordineCronologico) // la moneta resta quella della serie 2
    }

    @Test
    fun explicitEndsAreKept() {
        // il Vaticano 2005 appartiene alle serie 1 e 2: la fine esplicita della serie 1 non si taglia
        val jp2 = denom("Città del Vaticano", 1, "2 euro").image
        assertEquals(2005, jp2.annoFine)
        // 10 cent francese serie 1: finito nel 2023, ma dentro la finestra della serie 2 (2022-2023)
        val tenCent = denom("Francia", 2, "10 cent").image
        assertEquals(2022 to 2023, tenCent.annoInizio to tenCent.annoFine)
    }

    @Test
    fun containsUsesTheWindow() {
        val s2 = denom("Francia", 2, "5 cent")
        assertEquals(false, s2.contains(2005))
        assertEquals(true, s2.contains(2022))
        assertEquals(false, s2.contains(2024))
    }
}
