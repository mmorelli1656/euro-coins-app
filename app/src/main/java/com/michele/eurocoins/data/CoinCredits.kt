package com.michele.eurocoins.data

/**
 * Dove è stata coniata la moneta, come paese della zecca ("Italy", "Netherlands"; vedi
 * [mintCountry] in MintNames.kt), non il paese emittente (`displayCountry()`).
 * Fonte: campo 'mints' di Numista, più valori uniti da "; " quando la moneta è stata coniata in
 * più zecche (anche identiche: es. "Royal Dutch Mint; Royal Dutch Mint" nel dataset grezzo).
 * Le 5 zecche regionali tedesche (mintmark A/D/F/G/J, su ogni moneta tedesca) collassano in un
 * solo "Germany" dopo la mappatura, quindi non serve più il conteggio "5 mints" che serviva
 * quando si mostravano i nomi per esteso. Il valore non contiene "Mint": c'è già l'etichetta.
 */
fun Coin.displayMint(): String? = formatMints(zeccaFisicaRaw)

/** Stesso criterio di [Coin.displayMint], per la zecca di un taglio delle Regular Issues. */
fun RegularIssueImage.displayMint(): String? = formatMints(zeccaFisicaRaw)

/** Incisore del lato nazionale del taglio; ruolo distinto da [displayDesigner], vedi [Coin.displayEngraver]. */
fun RegularIssueImage.displayEngraver(): String? = incisoreRaw?.takeIf { it.isNotBlank() }

fun RegularIssueImage.displayDesigner(): String? = disegnatoreRaw?.takeIf { it.isNotBlank() }

private fun formatMints(raw: String?): String? {
    // distinct() DOPO la mappatura: Berlin/Munich/... sono cinque stringhe e un solo "Germany"
    val mints = raw?.split("; ")?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?.map(::mintCountry)?.distinct().orEmpty()
    return mints.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

/**
 * Chi ha inciso il conio del disegno commemorativo (non il lato comune europeo, quasi sempre
 * "Luc Luycx" e non mostrato in UI). Ruolo distinto da [displayDesigner] (chi ha ideato il
 * soggetto): la pipeline dati li tiene separati apposta, spesso persone diverse — 25 monete su
 * 584 hanno entrambi i campi valorizzati con nomi diversi (es. Lettonia 2014-2017: incisore
 * sempre "Jānis Strupulis", disegnatore diverso ogni anno). NON un ripiego l'uno dell'altro:
 * mostrarne solo uno perderebbe l'informazione su quelle monete.
 */
fun Coin.displayEngraver(): String? = incisoreRetroRaw?.takeIf { it.isNotBlank() }

/** Chi ha ideato il soggetto del disegno commemorativo; vedi [displayEngraver]. */
fun Coin.displayDesigner(): String? = disegnatoreRetroRaw?.takeIf { it.isNotBlank() }
