package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L'elenco degli anni del pannello di collezione, sull'asset vero (che porta `anno_inizio`/
 * `anno_fine` anche nell'export senza dati Numista: non vengono da Numista).
 */
class RegularYearOptionsTest {

    // Gradle esegue gli unit test con la cartella del modulo (app/) come directory corrente.
    private val series: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun series(paese: String, ordine: Int) = series.first { it.paese == paese && it.ordineCronologico == ordine }
    private fun image(paese: String, ordine: Int, taglio: String) = series(paese, ordine).immagini.first { it.taglio == taglio }
    private fun years(paese: String, ordine: Int, taglio: String, owned: List<YearOption> = emptyList(), now: Int = 2026) =
        regularYearOptions(series(paese, ordine), image(paese, ordine, taglio), owned, now)

    @Test
    fun everyTaglioHasItsYearRange() {
        assertTrue(series.flatMap { it.immagini }.all { it.annoInizio != null })
    }

    @Test
    fun closedSeriesEndsWhereTheNextOneStarts() {
        val belgium = years("Belgio", 1, "1 cent").map { it.year }
        assertEquals((1999..2007).toList(), belgium)
        assertEquals((2008..2013).toList(), years("Belgio", 2, "1 cent").map { it.year })
    }

    @Test
    fun openSeriesRunsUntilThisYear() {
        assertEquals((2014..2026).toList(), years("Belgio", 3, "2 euro").map { it.year })
        assertEquals(listOf(2026), years("Bulgaria", 1, "1 euro").map { it.year })
    }

    @Test
    fun seriesThatChangeOnlySomeTaglioHaveTheirOwnRange() {
        // Francia 2022 cambia solo 1 e 2 euro, 2024 i 10-50 cent: la serie 1 dura quanto il taglio.
        assertEquals(2021, years("Francia", 1, "1 euro").last().year)
        assertEquals(2023, years("Francia", 1, "10 cent").last().year)
        assertEquals(2024, years("Francia", 3, "10 cent").first().year)
    }

    @Test
    fun vaticanJohnPaulCoinsDatedTwoThousandFiveAreInTheList() {
        val vatican = "Città del Vaticano"
        assertEquals((2002..2005).toList(), years(vatican, 1, "1 cent").map { it.year })
        assertEquals(listOf(2005), years(vatican, 2, "1 cent").map { it.year })
    }

    @Test
    fun greekTwoThousandTwoOffersTheEfsVarietyRightAfterTheNormalCoin() {
        val list = years("Grecia", 1, "2 euro")
        val index = list.indexOf(YearOption(2002))
        assertEquals(YearOption(2002, VARIETY_EFS), list[index + 1])
        assertEquals("2002 · EFS variety", list[index + 1].label)
        // il 2003 non ha varietà
        assertEquals(1, list.count { it.year == 2003 })
    }

    @Test
    fun yearsAlreadyInTheCollectionNeverDisappear() {
        val legacy = YearOption(1995)
        assertEquals(legacy, years("Germania", 1, "2 euro", owned = listOf(legacy)).first())
    }

    @Test
    fun defaultIsTheFirstYearButNotBeforeTheFirstCirculationYear() {
        assertEquals(YearOption(2002), defaultYearOption(years("Belgio", 1, "1 cent"), emptyList()))
        assertEquals(YearOption(2008), defaultYearOption(years("Belgio", 2, "1 cent"), emptyList()))
        assertEquals(YearOption(2023), defaultYearOption(years("Croazia", 1, "1 cent"), emptyList()))
    }

    @Test
    fun defaultOpensOnTheFirstOwnedYearWhenThereIsOne() {
        val owned = listOf(YearOption(2011), YearOption(2005))
        assertEquals(YearOption(2005), defaultYearOption(years("Germania", 1, "1 euro", owned = owned), owned))
    }
}
