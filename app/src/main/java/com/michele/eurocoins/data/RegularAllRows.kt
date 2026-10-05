package com.michele.eurocoins.data

/** Cosa viene prima nell'elenco "All": le righe si raggruppano per paese o per taglio. */
enum class RegularAllGroup(val label: String) {
    COUNTRY("Country"),
    VALUE("Value"),
}

/**
 * Ordine dell'elenco "All" di Regular Issues: tre scelte indipendenti. [group] dice quale asse ha la
 * precedenza (paese: tutte le righe di un paese insieme; taglio: tutte le righe di un taglio insieme),
 * le altre due la direzione di ciascun asse. Il predefinito è il vecchio "Country A → Z".
 */
data class RegularAllOrder(
    val group: RegularAllGroup = RegularAllGroup.COUNTRY,
    val countryAscending: Boolean = true,
    val largestFirst: Boolean = true,
)

/**
 * Tutte le righe di tutti i tagli in un solo elenco (328 = 41 serie × 8), nell'ordine scelto.
 * Parte da [denominationRows] (già ordinate per paese e serie dentro ogni taglio), quindi il
 * possesso e le finestre di anni sono gli stessi di ogni altra schermata. Dentro lo stesso paese la
 * serie va sempre dalla 1 in su. [REGULAR_DENOMINATIONS] è dal più piccolo al più grande.
 */
fun allDenominationRows(
    byDenomination: Map<String, List<DenominationRow>>,
    order: RegularAllOrder,
): List<DenominationRow> {
    val rows = REGULAR_DENOMINATIONS.flatMap { byDenomination[it].orEmpty() }
    val rank = REGULAR_DENOMINATIONS.withIndex().associate { (index, taglio) -> taglio to index }
    fun DenominationRow.rank() = rank[denomination.image.taglio] ?: Int.MAX_VALUE
    val byValue = if (order.largestFirst) compareByDescending<DenominationRow> { it.rank() } else compareBy { it.rank() }
    val byCountry = if (order.countryAscending) compareBy<DenominationRow> { it.countryName } else compareByDescending { it.countryName }
    val bySeries = compareBy<DenominationRow> { it.seriesNumber }
    return when (order.group) {
        RegularAllGroup.COUNTRY -> rows.sortedWith(byCountry.then(bySeries).then(byValue))
        RegularAllGroup.VALUE -> rows.sortedWith(byValue.then(byCountry).then(bySeries))
    }
}

/** Filtro "Owned quality": vuoto = nessun vincolo, altrimenti almeno un'annata della riga in una di queste qualità. */
fun DenominationRow.ownsAnyQuality(qualities: Set<CoinQuality>): Boolean =
    qualities.isEmpty() || items.any { it.quality in qualities }

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
