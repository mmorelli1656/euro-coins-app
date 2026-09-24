package com.michele.eurocoins.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Espone le monete dal database locale, e si occupa di popolarlo la prima
 * volta leggendo `assets/coins.json` (esportato dalla pipeline dati — vedi
 * repo euro-coins-data-pipeline, `data/processed/ecb_coins.jsonl`).
 *
 * Le immagini NON sono incluse nell'asset: vengono caricate on-demand da
 * `Coin.urlImmagineFonte` (gli URL originali della fonte) con Coil, che le
 * mette in cache su disco dopo il primo caricamento.
 */
class CoinRepository(
    private val context: Context,
    private val dao: CoinDao,
    private val collectionDao: CollectionDao,
    hideMicrostates: Flow<Boolean>,
) {
    /** Catalogo visibile: senza i microstati quando l'utente li ha nascosti dalle Impostazioni. */
    val coins: Flow<List<Coin>> = combine(dao.observeAll(), hideMicrostates) { all, hide ->
        if (hide) all.filterNot { it.isMicrostate } else all
    }
    val paesi: Flow<List<String>> = combine(dao.observePaesi(), hideMicrostates) { all, hide ->
        if (hide) all.filterNot { it in MICROSTATE_PAESI } else all
    }

    /** Tutte le voci di collezione dell'utente (una per moneta+qualità). */
    val collectionItems: Flow<List<CollectionItem>> = collectionDao.observeAll()

    /** Chiavi delle monete possedute in almeno una qualità. */
    val ownedKeys: Flow<Set<String>> = collectionItems.map { items -> items.map { it.coinKey }.toSet() }

    /** Quante monete distinte sono possedute (per il messaggio di conferma del reset). */
    val ownedCount: Flow<Int> = ownedKeys.map { it.size }

    /** Svuota la collezione dell'utente; il catalogo `coins` non viene toccato. */
    suspend fun resetCollection() = collectionDao.deleteAll()

    /**
     * Salva in blocco le qualità possedute di una moneta ([entries]: qualità
     * -> prezzo in centesimi, null se non indicato); le qualità non presenti
     * vengono rimosse. Conserva la data di aggiunta delle voci già esistenti.
     */
    suspend fun saveCollection(coin: Coin, entries: Map<CoinQuality, Int?>) {
        val key = coin.stableKey
        val existing = collectionDao.itemsFor(key).associateBy { it.quality }
        val now = System.currentTimeMillis()
        collectionDao.replaceForCoin(
            key,
            entries.map { (quality, priceCents) ->
                CollectionItem(
                    coinKey = key,
                    quality = quality,
                    priceCents = priceCents,
                    anno = coin.anno,
                    paese = coin.paese,
                    tema = coin.tema,
                    addedAt = existing[quality]?.addedAt ?: now,
                )
            },
        )
    }

    // Il seeding parte dalla schermata d'ingresso ma qualunque ViewModel può
    // chiamarlo: senza il mutex due chiamate concorrenti al primo avvio
    // vedrebbero entrambe un database da popolare e inserirebbero le monete
    // due volte.
    private val seedMutex = Mutex()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Allinea il database a `assets/coins.json`: lo popola al primo avvio e lo
     * RIPOPOLA quando l'asset cambia (nuova versione dell'app con un dataset
     * aggiornato dalla pipeline). Prima si popolava solo a database vuoto,
     * quindi un telefono che aveva già seminato una versione precedente
     * continuava a mostrare per sempre i dati vecchi (visto con la frase
     * "Issue date" già rimossa dal dataset ma ancora in descrizione).
     *
     * Cambio rilevato con l'hash SHA-256 dell'asset, non con un numero di
     * versione da ricordarsi di incrementare a mano.
     *
     * NOTA per la futura collezione utente ("posseduta", qualità, prezzo):
     * gli id delle monete vengono rigenerati a ogni ripopolamento, quindi i
     * dati dell'utente NON vanno agganciati a `Coin.id` ma a una chiave
     * naturale stabile della moneta, in una tabella separata.
     */
    suspend fun ensureSeeded() = seedMutex.withLock {
        withContext(Dispatchers.IO) {
            val bytes = context.assets.open(ASSET_FILE_NAME).use { it.readBytes() }
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            if (prefs.getString(KEY_ASSET_HASH, null) == hash && dao.count() > 0) return@withContext

            val parsed = jsonFormat.decodeFromString<List<CoinJson>>(bytes.decodeToString())
            dao.replaceAll(parsed.map { it.toEntity() })
            prefs.edit().putString(KEY_ASSET_HASH, hash).apply()
        }
    }

    suspend fun getById(id: Long): Coin? = dao.getById(id)

    companion object {
        private const val ASSET_FILE_NAME = "coins.json"
        private const val PREFS_NAME = "dataset"
        private const val KEY_ASSET_HASH = "coins_asset_sha256"
        private val jsonFormat = Json { ignoreUnknownKeys = true }
    }
}
