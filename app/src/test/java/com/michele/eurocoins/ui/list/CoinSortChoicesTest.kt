package com.michele.eurocoins.ui.list

import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinJson
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.inCatalogOrder
import com.michele.eurocoins.data.stableKey
import com.michele.eurocoins.data.toEntity
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il pulsante "Default" dell'ordinamento delle liste di un anno o di un paese è stato tolto (2026-10-08): coincideva con la
 * prima voce. Questi test fissano che togliendolo nessun ordine cambia, sul dataset vero.
 */
class CoinSortChoicesTest {

    private val coins: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }
        .inCatalogOrder()

    @Test
    fun noListOffersADefaultButton() {
        assertFalse(CoinFilter.Year(2025).sortChoices().contains(CoinSort.DEFAULT))
        assertFalse(CoinFilter.Country("Italia").sortChoices().contains(CoinSort.DEFAULT))
    }

    @Test
    fun theFirstChoiceIsTheDefaultOne() {
        assertEquals(CoinSort.COUNTRY_AZ, CoinFilter.Year(2025).defaultSort())
        assertEquals(CoinSort.YEAR_DESC, CoinFilter.Country("Italia").defaultSort())
        assertEquals(CoinSort.DEFAULT, CoinFilter.All.defaultSort())
    }

    @Test
    fun everyYearListInBaseOrderIsAlreadyCountryAtoZ() {
        for (year in coins.map { it.anno }.distinct()) {
            val list = coins.filter { it.anno == year && !it.emissioneComune }
            assertEquals("anno $year", list.map { it.stableKey }, list.sortedBy { it.displayCountry() }.map { it.stableKey })
        }
    }

    @Test
    fun everyCountryListInBaseOrderIsAlreadyNewestFirst() {
        for (paese in coins.map { it.paese }.distinct()) {
            val list = coins.filter { it.paese == paese }
            assertEquals("paese $paese", list.map { it.stableKey }, list.sortedByDescending { it.anno }.map { it.stableKey })
        }
    }

    @Test
    fun theOnlyRemainingChoicesAreTheOpposites() {
        assertEquals(listOf(CoinSort.COUNTRY_AZ, CoinSort.COUNTRY_ZA), CoinFilter.Year(2025).sortChoices())
        assertEquals(listOf(CoinSort.YEAR_DESC, CoinSort.YEAR_ASC), CoinFilter.Country("Italia").sortChoices())
        assertTrue(CoinListOptions().sort == CoinSort.DEFAULT)
    }
}
