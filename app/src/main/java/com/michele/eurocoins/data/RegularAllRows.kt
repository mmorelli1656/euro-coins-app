package com.michele.eurocoins.data

/** Ordine dell'elenco "All" di Regular Issues. */
enum class RegularAllSort(val label: String) {
    COUNTRY_ASC("Country A → Z"),
    COUNTRY_DESC("Country Z → A"),
    LARGEST_FIRST("Largest first"),
    SMALLEST_FIRST("Smallest first"),
}

/**
 * Tutte le righe di tutti i tagli in un solo elenco (328 = 41 serie × 8), nell'ordine scelto.
 * Parte da [denominationRows] (già ordinate per paese e serie dentro ogni taglio), quindi il
 * possesso e le finestre di anni sono gli stessi di ogni altra schermata. Ordine per paese:
 * paese, serie, taglio dal più grande; per taglio: taglio, paese, serie. [REGULAR_DENOMINATIONS]
 * è dal più piccolo al più grande.
 */
fun allDenominationRows(
    byDenomination: Map<String, List<DenominationRow>>,
    sort: RegularAllSort,
): List<DenominationRow> {
    return when (sort) {
        RegularAllSort.SMALLEST_FIRST -> REGULAR_DENOMINATIONS.flatMap { byDenomination[it].orEmpty() }
        RegularAllSort.LARGEST_FIRST -> REGULAR_DENOMINATIONS.asReversed().flatMap { byDenomination[it].orEmpty() }
        // sortedWith è stabile: l'ordine di partenza (taglio dal più grande) resta dentro serie e paese.
        RegularAllSort.COUNTRY_ASC -> REGULAR_DENOMINATIONS.asReversed().flatMap { byDenomination[it].orEmpty() }
            .sortedWith(compareBy<DenominationRow> { it.countryName }.thenBy { it.seriesNumber })
        RegularAllSort.COUNTRY_DESC -> REGULAR_DENOMINATIONS.asReversed().flatMap { byDenomination[it].orEmpty() }
            .sortedWith(compareByDescending<DenominationRow> { it.countryName }.thenBy { it.seriesNumber })
    }
}

private val DENOMINATION_IN_QUERY = Regex("""(\d+)\s*(euro|cent)s?""", RegexOption.IGNORE_CASE)
private val SERIES_IN_QUERY = Regex("""series\s*(\d+)""", RegexOption.IGNORE_CASE)

/**
 * Ricerca nell'elenco "All": paese, serie, periodo ("2022") e taglio. Una serie ("series 2") e un taglio
 * scritto per intero ("5 cent", "2euro") devono coincidere con quelli della riga — altrimenti "2" troverebbe
 * anche le serie con un 2 negli anni e "5" i 50 cent —, il resto della query si cerca in paese, serie e
 * periodo; "euro" o "cent" da soli trovano tutti i tagli di quel tipo.
 */
fun DenominationRow.matchesAllQuery(query: String): Boolean {
    var rest = query.trim()
    if (rest.isEmpty()) return true
    val series = SERIES_IN_QUERY.find(rest)
    if (series != null) {
        if (series.groupValues[1].toIntOrNull() != seriesNumber) return false
        rest = rest.removeRange(series.range).trim()
        if (rest.isEmpty()) return true
    }
    val wanted = DENOMINATION_IN_QUERY.find(rest)
    if (wanted != null) {
        val (number, unit) = wanted.destructured
        if (denomination.image.taglio.replace(" ", "") != "$number${unit.lowercase()}") return false
        rest = rest.removeRange(wanted.range).trim()
        if (rest.isEmpty()) return true
    }
    val fields = listOf(countryName, paese, seriesLabel, denomination.image.taglio)
    fun has(text: String) = fields.any { it.contains(text, ignoreCase = true) }
    return has(rest) || rest.split(Regex("\\s+")).all(::has)
}
