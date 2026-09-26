package com.michele.eurocoins.ui.home

import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinJson
import com.michele.eurocoins.data.stableKey
import com.michele.eurocoins.data.toEntity
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La scelta delle monete della Home sul dataset vero: deterministica per giorno, una per paese, sempre con foto. */
class HomeShowcaseTest {

    private val coins: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }

    @Test
    fun sameDaySameCoins() {
        assertEquals(pickShowcase(coins, 20_000), pickShowcase(coins, 20_000))
    }

    @Test
    fun fourCoinsFromDifferentCountriesWithPhoto() {
        listOf(null, 20_000L, 20_001L, 20_123L).forEach { seed ->
            val set = pickShowcase(coins, seed)
            assertEquals(SHOWCASE_SIZE, set.size)
            assertEquals(SHOWCASE_SIZE, set.map { it.paese }.toSet().size)
            assertTrue(set.all { !it.immaginePlaceholder && it.urlImmagineFonte != null })
        }
    }

    @Test
    fun setChangesFromOneDayToTheNext() {
        val sets = (20_000L..20_006L).map { pickShowcase(coins, it).map { c -> c.stableKey } }
        assertTrue("Set uguale per 7 giorni di fila", sets.toSet().size > 1)
        assertNotEquals(pickShowcase(coins, null), pickShowcase(coins, 20_000))
    }
}
