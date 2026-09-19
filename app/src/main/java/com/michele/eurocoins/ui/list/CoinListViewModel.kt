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
    private val groupMode = MutableStateFlow(GroupMode.FLAT)

    val uiState: StateFlow<CoinListUiState> = combine(repository.coins, query, groupMode) { coins, q, mode ->
        val filtered = if (q.isBlank()) {
            coins
        } else {
            coins.filter {
                it.zeccaRaw.contains(q, ignoreCase = true) ||
                    it.paese.contains(q, ignoreCase = true) ||
                    it.tema.contains(q, ignoreCase = true)
            }
        }
        CoinListUiState(
            query = q,
            coins = filtered,
            groupMode = mode,
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

    fun onGroupModeChange(mode: GroupMode) {
        groupMode.value = mode
    }
}

/** Come raggruppare l'elenco monete — vedi [com.michele.eurocoins.ui.list.groupCoins] in CoinListScreen.kt. */
enum class GroupMode {
    FLAT,
    BY_YEAR,
    BY_COUNTRY,
}

data class CoinListUiState(
    val query: String = "",
    val coins: List<Coin> = emptyList(),
    val groupMode: GroupMode = GroupMode.FLAT,
    val loading: Boolean = true,
)
