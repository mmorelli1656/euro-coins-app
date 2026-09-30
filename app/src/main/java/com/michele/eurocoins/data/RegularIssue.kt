package com.michele.eurocoins.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Una "serie" (sezione così come strutturata dalla fonte EC "National sides
 * of euro coins") delle monete divisionali (1 cent - 2 euro) di un paese,
 * così come vive nel database locale. Alcuni paesi hanno più serie (cambi
 * di ritratto/stemma): vedi `ordineCronologico`. Testo/struttura vengono
 * dalla fonte EC ([fonteDati]), le immagini quasi sempre dalla BCE — vedi
 * [RegularIssueImage.fonteDati], che può differire da questo.
 *
 * A differenza di `Coin`, [immagini] è una lista annidata (fino a 8, una per
 * taglio): non ha una tabella propria perché è sempre letta/scritta insieme
 * alla serie, mai interrogata da sola — vedi [RegularIssueConverters].
 */
@Entity(tableName = "regular_issue_series")
data class RegularIssueSeries(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paese: String,
    val zeccaEmittente: String,
    val zeccaRaw: String,
    val ordineCronologico: Int,
    val numeroSerieIpotesi: Int,
    val intestazioneRaw: String?,
    val descrizione: String,
    /** Anni a 4 cifre citati nel testo, NON affidabile come "anno di inizio" della serie — vedi NOTES.md della pipeline. */
    val anniCitati: List<Int>,
    val immagini: List<RegularIssueImage>,
    val possibileIncongruenza: Boolean,
    val fonteDati: String,
)

@Serializable
data class RegularIssueImage(
    val taglio: String,
    val taglioRaw: String,
    val urlImmagineFonte: String?,
    val licenzaImmagine: String,
    val attribuzioneImmagineRaw: String?,
    /**
     * Fonte di QUESTA immagine ("ecb" quasi sempre, "bcl" per il Lussemburgo): può differire dal
     * `fonteDati` della serie che la contiene. Default "" (non null: kotlinx.serialization ha
     * bisogno di un default, non basta renderlo nullable, per non fallire in decodifica) perché
     * [immagini] è salvata come blob JSON in Room (vedi [RegularIssueConverters]): senza,
     * l'aggiunta di questo campo mandava in crash all'avvio chi aveva già righe vecchie salvate,
     * lette prima che `ensureSeeded()` facesse in tempo a ripopolarle con l'asset nuovo.
     */
    val fonteDati: String = "",
    /**
     * Tirature per anno e qualità di QUESTO taglio, se la pipeline le fornisce. Dataset attuale:
     * sempre vuota, nessuna serie ha ancora questo dato — la card MINTAGES del dettaglio taglio
     * mostra "—" finché resta così (vedi CLAUDE.md § Regular Issues). Default lista vuota, non
     * null: stesso motivo di [fonteDati] sopra, un campo nuovo su un blob JSON Room deve avere un
     * default o kotlinx.serialization crasha leggendo righe già salvate col JSON vecchio.
     */
    val tirature: List<RegularIssueMintage> = emptyList(),
)

/** Una tiratura: un anno, una qualità, un numero — più di una per taglio quando la serie copre più anni. */
@Serializable
data class RegularIssueMintage(
    val anno: Int,
    val quality: CoinQuality,
    val tiratura: Int,
)

/** Serializza [RegularIssueSeries.anniCitati] e [RegularIssueSeries.immagini] a stringa JSON per Room. */
class RegularIssueConverters {
    @TypeConverter
    fun anniCitatiToString(value: List<Int>): String = json.encodeToString(value)

    @TypeConverter
    fun anniCitatiFromString(value: String): List<Int> = json.decodeFromString(value)

    @TypeConverter
    fun immaginiToString(value: List<RegularIssueImage>): String = json.encodeToString(value)

    @TypeConverter
    fun immaginiFromString(value: String): List<RegularIssueImage> = json.decodeFromString(value)

    companion object {
        private val json = Json
    }
}
