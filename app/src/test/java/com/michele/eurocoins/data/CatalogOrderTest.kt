package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogOrderTest {

    private val coins: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }

    @Test
    fun countriesFollowTheDisplayedNameWithinAYear() {
        val ordered = coins.inCatalogOrder()
        for ((year, ofYear) in ordered.groupBy { it.anno }) {
            val names = ofYear.map { it.displayCountry() }
            assertEquals("anno $year", names.sorted(), names)
        }
    }

    @Test
    fun vaticanCityComesLastAndCroatiaBeforeCyprus() {
        val names = coins.inCatalogOrder().filter { it.anno == 2026 }.map { it.displayCountry() }.distinct()
        assertEquals("Vatican City", names.last())
        assertTrue(names.indexOf("Croatia") < names.indexOf("Cyprus"))
    }

    @Test
    fun yearsStayNewestFirst() {
        val years = coins.inCatalogOrder().map { it.anno }
        assertEquals(years.sortedDescending(), years)
    }
}
