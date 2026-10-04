package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Controlla `assets/regular_issues.json` (prodotto da `scripts/export-regular-issues.ps1`) con le
 * stesse classi che lo leggono a runtime: se lo script cambia forma, o un campo nuovo non ha un
 * default, il test lo segnala prima che lo faccia il telefono (che andrebbe in crash all'avvio).
 *
 * L'asset COMMITTATO è l'export `-ExcludeNumista` (il repo è pubblico, vedi CLAUDE.md § Backlog):
 * i test sui dati Numista (tirature, zecca, abbinamento per anni) si saltano se non ci sono, e
 * girano sull'export completo che si genera in locale per provare l'app sul telefono.
 */
class RegularIssuesAssetTest {

    // Gradle esegue gli unit test con la cartella del modulo (app/) come directory corrente.
    private val series: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private val hasNumista = series.any { s -> s.immagini.any { it.numistaId != null } }

    private fun image(paese: String, ordine: Int, taglio: String): RegularIssueImage =
        series.first { it.paese == paese && it.ordineCronologico == ordine }.immagini.first { it.taglio == taglio }

    @Test
    fun everyTaglioHasATextExceptWhereNoSourceIsCurrentEnough() {
        val images = series.flatMap { it.immagini }
        assertEquals(303, images.size)
        // Senza testo: solo la serie 2026 del Lussemburgo (fonte bcl, più recente della BCE), che
        // non ripiega sul testo BCE perché descriverebbe la serie precedente.
        val withoutText = series.filter { s -> s.immagini.any { it.descrizione == null } }
        assertTrue(withoutText.all { it.fonteDati == "bcl" })
        assertTrue(images.all { it.descrizioneFonte in listOf(null, "ecb", "numista") })
    }

    @Test
    fun numistaDataComesWithItsAttribution() {
        assumeTrue(hasNumista)
        val images = series.flatMap { it.immagini }
        // 0 dal dataset del 2026-10-04: il Lussemburgo 2026 2 euro ha ora il suo type (N#585823)
        assertEquals(0, images.count { it.descrizione == null })
        assertTrue(images.all { it.numistaId != null || it.tirature.isEmpty() })
    }

    @Test
    fun billionsFitAndSurviveTheRoundTrip() {
        assumeTrue(hasNumista)
        val germany = image("Germania", 1, "1 cent")
        val first = germany.tirature.first { it.anno == 2002 && it.quality == CoinQuality.STANDARD }
        assertEquals(4_000_000_000L, first.tiratura)
    }

    @Test
    fun designsThatChangeAreSplitAcrossSeriesByYear() {
        assumeTrue(hasNumista)
        // Vaticano 2005: esiste sia come Giovanni Paolo II (serie 1) sia come Sede Vacante (serie 2).
        val vatican = "Città del Vaticano"
        assertEquals(listOf(2002, 2003, 2004, 2005), image(vatican, 1, "2 euro").tirature.map { it.anno }.distinct())
        assertEquals(listOf(2005), image(vatican, 2, "2 euro").tirature.map { it.anno }.distinct())
        // Belgio: tre serie, tre intervalli senza sovrapposizioni.
        val ranges = (1..3).map { n -> image("Belgio", n, "1 euro").tirature.map { it.anno } }
        assertTrue(ranges[0].max() < ranges[1].min() && ranges[1].max() < ranges[2].min())
    }

    @Test
    fun mintsAreShownAsCountryWithoutDuplicates() {
        assumeTrue(hasNumista)
        // 5 zecche regionali tedesche → un solo "Germany"
        assertEquals("Germany", image("Germania", 1, "1 euro").displayMint())
        assertEquals("Italy", image("Italia", 1, "2 euro").displayMint())
        // "Mint of Finland; Mint of Finland; Royal Dutch Mint" nel dataset grezzo: un solo Finland.
        assertEquals("Finland, Netherlands", image("Finlandia", 1, "1 cent").displayMint())
        // il dato grezzo non si tocca
        assertTrue(image("Germania", 1, "1 euro").zeccaFisicaRaw!!.contains("Berlin"))
    }

    private fun labelsFor(paese: String, ordine: Int, taglio: String): Map<Int, YearMintLabel> {
        val image = image(paese, ordine, taglio)
        return yearMintLabels(image.zecchePerAnno, groupMintagesByYear(image.tirature).map { it.first })
    }

    @Test
    fun mintPerYearIsLabelledOnlyWhereItVaries() {
        assumeTrue(hasNumista)
        // Lussemburgo 1 euro: Royal Dutch Mint 2002-2004, poi Mint of Finland (lettera S → F, marchio)
        val lux = labelsFor("Lussemburgo", 1, "1 euro")
        assertEquals("Netherlands", lux.getValue(2002).text)
        assertEquals("Finland", lux.getValue(2005).text)
        // Slovenia: il 2007 è certo, gli anni senza circolazione solo probabili (mai uguali al certo)
        val slovenia = labelsFor("Slovenia", 1, "1 euro")
        assertEquals(MintLevel.CERTAIN, slovenia.getValue(2007).level)
        assertEquals(MintLevel.PROBABLE, slovenia.getValue(2008).level)
        // zecca fissa (anche Germania con 5 lettere, Irlanda per regola nazionale): niente etichette
        for ((paese, taglio) in listOf("Italia" to "2 euro", "Germania" to "1 euro", "Austria" to "2 euro", "Francia" to "1 euro")) {
            assertTrue("$paese $taglio", labelsFor(paese, 1, taglio).isEmpty())
        }
    }

    @Test
    fun greece2002IsSplitBetweenTheNationalMintAndTheHelpingOnes() {
        assumeTrue(hasNumista)
        val image = image("Grecia", 1, "1 euro")
        val parts = yearMintParts(image.zecchePerAnno.first { it.anno == 2002 })
        assertEquals(listOf("Finland", "Greece"), parts.map { it.country })
        // la divisione somma esattamente il totale dell'anno mostrato nella riga
        val total = image.tirature.first { it.anno == 2002 && it.quality == CoinQuality.STANDARD }.tiratura
        assertEquals(total, parts.sumOf { it.values.getValue(CoinQuality.STANDARD) })
        val bu = image.tirature.first { it.anno == 2002 && it.quality == CoinQuality.BU }.tiratura
        assertEquals(bu, parts.sumOf { it.values[CoinQuality.BU] ?: 0L })
        // 20 cent: la zecca estera è Madrid; il solo 2002 greco si divide, nessun altro paese/anno
        assertEquals(listOf("Spain", "Greece"), yearMintParts(image("Grecia", 1, "20 cent").zecchePerAnno.first { it.anno == 2002 }).map { it.country })
        val split = series.flatMap { s -> s.immagini.map { s.paese to it } }
            .flatMap { (paese, img) -> img.zecchePerAnno.filter { yearMintParts(it).isNotEmpty() }.map { paese to it.anno } }
        assertTrue("Divisi solo Grecia 2002: $split", split.all { it == ("Grecia" to 2002) })
        assertEquals(8, split.size)
    }

    @Test
    fun everyYearMintInTheAssetHasACountry() {
        val unknown = series.flatMap { it.immagini }.flatMap { it.zecchePerAnno }
            .flatMap { it.zecche + it.probabili }.filter { !isKnownMint(it) }.toSet()
        assertTrue("Zecche per anno senza paese in MintNames.kt: $unknown", unknown.isEmpty())
    }

    @Test
    fun everyMintInTheAssetHasACountry() {
        val unknown = series.flatMap { it.immagini }
            .flatMap { it.zeccaFisicaRaw?.split("; ").orEmpty() }
            .map { it.trim() }.filter { it.isNotEmpty() && !isKnownMint(it) }.toSet()
        assertTrue("Zecche senza paese in MintNames.kt: $unknown", unknown.isEmpty())
    }
}
