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

    private val defaultOrder = RegularAllOrder()

    private fun all(order: RegularAllOrder = defaultOrder) = allDenominationRows(byDenomination, order)

    private fun search(query: String, order: RegularAllOrder = defaultOrder) =
        all(order).filter { it.matchesAllQuery(query) }

    private val everyOrder = RegularAllGroup.entries.flatMap { group ->
        listOf(true, false).flatMap { country -> listOf(true, false).map { largest -> RegularAllOrder(group, country, largest) } }
    }

    @Test
    fun everyOrderHasTheWholeCatalogOnce() {
        for (order in everyOrder) {
            val rows = all(order)
            assertEquals(order.toString(), series.regularProgress(emptyList()).total, rows.size)
            assertEquals(order.toString(), rows.size, rows.map { Triple(it.paese, it.viewedSeries.ordineCronologico, it.denomination.image.taglio) }.toSet().size)
        }
    }

    @Test
    fun byCountryTheEightCoinsOfASeriesAreTogetherFromLargestToSmallest() {
        val rows = all()
        assertEquals(rows.map { it.countryName }, rows.map { it.countryName }.sorted())
        val germany = rows.filter { it.paese == "Germania" }
        assertEquals(REGULAR_DENOMINATIONS.asReversed(), germany.map { it.denomination.image.taglio })
        val belgium = rows.filter { it.paese == "Belgio" }
        assertEquals(listOf(1, 2, 3), belgium.map { it.seriesNumber }.distinct())
        assertEquals(REGULAR_DENOMINATIONS.asReversed(), belgium.take(8).map { it.denomination.image.taglio })
        assertEquals(rows.map { it.countryName }.asReversed().distinct(), all(RegularAllOrder(countryAscending = false)).map { it.countryName }.distinct())
    }

    @Test
    fun byCountryTheDirectionOfTheValueOrderIsIndependent() {
        val smallestFirst = all(RegularAllOrder(largestFirst = false))
        // I paesi restano A → Z, dentro ogni serie i tagli vanno dal più piccolo.
        assertEquals(smallestFirst.map { it.countryName }, smallestFirst.map { it.countryName }.sorted())
        assertEquals(REGULAR_DENOMINATIONS, smallestFirst.filter { it.paese == "Germania" }.map { it.denomination.image.taglio })
        // Paesi Z → A e tagli dal più piccolo: tutte e due le direzioni invertite insieme.
        val both = all(RegularAllOrder(countryAscending = false, largestFirst = false))
        assertEquals(both.map { it.countryName }.distinct(), all().map { it.countryName }.distinct().asReversed())
        assertEquals(REGULAR_DENOMINATIONS, both.filter { it.paese == "Germania" }.map { it.denomination.image.taglio })
    }

    @Test
    fun byValueAllTheCountriesOfADenominationAreTogether() {
        val rows = all(RegularAllOrder(group = RegularAllGroup.VALUE))
        assertEquals(REGULAR_DENOMINATIONS.asReversed(), rows.map { it.denomination.image.taglio }.distinct())
        assertEquals(
            REGULAR_DENOMINATIONS,
            all(RegularAllOrder(group = RegularAllGroup.VALUE, largestFirst = false)).map { it.denomination.image.taglio }.distinct(),
        )
        val twoEuro = rows.take(series.size)
        assertTrue(twoEuro.all { it.denomination.image.taglio == "2 euro" })
        assertEquals(twoEuro.map { it.countryName }, twoEuro.map { it.countryName }.sorted())
    }

    @Test
    fun byValueTheDirectionOfTheCountryOrderIsIndependent() {
        val rows = all(RegularAllOrder(group = RegularAllGroup.VALUE, countryAscending = false))
        val twoEuro = rows.take(series.size)
        assertTrue(twoEuro.all { it.denomination.image.taglio == "2 euro" })
        assertEquals(twoEuro.map { it.countryName }, twoEuro.map { it.countryName }.sortedDescending())
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
    fun ownedQualityFilterKeepsRowsWithAtLeastOneOfTheChosenFinishes() {
        val germany = series.first { it.paese == "Germania" }
        fun item(taglio: String, quality: CoinQuality) = RegularCollectionItem(
            seriesKey = germany.stableKey, taglio = taglio, anno = 2015, quality = quality, paese = germany.paese, addedAt = 0L,
        )
        val rows = allDenominationRows(
            denominationRows(series, listOf(item("2 euro", CoinQuality.PROOF), item("1 euro", CoinQuality.STANDARD), item("1 euro", CoinQuality.BU))),
            RegularAllOrder(),
        )
        fun names(vararg q: CoinQuality) = rows.filter { it.ownsAnyQuality(q.toSet()) }.map { it.denomination.image.taglio }
        assertEquals(rows.size, rows.count { it.ownsAnyQuality(emptySet()) })
        assertEquals(listOf("2 euro"), names(CoinQuality.PROOF))
        assertEquals(listOf("1 euro"), names(CoinQuality.STANDARD))
        assertEquals(listOf("2 euro", "1 euro"), names(CoinQuality.PROOF, CoinQuality.BU)) // basta una delle scelte
    }

    @Test
    fun looseWordsMatchCountryOrKind() {
        assertEquals(all().size, search("").size)
        assertTrue(search("cent").all { it.denomination.image.taglio.endsWith("cent") })
        assertEquals(6 * series.size, search("cent").size)
        assertEquals(0, search("zzz").size)
    }
}
