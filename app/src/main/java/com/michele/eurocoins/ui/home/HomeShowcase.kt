package com.michele.eurocoins.ui.home

import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.RegularIssueSeries
import kotlin.random.Random

/** Quante monete mostra la fascia della scheda. */
const val SHOWCASE_SIZE = 4

/**
 * Nucleo condiviso della scelta: da una lista di (paese, elemento) sceglie [size] elementi di
 * paesi diversi. [daySeed] null = set fisso (il primo elemento per ogni paese, nell'ordine
 * d'arrivo: è ciò che si vede con la rotazione spenta). Con un valore (il giorno,
 * `LocalDate.toEpochDay()`) il set è casuale ma DETERMINISTICO: lo stesso giorno dà sempre lo
 * stesso risultato, senza salvare nulla.
 */
private fun <T> pickByCountry(items: List<Pair<String, T>>, daySeed: Long?, size: Int): List<T> {
    val byCountry = items.groupBy(keySelector = { it.first }, valueTransform = { it.second })
    if (daySeed == null) return byCountry.values.mapNotNull { it.firstOrNull() }.take(size)
    val random = Random(daySeed)
    return byCountry.values.shuffled(random).take(size).map { it[random.nextInt(it.size)] }
}

/**
 * Sceglie le [SHOWCASE_SIZE] monete commemorative della fascia della Home: con foto reale, di
 * paesi diversi. Le foto restano nella cache di Coil per tutto il giorno di rotazione.
 */
fun pickShowcase(coins: List<Coin>, daySeed: Long?): List<Coin> {
    val withPhoto = coins.filter { !it.immaginePlaceholder && it.urlImmagineFonte != null }
    return pickByCountry(withPhoto.map { it.paese to it }, daySeed, SHOWCASE_SIZE)
}

/**
 * Stesso criterio per la fascia di Regular Issues: qui non c'è un'entità "moneta" ma una serie
 * con fino a 8 immagini di taglio, quindi si sceglie direttamente l'URL (una foto per paese, tra
 * tutte le sue serie e i suoi tagli fotografati).
 */
fun pickRegularIssueShowcaseUrls(series: List<RegularIssueSeries>, daySeed: Long?): List<String> {
    val urls = series.flatMap { s -> s.immagini.mapNotNull { it.urlImmagineFonte }.map { s.paese to it } }
    return pickByCountry(urls, daySeed, SHOWCASE_SIZE)
}
