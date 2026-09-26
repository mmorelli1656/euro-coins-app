package com.michele.eurocoins.ui.home

import com.michele.eurocoins.data.Coin
import kotlin.random.Random

/** Quante monete mostra la fascia della scheda. */
const val SHOWCASE_SIZE = 4

/**
 * Sceglie le [SHOWCASE_SIZE] monete della fascia della Home: con foto reale, di paesi diversi.
 *
 * [daySeed] null = set fisso (i primi paesi in ordine di catalogo: è ciò che si vede con la
 * rotazione spenta). Con un valore (il giorno, `LocalDate.toEpochDay()`) il set è casuale ma
 * DETERMINISTICO: lo stesso giorno dà sempre le stesse monete, senza salvare nulla, e le foto
 * restano nella cache di Coil per tutto il giorno. Il criterio "una moneta per paese" vale
 * anche per le future Regular Issues: quando esisteranno basterà chiamare questa funzione con
 * le loro monete, sotto la stessa impostazione.
 */
fun pickShowcase(coins: List<Coin>, daySeed: Long?): List<Coin> {
    val withPhoto = coins.filter { !it.immaginePlaceholder && it.urlImmagineFonte != null }
    if (daySeed == null) return withPhoto.distinctBy { it.paese }.take(SHOWCASE_SIZE)
    val random = Random(daySeed)
    return withPhoto
        .groupBy { it.paese }
        .values
        .shuffled(random)
        .take(SHOWCASE_SIZE)
        .map { it[random.nextInt(it.size)] }
}
