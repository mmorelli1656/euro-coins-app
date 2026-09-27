package com.michele.eurocoins.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Forma esatta di un record in `assets/regular_issues.json`, esportato da
 * `data/processed/ec_national_sides.jsonl` della pipeline dati (repo
 * euro-coins-data-pipeline). I nomi dei campi ricalcano lo schema pydantic
 * `SezioneSerieDivisionale`/`ImmagineTaglio` — vedi quel repo per il
 * significato di ciascuno.
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
        )
    },
    possibileIncongruenza = possibileIncongruenza,
    fonteDati = fonteDati,
)
