package com.michele.eurocoins.data

import com.michele.eurocoins.ui.regular.matchesDenomination
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegularDenominationRowsTest {

    private val all: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun item(series: RegularIssueSeries, taglio: String, anno: Int) = RegularCollectionItem(
        seriesKey = series.stableKey, taglio = taglio, anno = anno, quality = CoinQuality.STANDARD, paese = series.paese, addedAt = 0L,
    )

    @Test
    fun everyDenominationHasOneRowPerSeries() {
        val rows = denominationRows(all, emptyList())
        assertEquals(REGULAR_DENOMINATIONS, rows.keys.toList())
        for ((taglio, list) in rows) assertEquals(taglio, all.size, list.size) // 41 serie: una riga per serie
        assertEquals(all.regularProgress(emptyList()).total, rows.values.sumOf { it.size })
    }

    @Test
    fun rowsAreSortedByCountryThenSeries() {
        val list = denominationRows(all, emptyList())["2 euro"]!!
        assertEquals(list.map { it.countryName }, list.map { it.countryName }.sorted())
        val belgium = list.filter { it.paese == "Belgio" }
        assertEquals(listOf(1, 2, 3), belgium.map { it.seriesNumber })
        assertEquals("Series 2 · 2008 – 2013", belgium[1].seriesLabel)
    }

    @Test
    fun theSameFrenchCoinOwnedOnceFillsOnlyTheRowOfItsWindow() {
        val france = all.filter { it.paese == "Francia" }
        val s1 = france.first { it.ordineCronologico == 1 }
        val rows = denominationRows(all, listOf(item(s1, "5 cent", 2023)))["5 cent"]!!.filter { it.paese == "Francia" }
        assertEquals(listOf(false, true, false), rows.map { it.owned }) // 2023 cade nella serie 2 (2022-2023)
        assertEquals(1, rows[1].ownedYears)
    }

    @Test
    fun progressPerDenominationAddsUpToTheCatalog() {
        val germany = all.first { it.paese == "Germania" }
        val collection = listOf(item(germany, "1 cent", 2002), item(germany, "2 euro", 2015))
        val perDenomination = denominationRows(all, collection).mapValues { (_, rows) -> rows.denominationProgress() }
        assertEquals(Progress(1, 41), perDenomination["1 cent"])
        assertEquals(Progress(1, 41), perDenomination["2 euro"])
        assertEquals(Progress(0, 41), perDenomination["5 cent"])
        assertEquals(all.regularProgress(collection).owned, perDenomination.values.sumOf { it.owned })
    }

    @Test
    fun denominationSearchIgnoresCaseAndSpaces() {
        assertTrue(matchesDenomination("2 euro", "euro"))
        assertTrue(matchesDenomination("2 euro", "2 EURO"))
        assertTrue(matchesDenomination("2 euro", "2euro"))
        assertTrue(matchesDenomination("50 cent", "cent"))
        assertTrue(matchesDenomination("50 cent", "  "))
        assertFalse(matchesDenomination("50 cent", "euro"))
        assertFalse(matchesDenomination("1 euro", "2"))
    }
}
