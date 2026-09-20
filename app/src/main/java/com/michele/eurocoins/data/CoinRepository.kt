package com.michele.eurocoins.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
) {
    val coins: Flow<List<Coin>> = dao.observeAll()
    val paesi: Flow<List<String>> = dao.observePaesi()

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
