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
     * Tirature per anno e qualità di QUESTO taglio in QUESTA serie (Numista, abbinate per anni:
     * vedi `scripts/export-regular-issues.ps1`). Vuota dove Numista non ha il type (Bulgaria,
     * Lussemburgo 2026 2 euro...): la card MINTAGES mostra "—". Default lista vuota, non null:
     * stesso motivo di [fonteDati] sopra, un campo nuovo su un blob JSON Room deve avere un
     * default o kotlinx.serialization crasha leggendo righe già salvate col JSON vecchio — vale
     * per TUTTI i campi sotto.
     */
    val tirature: List<RegularIssueMintage> = emptyList(),
    /**
     * Descrizione del disegno nazionale di QUESTO taglio, verbatim e in inglese: da Numista dove
     * c'è il type ([descrizioneFonte] = "numista"), altrimenti dalla pagina BCE del taglio ("ecb",
     * un unico testo per paese che a volte descrive più serie insieme). Null se nessuna fonte
     * copre quella serie (Lussemburgo 2026 2 euro).
     */
    val descrizione: String? = null,
    val descrizioneFonte: String? = null,
    /** Pagina da cui viene [descrizione] (Numista o BCE): attribuzione sempre visibile nei crediti. */
    val fonteUrl: String? = null,
    /** N# del type Numista "principale" di questa serie+taglio (quello di [descrizione] e dei crediti sotto). */
    val numistaId: Int? = null,
    /** Zecche fisiche unite da "; " e già senza doppioni — si mostra con [displayMint]. */
    val zeccaFisicaRaw: String? = null,
    /** Lato nazionale (campo `obverse` di Numista: la convenzione è invertita, vedi NOTES.md della pipeline). */
    val incisoreRaw: String? = null,
    val disegnatoreRaw: String? = null,
)

/**
 * Una tiratura: un anno, una qualità, un numero — più di una per taglio quando la serie copre più
 * anni. `Long` e non `Int`: la Germania 2002 ha 4 miliardi di 1 cent standard, e la somma su tutti
 * gli anni sfora i 2,1 miliardi anche per tagli più modesti.
 */
@Serializable
data class RegularIssueMintage(
    val anno: Int,
    val quality: CoinQuality,
    val tiratura: Long,
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
