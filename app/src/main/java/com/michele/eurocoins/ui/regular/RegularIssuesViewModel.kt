package com.michele.eurocoins.ui.regular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.flagEmojiForCountry
import com.michele.eurocoins.data.regularProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** [paese] è la chiave stabile usata per navigare, [name] il nome mostrato. */
/** [progress]: tagli distinti posseduti sui disegni di taglio del paese, vedi [regularProgress]. */
data class RegularIssueCountryCardData(
    val paese: String,
    val name: String,
    val flag: String,
    val seriesCount: Int,
    val progress: Progress,
)

data class RegularIssuesUiState(
    /** false finché il dataset non è stato letto e raggruppato: la griglia resta trasparente finché non arriva. */
    val loaded: Boolean = false,
    val countries: List<RegularIssueCountryCardData> = emptyList(),
)

/** Griglia dei paesi di Regular Issues: solo consultazione, niente ricerca/ordinamento in questa prima versione. */
class RegularIssuesViewModel(
    private val repository: RegularIssueRepository,
) : ViewModel() {

    val uiState: StateFlow<RegularIssuesUiState> = combine(repository.series, repository.collectionItems) { series, collection ->
        buildState(series, collection)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    private fun initialState(): RegularIssuesUiState {
        val series = repository.seriesNow
        val collection = repository.collectionNow
        return if (series != null && collection != null) buildState(series, collection) else RegularIssuesUiState()
    }

    private fun buildState(series: List<RegularIssueSeries>, collection: List<RegularCollectionItem>): RegularIssuesUiState = RegularIssuesUiState(
        loaded = true,
        countries = series
            .groupBy { it.paese }
            .map { (paese, list) ->
                RegularIssueCountryCardData(
                    paese = paese,
                    name = list.first().displayCountry(),
                    flag = flagEmojiForCountry(paese),
                    seriesCount = list.size,
                    progress = list.regularProgress(collection),
                )
            }
            .sortedBy { it.name },
    )
}
