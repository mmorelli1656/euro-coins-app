package com.michele.eurocoins.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CoinListViewModel(private val repository: CoinRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<CoinListUiState> = combine(repository.coins, query) { coins, q ->
        val filtered = if (q.isBlank()) {
            coins
        } else {
            coins.filter {
                it.paese.contains(q, ignoreCase = true) || it.tema.contains(q, ignoreCase = true)
            }
        }
        CoinListUiState(
            query = q,
            coins = filtered,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoinListUiState(loading = true),
    )

    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }
}

data class CoinListUiState(
    val query: String = "",
    val coins: List<Coin> = emptyList(),
    val loading: Boolean = true,
)
