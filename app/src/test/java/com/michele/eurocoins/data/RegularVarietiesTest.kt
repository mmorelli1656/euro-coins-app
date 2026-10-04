package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/** La varietà EFS della Grecia 2002: dove è offerta nel pannello di collezione e con quale lettera. */
class RegularVarietiesTest {

    private fun series(paese: String, ordine: Int) = RegularIssueSeries(
        paese = paese, zeccaEmittente = paese, zeccaRaw = paese, ordineCronologico = ordine,
        numeroSerieIpotesi = ordine, intestazioneRaw = null, descrizione = "", anniCitati = emptyList(),
        immagini = emptyList(), possibileIncongruenza = false, fonteDati = "ec",
    )

    @Test
    fun greece2002OffersEfsOnEveryDenominationWithTheRightLetter() {
        val greece = series("Grecia", 1)
        val letters = mapOf(
            "1 cent" to "F", "2 cent" to "F", "5 cent" to "F", "10 cent" to "F", "50 cent" to "F",
            "20 cent" to "E", "1 euro" to "S", "2 euro" to "S",
        )
        letters.forEach { (taglio, letter) ->
            val variety = greece.varietyFor(taglio, 2002)
            assertNotNull(taglio, variety)
            assertEquals(VARIETY_EFS, variety!!.code)
            assertEquals("letter $letter in the star", variety.detail)
        }
    }

    @Test
    fun noVarietyElsewhere() {
        assertNull(series("Grecia", 1).varietyFor("1 euro", 2003))
        assertNull(series("Grecia", 1).varietyFor("1 euro", 2001))
        assertNull(series("Italia", 1).varietyFor("1 euro", 2002))
        assertNull(series("Grecia", 2).varietyFor("1 euro", 2002))
        assertNull(series("Grecia", 1).varietyFor("3 euro", 2002))
    }

    @Test
    fun lettersMatchTheForeignMintInTheDataset() {
        // sul dataset completo (le zecche per anno non sono nell'asset `-ExcludeNumista`)
        val all = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
            .map { it.toEntity() }
        val greece = all.first { it.paese == "Grecia" && it.ordineCronologico == 1 }
        assumeTrue(greece.immagini.any { it.zecchePerAnno.isNotEmpty() })
        val letterCountry = mapOf("F" to "France", "E" to "Spain", "S" to "Finland")
        greece.immagini.forEach { image ->
            val foreign = yearMintParts(image.zecchePerAnno.first { it.anno == 2002 }).first().country
            val letter = greece.varietyFor(image.taglio, 2002)!!.detail.substringAfter("letter ").substringBefore(" ")
            assertEquals(image.taglio, letterCountry[letter], foreign)
        }
    }
}
