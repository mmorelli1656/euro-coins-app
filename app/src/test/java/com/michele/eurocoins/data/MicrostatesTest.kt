package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Controlla il filtro "Hide microstates" sul dataset vero (`assets/coins.json`): se la pipeline
 * cambia i valori di `Coin.paese`, o il numero di monete, il test lo segnala invece di lasciare
 * il filtro a nascondere niente (o troppo) senza che nessuno se ne accorga.
 */
class MicrostatesTest {

    // Gradle esegue gli unit test con la cartella del modulo (app/) come directory corrente.
    private val coins: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }

    @Test
    fun everyMicrostateNameExistsInDataset() {
        val paesi = coins.map { it.paese }.toSet()
        assertTrue("Nome paese non presente nel dataset: ${MICROSTATE_PAESI - paesi}", paesi.containsAll(MICROSTATE_PAESI))
    }

    @Test
    fun hidingMicrostatesLeavesTwentyCountries() {
        val visible = coins.filterNot { it.isMicrostate }
        assertEquals(24, coins.map { it.paese }.distinct().size)
        assertEquals(20, visible.map { it.paese }.distinct().size)
        assertEquals(coins.size - coins.count { it.isMicrostate }, visible.size)
    }

    @Test
    fun stableKeysAreUniqueSoCollectionEntriesNeverCollide() {
        assertEquals(coins.size, coins.map { it.stableKey }.toSet().size)
    }
}
