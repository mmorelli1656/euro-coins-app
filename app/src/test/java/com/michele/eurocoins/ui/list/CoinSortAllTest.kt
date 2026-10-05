package com.michele.eurocoins.ui.list

import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinJson
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.stableKey
import com.michele.eurocoins.data.toEntity
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ordine dell'elenco "All" delle commemorative: tre scelte indipendenti sul dataset vero. */
class CoinSortAllTest {

    private val coins: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }
        // L'ordine del database: anno decrescente, poi paese.
        .sortedWith(compareByDescending<Coin> { it.anno }.thenBy { it.paese })

    private fun sorted(group: CoinGroup = CoinGroup.YEAR, countryAscending: Boolean = true, newestFirst: Boolean = true) =
        sortAll(coins, CoinListOptions(group = group, countryAscending = countryAscending, newestFirst = newestFirst))

    @Test
    fun defaultKeepsTheDatabaseOrder() {
        assertSame(coins, sortAll(coins, CoinListOptions()))
    }

    @Test
    fun everyCombinationKeepsEveryCoinOnce() {
        for (group in CoinGroup.entries) for (country in listOf(true, false)) for (newest in listOf(true, false)) {
            val rows = sorted(group, country, newest)
            assertEquals("$group $country $newest", coins.size, rows.size)
            assertEquals("$group $country $newest", coins.map { it.stableKey }.toSet(), rows.map { it.stableKey }.toSet())
        }
    }

    @Test
    fun byCountryTheDirectionOfTheYearIsIndependent() {
        val newest = sorted(CoinGroup.COUNTRY, newestFirst = true).filter { it.paese == "Italia" }
        assertEquals(newest.map { it.anno }, newest.map { it.anno }.sortedDescending())
        val oldest = sorted(CoinGroup.COUNTRY, newestFirst = false).filter { it.paese == "Italia" }
        assertEquals(oldest.map { it.anno }, oldest.map { it.anno }.sorted())
        // I paesi restano A → Z in tutte e due.
        val names = sorted(CoinGroup.COUNTRY, newestFirst = false).map { it.displayCountry() }
        assertEquals(names, names.sorted())
        val reversed = sorted(CoinGroup.COUNTRY, countryAscending = false).map { it.displayCountry() }
        assertEquals(reversed, reversed.sortedDescending())
    }

    @Test
    fun byYearTheDirectionOfTheCountryIsIndependent() {
        val rows = sorted(CoinGroup.YEAR, countryAscending = false, newestFirst = false)
        assertEquals(rows.map { it.anno }, rows.map { it.anno }.sorted())
        val oneYear = rows.filter { it.anno == rows.first().anno }.map { it.displayCountry() }
        assertTrue(oneYear.size > 1)
        assertEquals(oneYear, oneYear.sortedDescending())
    }
}
