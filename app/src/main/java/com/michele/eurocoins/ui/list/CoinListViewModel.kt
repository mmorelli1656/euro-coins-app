package com.michele.eurocoins.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.stableKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Quale sottoinsieme di monete mostra una lista. */
sealed interface CoinFilter {
    data object All : CoinFilter
    data class Year(val year: Int) : CoinFilter

    /** [paese] è il valore stabile `Coin.paese`, non il nome mostrato in UI. */
    data class Country(val paese: String) : CoinFilter
}

class CoinListViewModel(
    private val repository: CoinRepository,
    private val filter: CoinFilter,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val options = MutableStateFlow(CoinListOptions())

    val sortChoices: List<CoinSort> = filter.sortChoices()

    val uiState: StateFlow<CoinListUiState> = combine(
        repository.coins,
        query,
        repository.collectionItems,
        options,
    ) { coins, q, items, opts ->
        val scoped = when (filter) {
            CoinFilter.All -> coins
            is CoinFilter.Year -> coins.filter { it.anno == filter.year }
            is CoinFilter.Country -> coins.filter { it.paese == filter.paese }
        }
        val byKey = items.groupBy { it.coinKey }
        val searched = if (q.isBlank()) {
            scoped
        } else {
            scoped.filter {
                it.zeccaRaw.contains(q, ignoreCase = true) ||
                    it.paese.contains(q, ignoreCase = true) ||
                    it.tema.contains(q, ignoreCase = true) ||
                    it.anno.toString().contains(q)
            }
        }
        val filtered = searched
            .filter { coin ->
                val mine = byKey[coin.stableKey].orEmpty()
                when (opts.ownership) {
                    OwnershipFilter.ALL -> true
                    OwnershipFilter.OWNED -> mine.isNotEmpty()
                    OwnershipFilter.MISSING -> mine.isEmpty()
                } && (opts.qualities.isEmpty() || mine.any { it.quality in opts.qualities })
            }
            .let { list ->
                when (opts.sort) {
                    CoinSort.DEFAULT -> list
                    CoinSort.YEAR_DESC -> list.sortedByDescending { it.anno }
                    CoinSort.YEAR_ASC -> list.sortedBy { it.anno }
                    CoinSort.COUNTRY_AZ -> list.sortedBy { it.displayCountry() }
                    CoinSort.COUNTRY_ZA -> list.sortedByDescending { it.displayCountry() }
                }
            }
        CoinListUiState(
            title = when (filter) {
                CoinFilter.All -> "All coins"
                is CoinFilter.Year -> filter.year.toString()
                is CoinFilter.Country -> scoped.firstOrNull()?.displayCountry().orEmpty()
            },
            query = q,
            options = opts,
            coins = filtered,
            collection = byKey,
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

    fun onOptionsChange(newOptions: CoinListOptions) {
        options.value = newOptions
    }

    fun onSaveCollection(coin: Coin, entries: Map<CoinQuality, Int?>) {
        viewModelScope.launch { repository.saveCollection(coin, entries) }
    }
}

data class CoinListUiState(
    val title: String = "",
    val query: String = "",
    val options: CoinListOptions = CoinListOptions(),
    val coins: List<Coin> = emptyList(),
    /** Voci di collezione per chiave stabile della moneta (vedi Coin.stableKey): chiave presente = posseduta. */
    val collection: Map<String, List<CollectionItem>> = emptyMap(),
    val loading: Boolean = true,
)
