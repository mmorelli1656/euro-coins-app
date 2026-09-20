package com.michele.eurocoins.data.backup

import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CollectionItem
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
) {
    companion object {
        const val SCHEMA_VERSION = 1

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

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
