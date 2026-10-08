package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewCoinNotesTest {

    private val coins2026: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }
        .filter { it.anno == 2026 }

    @Test
    fun the2026CoinsAreInTheCatalog() {
        assertEquals(29, coins2026.size)
    }

    @Test
    fun every2026NoteIsAnEnglishSentence() {
        val notes = coins2026.mapNotNull { it.noteStoriche }
        assertTrue(notes.size >= 22)
        // Iniziale maiuscola, punto finale (anche prima di una chiusura di virgolette), niente "[es]"/"Sintesi…".
        assertTrue(notes.all { it.first().isUpperCase() })
        assertTrue(notes.all { it.trimEnd('"', '”', '’', ')').last() in ".!?" })
        assertTrue(notes.none { it.startsWith("[") || it.contains("Sintesi") })
    }
}
