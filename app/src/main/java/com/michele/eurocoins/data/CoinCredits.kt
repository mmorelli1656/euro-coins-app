package com.michele.eurocoins.data

/**
 * Nome della zecca fisica che ha coniato la moneta (es. "Rome"), non il paese emittente
 * (`displayCountry()`). Fonte: campo 'mints' di Numista, più valori uniti da "; " quando la
 * moneta è stata coniata in più zecche (unite anche se identiche: es. "Royal Dutch Mint;
 * Royal Dutch Mint" nel dataset grezzo). La Germania è il caso limite: ogni moneta è coniata in
 * tutte e 5 le zecche regionali (mintmark A/D/F/G/J), quindi lo stesso elenco fisso e lunghissimo
 * di 5 nomi si ripete identico su 33 monete — mostrarlo per intero eccede sempre le 2 righe della
 * card e tronca in un punto qualunque, senza comunicare nulla. Oltre le 3 zecche uniche si passa
 * a un conteggio ("5 mints"); con 2-3 restano elencate (leggibili anche troncate).
 */
fun Coin.displayMint(): String? = formatMints(zeccaFisicaRaw)

/** Stesso criterio di [Coin.displayMint], per la zecca di un taglio delle Regular Issues. */
fun RegularIssueImage.displayMint(): String? = formatMints(zeccaFisicaRaw)

/** Incisore del lato nazionale del taglio; ruolo distinto da [displayDesigner], vedi [Coin.displayEngraver]. */
fun RegularIssueImage.displayEngraver(): String? = incisoreRaw?.takeIf { it.isNotBlank() }

fun RegularIssueImage.displayDesigner(): String? = disegnatoreRaw?.takeIf { it.isNotBlank() }

private fun formatMints(raw: String?): String? {
    val mints = raw?.split("; ")?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct().orEmpty()
    return when {
        mints.isEmpty() -> null
        mints.size == 1 -> mints.first()
        mints.size <= 3 -> mints.joinToString(", ")
        else -> "${mints.size} mints"
    }
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
