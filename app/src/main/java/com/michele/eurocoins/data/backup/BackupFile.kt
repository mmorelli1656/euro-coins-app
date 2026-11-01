package com.michele.eurocoins.data.backup

import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.Goal
import com.michele.eurocoins.data.GoalKind
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
    /**
     * Gli obiettivi personali di Milestones (dalla versione 6; vedi [Goal]). Default vuoto: un file precedente non li porta, quindi
     * ripristinarlo NON deve toccare quelli del telefono (vedi [includesGoals]).
     */
    val goals: List<BackupGoal> = emptyList(),
) {
    /** I backup della versione 1 non sanno nulla di Regular Issues: ripristinarli non deve azzerarla. */
    val includesRegularIssues: Boolean get() = schemaVersion >= 2

    /** I backup precedenti alla versione 6 non sanno nulla di obiettivi: ripristinarli non deve cancellare quelli locali. */
    val includesGoals: Boolean get() = schemaVersion >= 6

    companion object {
        /**
         * 6 = ci sono gli obiettivi personali (`goals`); un'app più vecchia li perderebbe in silenzio al prossimo backup, quindi rifiuta il
         * file come "più nuovo". 5 = le voci hanno una zecca (`mint`, vuota = non indicata: vedi `NO_MINT`); un'app più vecchia la
         * perderebbe in silenzio e confonderebbe due zecche della stessa moneta, quindi la rifiuta come "più
         * nuova". 4 = le voci Regular possono avere `anno` 0 ("anno non indicato", Quick add: vedi `NO_YEAR`).
         */
        const val SCHEMA_VERSION = 6

        /** L'ultima versione che non sa niente di zecche: la si scrive ancora quando nessuna voce ne ha una. */
        const val SCHEMA_VERSION_WITHOUT_MINTS = 4

        /** La versione delle zecche, senza obiettivi. */
        const val SCHEMA_VERSION_WITH_MINTS = 5

        /**
         * La versione da scrivere: 6 solo se c'è almeno un obiettivo, 5 solo se almeno una voce ha una zecca, altrimenti 4. Chi non
         * usa le lettere né gli obiettivi non obbliga un secondo telefono ad aggiornare l'app per ripristinare il backup.
         */
        fun versionFor(hasMints: Boolean, hasGoals: Boolean = false): Int = when {
            hasGoals -> SCHEMA_VERSION
            hasMints -> SCHEMA_VERSION_WITH_MINTS
            else -> SCHEMA_VERSION_WITHOUT_MINTS
        }

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
    /** Zecca (dalla versione 5); vuota = non indicata, anche in tutti i file precedenti. */
    val mint: String = "",
    val priceCents: Int? = null,
    val purchasedOn: Long? = null,
    val anno: Int,
    val paese: String,
    val tema: String,
    val addedAt: Long,
)

fun CollectionItem.toBackupItem() = BackupItem(
    coinKey = coinKey,
    quality = quality.name,
    mint = mint,
    priceCents = priceCents,
    purchasedOn = purchasedOn,
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
        mint = mint,
        priceCents = priceCents,
        purchasedOn = purchasedOn,
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
    /** Zecca (dalla versione 5); vuota = non indicata, anche in tutti i file precedenti. */
    val mint: String = "",
    val priceCents: Int? = null,
    val purchasedOn: Long? = null,
    val paese: String,
    val addedAt: Long,
)

fun RegularCollectionItem.toBackupItem() = RegularBackupItem(
    seriesKey = seriesKey,
    taglio = taglio,
    anno = anno,
    quality = quality.name,
    variety = variety,
    mint = mint,
    priceCents = priceCents,
    purchasedOn = purchasedOn,
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
        mint = mint,
        priceCents = priceCents,
        purchasedOn = purchasedOn,
        paese = paese,
        addedAt = addedAt,
    )
}

/**
 * Un obiettivo personale nel backup: la chiave NATURALE (paese stabile, anno, `RegularIssueSeries.stableKey`, numero di monete), mai un
 * id. [kind] è il nome dell'enum `GoalKind` come stringa: un tipo che un'app futura aggiungesse non rompe la lettura, la voce si salta.
 */
@Serializable
data class BackupGoal(val kind: String, val key: String, val doneAt: Long? = null)

fun Goal.toBackupGoal() = BackupGoal(kind = kind.name, key = key, doneAt = doneAt)

/** Null se il tipo non è più riconosciuto: la voce viene saltata. */
fun BackupGoal.toGoal(): Goal? {
    val parsed = GoalKind.entries.firstOrNull { it.name == kind } ?: return null
    return Goal(parsed, key, doneAt)
}

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
