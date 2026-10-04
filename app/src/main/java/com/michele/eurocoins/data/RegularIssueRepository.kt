package com.michele.eurocoins.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Espone le serie divisionali (1 cent - 2 euro) dal database locale, e si
 * occupa di popolarlo la prima volta leggendo `assets/regular_issues.json`
 * (esportato dalla pipeline dati — vedi repo euro-coins-data-pipeline,
 * `data/processed/ec_national_sides.jsonl`). Stesso meccanismo di
 * [CoinRepository] (hash dell'asset per ripopolare, mutex per evitare
 * doppio seeding), ma dataset separato: nessuna collezione utente qui.
 */
class RegularIssueRepository(
    private val context: Context,
    private val dao: RegularIssueDao,
    private val collectionDao: RegularCollectionDao,
    hideMicrostates: Flow<Boolean>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Tutte le serie, senza i microstati quando l'utente li ha nascosti dalle Impostazioni. */
    val series: SharedFlow<List<RegularIssueSeries>> = combine(dao.observeAll(), hideMicrostates) { all, hide ->
        if (hide) all.filterNot { it.paese in MICROSTATE_PAESI } else all
    }.shareIn(scope, SharingStarted.Eagerly, replay = 1)

    /** Ultimo dataset già letto, o null se Room non ha ancora risposto: come `CoinRepository.coinsNow`. */
    val seriesNow: List<RegularIssueSeries>? get() = series.replayCache.firstOrNull()

    /** Tutte le voci di collezione dell'utente sulle monete circolanti (una per taglio+anno+qualità). */
    val collectionItems: SharedFlow<List<RegularCollectionItem>> = collectionDao.observeAll()
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    /** Stesso discorso di [seriesNow] per la collezione. */
    val collectionNow: List<RegularCollectionItem>? get() = collectionItems.replayCache.firstOrNull()

    /**
     * Tagli distinti posseduti (in qualsiasi serie, anno e qualità): quelli che il reset toglie.
     * Non è il conteggio della barra della Home, che conta le righe dentro le finestre delle serie.
     */
    val ownedCount: Flow<Int> = collectionItems.map { items -> items.map { it.seriesKey to it.taglio }.toSet().size }

    /** Svuota la collezione Regular dell'utente; il catalogo `regular_issue_series` non viene toccato. */
    suspend fun resetCollection() = collectionDao.deleteAll()

    /**
     * Salva in blocco le annate/qualità possedute di un taglio ([entries]): quelle non presenti
     * vengono rimosse. Conserva la data di aggiunta delle voci già esistenti (stessa (anno,
     * qualità, varietà)). Stesso pattern di `CoinRepository.saveCollection`, ma qui la chiave include
     * anche l'anno perché più annate dello stesso taglio possono coesistere.
     *
     * [window]: la serie guardata mostra (e quindi può cambiare) solo gli anni della sua finestra di
     * tempo (vedi [denominationsOf]); un taglio rimasto invariato è la stessa moneta in più serie, e
     * salvare dalla serie 2 non deve cancellare le annate della serie 1. Le voci FUORI finestra
     * restano com'erano; senza finestra si sostituisce tutto, come prima.
     */
    suspend fun saveCollection(
        seriesKey: String,
        taglio: String,
        paese: String,
        entries: List<RegularCollectionEntry>,
        window: SeriesDenomination? = null,
    ) {
        val all = collectionDao.itemsFor(seriesKey, taglio)
        val kept = if (window == null) emptyList() else all.filterNot { window.contains(it.anno) }
        val existing = all.associateBy { Triple(it.anno, it.quality, it.variety) }
        val now = System.currentTimeMillis()
        collectionDao.replaceForDenomination(
            seriesKey,
            taglio,
            kept + entries.map { entry ->
                RegularCollectionItem(
                    seriesKey = seriesKey,
                    taglio = taglio,
                    anno = entry.anno,
                    quality = entry.quality,
                    variety = entry.variety,
                    priceCents = entry.priceCents,
                    paese = paese,
                    addedAt = existing[Triple(entry.anno, entry.quality, entry.variety)]?.addedAt ?: now,
                )
            },
        )
    }

    private val seedMutex = Mutex()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Allinea il database a `assets/regular_issues.json`, come `CoinRepository.ensureSeeded()`. */
    suspend fun ensureSeeded() = seedMutex.withLock {
        withContext(Dispatchers.IO) {
            val bytes = context.assets.open(ASSET_FILE_NAME).use { it.readBytes() }
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            if (prefs.getString(KEY_ASSET_HASH, null) == hash && dao.count() > 0) return@withContext

            val parsed = jsonFormat.decodeFromString<List<RegularIssueSeriesJson>>(bytes.decodeToString())
            dao.replaceAll(parsed.map { it.toEntity() })
            prefs.edit().putString(KEY_ASSET_HASH, hash).apply()
        }
    }

    companion object {
        private const val ASSET_FILE_NAME = "regular_issues.json"
        private const val PREFS_NAME = "regular_issues_dataset"
        private const val KEY_ASSET_HASH = "regular_issues_asset_sha256"
        private val jsonFormat = Json { ignoreUnknownKeys = true }
    }
}
