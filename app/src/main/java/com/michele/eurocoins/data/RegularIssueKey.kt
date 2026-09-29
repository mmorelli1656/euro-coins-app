package com.michele.eurocoins.data

/**
 * Chiave stabile di una serie di Regular Issues, per agganciare la collezione utente senza
 * dipendere da `RegularIssueSeries.id`: quell'id viene rigenerato a ogni ripopolamento del
 * catalogo (vedi `RegularIssueRepository.ensureSeeded`), stesso problema di `Coin.id` risolto da
 * [stableKey] per le commemorative.
 *
 * paese + ordineCronologico: è già l'ordine garantito univoco per paese usato da
 * `RegularIssueDao.observeAll`.
 */
val RegularIssueSeries.stableKey: String
    get() = "$paese|$ordineCronologico"
