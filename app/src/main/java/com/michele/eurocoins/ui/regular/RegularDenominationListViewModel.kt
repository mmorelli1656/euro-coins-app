package com.michele.eurocoins.ui.regular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.DenominationRow
import com.michele.eurocoins.data.RegularCollectionEntry
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.denominationRows
import com.michele.eurocoins.data.stableKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Filtro sul possesso di una riga dell'elenco di un taglio. */
enum class OwnershipFilter(val label: String) {
    ALL("All"),
    OWNED("Owned"),
    MISSING("Missing"),
}

data class RegularDenominationListUiState(
    val loading: Boolean = true,
    val query: String = "",
    val ownership: OwnershipFilter = OwnershipFilter.ALL,
    /** Righe del taglio dopo ricerca e filtro. */
    val rows: List<DenominationRow> = emptyList(),
    /** Tutte le righe del taglio, prima di ricerca e filtro. */
    val total: Int = 0,
) {
    val filterActive: Boolean get() = ownership != OwnershipFilter.ALL
}

/**
 * Un taglio ("2 euro") in tutti i paesi: una riga per serie, ordinate per paese e poi per serie
 * ([denominationRows], stesso conto di [com.michele.eurocoins.data.regularProgress]). Ricerca per
 * paese, numero della serie o anno; filtro Owned / Missing.
 */
class RegularDenominationListViewModel(
    private val repository: RegularIssueRepository,
    private val taglio: String,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val ownership = MutableStateFlow(OwnershipFilter.ALL)

    val uiState: StateFlow<RegularDenominationListUiState> = combine(
        repository.series,
        repository.collectionItems,
        query,
        ownership,
    ) { series, collection, q, o -> buildState(series, collection, q, o) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    private fun initialState(): RegularDenominationListUiState {
        val series = repository.seriesNow
        val collection = repository.collectionNow
        return if (series != null && collection != null) {
            buildState(series, collection, query.value, ownership.value)
        } else {
            RegularDenominationListUiState()
        }
    }

    private fun buildState(
        series: List<RegularIssueSeries>,
        collection: List<RegularCollectionItem>,
        q: String,
        o: OwnershipFilter,
    ): RegularDenominationListUiState {
        val all = denominationRows(series, collection)[taglio].orEmpty()
        val needle = q.trim()
        return RegularDenominationListUiState(
            loading = false,
            query = q,
            ownership = o,
            total = all.size,
            rows = all
                .filter { o == OwnershipFilter.ALL || (o == OwnershipFilter.OWNED) == it.owned }
                .filter { needle.isEmpty() || it.matches(needle) },
        )
    }

    /** Query attuale, letta subito (uiState arriva con qualche fotogramma di ritardo). */
    val queryNow: String get() = query.value

    private val _scrollTo = MutableStateFlow<RegularPageKey?>(null)

    /** Riga da portare in vista al ritorno dal dettaglio a pagine (vedi [RegularPagerSession]); la lista la azzera. */
    val scrollTo: StateFlow<RegularPageKey?> = _scrollTo

    fun requestScrollTo(key: RegularPageKey) {
        _scrollTo.value = key
    }

    fun consumeScrollTo() {
        _scrollTo.value = null
    }

    fun onQueryChange(value: String) = query.update { value }
    fun onOwnershipChange(value: OwnershipFilter) = ownership.update { value }
    fun resetFilters() = ownership.update { OwnershipFilter.ALL }

    fun onSaveCollection(row: DenominationRow, entries: List<RegularCollectionEntry>) {
        val series = row.denomination.series
        viewModelScope.launch {
            repository.saveCollection(series.stableKey, taglio, series.paese, entries, window = row.denomination)
        }
    }
}

/** Paese ("france"), serie ("series 2", "2") o periodo ("2022") della riga. */
private fun DenominationRow.matches(needle: String): Boolean =
    countryName.contains(needle, ignoreCase = true) ||
        seriesLabel.contains(needle, ignoreCase = true) ||
        paese.contains(needle, ignoreCase = true)
