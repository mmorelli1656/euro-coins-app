package com.michele.eurocoins.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.flagEmoji
import com.michele.eurocoins.data.progress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class BrowseMode { YEARS, COUNTRIES, ALL }

/** Filtro sullo stato di completamento di una card (anno o paese). */
enum class CompletionFilter(val label: String) {
    ALL("All"),
    INCOMPLETE("Incomplete"),
    COMPLETE("Complete"),
}

data class YearCardData(val year: Int, val progress: Progress)

/** [paese] è la chiave stabile (`Coin.paese`) usata per navigare, [name] il nome mostrato. */
data class CountryCardData(val paese: String, val name: String, val flag: String, val progress: Progress)

/** Query, ordine e filtro delle due griglie; ognuna ha i suoi, così cambiando scheda non si perdono. */
data class GridPrefs(
    /** false = dal più recente (2025 → 2004, default); true = dal 2004. */
    val yearsAscending: Boolean = false,
    /** true = A → Z (default); false = Z → A. */
    val countriesAscending: Boolean = true,
    val yearsQuery: String = "",
    val countriesQuery: String = "",
    val yearsCompletion: CompletionFilter = CompletionFilter.ALL,
    val countriesCompletion: CompletionFilter = CompletionFilter.ALL,
) {
    val yearsFilterActive: Boolean
        get() = yearsAscending || yearsCompletion != CompletionFilter.ALL
    val countriesFilterActive: Boolean
        get() = !countriesAscending || countriesCompletion != CompletionFilter.ALL
}

data class BrowseUiState(
    val mode: BrowseMode = BrowseMode.YEARS,
    val prefs: GridPrefs = GridPrefs(),
    val years: List<YearCardData> = emptyList(),
    val countries: List<CountryCardData> = emptyList(),
)

private fun Progress.matches(filter: CompletionFilter): Boolean = when (filter) {
    CompletionFilter.ALL -> true
    CompletionFilter.INCOMPLETE -> owned < total
    CompletionFilter.COMPLETE -> total > 0 && owned == total
}

class BrowseViewModel(repository: CoinRepository) : ViewModel() {

    private val mode = MutableStateFlow(BrowseMode.YEARS)
    private val prefs = MutableStateFlow(GridPrefs())

    val uiState: StateFlow<BrowseUiState> = combine(
        repository.coins,
        repository.ownedKeys,
        mode,
        prefs,
    ) { coins, owned, currentMode, p ->
        BrowseUiState(
            mode = currentMode,
            prefs = p,
            years = coins
                .groupBy { it.anno }
                .toSortedMap(if (p.yearsAscending) naturalOrder() else reverseOrder())
                .map { (year, list) -> YearCardData(year, list.progress(owned)) }
                .filter { it.progress.matches(p.yearsCompletion) }
                .filter { p.yearsQuery.isBlank() || it.year.toString().contains(p.yearsQuery.trim()) },
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
                .filter { it.progress.matches(p.countriesCompletion) }
                .filter { p.countriesQuery.isBlank() || it.name.contains(p.countriesQuery.trim(), ignoreCase = true) }
                .let { list ->
                    if (p.countriesAscending) list.sortedBy { it.name } else list.sortedByDescending { it.name }
                },
        )
    }
        // Raggruppamenti e progressi su 584 monete: fuori dal thread principale, altrimenti
        // bloccano la transizione di apertura della schermata.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    fun onModeChange(newMode: BrowseMode) {
        mode.value = newMode
    }

    fun setYearsAscending(value: Boolean) = prefs.update { it.copy(yearsAscending = value) }
    fun setCountriesAscending(value: Boolean) = prefs.update { it.copy(countriesAscending = value) }
    fun setYearsQuery(value: String) = prefs.update { it.copy(yearsQuery = value) }
    fun setCountriesQuery(value: String) = prefs.update { it.copy(countriesQuery = value) }
    fun setYearsCompletion(value: CompletionFilter) = prefs.update { it.copy(yearsCompletion = value) }
    fun setCountriesCompletion(value: CompletionFilter) = prefs.update { it.copy(countriesCompletion = value) }

    fun resetYears() = prefs.update {
        it.copy(yearsAscending = false, yearsCompletion = CompletionFilter.ALL)
    }

    fun resetCountries() = prefs.update {
        it.copy(countriesAscending = true, countriesCompletion = CompletionFilter.ALL)
    }
}
