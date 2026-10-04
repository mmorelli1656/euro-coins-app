package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RegularProgressTest {

    private val all: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun forCountry(paese: String) = all.filter { it.paese == paese }

    private fun item(series: RegularIssueSeries, taglio: String, anno: Int, quality: CoinQuality = CoinQuality.STANDARD) =
        RegularCollectionItem(
            seriesKey = series.stableKey, taglio = taglio, anno = anno, quality = quality, paese = series.paese, addedAt = 0L,
        )

    @Test
    fun totalIsTheRowsTheUserSees() {
        assertEquals(Progress(0, 8), forCountry("Germania").regularProgress(emptyList()))
        // 3 serie x 8 righe, anche se i tagli invariati sono lo stesso disegno (non 13 e non 18)
        assertEquals(Progress(0, 24), forCountry("Francia").regularProgress(emptyList()))
        assertEquals(Progress(0, 24), forCountry("Spagna").regularProgress(emptyList()))
        assertEquals(Progress(0, 48), forCountry("Città del Vaticano").regularProgress(emptyList())) // 6 serie x 8, anche la 2026 senza foto
    }

    @Test
    fun countsRowsNotYears() {
        val germany = forCountry("Germania")
        val s = germany.first()
        val collection = listOf(
            item(s, "1 cent", 2002), item(s, "1 cent", 2003), item(s, "1 cent", 2010, CoinQuality.PROOF), // un taglio, tre annate
            item(s, "2 euro", 2015),
        )
        assertEquals(Progress(2, 8), germany.regularProgress(collection))
    }

    @Test
    fun otherCountriesDoNotInterfere() {
        val italy = forCountry("Italia").first()
        assertEquals(Progress(0, 8), forCountry("Germania").regularProgress(listOf(item(italy, "1 cent", 2002))))
        assertEquals(Progress(1, 8), forCountry("Italia").regularProgress(listOf(item(italy, "1 cent", 2002))))
    }

    @Test
    fun theSameFrenchCoinFillsOneRowPerSeriesWindow() {
        val france = forCountry("Francia")
        val s1 = france.first { it.ordineCronologico == 1 }
        // il 5 cent e' della serie 1 anche quando lo si spunta dalla serie 2 o 3: cambia la finestra
        assertEquals(Progress(1, 24), france.regularProgress(listOf(item(s1, "5 cent", 2005))))  // riga serie 1
        assertEquals(Progress(1, 24), france.regularProgress(listOf(item(s1, "5 cent", 2023))))  // riga serie 2
        assertEquals(Progress(3, 24), france.regularProgress(listOf(item(s1, "5 cent", 2005), item(s1, "5 cent", 2023), item(s1, "5 cent", 2025))))
        assertEquals(Progress(1, 24), france.regularProgress(listOf(item(s1, "5 cent", 2005), item(s1, "5 cent", 2006))))
    }

    @Test
    fun everySeriesHasEightRows() {
        // 41 serie x 8 = 328: il totale di Home
        assertEquals(41 * 8, all.regularProgress(emptyList()).total)
    }

    @Test
    fun catalogTotalIsTheSumOfTheCountries() {
        val perCountry = all.groupBy { it.paese }.values.sumOf { it.regularProgress(emptyList()).total }
        assertEquals(perCountry, all.regularProgress(emptyList()).total)
    }
}
