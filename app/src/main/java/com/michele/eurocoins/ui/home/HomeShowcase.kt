package com.michele.eurocoins.ui.home

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size
import coil3.transform.Transformation
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.ui.regular.RegularIssueImageTrim
import com.michele.eurocoins.ui.settings.UserSettings
import java.time.LocalDate
import kotlin.random.Random

/** Quante monete mostra la fascia della scheda. */
const val SHOWCASE_SIZE = 4

/**
 * Richiesta Coil di una foto della fascia, costruita sempre qui: `HomeScreen` e il precaricamento
 * di [preloadHomeShowcase] devono produrre la STESSA chiave di cache in memoria (dati + dimensione +
 * trasformazioni). La dimensione è fissa (`Size.ORIGINAL`) e non quella del layout: così il
 * precaricamento non deve conoscerla e `AsyncImage` non aspetta la misura per partire; la foto
 * (270 o 540 px) la scala poi Compose nel cerchio, che è comunque di ~260-340 px.
 */
fun showcaseRequest(context: Context, url: String, transformations: List<Transformation> = emptyList()): ImageRequest =
    ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).transformations(transformations).build()

/**
 * Foto note al primo fotogramma, senza il database: il set di oggi se già calcolato (ieri o
 * un'apertura precedente di oggi), altrimenti l'ultimo mostrato per intero.
 */
fun firstFrameShowcaseUrls(settings: UserSettings): List<String> =
    (if (settings.rotateHomeCoins.value) settings.showcaseUrlsFor(LocalDate.now().toEpochDay()) else null) ?: settings.lastShowcase

/** Come [firstFrameShowcaseUrls], per Regular Issues (posizioni preservate: una può essere null). */
fun firstFrameRegularIssueShowcaseUrls(settings: UserSettings): List<String?> =
    (if (settings.rotateHomeCoins.value) settings.regularIssueShowcaseUrlsFor(LocalDate.now().toEpochDay()) else null)
        ?: settings.regularIssueLastShowcase

/**
 * Avvia la lettura dalla cache su disco e la decodifica delle 8 foto della Home già in `onCreate`,
 * in parallelo alla prima composizione: a freddo la composizione di `AsyncImage` partiva solo dopo
 * il layout (60-160 ms dopo `onCreate`) e la foto arrivava altri ~40 ms più tardi, quindi il primo
 * fotogramma usciva con i tondi vuoti. Così finiscono nella cache in memoria di Coil, che le
 * richieste della Home trovano subito.
 */
fun preloadHomeShowcase(context: Context, settings: UserSettings) {
    val loader = SingletonImageLoader.get(context)
    firstFrameShowcaseUrls(settings).take(SHOWCASE_SIZE).forEach { loader.enqueue(showcaseRequest(context, it)) }
    firstFrameRegularIssueShowcaseUrls(settings).take(SHOWCASE_SIZE).filterNotNull().forEach {
        loader.enqueue(showcaseRequest(context, it, listOf(RegularIssueImageTrim)))
    }
}

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
 * Le 4 posizioni della fascia di Regular Issues rappresentano una fascia di taglio, non un paese
 * come in Commemorative (qui l'entità è una serie con fino a 8 tagli fotografati, non una singola
 * moneta): centesimi bassi, poi 1€ e 2€ al centro (le posizioni più grandi del layout, vedi
 * `BandRatios` in HomeScreen.kt — le monete bimetalliche "di pregio" meritano lo spazio maggiore),
 * poi centesimi alti a destra. Non strettamente in ordine di valore crescente: è una fascia
 * decorativa, non deve insegnare l'ordine dei tagli, e i due bordi in metallo caldo (rame/oro
 * nordico) fanno da cornice simmetrica alle due bimetalliche lucide al centro.
 */
private val DENOMINATION_TIERS: List<Set<String>> = listOf(
    setOf("1 cent", "2 cent", "5 cent"),
    setOf("1 euro"),
    setOf("2 euro"),
    setOf("10 cent", "20 cent", "50 cent"),
)

/**
 * Set fisso (rotazione spenta), nello stesso ordine di [DENOMINATION_TIERS]: monete note e
 * fotografate bene dalla BCE, un paese diverso per fascia (non microstati, sempre presenti anche
 * con "Hide microstates" attivo). Senza questa scelta esplicita il primo trovato in ordine
 * alfabetico di paese vinceva sempre — Andorra, per tutte e 4 le posizioni. L'Italia (Uomo
 * Vitruviano) e la Spagna (Cervantes) sono state scartate dopo un controllo alla fonte: a
 * differenza delle altre, quelle due foto BCE sono leggermente sfocate anche a piena risoluzione
 * (non un problema di ridimensionamento).
 */
private val CURATED_DEFAULTS: List<Pair<String, String>> = listOf(
    "Finlandia" to "1 cent",
    "Germania" to "1 euro",
    "Grecia" to "2 euro",
    "Paesi Bassi" to "20 cent",
)

/**
 * Sceglie una foto per ciascuna fascia di taglio di [DENOMINATION_TIERS], in ordine: non "una per
 * paese" come [pickShowcase] (il taglio conta più del paese, qui), quindi lo stesso paese può
 * comparire in più posizioni. [daySeed] null = la scelta curata di [CURATED_DEFAULTS] (ripiego sulla
 * prima foto trovata se quel paese non ha quel taglio, non dovrebbe succedere sul dataset attuale);
 * con un valore, una scelta deterministica sul giorno. Null in una posizione se nessuna serie ha una
 * foto per quella fascia.
 */
fun pickRegularIssueShowcaseUrls(series: List<RegularIssueSeries>, daySeed: Long?): List<String?> {
    val random = daySeed?.let(::Random)
    return DENOMINATION_TIERS.mapIndexed { index, tier ->
        val pool = series.flatMap { s -> s.immagini.filter { it.taglio in tier }.mapNotNull { it.urlImmagineFonte } }
        when {
            pool.isEmpty() -> null
            random != null -> pool[random.nextInt(pool.size)]
            else -> curatedUrl(series, CURATED_DEFAULTS[index]) ?: pool.first()
        }
    }
}

private fun curatedUrl(series: List<RegularIssueSeries>, pick: Pair<String, String>): String? {
    val (paese, taglio) = pick
    return series.firstOrNull { it.paese == paese }?.immagini?.firstOrNull { it.taglio == taglio }?.urlImmagineFonte
}
