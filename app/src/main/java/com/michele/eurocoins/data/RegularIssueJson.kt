package com.michele.eurocoins.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Forma esatta di un record in `assets/regular_issues.json`, esportato da
 * `data/processed/ec_national_sides.jsonl` della pipeline dati (repo
 * euro-coins-data-pipeline). I nomi dei campi ricalcano lo schema pydantic
 * `SezioneSerieDivisionale`/`ImmagineTaglio` — vedi quel repo per il
 * significato di ciascuno. Il file NON è l'export diretto di quel JSONL: lo produce
 * `scripts/export-regular-issues.ps1`, che aggiunge a ogni immagine i campi Numista/BCE per taglio.
 */
@Serializable
data class RegularIssueSeriesJson(
    val paese: String,
    @SerialName("zecca_emittente") val zeccaEmittente: String,
    @SerialName("zecca_raw") val zeccaRaw: String,
    @SerialName("ordine_cronologico") val ordineCronologico: Int,
    @SerialName("numero_serie_ipotesi") val numeroSerieIpotesi: Int,
    @SerialName("intestazione_raw") val intestazioneRaw: String? = null,
    val descrizione: String,
    @SerialName("anni_citati") val anniCitati: List<Int> = emptyList(),
    val immagini: List<RegularIssueImageJson> = emptyList(),
    @SerialName("possibile_incongruenza") val possibileIncongruenza: Boolean = false,
    @SerialName("fonte_dati") val fonteDati: String,
)

@Serializable
data class RegularIssueImageJson(
    val taglio: String,
    @SerialName("taglio_raw") val taglioRaw: String,
    @SerialName("url_immagine_fonte") val urlImmagineFonte: String? = null,
    @SerialName("licenza_immagine") val licenzaImmagine: String,
    @SerialName("attribuzione_immagine_raw") val attribuzioneImmagineRaw: String? = null,
    /** Fonte di QUESTA immagine ("ecb", "bcl", ...): può differire da quella del testo della serie — vedi RegularIssueSeries.fonteDati. */
    @SerialName("fonte_dati") val fonteDati: String = "",
    /** Vedi RegularIssueMintage. */
    val tirature: List<RegularIssueMintageJson> = emptyList(),
    /** Campi aggiunti dal merge con Numista/BCE (`scripts/export-regular-issues.ps1`): vedi RegularIssueImage. */
    val descrizione: String? = null,
    @SerialName("descrizione_fonte") val descrizioneFonte: String? = null,
    @SerialName("fonte_url") val fonteUrl: String? = null,
    @SerialName("numista_id") val numistaId: Int? = null,
    @SerialName("zecca_fisica_raw") val zeccaFisicaRaw: String? = null,
    @SerialName("incisore_raw") val incisoreRaw: String? = null,
    @SerialName("disegnatore_raw") val disegnatoreRaw: String? = null,
    /** Zecca per anno (stessa forma dell'entity: `anno`/`zecche`/`probabili`), vedi RegularIssueYearMint. */
    @SerialName("zecche_per_anno") val zecchePerAnno: List<RegularIssueYearMint> = emptyList(),
    /** Primo/ultimo anno del taglio nella serie (null = aperta), vedi RegularIssueImage.annoInizio. */
    @SerialName("anno_inizio") val annoInizio: Int? = null,
    @SerialName("anno_fine") val annoFine: Int? = null,
)

@Serializable
data class RegularIssueMintageJson(
    val anno: Int,
    val quality: CoinQuality,
    val tiratura: Long,
)

fun RegularIssueSeriesJson.toEntity(): RegularIssueSeries = RegularIssueSeries(
    paese = paese,
    zeccaEmittente = zeccaEmittente,
    zeccaRaw = zeccaRaw,
    ordineCronologico = ordineCronologico,
    numeroSerieIpotesi = numeroSerieIpotesi,
    intestazioneRaw = intestazioneRaw,
    descrizione = descrizione,
    anniCitati = anniCitati,
    immagini = immagini.map {
        RegularIssueImage(
            taglio = it.taglio,
            taglioRaw = it.taglioRaw,
            urlImmagineFonte = it.urlImmagineFonte,
            licenzaImmagine = it.licenzaImmagine,
            attribuzioneImmagineRaw = it.attribuzioneImmagineRaw,
            fonteDati = it.fonteDati,
            tirature = it.tirature.map { m -> RegularIssueMintage(anno = m.anno, quality = m.quality, tiratura = m.tiratura) },
            descrizione = it.descrizione,
            descrizioneFonte = it.descrizioneFonte,
            fonteUrl = it.fonteUrl,
            numistaId = it.numistaId,
            zeccaFisicaRaw = it.zeccaFisicaRaw,
            incisoreRaw = it.incisoreRaw,
            disegnatoreRaw = it.disegnatoreRaw,
            zecchePerAnno = it.zecchePerAnno,
            annoInizio = it.annoInizio,
            annoFine = it.annoFine,
        )
    },
    possibileIncongruenza = possibileIncongruenza,
    fonteDati = fonteDati,
)
