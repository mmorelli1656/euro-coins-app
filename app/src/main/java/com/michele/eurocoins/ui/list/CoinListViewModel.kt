package com.michele.eurocoins.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.displayCountry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Quale sottoinsieme di monete mostra una lista. */
sealed interface CoinFilter {
    data object All : CoinFilter
    data class Year(val year: Int) : CoinFilter

    /** [paese] è il valore stabile `Coin.paese`, non il nome mostrato in UI. */
    data class Country(val paese: String) : CoinFilter
}

class CoinListViewModel(
    repository: CoinRepository,
    private val filter: CoinFilter,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<CoinListUiState> = combine(repository.coins, query, repository.ownedKeys) { coins, q, ownedKeys ->
        val scoped = when (filter) {
            CoinFilter.All -> coins
            is CoinFilter.Year -> coins.filter { it.anno == filter.year }
            is CoinFilter.Country -> coins.filter { it.paese == filter.paese }
        }
        val filtered = if (q.isBlank()) {
            scoped
        } else {
            scoped.filter {
                it.zeccaRaw.contains(q, ignoreCase = true) ||
                    it.paese.contains(q, ignoreCase = true) ||
                    it.tema.contains(q, ignoreCase = true)
            }
        }
        CoinListUiState(
            title = when (filter) {
                CoinFilter.All -> "All coins"
                is CoinFilter.Year -> filter.year.toString()
                is CoinFilter.Country -> scoped.firstOrNull()?.displayCountry().orEmpty()
            },
            query = q,
            coins = filtered,
            ownedKeys = ownedKeys,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoinListUiState(loading = true),
    )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }
}

data class CoinListUiState(
    val title: String = "",
    val query: String = "",
    val coins: List<Coin> = emptyList(),
    /** Chiavi stabili delle monete possedute (vedi Coin.stableKey), per il segno nella riga. */
    val ownedKeys: Set<String> = emptySet(),
    val loading: Boolean = true,
)
