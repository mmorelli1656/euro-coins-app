package com.michele.eurocoins.data

/** Quante monete di un gruppo (anno, paese, catalogo) sono possedute. */
data class Progress(val owned: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else owned.toFloat() / total
}

/** Una moneta conta come posseduta se l'utente ne ha almeno una qualità. */
fun List<Coin>.progress(ownedKeys: Set<String>): Progress =
    Progress(owned = count { it.stableKey in ownedKeys }, total = size)

/**
 * Avanzamento di un gruppo di serie divisionali (un paese, o tutto il catalogo): le RIGHE che l'utente
 * vede nelle schermate delle serie, cioe' ogni taglio di ogni serie ([denominationsOf]: 8 per serie,
 * anche quelli rimasti invariati). La Francia e' 24 (3 serie x 8), la Spagna 24: non i disegni
 * distinti (13 e 18), che e' il numero che si vedeva prima e non corrispondeva a niente di visibile.
 * Una riga e' posseduta se il taglio ha almeno un'annata IN COLLEZIONE DENTRO LA FINESTRA di quella
 * serie, lo stesso criterio della casella spuntata nella riga: il 5 cent francese del 2005 riempie la
 * riga della serie 1, quello del 2023 la riga della serie 2 (stessa moneta, finestre diverse). Non le
 * annate: un taglio con tre annate nella stessa serie conta 1. Stesso conto per la barra della Home
 * e per quella della card del paese. Una serie senza immagini (Vaticano 2026) non ha righe.
 */
fun List<RegularIssueSeries>.regularProgress(collection: List<RegularCollectionItem>): Progress {
    val years = collection.groupBy({ it.seriesKey to it.taglio }, { it.anno })
    var owned = 0
    var total = 0
    for (country in groupBy { it.paese }.values) {
        for (series in country) {
            for (row in denominationsOf(country, series)) {
                total++
                if (years[row.series.stableKey to row.image.taglio]?.any { row.contains(it) } == true) owned++
            }
        }
    }
    return Progress(owned, total)
}
