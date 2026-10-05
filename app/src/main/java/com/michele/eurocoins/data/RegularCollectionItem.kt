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
@Entity(tableName = "regular_collection_items", primaryKeys = ["seriesKey", "taglio", "anno", "quality", "variety"])
data class RegularCollectionItem(
    val seriesKey: String,
    val taglio: String,
    val anno: Int,
    val quality: CoinQuality,
    /**
     * Varietà dell'annata (oggi solo [VARIETY_EFS], Grecia 2002), stringa vuota per la moneta
     * normale. Nella chiave: il 2002 greco di Atene e il 2002 EFS sono lo stesso anno e la stessa
     * qualità ma due monete, e un utente può averle entrambe. Vedi [RegularIssueSeries.varietyFor].
     */
    val variety: String = "",
    /** Prezzo pagato in centesimi di euro, se l'utente lo ha inserito. */
    val priceCents: Int? = null,
    /** Giorno di acquisto (`LocalDate.toEpochDay()`), se l'utente lo ha inserito: vedi `PurchaseDates.kt`. */
    val purchasedOn: Long? = null,
    val paese: String,
    val addedAt: Long,
)

/** Un'annata inserita nel pannello di collezione, prima di essere salvata come [RegularCollectionItem]. */
data class RegularCollectionEntry(
    val anno: Int,
    val quality: CoinQuality,
    val priceCents: Int?,
    val variety: String = "",
    val purchasedOn: Long? = null,
)
