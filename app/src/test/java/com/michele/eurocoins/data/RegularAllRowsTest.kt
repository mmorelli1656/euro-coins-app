package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegularAllRowsTest {

    private val series: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private val byDenomination = denominationRows(series, emptyList())

    private fun all(sort: RegularAllSort) = allDenominationRows(byDenomination, sort)

    private fun search(query: String, sort: RegularAllSort = RegularAllSort.COUNTRY_ASC) =
        all(sort).filter { it.matchesAllQuery(query) }

    @Test
    fun everySortHasTheWholeCatalogOnce() {
        for (sort in RegularAllSort.entries) {
            val rows = all(sort)
            assertEquals(sort.name, series.regularProgress(emptyList()).total, rows.size)
            assertEquals(sort.name, rows.size, rows.map { Triple(it.paese, it.viewedSeries.ordineCronologico, it.denomination.image.taglio) }.toSet().size)
        }
    }

    @Test
    fun byCountryTheEightCoinsOfASeriesAreTogetherFromLargestToSmallest() {
        val rows = all(RegularAllSort.COUNTRY_ASC)
        assertEquals(rows.map { it.countryName }, rows.map { it.countryName }.sorted())
        val germany = rows.filter { it.paese == "Germania" }
        assertEquals(REGULAR_DENOMINATIONS.asReversed(), germany.map { it.denomination.image.taglio })
        val belgium = rows.filter { it.paese == "Belgio" }
        assertEquals(listOf(1, 2, 3), belgium.map { it.seriesNumber }.distinct())
        assertEquals(REGULAR_DENOMINATIONS.asReversed(), belgium.take(8).map { it.denomination.image.taglio })
        assertEquals(rows.map { it.countryName }.asReversed().distinct(), all(RegularAllSort.COUNTRY_DESC).map { it.countryName }.distinct())
    }

    @Test
    fun byValueAllTheCountriesOfADenominationAreTogether() {
        val rows = all(RegularAllSort.LARGEST_FIRST)
        assertEquals(REGULAR_DENOMINATIONS.asReversed(), rows.map { it.denomination.image.taglio }.distinct())
        assertEquals(REGULAR_DENOMINATIONS, all(RegularAllSort.SMALLEST_FIRST).map { it.denomination.image.taglio }.distinct())
        val twoEuro = rows.take(series.size)
        assertTrue(twoEuro.all { it.denomination.image.taglio == "2 euro" })
        assertEquals(twoEuro.map { it.countryName }, twoEuro.map { it.countryName }.sorted())
    }

    @Test
    fun aWholeDenominationInTheQueryMustMatchExactly() {
        // "5 cent" non deve trovare i 50 cent, ne' "2 euro" gli "1 euro" del 2002
        assertTrue(search("5 cent").all { it.denomination.image.taglio == "5 cent" })
        assertEquals(series.size, search("5 cent").size)
        assertEquals(series.size, search("2euro").size)
        assertEquals(series.size, search("50 CENT").size)
    }

    @Test
    fun denominationCanBeCombinedWithCountryAndSeries() {
        val france = search("france 5 cent")
        assertEquals(3, france.size) // la stessa moneta in tre serie: la riga porta il numero della serie
        assertEquals(listOf(1, 2, 3), france.map { it.seriesNumber })
        assertEquals(1, search("5 cent france series 2").size)
        assertEquals(8, search("belgium series 2").size)
        assertTrue(search("belgium series 2").all { it.paese == "Belgio" && it.seriesNumber == 2 })
    }

    @Test
    fun looseWordsMatchCountryOrKind() {
        assertEquals(all(RegularAllSort.COUNTRY_ASC).size, search("").size)
        assertTrue(search("cent").all { it.denomination.image.taglio.endsWith("cent") })
        assertEquals(6 * series.size, search("cent").size)
        assertEquals(0, search("zzz").size)
    }
}
