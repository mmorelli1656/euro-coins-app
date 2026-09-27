package com.michele.eurocoins.ui.regular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.flagEmojiForCountry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RegularIssueCountryUiState(
    val countryName: String = "",
    val flag: String = "",
    val series: List<RegularIssueSeries> = emptyList(),
)

/** Le serie di un singolo paese, ordinate per `ordineCronologico` (l'ordine già garantito da `RegularIssueDao.observeAll`). */
class RegularIssueCountryViewModel(
    private val repository: RegularIssueRepository,
    private val paese: String,
) : ViewModel() {

    val uiState: StateFlow<RegularIssueCountryUiState> = repository.series
        .map { buildState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    private fun initialState(): RegularIssueCountryUiState =
        repository.seriesNow?.let { buildState(it) } ?: RegularIssueCountryUiState()

    private fun buildState(all: List<RegularIssueSeries>): RegularIssueCountryUiState {
        val forCountry = all.filter { it.paese == paese }
        return RegularIssueCountryUiState(
            countryName = forCountry.firstOrNull()?.displayCountry() ?: paese,
            flag = flagEmojiForCountry(paese),
            series = forCountry,
        )
    }
}
