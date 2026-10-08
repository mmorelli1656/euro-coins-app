package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinNotesTest {

    private val coins: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }

    private fun noted(text: String?) = coins.first().copy(noteStoriche = text)

    @Test
    fun startsWithACapitalAndEndsWithADot() {
        assertEquals("The twelve stars.", noted("the twelve stars").displayNotes())
        assertEquals("Already fine.", noted("Already fine.").displayNotes())
        assertEquals("Question?", noted("question?").displayNotes())
        assertEquals("Said \"stop.\"", noted("Said \"stop.\"").displayNotes())
        assertEquals("Trimmed.", noted("  trimmed \n").displayNotes())
    }

    @Test
    fun noNoteMeansNull() {
        assertNull(noted(null).displayNotes())
        assertNull(noted("   ").displayNotes())
    }

    @Test
    fun everyNoteInTheCatalogReadsAsASentence() {
        val notes = coins.mapNotNull { it.displayNotes() }
        assertTrue(notes.size > 580)
        assertTrue(notes.all { it.first().isUpperCase() })
        assertTrue(notes.all { it.trimEnd('"', '”', '’', ')').last() in ".!?" })
    }
}
