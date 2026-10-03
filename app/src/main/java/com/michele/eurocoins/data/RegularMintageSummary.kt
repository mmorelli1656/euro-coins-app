package com.michele.eurocoins.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Tiratura riassunta di una qualità nella card compatta: total e caption null se non c'è alcun dato.
 * [isSum] = somma su più anni, da mostrare arrotondata con [formatApproxTotal]; un anno solo resta esatto.
 */
data class MintageSummary(val total: Long?, val caption: String?, val isSum: Boolean = false)

/**
 * Somma le tirature di [quality] su tutti gli anni. Con una sola annata la didascalia è l'anno
 * stesso (niente da sommare, coerente con le commemorative che mostrano solo l'anno); con più
 * annate è "all years" — senza, la somma si legge come se fosse la tiratura di un solo anno,
 * un ordine di grandezza fuorviante per chi guarda la card.
 *
 * **"all years" solo se quella qualità ha un dato in OGNI anno in cui il taglio ne ha uno** (per
 * qualunque qualità): Numista spesso non ha lo standard di un anno (o la qualità non è mai stata
 * coniata quell'anno: BU/Proof esistono solo in alcuni), e una somma su 12 anni su 25 spacciata
 * per "all years" sarebbe un totale sbagliato scritto come esatto. Con buchi la didascalia
 * diventa "12 of 25 years" — il numero resta quello che si può sommare, ma dichiara su quanti
 * anni poggia.
 */
fun summarizeMintages(tirature: List<RegularIssueMintage>, quality: CoinQuality): MintageSummary {
    val forQuality = tirature.filter { it.quality == quality }
    return when (forQuality.size) {
        0 -> MintageSummary(null, null)
        1 -> MintageSummary(forQuality[0].tiratura, forQuality[0].anno.toString())
        else -> {
            val yearsWithData = forQuality.map { it.anno }.distinct().size
            val allYears = tirature.map { it.anno }.distinct().size
            val caption = if (yearsWithData == allYears) "all years" else "$yearsWithData of $allYears years"
            MintageSummary(forQuality.sumOf { it.tiratura }, caption, isSum = true)
        }
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
fun groupMintagesByYear(tirature: List<RegularIssueMintage>): List<Pair<Int, Map<CoinQuality, Long>>> =
    tirature.groupBy { it.anno }
        .toSortedMap()
        .map { (year, entries) -> year to entries.associate { it.quality to it.tiratura } }

/**
 * Somma su più anni a 3 cifre significative, per la card compatta: "≈ 7.87 B", "≈ 1.32 M",
 * "≈ 258,000". Il "≈" compare solo se l'arrotondamento ha davvero cambiato il numero (una somma di
 * 300.000 resta "300,000"). Perché arrotondare: sono totali di decine di anni, la cifra a 10 cifre
 * (7,865,704,755) non stava in una colonna da un terzo e prometteva una precisione che una somma
 * di dati incompleti non ha; l'esatto sta nel pannello "View by year". Miliardi con "B" e
 * milioni con "M": scelta dell'utente (la "B" accanto all'etichetta "BU" è accettata).
 */
fun formatApproxTotal(total: Long): String {
    val rounded = BigDecimal(total).round(MathContext(3, RoundingMode.HALF_UP))
    val value = rounded.toLong()
    val text = when {
        value >= 1_000_000_000L -> BigDecimal(value).movePointLeft(9).stripTrailingZeros().toPlainString() + " B"
        value >= 1_000_000L -> BigDecimal(value).movePointLeft(6).stripTrailingZeros().toPlainString() + " M"
        else -> NumberFormat.getIntegerInstance(Locale.ENGLISH).format(value)
    }
    return if (value == total) text else "≈ $text"
}
