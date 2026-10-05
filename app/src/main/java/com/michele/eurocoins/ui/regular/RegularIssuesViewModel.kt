package com.michele.eurocoins.ui.regular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.DenominationRow
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.REGULAR_DENOMINATIONS
import com.michele.eurocoins.data.RegularAllSort
import com.michele.eurocoins.data.RegularCollectionEntry
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.allDenominationRows
import com.michele.eurocoins.data.denominationProgress
import com.michele.eurocoins.data.denominationRows
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.flagEmojiForCountry
import com.michele.eurocoins.data.matchesAllQuery
import com.michele.eurocoins.data.ownsAnyQuality
import com.michele.eurocoins.data.regularProgress
import com.michele.eurocoins.data.stableKey
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
import kotlinx.coroutines.launch

enum class RegularBrowseMode { COUNTRIES, DENOMINATIONS, ALL }

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

/** Query, ordine e filtro delle tre schede; ognuna ha i suoi, così cambiando scheda non si perdono. */
data class RegularGridPrefs(
    /** true = A → Z (default); false = Z → A. */
    val countriesAscending: Boolean = true,
    /** true = dal più grande (2 euro → 1 cent, default); false = dal più piccolo. */
    val denominationsLargestFirst: Boolean = true,
    val countriesQuery: String = "",
    val denominationsQuery: String = "",
    val countriesCompletion: CompletionFilter = CompletionFilter.ALL,
    val denominationsCompletion: CompletionFilter = CompletionFilter.ALL,
    val allSort: RegularAllSort = RegularAllSort.COUNTRY_ASC,
    val allQuery: String = "",
    val allOwnership: OwnershipFilter = OwnershipFilter.ALL,
    /** Vuoto = nessun vincolo; altrimenti la riga deve avere almeno un'annata in una di queste qualità. */
    val allQualities: Set<CoinQuality> = emptySet(),
) {
    val countriesFilterActive: Boolean
        get() = !countriesAscending || countriesCompletion != CompletionFilter.ALL
    val denominationsFilterActive: Boolean
        get() = !denominationsLargestFirst || denominationsCompletion != CompletionFilter.ALL
    val allFilterActive: Boolean
        get() = allSort != RegularAllSort.COUNTRY_ASC || allOwnership != OwnershipFilter.ALL || allQualities.isNotEmpty()
}

data class RegularIssuesUiState(
    val mode: RegularBrowseMode = RegularBrowseMode.COUNTRIES,
    val prefs: RegularGridPrefs = RegularGridPrefs(),
    /** false finché il dataset non è stato letto e raggruppato: la griglia resta trasparente finché non arriva. */
    val loaded: Boolean = false,
    val countries: List<RegularIssueCountryCardData> = emptyList(),
    val denominations: List<DenominationCardData> = emptyList(),
    /** Righe della scheda All dopo ricerca e filtro (una per serie e taglio). */
    val all: List<DenominationRow> = emptyList(),
)

/**
 * Catalogo Regular Issues: tre schede, Countries (la griglia dei paesi, con ricerca, ordine e filtro
 * di completamento come in Commemorative), Denominations (una card per taglio, con il suo avanzamento
 * su tutti i paesi) e All (tutte le righe di tutti i tagli in un elenco, come All di Commemorative:
 * l'unico posto dove cercare una moneta precisa o filtrare "Missing" su tutto il catalogo).
 */
class RegularIssuesViewModel(
    private val repository: RegularIssueRepository,
    /** Scheda che si apre per prima, scelta in Impostazioni: letta una volta alla creazione, come in `BrowseViewModel`. */
    initialMode: RegularBrowseMode = RegularBrowseMode.COUNTRIES,
) : ViewModel() {

    private val mode = MutableStateFlow(initialMode)
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
        val all = allDenominationRows(rows, p.allSort)
            .filter { p.allOwnership == OwnershipFilter.ALL || (p.allOwnership == OwnershipFilter.OWNED) == it.owned }
            .filter { it.ownsAnyQuality(p.allQualities) }
            .filter { it.matchesAllQuery(p.allQuery) }
        return RegularIssuesUiState(
            mode = currentMode,
            prefs = p,
            loaded = true,
            countries = countries,
            denominations = denominations,
            all = all,
        )
    }

    /** Query attuali delle due griglie, lette subito (uiState arriva con qualche fotogramma di ritardo). */
    val countriesQueryNow: String get() = prefs.value.countriesQuery
    val denominationsQueryNow: String get() = prefs.value.denominationsQuery
    val allQueryNow: String get() = prefs.value.allQuery

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

    fun setAllQuery(value: String) = prefs.update { it.copy(allQuery = value) }
    fun setAllSort(value: RegularAllSort) = prefs.update { it.copy(allSort = value) }
    fun setAllOwnership(value: OwnershipFilter) = prefs.update { it.copy(allOwnership = value) }
    fun toggleAllQuality(value: CoinQuality) = prefs.update {
        it.copy(allQualities = if (value in it.allQualities) it.allQualities - value else it.allQualities + value)
    }
    fun resetAll() = prefs.update {
        it.copy(allSort = RegularAllSort.COUNTRY_ASC, allOwnership = OwnershipFilter.ALL, allQualities = emptySet())
    }

    /** Salva dal pannello di una riga di All: stessa chiave (serie di origine) e finestra dell'elenco di un taglio. */
    fun onSaveCollection(row: DenominationRow, entries: List<RegularCollectionEntry>) {
        val series = row.denomination.series
        viewModelScope.launch {
            repository.saveCollection(series.stableKey, row.denomination.image.taglio, series.paese, entries, window = row.denomination)
        }
    }
}

/** "euro", "2 euro", "2euro", "cent" trovano il taglio senza badare a maiuscole e spazi. */
internal fun matchesDenomination(taglio: String, query: String): Boolean {
    val q = query.trim().replace(" ", "")
    return q.isEmpty() || taglio.replace(" ", "").contains(q, ignoreCase = true)
}
