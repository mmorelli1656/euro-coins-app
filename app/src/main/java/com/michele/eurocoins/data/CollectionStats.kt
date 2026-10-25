package com.michele.eurocoins.data

import java.time.LocalDate

/** Il completamento di un paese in un catalogo ([name] già nel nome mostrato in UI). */
data class CountryStat(val paese: String, val name: String, val progress: Progress)

/** Il completamento di un anno delle commemorative. */
data class YearStat(val anno: Int, val progress: Progress)

/** Quante monete commemorative hanno almeno una voce in ciascuna finitura (una moneta può averne più d'una). */
data class FinishCounts(val standard: Int, val bu: Int, val proof: Int)

/**
 * I traguardi: ciascuno è "completi / totali". [countries] e [years] contano le commemorative, [regularSeries] le serie
 * divisionali con tutti i loro tagli (8 per serie), [jointIssues] le cinque emissioni comuni con la moneta di ogni paese.
 */
data class Milestones(
    val countries: Progress,
    val years: Progress,
    val regularSeries: Progress,
    val jointIssues: Progress,
)

/**
 * Quanto speso, solo da ciò che l'utente ha scritto (prezzo e data sono facoltativi). [itemsWithPrice] su [totalItems] è
 * la copertura che la schermata mostra accanto ai numeri: senza, un totale parziale si leggerebbe come completo.
 * [byYear] è la spesa per anno di acquisto delle sole voci che hanno SIA il prezzo SIA la data ([itemsInYears]).
 */
data class SpendingStats(
    val totalCents: Long,
    val itemsWithPrice: Int,
    val totalItems: Int,
    val byYear: List<Pair<Int, Long>>,
    val itemsInYears: Int,
) {
    val averageCents: Long get() = if (itemsWithPrice == 0) 0 else totalCents / itemsWithPrice
}

/** Tutti i numeri della scheda Statistics di Insights, calcolati in un colpo solo da catalogo e collezione. */
data class CollectionStats(
    val commemorative: Progress,
    val regular: Progress,
    val commemorativeCountries: List<CountryStat>,
    val regularCountries: List<CountryStat>,
    val years: List<YearStat>,
    val milestones: Milestones,
    val finishes: FinishCounts,
    val spending: SpendingStats?,
) {
    /** Le due barre insieme: 93 / 613 e 195 / 328 fanno 288 / 941. */
    val total: Progress get() = Progress(commemorative.owned + regular.owned, commemorative.total + regular.total)
}

/**
 * Calcola [CollectionStats]. Una moneta commemorativa vale se ha almeno una finitura; una riga Regular (un taglio di
 * una serie) se ha almeno un'annata dentro la finestra della serie: gli stessi criteri della Home e delle barre dei paesi,
 * quindi i totali coincidono con quelli già visibili (93 / 613 e 195 / 328). Il catalogo arriva già senza i
 * microstati nascosti dalle Impostazioni; la spesa invece conta TUTTE le voci, perché i soldi spesi sono spesi anche per
 * una moneta che si è scelto di non vedere.
 */
fun collectionStats(
    coins: List<Coin>,
    items: List<CollectionItem>,
    series: List<RegularIssueSeries>,
    regularItems: List<RegularCollectionItem>,
): CollectionStats {
    val owned = items.map { it.coinKey }.toSet()
    val visibleKeys = coins.map { it.stableKey }.toSet()

    val countries = coins.groupBy { it.paese }.map { (paese, list) ->
        CountryStat(paese, list.first().displayCountry(), list.progress(owned))
    }
    val years = coins.groupBy { it.anno }.toSortedMap().map { (anno, list) -> YearStat(anno, list.progress(owned)) }
    val joint = coins.filter { it.emissioneComune }.groupBy { it.anno }.values.map { it.progress(owned) }

    val regularCountries = series.groupBy { it.paese }.map { (paese, list) ->
        CountryStat(paese, list.first().displayCountry(), list.regularProgress(regularItems))
    }
    val byRow = regularItems.groupBy { it.seriesKey to it.taglio }
    val seriesComplete = series.groupBy { it.paese }.values.flatMap { country ->
        country.mapNotNull { s ->
            val rows = denominationsOf(country, s)
            if (rows.isEmpty()) null else rows.all { it.rowItemsFrom(byRow).isNotEmpty() }
        }
    }

    val finishItems = items.filter { it.coinKey in visibleKeys }
    fun finish(quality: CoinQuality) = finishItems.filter { it.quality == quality }.map { it.coinKey }.toSet().size

    return CollectionStats(
        commemorative = coins.progress(owned),
        regular = series.regularProgress(regularItems),
        commemorativeCountries = countries,
        regularCountries = regularCountries,
        years = years,
        milestones = Milestones(
            countries = completeOf(countries.map { it.progress }),
            years = completeOf(years.map { it.progress }),
            regularSeries = Progress(seriesComplete.count { it }, seriesComplete.size),
            jointIssues = completeOf(joint),
        ),
        finishes = FinishCounts(finish(CoinQuality.STANDARD), finish(CoinQuality.BU), finish(CoinQuality.PROOF)),
        spending = spendingOf(items, regularItems),
    )
}

/** Quanti gruppi sono completi su quanti sono (un gruppo vuoto non conta come completo). */
private fun completeOf(groups: List<Progress>): Progress =
    Progress(owned = groups.count { it.total > 0 && it.owned == it.total }, total = groups.size)

/** `null` se nessuna voce ha un prezzo: la card della spesa non deve comparire senza dati. */
internal fun spendingOf(items: List<CollectionItem>, regularItems: List<RegularCollectionItem>): SpendingStats? {
    // (prezzo, giorno di acquisto) di ogni voce. Un prezzo a 0 vale "non indicato", come altrove nell'app.
    val entries = items.map { it.priceCents to it.purchasedOn } + regularItems.map { it.priceCents to it.purchasedOn }
    val priced = entries.filter { (price, _) -> price != null && price > 0 }
    if (priced.isEmpty()) return null
    val dated = priced.filter { (_, day) -> day != null }
    val byYear = dated
        .groupBy { (_, day) -> LocalDate.ofEpochDay(day!!).year }
        .map { (year, list) -> year to list.sumOf { (price, _) -> price!!.toLong() } }
        .sortedBy { it.first }
    return SpendingStats(
        totalCents = priced.sumOf { (price, _) -> price!!.toLong() },
        itemsWithPrice = priced.size,
        totalItems = entries.size,
        byYear = byYear,
        itemsInYears = dated.size,
    )
}
