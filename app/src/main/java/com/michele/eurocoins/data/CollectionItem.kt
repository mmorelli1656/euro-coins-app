package com.michele.eurocoins.data

import androidx.room.Entity

/**
 * Una moneta posseduta in una certa qualità. Dati dell'UTENTE: vivono in una
 * tabella separata da `coins` (che è un catalogo rigenerabile) e non vengono
 * mai toccati dal ripopolamento del dataset.
 *
 * [anno]/[paese]/[tema] sono una copia di quando la voce è stata salvata:
 * servono solo a riconoscere una voce "orfana" se la chiave della moneta
 * cambiasse (vedi [stableKey]).
 */
@Entity(tableName = "collection_items", primaryKeys = ["coinKey", "quality"])
data class CollectionItem(
    val coinKey: String,
    val quality: CoinQuality,
    /** Prezzo pagato in centesimi di euro, se l'utente lo ha inserito. */
    val priceCents: Int? = null,
    val anno: Int,
    val paese: String,
    val tema: String,
    val addedAt: Long,
)
