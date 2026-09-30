package com.michele.eurocoins.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RegularMintageSummaryTest {

    @Test
    fun noDataReturnsNullSummary() {
        assertEquals(MintageSummary(null, null), summarizeMintages(emptyList(), CoinQuality.STANDARD))
    }

    @Test
    fun singleYearCaptionIsTheYearNotSummed() {
        val tirature = listOf(RegularIssueMintage(anno = 2020, quality = CoinQuality.PROOF, tiratura = 12_000))
        assertEquals(MintageSummary(12_000, "2020"), summarizeMintages(tirature, CoinQuality.PROOF))
    }

    @Test
    fun multipleYearsAreSummedWithAnAllYearsCaption() {
        val tirature = listOf(
            RegularIssueMintage(anno = 2002, quality = CoinQuality.STANDARD, tiratura = 1_200_000),
            RegularIssueMintage(anno = 2003, quality = CoinQuality.STANDARD, tiratura = 980_000),
        )
        assertEquals(MintageSummary(2_180_000, "all years"), summarizeMintages(tirature, CoinQuality.STANDARD))
    }

    @Test
    fun otherQualitiesDoNotLeakIntoTheSum() {
        val tirature = listOf(
            RegularIssueMintage(anno = 2020, quality = CoinQuality.STANDARD, tiratura = 1_000_000),
            RegularIssueMintage(anno = 2020, quality = CoinQuality.PROOF, tiratura = 12_000),
        )
        assertEquals(MintageSummary(1_000_000, "2020"), summarizeMintages(tirature, CoinQuality.STANDARD))
    }

    @Test
    fun groupingByYearIsAscendingAndOnlyIncludesYearsWithData() {
        val tirature = listOf(
            RegularIssueMintage(anno = 2020, quality = CoinQuality.PROOF, tiratura = 12_000),
            RegularIssueMintage(anno = 2002, quality = CoinQuality.STANDARD, tiratura = 1_200_000),
            RegularIssueMintage(anno = 2015, quality = CoinQuality.BU, tiratura = 25_000),
        )
        val rows = groupMintagesByYear(tirature)
        assertEquals(listOf(2002, 2015, 2020), rows.map { it.first })
        assertEquals(mapOf(CoinQuality.STANDARD to 1_200_000), rows[0].second)
        assertEquals(mapOf(CoinQuality.BU to 25_000), rows[1].second)
        assertEquals(mapOf(CoinQuality.PROOF to 12_000), rows[2].second)
    }

    @Test
    fun sameYearMultipleQualitiesMergeIntoOneRow() {
        val tirature = listOf(
            RegularIssueMintage(anno = 2020, quality = CoinQuality.STANDARD, tiratura = 1_000_000),
            RegularIssueMintage(anno = 2020, quality = CoinQuality.PROOF, tiratura = 12_000),
        )
        val rows = groupMintagesByYear(tirature)
        assertEquals(1, rows.size)
        assertEquals(mapOf(CoinQuality.STANDARD to 1_000_000, CoinQuality.PROOF to 12_000), rows[0].second)
    }
}
