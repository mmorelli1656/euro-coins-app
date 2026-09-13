package com.michele.eurocoins.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Forma esatta di un record in `assets/coins.json`, esportato da
 * `data/processed/ecb_coins.jsonl` della pipeline dati (repo
 * euro-coins-data-pipeline). I nomi dei campi ricalcano lo schema pydantic
 * `MonetaCommemorativa` — vedi quel repo per il significato di ciascuno.
 */
@Serializable
data class CoinJson(
    val paese: String,
    val anno: Int,
    val tema: String,
    val tiratura: Int? = null,
    @SerialName("zecca_emittente") val zeccaEmittente: String,
    @SerialName("zecca_raw") val zeccaRaw: String,
    @SerialName("zecca_fisica_raw") val zeccaFisicaRaw: String? = null,
    @SerialName("note_storiche") val noteStoriche: String? = null,
    @SerialName("url_immagine_fonte") val urlImmagineFonte: String? = null,
    @SerialName("licenza_immagine") val licenzaImmagine: String,
    @SerialName("attribuzione_immagine_raw") val attribuzioneImmagineRaw: String? = null,
    @SerialName("immagine_placeholder") val immaginePlaceholder: Boolean = false,
    @SerialName("fonte_dati") val fonteDati: String,
)

fun CoinJson.toEntity(): Coin = Coin(
    paese = paese,
    anno = anno,
    tema = tema,
    tiratura = tiratura,
    zeccaEmittente = zeccaEmittente,
    zeccaRaw = zeccaRaw,
    zeccaFisicaRaw = zeccaFisicaRaw,
    noteStoriche = noteStoriche,
    urlImmagineFonte = urlImmagineFonte,
    licenzaImmagine = licenzaImmagine,
    attribuzioneImmagineRaw = attribuzioneImmagineRaw,
    immaginePlaceholder = immaginePlaceholder,
    fonteDati = fonteDati,
)
