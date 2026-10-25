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
    /** Tutte le voci con un prezzo, dalla più cara: il podio ne mostra tre, il pannello "See all" tutte. */
    val ranking: List<SpentItem> = emptyList(),
) {
    val averageCents: Long get() = if (itemsWithPrice == 0) 0 else totalCents / itemsWithPrice
}

/**
 * Una voce di collezione con un prezzo, già in forma da mostrare. Si classificano le VOCI (finitura, annata, zecca) e non
 * le monete: la stessa moneta in BU a 35 € e Standard a 5 € sono due righe, con la finitura che le distingue.
 * [what] è l'anno per le commemorative, "2 euro · 2005" per i tagli Regular; [finish] "BU", "Standard · D" (zecca)
 * o "Standard · EFS" (varietà). [imageUrl] è la foto del lato nazionale: null per le monete senza foto o fuori
 * catalogo (microstato nascosto dalle Impostazioni: la spesa le conta lo stesso, la foto non c'è).
 */
data class SpentItem(
    val country: String,
    val what: String,
    val finish: String,
    val imageUrl: String?,
    val isRegular: Boolean,
    val priceCents: Int,
    val purchasedOn: Long?,
)

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
        spending = spendingOf(items, regularItems, coins, series),
    )
}

/** Quanti gruppi sono completi su quanti sono (un gruppo vuoto non conta come completo). */
private fun completeOf(groups: List<Progress>): Progress =
    Progress(owned = groups.count { it.total > 0 && it.owned == it.total }, total = groups.size)

/** `null` se nessuna voce ha un prezzo: la card della spesa non deve comparire senza dati. */
internal fun spendingOf(
    items: List<CollectionItem>,
    regularItems: List<RegularCollectionItem>,
    coins: List<Coin> = emptyList(),
    series: List<RegularIssueSeries> = emptyList(),
): SpendingStats? {
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
        ranking = spentRanking(items, regularItems, coins, series),
    )
}

private fun mintSuffix(mint: String) = if (mint == NO_MINT) "" else " · $mint"

/** Le voci con prezzo, dalla più cara; a pari prezzo la più recente (data di acquisto) e poi il nome, per un ordine stabile. */
private fun spentRanking(
    items: List<CollectionItem>,
    regularItems: List<RegularCollectionItem>,
    coins: List<Coin>,
    series: List<RegularIssueSeries>,
): List<SpentItem> {
    val coinByKey = coins.associateBy { it.stableKey }
    val seriesByKey = series.associateBy { it.stableKey }
    val commemorative = items.filter { (it.priceCents ?: 0) > 0 }.map { item ->
        val coin = coinByKey[item.coinKey]
        SpentItem(
            country = coin?.displayCountry() ?: item.paese,
            what = (coin?.anno ?: item.anno).toString(),
            finish = item.quality.label + mintSuffix(item.mint),
            imageUrl = coin?.takeIf { !it.immaginePlaceholder }?.urlImmagineFonte,
            isRegular = false,
            priceCents = item.priceCents!!,
            purchasedOn = item.purchasedOn,
        )
    }
    val regular = regularItems.filter { (it.priceCents ?: 0) > 0 }.map { item ->
        val owner = seriesByKey[item.seriesKey]
        val year = if (item.anno == NO_YEAR) "" else " · ${item.anno}"
        SpentItem(
            country = owner?.displayCountry() ?: item.paese,
            what = item.taglio + year,
            finish = item.quality.label + (if (item.variety.isEmpty()) "" else " · ${item.variety}") + mintSuffix(item.mint),
            imageUrl = owner?.immagini?.firstOrNull { it.taglio == item.taglio }?.urlImmagineFonte,
            isRegular = true,
            priceCents = item.priceCents!!,
            purchasedOn = item.purchasedOn,
        )
    }
    return (commemorative + regular).sortedWith(
        compareByDescending<SpentItem> { it.priceCents }.thenByDescending { it.purchasedOn ?: Long.MIN_VALUE }.thenBy { it.country },
    )
}
