package com.michele.eurocoins.data

/**
 * Una voce dell'elenco anni del pannello di collezione: un anno, più la varietà quando quell'anno
 * ne offre una ([VARIETY_EFS], Grecia 2002). La varietà è una VOCE A PARTE e non un'opzione dentro
 * l'anno: il 2002 greco di Atene e il 2002 EFS sono due monete diverse che l'utente può avere
 * entrambe (stessa chiave di [RegularCollectionItem]), e come voci distinte il pannello resta
 * quello delle commemorative, tre card per finitura, senza un controllo in più.
 */
data class YearOption(val year: Int, val variety: String = "") {
    val label: String
        get() = if (variety.isEmpty()) year.toString() else "$year · ${varietyName(variety)}"
}

private fun varietyName(code: String): String = if (code == VARIETY_EFS) "EFS variety" else code

/** Primo anno dell'elenco quando il dato non c'è (asset vecchio): l'ingresso nell'euro. */
private const val FALLBACK_START_YEAR = 2002

/** Prima annata di circolazione dell'euro: il default non cade sulle monete datate 1999-2001. */
private const val FIRST_CIRCULATION_YEAR = 2002

/**
 * Anni tra cui l'utente sceglie, in ordine crescente: dal primo anno del taglio nella serie fino
 * all'ultimo (o all'anno corrente se la serie è aperta), con la voce EFS subito dopo il 2002
 * greco. Include anche gli anni GIÀ in collezione fuori da quell'intervallo ([owned]), per non
 * perdere voci salvate prima che l'anno fosse una scelta. Il range viene dalla serie, non dalle
 * tirature: dove il taglio non fu coniato in un anno (es. alcuni cent) la voce c'è comunque, è il
 * prezzo di non dipendere da Numista (assente dall'asset pubblico).
 */
fun regularYearOptions(
    series: RegularIssueSeries,
    image: RegularIssueImage,
    owned: Collection<YearOption>,
    currentYear: Int,
): List<YearOption> {
    val start = image.annoInizio ?: FALLBACK_START_YEAR
    val end = maxOf(minOf(image.annoFine ?: currentYear, currentYear), start)
    val options = (start..end).flatMap { year ->
        val variety = series.varietyFor(image.taglio, year)
        listOfNotNull(YearOption(year), variety?.let { YearOption(year, it.code) })
    }
    return (options + owned).distinct().sortedWith(compareBy({ it.year }, { it.variety }))
}

/**
 * Voce su cui si apre il pannello: la prima già in collezione (si apre sul tuo anno e "Edit" non
 * ti sposta altrove), altrimenti il PRIMO anno della serie (scelta dell'utente), ma non prima del
 * 2002: le monete datate 1999-2001 di Belgio/Francia/... entrano in circolazione nel 2002 ed
 * esistono nell'elenco, non come default.
 */
fun defaultYearOption(options: List<YearOption>, owned: Collection<YearOption>): YearOption =
    options.firstOrNull { it in owned }
        ?: options.firstOrNull { it.variety.isEmpty() && it.year >= FIRST_CIRCULATION_YEAR }
        ?: options.first()
