package com.michele.eurocoins.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
    // vedrebbero entrambe count() == 0 e inserirebbero le monete due volte.
    private val seedMutex = Mutex()

    suspend fun ensureSeeded() = seedMutex.withLock {
        if (dao.count() > 0) return@withLock
        val json = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        val parsed = jsonFormat.decodeFromString<List<CoinJson>>(json)
        dao.insertAll(parsed.map { it.toEntity() })
    }

    suspend fun getById(id: Long): Coin? = dao.getById(id)

    companion object {
        private const val ASSET_FILE_NAME = "coins.json"
        private val jsonFormat = Json { ignoreUnknownKeys = true }
    }
}
