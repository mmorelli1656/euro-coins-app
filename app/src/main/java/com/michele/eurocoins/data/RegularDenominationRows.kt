package com.michele.eurocoins.data

/**
 * Una riga dell'elenco di un taglio: quel taglio in UNA serie di UN paese, come lo vede la serie
 * ([denomination]: serie di origine e immagine già ritagliata alla finestra di anni, vedi
 * [denominationsOf]). [viewedSeries] è la serie in cui compare la riga (e la cui posizione è
 * [seriesNumber], 1-based come nei chip), [items] le annate in collezione dentro la finestra.
 */
data class DenominationRow(
    val paese: String,
    val countryName: String,
    val flag: String,
    val viewedSeries: RegularIssueSeries,
    val seriesNumber: Int,
    val period: SeriesPeriod,
    val denomination: SeriesDenomination,
    val items: List<RegularCollectionItem>,
) {
    /** Annate distinte possedute dentro la finestra (come "N years owned" nella schermata del paese). */
    val ownedYears: Int get() = items.map { it.anno }.distinct().size
    val owned: Boolean get() = items.isNotEmpty()

    /** "Series 2 · 2008 – 2013", o solo "Series 2" se il periodo non è noto. */
    val seriesLabel: String
        get() = period.label?.let { "${seriesTitle(seriesNumber)} · $it" } ?: seriesTitle(seriesNumber)
}

/**
 * Le righe di ogni taglio ([REGULAR_DENOMINATIONS]) in tutti i paesi: una per serie (41 per taglio),
 * ordinate per nome del paese e poi per serie. Stesso conto di [regularProgress]: la riga è
 * posseduta se il taglio ha almeno un'annata in collezione dentro la finestra della serie.
 */
fun denominationRows(
    all: List<RegularIssueSeries>,
    collection: List<RegularCollectionItem>,
): Map<String, List<DenominationRow>> {
    val byKey = collection.groupBy { it.seriesKey to it.taglio }
    val rows = all.groupBy { it.paese }.values
        .map { it.sortedBy { s -> s.ordineCronologico } }
        .sortedBy { it.first().displayCountry() }
        .flatMap { country ->
            country.flatMapIndexed { index, series ->
                val period = seriesPeriod(country, series)
                denominationsOf(country, series).map { d ->
                    DenominationRow(
                        paese = series.paese,
                        countryName = series.displayCountry(),
                        flag = flagEmojiForCountry(series.paese),
                        viewedSeries = series,
                        seriesNumber = index + 1,
                        period = period,
                        denomination = d,
                        items = byKey[d.series.stableKey to d.image.taglio].orEmpty().filter { d.contains(it.anno) },
                    )
                }
            }
        }
    return REGULAR_DENOMINATIONS.associateWith { taglio -> rows.filter { it.denomination.image.taglio == taglio } }
}

/** Avanzamento di un taglio: righe possedute su righe totali (tutti i paesi e le serie). */
fun List<DenominationRow>.denominationProgress(): Progress = Progress(owned = count { it.owned }, total = size)
