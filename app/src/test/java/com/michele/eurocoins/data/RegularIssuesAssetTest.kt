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
        assertEquals(1, images.count { it.descrizione == null })
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
    fun mintsAreDeduplicatedAndCollapsedLikeCommemoratives() {
        assumeTrue(hasNumista)
        assertEquals("5 mints", image("Germania", 1, "1 euro").displayMint())
        assertEquals("Rome", image("Italia", 1, "2 euro").displayMint())
        // "Mint of Finland; Mint of Finland; Royal Dutch Mint" nel dataset grezzo: un solo Finland.
        assertEquals("Mint of Finland, Royal Dutch Mint", image("Finlandia", 1, "1 cent").displayMint())
    }
}
