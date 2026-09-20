package com.michele.eurocoins.data

/**
 * Forma canonica del nome paese in inglese per la UI, quando `zeccaRaw` da
 * solo non basta.
 *
 * `zeccaRaw` è quasi sempre già il nome inglese così come letto dalla
 * pagina BCE della singola moneta (non una traduzione nostra), ma su 2 dei
 * 24 paesi pagine diverse usano testo leggermente diverso: "Vatican" (20
 * monete) contro "Vatican City" (14), "Netherlands" (3) contro "The
 * Netherlands" (1) (la forma canonica scelta è "Netherlands") — verificato sull'intero dataset. Usare `zeccaRaw` senza
 * normalizzare mostrerebbe lo stesso paese con due nomi diversi nella
 * stessa lista.
 *
 * Questa mappa sceglie una forma unica per questi 2 casi, chiave sul
 * valore `paese`/`zeccaEmittente` — l'unico dei tre campi sempre identico
 * su tutti i record dello stesso paese. Non è una traduzione: i valori
 * vengono da `zeccaRaw` stesso, solo disambiguati. Se la pipeline aggiunge
 * un paese non ancora qui, si ricade su `zeccaRaw` del singolo record
 * (nessun crash, solo forma non normalizzata finché non si aggiorna
 * questa mappa).
 */
private val CANONICAL_COUNTRY_NAMES = mapOf(
    "Città del Vaticano" to "Vatican City",
    "Paesi Bassi" to "Netherlands",
)

fun Coin.displayCountry(): String = CANONICAL_COUNTRY_NAMES[paese] ?: zeccaRaw
