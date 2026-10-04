package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegularIssueTextTest {

    private val series: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun described(text: String) = series.first().copy(descrizione = text)

    @Test
    fun removesTheBoilerplateSentenceInTheMiddle() {
        val text = "Design A. The coin's outer ring depicts the 12 stars of the European flag. The edge lettering is X."
        assertEquals("Design A. The edge lettering is X.", described(text).displayDescription())
    }

    @Test
    fun removesItAtTheEndAndWithTypographicApostrophe() {
        val text = "Design A. The coin’s outer ring depicts the 12 stars of the European flag."
        assertEquals("Design A.", described(text).displayDescription())
    }

    @Test
    fun leavesOtherOuterRingSentencesAlone() {
        val belgian = "inner part of the coin – not in the outer ring – together with two new elements."
        val spanish = "The twelve stars in the outer ring were depicted as on the European flag."
        assertEquals(belgian, described(belgian).displayDescription())
        assertEquals(spanish, described(spanish).displayDescription())
    }

    @Test
    fun noSeriesKeepsTheBoilerplateInTheRealDataset() {
        assertTrue(series.none { it.displayDescription().contains("outer ring depicts") })
        assertTrue(series.any { it.descrizione.contains("outer ring depicts") })
        assertFalse(series.any { it.displayDescription().isBlank() && it.descrizione.isNotBlank() })
    }
}
