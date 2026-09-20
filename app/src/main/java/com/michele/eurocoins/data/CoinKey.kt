package com.michele.eurocoins.data

/**
 * Chiave stabile di una moneta, per agganciare i dati dell'utente
 * (collezione) senza dipendere da `Coin.id`: quell'id viene rigenerato a
 * ogni ripopolamento del catalogo (vedi CoinRepository.ensureSeeded).
 *
 * fonte + anno + paese + tema (in forma normalizzata): verificato univoco
 * su tutte le 499 monete. Punto debole noto: se la pipeline correggesse il
 * testo di `tema` di una moneta, la sua chiave cambierebbe e le voci di
 * collezione collegate resterebbero "orfane" — per questo ogni
 * [CollectionItem] conserva anche anno/paese/tema di quando è stata
 * salvata, così un orfano si può riconoscere e ricollegare. La soluzione
 * definitiva è un id stabile emesso dalla pipeline dati.
 */
val Coin.stableKey: String
    get() = listOf(fonteDati, anno.toString(), paese, tema.toKeySlug()).joinToString("|")

private fun String.toKeySlug(): String =
    lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
