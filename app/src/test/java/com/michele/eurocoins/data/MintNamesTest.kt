package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le zecche delle commemorative (`assets/coins.json`) devono avere tutte un paese in
 * `MintNames.kt`: una zecca nuova dalla pipeline comparirebbe nel dettaglio col nome grezzo,
 * diverso da tutte le altre. Le regolari hanno lo stesso controllo in `RegularIssuesAssetTest`.
 */
class MintNamesTest {

    // Gradle esegue gli unit test con la cartella del modulo (app/) come directory corrente.
    private val coins: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }

    @Test
    fun everyCommemorativeMintHasACountry() {
        val unknown = coins.flatMap { it.zeccaFisicaRaw?.split("; ").orEmpty() }
            .map { it.trim() }.filter { it.isNotEmpty() && !isKnownMint(it) }.toSet()
        assertTrue("Zecche senza paese in MintNames.kt: $unknown", unknown.isEmpty())
    }

    @Test
    fun germanMintsCollapseAndRawDataIsKept() {
        val german = coins.first { it.paese == "Germania" && it.zeccaFisicaRaw != null }
        assertEquals("Germany", german.displayMint())
        assertTrue(german.zeccaFisicaRaw!!.split("; ").size >= 5)
    }

    @Test
    fun outsourcedMintsReadAsWhereTheyWereStruck() {
        assertEquals("Italy", coins.first { it.paese == "San Marino" && it.zeccaFisicaRaw != null }.displayMint())
        assertEquals("Mystery", formatMintsForTest("Mystery"))
        assertEquals("Italy, France", formatMintsForTest("Rome; Monnaie de Paris; Rome"))
    }

    private fun formatMintsForTest(raw: String) = coins.first().copy(zeccaFisicaRaw = raw).displayMint()
}
