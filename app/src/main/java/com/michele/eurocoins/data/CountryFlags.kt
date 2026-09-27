package com.michele.eurocoins.data

/**
 * Codice ISO 3166-1 alpha-2 per ciascun paese, chiave su `paese` (lo stesso
 * campo stabile usato da [displayCountry] in CountryNames.kt — non su
 * `zeccaRaw`, che varia). Copre i 25 valori dell'enum `ZeccaEmittente` della
 * pipeline (i 24 storici più `Bulgaria`, aggiunta con le serie divisionali —
 * vedi RegularIssue.kt): se un domani ne arriva un altro non mappato qui,
 * [flagEmojiForCountry] ricade su una bandiera "sconosciuta" invece di
 * lanciare un'eccezione.
 */
private val ISO_COUNTRY_CODES: Map<String, String> = mapOf(
    "Bulgaria" to "BG",
    "Austria" to "AT",
    "Belgio" to "BE",
    "Cipro" to "CY",
    "Croazia" to "HR",
    "Estonia" to "EE",
    "Finlandia" to "FI",
    "Francia" to "FR",
    "Germania" to "DE",
    "Grecia" to "GR",
    "Irlanda" to "IE",
    "Italia" to "IT",
    "Lettonia" to "LV",
    "Lituania" to "LT",
    "Lussemburgo" to "LU",
    "Malta" to "MT",
    "Monaco" to "MC",
    "Paesi Bassi" to "NL",
    "Portogallo" to "PT",
    "San Marino" to "SM",
    "Slovacchia" to "SK",
    "Slovenia" to "SI",
    "Spagna" to "ES",
    "Città del Vaticano" to "VA",
    "Andorra" to "AD",
)

/** Bandiera mostrata quando il paese non è nella mappa sopra (non dovrebbe succedere). */
private const val UNKNOWN_FLAG = "🏳️" // 🏳️ bandiera bianca

/** Un carattere "Regional Indicator Symbol" (U+1F1E6.. per A..Z): due di seguito compongono una bandiera. */
private fun regionalIndicatorSymbol(letter: Char): String =
    String(Character.toChars(0x1F1E6 + (letter.uppercaseChar() - 'A')))

private fun flagEmojiForIsoCode(isoCode: String): String =
    isoCode.map(::regionalIndicatorSymbol).joinToString(separator = "")

/** Bandiera per un valore `paese` grezzo: condivisa tra `Coin` e `RegularIssueSeries`, che usano lo stesso set di valori. */
fun flagEmojiForCountry(paese: String): String = ISO_COUNTRY_CODES[paese]?.let(::flagEmojiForIsoCode) ?: UNKNOWN_FLAG

fun Coin.flagEmoji(): String = flagEmojiForCountry(paese)
