package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceNamesTest {

    @Test
    fun knownSlugsGetReadableNames() {
        assertEquals("ECB", displaySourceName("ecb"))
        assertEquals("European Commission", displaySourceName("ec_national_sides"))
        assertEquals("BCL", displaySourceName("bcl"))
    }

    @Test
    fun unknownSlugIsShownUppercaseSoItGetsNoticed() {
        assertEquals("NEW_SOURCE", displaySourceName("new_source"))
    }

    @Test
    fun numistaLinkPointsToTheType() {
        assertEquals("https://en.numista.com/105", numistaUrl(105))
    }

    @Test
    fun commemorativesCarryTheNumistaIdNeededForAttribution() {
        // Gradle esegue gli unit test con la cartella del modulo (app/) come directory corrente.
        val coins = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
            .map { it.toEntity() }
        // Tirature/zecca/incisore Numista su ~580 monete su 584: ognuna deve avere il N# da citare.
        val withNumistaData = coins.filter {
            it.tiraturaNumistaStandard != null || it.tiraturaNumistaBu != null ||
                it.tiraturaNumistaProof != null || it.zeccaFisicaRaw != null
        }
        assertTrue(withNumistaData.size > 500)
        assertTrue(withNumistaData.all { it.numistaId != null })
    }
}
