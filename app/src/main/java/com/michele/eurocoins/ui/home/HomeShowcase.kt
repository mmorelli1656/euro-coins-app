package com.michele.eurocoins.ui.home

import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.RegularIssueSeries
import kotlin.random.Random

/** Quante monete mostra la fascia della scheda. */
const val SHOWCASE_SIZE = 4

/**
 * Da una lista di (paese, elemento) sceglie [size] elementi di paesi diversi (usata da
 * [pickShowcase]; Regular Issues ha un criterio diverso, vedi [pickRegularIssueShowcaseUrls]).
 * [daySeed] null = set fisso (il primo elemento per ogni paese, nell'ordine d'arrivo: è ciò che si
 * vede con la rotazione spenta). Con un valore (il giorno, `LocalDate.toEpochDay()`) il set è
 * casuale ma DETERMINISTICO: lo stesso giorno dà sempre lo stesso risultato, senza salvare nulla.
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
 * Le 4 posizioni della fascia di Regular Issues rappresentano una fascia di taglio crescente, non
 * un paese come in Commemorative (qui l'entità è una serie con fino a 8 tagli fotografati, non una
 * singola moneta): a sinistra un centesimo basso, poi uno alto, poi 1€, poi 2€ a destra — lo stesso
 * ordine con cui erano disegnate le monete segnaposto prima di questa rotazione.
 */
private val DENOMINATION_TIERS: List<Set<String>> = listOf(
    setOf("1 cent", "2 cent", "5 cent"),
    setOf("10 cent", "20 cent", "50 cent"),
    setOf("1 euro"),
    setOf("2 euro"),
)

/**
 * Sceglie una foto per ciascuna fascia di taglio di [DENOMINATION_TIERS], in ordine: non "una per
 * paese" come [pickShowcase] (il taglio conta più del paese, qui), quindi lo stesso paese può
 * comparire in più posizioni. [daySeed] null = la prima foto trovata per fascia (set fisso); con
 * un valore, una scelta deterministica sul giorno. Null in una posizione se nessuna serie ha una
 * foto per quella fascia (non dovrebbe succedere sul dataset attuale, ma non è garantito).
 */
fun pickRegularIssueShowcaseUrls(series: List<RegularIssueSeries>, daySeed: Long?): List<String?> {
    val random = daySeed?.let(::Random)
    return DENOMINATION_TIERS.map { tier ->
        val pool = series.flatMap { s -> s.immagini.filter { it.taglio in tier }.mapNotNull { it.urlImmagineFonte } }
        when {
            pool.isEmpty() -> null
            random == null -> pool.first()
            else -> pool[random.nextInt(pool.size)]
        }
    }
}
