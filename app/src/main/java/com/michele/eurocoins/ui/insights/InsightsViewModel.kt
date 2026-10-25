package com.michele.eurocoins.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.CollectionStats
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.collectionStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * I numeri della scheda Statistics (Pro). Si ricalcolano a ogni cambio di catalogo o collezione (una moneta aggiunta
 * mentre la pagina è aperta si vede subito) e fuori dal thread principale: sono conti su qualche centinaio di righe.
 * Lo stato iniziale è già completo se i repository hanno già letto Room (come `CoinListViewModel`), così aprendo la
 * scheda non passa da uno schermo vuoto.
 */
class InsightsViewModel(
    private val repository: CoinRepository,
    private val regularIssueRepository: RegularIssueRepository,
) : ViewModel() {

    val stats: StateFlow<CollectionStats?> = combine(
        repository.coins,
        repository.collectionItems,
        regularIssueRepository.series,
        regularIssueRepository.collectionItems,
    ) { coins, items, series, regular -> collectionStats(coins, items, series, regular) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    private fun initialState(): CollectionStats? {
        val coins = repository.coinsNow
        val items = repository.collectionNow
        val series = regularIssueRepository.seriesNow
        val regular = regularIssueRepository.collectionNow
        return if (coins != null && items != null && series != null && regular != null) {
            collectionStats(coins, items, series, regular)
        } else {
            null
        }
    }
}
