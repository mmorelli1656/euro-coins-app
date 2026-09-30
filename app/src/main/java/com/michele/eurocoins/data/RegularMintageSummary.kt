package com.michele.eurocoins.data

/** Tiratura riassunta di una qualità nella card compatta: entrambi null se non c'è alcun dato. */
data class MintageSummary(val total: Int?, val caption: String?)

/**
 * Somma le tirature di [quality] su tutti gli anni. Con una sola annata la didascalia è l'anno
 * stesso (niente da sommare, coerente con le commemorative che mostrano solo l'anno); con più
 * annate è "all years" — senza, la somma si legge come se fosse la tiratura di un solo anno,
 * un ordine di grandezza fuorviante per chi guarda la card.
 */
fun summarizeMintages(tirature: List<RegularIssueMintage>, quality: CoinQuality): MintageSummary {
    val forQuality = tirature.filter { it.quality == quality }
    return when (forQuality.size) {
        0 -> MintageSummary(null, null)
        1 -> MintageSummary(forQuality[0].tiratura, forQuality[0].anno.toString())
        else -> MintageSummary(forQuality.sumOf { it.tiratura }, "all years")
    }
}

/**
 * Righe della tabella "by year": un anno per riga (ordine crescente), valore per qualità o
 * assente se quella qualità non ha un dato per quell'anno. Include solo gli anni per cui esiste
 * almeno una tiratura: una serie uscita nel 2009 e terminata nel 2015 non genera righe fuori da
 * quell'intervallo, perché semplicemente non c'è nessuna tiratura da mostrare per quegli anni —
 * non serve un campo separato "anno di inizio/fine serie" (che tra l'altro non è affidabile nel
 * dataset, vedi `RegularIssueSeries.anniCitati`).
 */
fun groupMintagesByYear(tirature: List<RegularIssueMintage>): List<Pair<Int, Map<CoinQuality, Int>>> =
    tirature.groupBy { it.anno }
        .toSortedMap()
        .map { (year, entries) -> year to entries.associate { it.quality to it.tiratura } }
