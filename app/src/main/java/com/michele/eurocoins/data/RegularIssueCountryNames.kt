package com.michele.eurocoins.data

/**
 * Nome inglese del paese per la UI di Regular Issues, chiave su `paese`
 * (lo stesso valore stabile usato da [Coin.paese] — vedi RegularIssue.kt).
 *
 * A differenza di [Coin.displayCountry], qui non c'è un `zeccaRaw` inglese
 * su cui ripiegare: per questo dataset `zeccaRaw` è lo slug minuscolo della
 * pagina sorgente (es. "andorra"), non un nome leggibile. La mappa è quindi
 * esaustiva sui 25 valori noti, non solo un'eccezione come
 * `CANONICAL_COUNTRY_NAMES` in CountryNames.kt. Se un paese nuovo arrivasse
 * senza essere aggiunto qui, si ricade sul valore `paese` grezzo (in
 * italiano) invece di un crash.
 */
private val REGULAR_ISSUE_COUNTRY_NAMES: Map<String, String> = mapOf(
    "Andorra" to "Andorra",
    "Austria" to "Austria",
    "Belgio" to "Belgium",
    "Bulgaria" to "Bulgaria",
    "Cipro" to "Cyprus",
    "Città del Vaticano" to "Vatican City",
    "Croazia" to "Croatia",
    "Estonia" to "Estonia",
    "Finlandia" to "Finland",
    "Francia" to "France",
    "Germania" to "Germany",
    "Grecia" to "Greece",
    "Irlanda" to "Ireland",
    "Italia" to "Italy",
    "Lettonia" to "Latvia",
    "Lituania" to "Lithuania",
    "Lussemburgo" to "Luxembourg",
    "Malta" to "Malta",
    "Monaco" to "Monaco",
    "Paesi Bassi" to "Netherlands",
    "Portogallo" to "Portugal",
    "San Marino" to "San Marino",
    "Slovacchia" to "Slovakia",
    "Slovenia" to "Slovenia",
    "Spagna" to "Spain",
)

fun RegularIssueSeries.displayCountry(): String = REGULAR_ISSUE_COUNTRY_NAMES[paese] ?: paese
