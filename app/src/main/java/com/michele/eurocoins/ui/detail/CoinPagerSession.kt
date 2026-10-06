package com.michele.eurocoins.ui.detail

import androidx.lifecycle.ViewModel
import com.michele.eurocoins.ui.list.CoinListViewModel

/**
 * Contesto del dettaglio a pagine: l'elenco ORDINATO di monete da cui si è arrivati (stesso filtro,
 * ricerca e ordine della lista) e la lista stessa, a cui dire in che moneta si è finiti.
 *
 * Copia degli id presa al tocco, non un riferimento vivo alla lista: con "Missing" attivo,
 * segnare una moneta come posseduta la farebbe sparire dall'elenco e le pagine si sposterebbero
 * sotto le dita. È un `ViewModel` (dell'Activity) solo perché così sopravvive alla rotazione; dopo la
 * morte del processo è vuoto e il dettaglio ripiega su una pagina sola ([idsFor]).
 */
class CoinPagerSession : ViewModel() {
    private var ids: List<Long> = emptyList()
    private var origin: CoinListViewModel? = null

    /** Da chiamare al tocco di una moneta in [from]: fotografa l'elenco com'è in quel momento. */
    fun open(from: CoinListViewModel) {
        ids = from.uiState.value.coins.map { it.id }
        origin = from
    }

    /** Le pagine del dettaglio di [coinId]: l'elenco fotografato, o la sola moneta se non ne fa parte. */
    fun idsFor(coinId: Long): List<Long> = if (coinId in ids) ids else listOf(coinId)

    /** La moneta mostrata è cambiata: la lista di origine la porterà in vista al ritorno. */
    fun onPageShown(coinId: Long) {
        origin?.requestScrollTo(coinId)
    }

    override fun onCleared() {
        origin = null
    }
}
