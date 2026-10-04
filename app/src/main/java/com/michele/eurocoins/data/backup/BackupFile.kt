package com.michele.eurocoins.data.backup

import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.RegularCollectionItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Formato del backup della collezione: un unico file JSON versionato.
 *
 * Contiene SOLO i dati dell'utente (cosa possiede, in che qualità, a che
 * prezzo), mai il catalogo: quello viaggia con l'app. Le monete sono
 * identificate da `coinKey` (= `Coin.stableKey`), non da `Coin.id`, che viene
 * rigenerato a ogni ripopolamento del dataset. Anno/paese/tema restano nella
 * voce per poter riconoscere e ricollegare un "orfano" se la chiave cambiasse.
 *
 * Si usa JSON e non CSV perché il file deve poter crescere (note, data di
 * acquisto...) senza rompere i backup vecchi: [schemaVersion] serve a questo.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: Long,
    val items: List<BackupItem>,
    /**
     * Collezione di Regular Issues (dalla versione 2). Default vuoto: un file della versione 1 si
     * legge ancora, ma NON contiene questi dati, quindi il ripristino non deve toccare la
     * collezione Regular locale (vedi [includesRegularIssues]).
     */
    val regularItems: List<RegularBackupItem> = emptyList(),
) {
    /** I backup della versione 1 non sanno nulla di Regular Issues: ripristinarli non deve azzerarla. */
    val includesRegularIssues: Boolean get() = schemaVersion >= 2

    companion object {
        const val SCHEMA_VERSION = 2

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(file: BackupFile): String = json.encodeToString(serializer(), file)

        /** @throws BackupException se il file non è JSON valido o è di una versione più nuova dell'app. */
        fun decode(text: String): BackupFile {
            val file = try {
                json.decodeFromString(serializer(), text)
            } catch (e: Exception) {
                throw BackupException("The backup file is corrupted or not a valid backup.", e)
            }
            if (file.schemaVersion > SCHEMA_VERSION) {
                throw BackupException("This backup was made by a newer version of the app. Update the app to restore it.")
            }
            return file
        }
    }
}

@Serializable
data class BackupItem(
    val coinKey: String,
    /** Nome dell'enum [CoinQuality] (`STANDARD`, `BU`, `PROOF`). */
    val quality: String,
    val priceCents: Int? = null,
    val anno: Int,
    val paese: String,
    val tema: String,
    val addedAt: Long,
)

fun CollectionItem.toBackupItem() = BackupItem(
    coinKey = coinKey,
    quality = quality.name,
    priceCents = priceCents,
    anno = anno,
    paese = paese,
    tema = tema,
    addedAt = addedAt,
)

/** Null se la qualità non è più riconosciuta (backup di una versione futura): la voce viene saltata. */
fun BackupItem.toCollectionItem(): CollectionItem? {
    val parsed = CoinQuality.entries.firstOrNull { it.name == quality } ?: return null
    return CollectionItem(
        coinKey = coinKey,
        quality = parsed,
        priceCents = priceCents,
        anno = anno,
        paese = paese,
        tema = tema,
        addedAt = addedAt,
    )
}

/**
 * Una voce della collezione Regular Issues: (serie, taglio, anno, qualità, varietà), agganciata a
 * `RegularIssueSeries.stableKey` (paese + ordine cronologico), mai a `RegularIssueSeries.id`.
 */
@Serializable
data class RegularBackupItem(
    val seriesKey: String,
    val taglio: String,
    val anno: Int,
    /** Nome dell'enum [CoinQuality] (`STANDARD`, `BU`, `PROOF`). */
    val quality: String,
    val variety: String = "",
    val priceCents: Int? = null,
    val paese: String,
    val addedAt: Long,
)

fun RegularCollectionItem.toBackupItem() = RegularBackupItem(
    seriesKey = seriesKey,
    taglio = taglio,
    anno = anno,
    quality = quality.name,
    variety = variety,
    priceCents = priceCents,
    paese = paese,
    addedAt = addedAt,
)

/** Null se la qualità non è più riconosciuta (backup di una versione futura): la voce viene saltata. */
fun RegularBackupItem.toCollectionItem(): RegularCollectionItem? {
    val parsed = CoinQuality.entries.firstOrNull { it.name == quality } ?: return null
    return RegularCollectionItem(
        seriesKey = seriesKey,
        taglio = taglio,
        anno = anno,
        quality = parsed,
        variety = variety,
        priceCents = priceCents,
        paese = paese,
        addedAt = addedAt,
    )
}

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
