package com.michele.eurocoins.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.flagEmoji
import com.michele.eurocoins.data.progress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class BrowseMode { YEARS, COUNTRIES, ALL }

data class YearCardData(val year: Int, val progress: Progress)

/** [paese] è la chiave stabile (`Coin.paese`) usata per navigare, [name] il nome mostrato. */
data class CountryCardData(val paese: String, val name: String, val flag: String, val progress: Progress)

data class BrowseUiState(
    val mode: BrowseMode = BrowseMode.YEARS,
    /** false = dal più recente (2025 → 2004, default); true = dal 2004. */
    val yearsAscending: Boolean = false,
    /** true = A → Z (default); false = Z → A. */
    val countriesAscending: Boolean = true,
    val years: List<YearCardData> = emptyList(),
    val countries: List<CountryCardData> = emptyList(),
)

class BrowseViewModel(repository: CoinRepository) : ViewModel() {

    private val mode = MutableStateFlow(BrowseMode.YEARS)
    private val yearsAscending = MutableStateFlow(false)
    private val countriesAscending = MutableStateFlow(true)

    val uiState: StateFlow<BrowseUiState> = combine(
        repository.coins,
        repository.ownedKeys,
        mode,
        yearsAscending,
        countriesAscending,
    ) { coins, owned, currentMode, yearsAsc, countriesAsc ->
        BrowseUiState(
            mode = currentMode,
            yearsAscending = yearsAsc,
            countriesAscending = countriesAsc,
            years = coins
                .groupBy { it.anno }
                .toSortedMap(if (yearsAsc) naturalOrder() else reverseOrder())
                .map { (year, list) -> YearCardData(year, list.progress(owned)) },
            countries = coins
                .groupBy { it.paese }
                .map { (paese, list) ->
                    CountryCardData(
                        paese = paese,
                        name = list.first().displayCountry(),
                        flag = list.first().flagEmoji(),
                        progress = list.progress(owned),
                    )
                }
                .let { list -> if (countriesAsc) list.sortedBy { it.name } else list.sortedByDescending { it.name } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    fun onModeChange(newMode: BrowseMode) {
        mode.value = newMode
    }

    fun toggleYearsOrder() {
        yearsAscending.value = !yearsAscending.value
    }

    fun toggleCountriesOrder() {
        countriesAscending.value = !countriesAscending.value
    }
}
