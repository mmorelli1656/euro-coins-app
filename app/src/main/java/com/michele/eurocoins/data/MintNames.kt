package com.michele.eurocoins.data

/**
 * Zecca fisica → paese della zecca ("Rome" → "Italy"), per [displayMint]. Il dataset
 * mescola città ("Rome", "Berlin", "Kremnica"), istituzioni ("Monnaie de Paris", "Royal Dutch
 * Mint") e nomi lunghissimi ("State Mint of Stuttgart / State Mints of Baden-Württemberg"):
 * il nome del paese li porta tutti allo stesso tipo di etichetta (scelto dall'utente al posto
 * dell'aggettivo "Italian") e risponde a "dove è stata coniata", che non coincide con il paese
 * emittente (Vaticano e San Marino → "Italy", Lettonia → "Germany").
 * Niente "Mint" nel valore: l'etichetta DETAILS già lo dice.
 *
 * Le stringhe grezze restano intatte in `Coin.zeccaFisicaRaw`/`RegularIssueImage.zeccaFisicaRaw`
 * (e nel dataset della pipeline): questa è solo la forma mostrata, non un dato sostituito, così
 * il nome istituzionale o la città restano disponibili per usi futuri (es. zecca per anno).
 * Le 5 zecche tedesche (Berlin, Munich, Stuttgart, Karlsruhe, Hamburg) collassano da sole in un
 * solo "Germany" perché [displayMint] toglie i doppioni dopo la mappatura.
 *
 * Un valore non in tabella compare com'è scritto nel dataset (si nota e si aggiunge una riga:
 * `MintNamesTest` lo segnala per i dataset in repo). Elenco ricavato da coins_with_mintages e
 * numista_divisional della pipeline.
 */
private val MINT_COUNTRY = mapOf(
    "Austrian Mint" to "Austria",
    "Berlin" to "Germany",
    "Hamburg Mint" to "Germany",
    "Munich" to "Germany",
    "State Mint of Karlsruhe / State Mints of Baden-Württemberg" to "Germany",
    "State Mint of Stuttgart / State Mints of Baden-Württemberg" to "Germany",
    "Croatian Mint" to "Croatia",
    "Imprensa Nacional - Casa da Moeda" to "Portugal",
    "Irish Mint" to "Ireland",
    "Kremnica" to "Slovakia",
    "Lithuanian Mint" to "Lithuania",
    "Mint of Finland" to "Finland",
    "Mint of Poland" to "Poland",
    "Monnaie de Paris" to "France",
    "National Mint of the Bank of Greece" to "Greece",
    "Rome" to "Italy",
    "Royal Dutch Mint" to "Netherlands",
    "Royal Mint" to "United Kingdom",
    "Royal Mint of Belgium" to "Belgium",
    "Royal Mint of Madrid" to "Spain",
)

/** Paese della zecca; il testo grezzo se non è in tabella. */
fun mintCountry(raw: String): String = MINT_COUNTRY[raw] ?: raw

/** `true` se [raw] ha un paese in tabella (per i test sul dataset). */
internal fun isKnownMint(raw: String): Boolean = raw in MINT_COUNTRY
