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

class GenericEdgeLetteringTest {

    private val series: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun described(text: String) = series.first().copy(descrizione = text)

    @Test
    fun removesEveryWritingOfTheGenericSixTimesSentence() {
        val forms = listOf(
            "The edge lettering on the €2 coin is ‘2**’ repeated six times, alternately upright and inverted.",
            "In all series, the edge lettering on the €2 coin is ‘2*’, repeated six times, alternately upright and inverted.",
            "In both series, the edge lettering on the €2 coin is ‘2**’, repeated six times, alternately upright and inverted.",
            "The edge lettering on the €2 coin is 2**, repeated six times, alternately upright and inverted.",
            "The edge-lettering of the 2-euro coin is: 2 **, repeated six times, alternately from the bottom up and top down.",
            "The edge lettering is ‘2*’, repeated six times, alternately upright and inverted.",
            "The edge lettering is 2* repeated six times, alternately upright and inverted.",
        )
        for (form in forms) assertEquals(form, "Design A.", described("Design A. $form").displayDescription())
        assertEquals("Design A. Design B.", described("Design A. ${forms[0]} Design B.").displayDescription())
    }

    @Test
    fun keepsTheDot_whenItIsTheTailOfALongerSentence() {
        val text = "legend ‘San Marino’, arched 12 stars; The edge-lettering of the 2-euro coin is: 2 *, repeated six times, alternately upright and inverted."
        assertEquals("legend ‘San Marino’, arched 12 stars.", described(text).displayDescription())
    }

    @Test
    fun keepsCountrySpecificInscriptions() {
        val specific = listOf(
            "The edge lettering on the €2 coin is GOD * ZIJ * MET * ONS * (God be with us).",
            "The edge lettering on the €2 coin is ‘2 EURO ***’, repeated four times, alternately upright and inverted.",
            "The edge lettering of the 2 euro coin is \"EESTI\"\", repeated two times upright and inverted.",
            "The edge lettering consists of the words ‘SUOMI FINLAND ***’, where * represents three lion heads.",
        )
        for (text in specific) assertEquals(text, described(text).displayDescription())
    }

    @Test
    fun realDatasetLosesOnlyTheGenericOnes() {
        assertTrue(series.none { it.displayDescription().contains("repeated six times") })
        // restano le iscrizioni specifiche, ovunque il dato le abbia
        assertEquals(series.count { it.descrizione.contains("GOD * ZIJ") }, series.count { it.displayDescription().contains("GOD * ZIJ") })
        assertEquals(series.count { it.descrizione.contains("SUOMI FINLAND") }, series.count { it.displayDescription().contains("SUOMI FINLAND") })
        assertEquals(series.count { it.descrizione.contains("repeated four times") }, series.count { it.displayDescription().contains("repeated four times") })
        assertTrue(series.any { it.descrizione.contains("repeated six times") })
    }
}

class CoinDescriptionEndingTest {

    private val series: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun image(text: String?) = series.first().immagini.first().copy(descrizione = text)

    @Test
    fun addsTheDotWhereItIsMissing() {
        assertEquals("Twelve stars of Europe.", image("Twelve stars of Europe").displayCoinDescription())
        assertEquals("A capital \"A\" under a crown.", image("A capital \"A\" under a crown").displayCoinDescription())
        assertEquals("Coat of arms (the Double Cross).", image("Coat of arms (the Double Cross)").displayCoinDescription())
        assertEquals("Inscription \"SLOVENIJA\".", image("Inscription \"SLOVENIJA\"").displayCoinDescription())
        assertEquals("Trailing spaces.", image("Trailing spaces  \n").displayCoinDescription())
    }

    @Test
    fun leavesTheTextAloneWhenItAlreadyEnds() {
        for (text in listOf("Done.", "Really?", "Wow!", "He said \"stop.\"", "Lettering (in Dutch.)", "Done.”")) {
            assertEquals(text, image(text).displayCoinDescription())
        }
    }

    @Test
    fun joinsParagraphsIntoOneBlock() {
        // Senza questo l'ellissi della card a 4 righe finiva da sola sulla riga vuota tra due paragrafi.
        assertEquals(
            "First paragraph. Second one. Third.",
            image("First paragraph.\n\nSecond one.\r\n\r\n  Third.").displayCoinDescription(),
        )
    }

    @Test
    fun noDescriptionInTheRealDatasetHasLineBreaks() {
        val texts = series.flatMap { it.immagini }.mapNotNull { it.displayCoinDescription() }
        assertTrue(texts.none { '\n' in it || '\r' in it })
    }

    @Test
    fun noTextMeansNoCard() {
        assertEquals(null, image(null).displayCoinDescription())
        assertEquals(null, image("   ").displayCoinDescription())
    }

    @Test
    fun everyDescriptionInTheRealDatasetEndsWithTerminalPunctuation() {
        val texts = series.flatMap { it.immagini }.mapNotNull { it.displayCoinDescription() }
        assertTrue(texts.isNotEmpty())
        for (text in texts) {
            assertTrue("manca il punto: ...${text.takeLast(40)}", text.trimEnd { it in "\"”’)" }.last() in ".!?")
        }
    }
}
