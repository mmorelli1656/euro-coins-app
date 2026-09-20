package com.michele.eurocoins.data

/** Quante monete di un gruppo (anno, paese, catalogo) sono possedute. */
data class Progress(val owned: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else owned.toFloat() / total
}

fun List<Coin>.progress(ownedIds: Set<Long>): Progress = Progress(owned = count { it.id in ownedIds }, total = size)

/**
 * PLACEHOLDER: dati di possesso finti (una moneta ogni quattro) così le barre
 * di avanzamento hanno qualcosa da mostrare prima che esista la vera
 * collezione (punto 6: "segna come posseduta", qualità, prezzo).
 *
 * Unico punto da sostituire: quando la tabella di collezione locale esiste,
 * questa funzione va rimpiazzata con l'insieme reale degli id posseduti e
 * tutte le barre (home, anni, paesi) si aggiornano da sole — la UI legge
 * solo `Set<Long>`, non sa che è finto.
 */
fun fakeOwnedIds(coins: List<Coin>): Set<Long> = coins.filter { it.id % 4L == 0L }.map { it.id }.toSet()
