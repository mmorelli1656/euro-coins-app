package com.michele.eurocoins.data

import androidx.room.Entity

/**
 * Una moneta circolante posseduta: un taglio (denominazione) di una serie, in un anno e una
 * qualità specifici. Dati dell'UTENTE: tabella separata da `regular_issue_series` (il catalogo,
 * rigenerabile), mai toccata dal ripopolamento del dataset.
 *
 * A differenza di [CollectionItem] (Commemorative, dove anno+paese+tema identificano UNA moneta
 * precisa nel dataset), qui l'anno non è nel dataset — una `RegularIssueSeries` copre un
 * intervallo di anni con lo stesso disegno — quindi lo inserisce l'utente, e la stessa
 * (seriesKey, taglio) può avere più righe: un utente può possedere più annate dello stesso
 * disegno (es. 1 euro Belgio serie 2, sia 2018 sia 2020).
 *
 * [paese] è una copia di quando la voce è stata salvata: serve solo a riconoscere una voce
 * "orfana" se la chiave della serie cambiasse (vedi [stableKey]).
 */
@Entity(tableName = "regular_collection_items", primaryKeys = ["seriesKey", "taglio", "anno", "quality"])
data class RegularCollectionItem(
    val seriesKey: String,
    val taglio: String,
    val anno: Int,
    val quality: CoinQuality,
    /** Prezzo pagato in centesimi di euro, se l'utente lo ha inserito. */
    val priceCents: Int? = null,
    val paese: String,
    val addedAt: Long,
)

/** Un'annata inserita nel pannello di collezione, prima di essere salvata come [RegularCollectionItem]. */
data class RegularCollectionEntry(val anno: Int, val quality: CoinQuality, val priceCents: Int?)
