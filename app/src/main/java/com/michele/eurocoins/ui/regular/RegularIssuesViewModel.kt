package com.michele.eurocoins.ui.regular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.REGULAR_DENOMINATIONS
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.denominationProgress
import com.michele.eurocoins.data.denominationRows
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.flagEmojiForCountry
import com.michele.eurocoins.data.regularProgress
import com.michele.eurocoins.ui.browse.CompletionFilter
import com.michele.eurocoins.ui.browse.matches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class RegularBrowseMode { COUNTRIES, DENOMINATIONS }

/** [paese] è la chiave stabile usata per navigare, [name] il nome mostrato. */
data class RegularIssueCountryCardData(
    val paese: String,
    val name: String,
    val flag: String,
    val seriesCount: Int,
    val progress: Progress,
)

/** [taglio] è la chiave ("2 euro") usata per navigare e da mostrare; [progress] sulle righe di tutti i paesi. */
data class DenominationCardData(val taglio: String, val progress: Progress)

/** Query, ordine e filtro delle due griglie; ognuna ha i suoi, così cambiando scheda non si perdono. */
data class RegularGridPrefs(
    /** true = A → Z (default); false = Z → A. */
    val countriesAscending: Boolean = true,
    /** true = dal più grande (2 euro → 1 cent, default); false = dal più piccolo. */
    val denominationsLargestFirst: Boolean = true,
    val countriesQuery: String = "",
    val denominationsQuery: String = "",
    val countriesCompletion: CompletionFilter = CompletionFilter.ALL,
    val denominationsCompletion: CompletionFilter = CompletionFilter.ALL,
) {
    val countriesFilterActive: Boolean
        get() = !countriesAscending || countriesCompletion != CompletionFilter.ALL
    val denominationsFilterActive: Boolean
        get() = !denominationsLargestFirst || denominationsCompletion != CompletionFilter.ALL
}

data class RegularIssuesUiState(
    val mode: RegularBrowseMode = RegularBrowseMode.COUNTRIES,
    val prefs: RegularGridPrefs = RegularGridPrefs(),
    /** false finché il dataset non è stato letto e raggruppato: la griglia resta trasparente finché non arriva. */
    val loaded: Boolean = false,
    val countries: List<RegularIssueCountryCardData> = emptyList(),
    val denominations: List<DenominationCardData> = emptyList(),
)

/**
 * Catalogo Regular Issues: due schede, Countries (la griglia dei paesi, con ricerca, ordine e filtro
 * di completamento come in Commemorative) e Denominations (una card per taglio, con il suo avanzamento
 * su tutti i paesi). Niente scheda "All" (tutte le righe in un elenco): non è detto che serva, e
 * l'elenco di un taglio ne è già una versione filtrata — vedi CLAUDE.md § Regular Issues.
 */
class RegularIssuesViewModel(
    private val repository: RegularIssueRepository,
) : ViewModel() {

    private val mode = MutableStateFlow(RegularBrowseMode.COUNTRIES)
    private val prefs = MutableStateFlow(RegularGridPrefs())

    val uiState: StateFlow<RegularIssuesUiState> = combine(
        repository.series,
        repository.collectionItems,
        mode,
        prefs,
    ) { series, collection, currentMode, p -> buildState(series, collection, currentMode, p) }
        // Raggruppamenti e progressi su ~330 righe: fuori dal thread principale.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    private fun initialState(): RegularIssuesUiState {
        val series = repository.seriesNow
        val collection = repository.collectionNow
        return if (series != null && collection != null) {
            buildState(series, collection, mode.value, prefs.value)
        } else {
            RegularIssuesUiState(mode = mode.value)
        }
    }

    private fun buildState(
        series: List<RegularIssueSeries>,
        collection: List<RegularCollectionItem>,
        currentMode: RegularBrowseMode,
        p: RegularGridPrefs,
    ): RegularIssuesUiState {
        val countries = series
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
            .filter { it.progress.matches(p.countriesCompletion) }
            .filter { p.countriesQuery.isBlank() || it.name.contains(p.countriesQuery.trim(), ignoreCase = true) }
            .let { list -> if (p.countriesAscending) list.sortedBy { it.name } else list.sortedByDescending { it.name } }
        val rows = denominationRows(series, collection)
        val denominations = REGULAR_DENOMINATIONS
            .map { DenominationCardData(it, rows[it].orEmpty().denominationProgress()) }
            .filter { it.progress.matches(p.denominationsCompletion) }
            .filter { matchesDenomination(it.taglio, p.denominationsQuery) }
            .let { list -> if (p.denominationsLargestFirst) list.asReversed() else list }
        return RegularIssuesUiState(
            mode = currentMode,
            prefs = p,
            loaded = true,
            countries = countries,
            denominations = denominations,
        )
    }

    /** Query attuali delle due griglie, lette subito (uiState arriva con qualche fotogramma di ritardo). */
    val countriesQueryNow: String get() = prefs.value.countriesQuery
    val denominationsQueryNow: String get() = prefs.value.denominationsQuery

    fun onModeChange(newMode: RegularBrowseMode) {
        mode.value = newMode
    }

    fun setCountriesAscending(value: Boolean) = prefs.update { it.copy(countriesAscending = value) }
    fun setDenominationsLargestFirst(value: Boolean) = prefs.update { it.copy(denominationsLargestFirst = value) }
    fun setCountriesQuery(value: String) = prefs.update { it.copy(countriesQuery = value) }
    fun setDenominationsQuery(value: String) = prefs.update { it.copy(denominationsQuery = value) }
    fun setCountriesCompletion(value: CompletionFilter) = prefs.update { it.copy(countriesCompletion = value) }
    fun setDenominationsCompletion(value: CompletionFilter) = prefs.update { it.copy(denominationsCompletion = value) }

    fun resetCountries() = prefs.update { it.copy(countriesAscending = true, countriesCompletion = CompletionFilter.ALL) }
    fun resetDenominations() = prefs.update { it.copy(denominationsLargestFirst = true, denominationsCompletion = CompletionFilter.ALL) }
}

/** "euro", "2 euro", "2euro", "cent" trovano il taglio senza badare a maiuscole e spazi. */
internal fun matchesDenomination(taglio: String, query: String): Boolean {
    val q = query.trim().replace(" ", "")
    return q.isEmpty() || taglio.replace(" ", "").contains(q, ignoreCase = true)
}
