package com.michele.eurocoins.data

/** Quanto è affidabile la zecca di un anno (livelli della pipeline: certa, probabile, non nota). */
enum class MintLevel { CERTAIN, PROBABLE, UNKNOWN }

/**
 * Etichetta "Mint · …" di un periodo della tabella "by year". [countries] sono i paesi delle zecche
 * (via [mintCountry], senza doppioni, nell'ordine del dato); vuoti se [level] è [MintLevel.UNKNOWN].
 * Due anni hanno la stessa etichetta se hanno lo stesso livello e gli stessi paesi, in qualunque ordine.
 */
data class YearMintLabel(val level: MintLevel, val countries: List<String>) {
    /** Testo dopo "Mint ·": "Finland", "Greece, Spain", "Finland ?" (probabile), "not known". */
    val text: String
        get() = when (level) {
            MintLevel.CERTAIN -> countries.joinToString(", ")
            MintLevel.PROBABLE -> countries.joinToString(", ") + " ?"
            MintLevel.UNKNOWN -> "not known"
        }

    /** Chiave di confronto per raggruppare gli anni consecutivi in un solo periodo. */
    val sameAs: Pair<MintLevel, Set<String>> get() = level to countries.toSet()
}

/**
 * Etichetta della zecca per ciascuno di [years] (gli anni mostrati nella tabella), oppure mappa
 * VUOTA se non c'è niente da dire: la tabella resta com'era per i tagli a zecca unica (Italia,
 * Austria, Germania con le sue 5 zecche che diventano un solo "Germany"...) e per quelli senza
 * alcun dato (asset `-ExcludeNumista`, Bulgaria). Le etichette si mostrano solo se i paesi noti
 * sono più di uno nel corso degli anni, o se c'è almeno un anno non noto accanto a uno noto: una
 * riga "Mint · not known" accanto a un periodo noto dice qualcosa, da sola sarebbe rumore.
 *
 * Per anno: zecche certe se ci sono, altrimenti le probabili (mai mescolate: il probabile non va
 * presentato come certo), altrimenti [MintLevel.UNKNOWN] — anche per un anno senza elemento in
 * [zecchePerAnno]: "non nota" non significa "nessuna zecca".
 */
fun yearMintLabels(zecchePerAnno: List<RegularIssueYearMint>, years: Collection<Int>): Map<Int, YearMintLabel> {
    if (years.isEmpty()) return emptyMap()
    val byYear = zecchePerAnno.associateBy { it.anno }
    val labels = years.associateWith { year ->
        val entry = byYear[year]
        val certain = entry?.zecche.orEmpty().toCountries()
        val probable = entry?.probabili.orEmpty().toCountries()
        when {
            certain.isNotEmpty() -> YearMintLabel(MintLevel.CERTAIN, certain)
            probable.isNotEmpty() -> YearMintLabel(MintLevel.PROBABLE, probable)
            else -> YearMintLabel(MintLevel.UNKNOWN, emptyList())
        }
    }
    val known = labels.values.filter { it.level != MintLevel.UNKNOWN }
    val knownCountries = known.flatMap { it.countries }.toSet()
    val hasUnknown = known.size < labels.size
    return if (knownCountries.size > 1 || (hasUnknown && knownCountries.isNotEmpty())) labels else emptyMap()
}

private fun List<String>.toCountries(): List<String> =
    map { it.trim() }.filter { it.isNotEmpty() }.map(::mintCountry).distinct()
