package com.michele.eurocoins.ui.regular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.RegularCollectionEntry
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.SeriesDenomination
import com.michele.eurocoins.data.denominationsOf
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.stableKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RegularDenominationDetailUiState(
    val countryName: String = "",
    val series: RegularIssueSeries? = null,
    /** Posizione (1-based) della serie nella lista del paese, come in `RegularIssueCountryScreen`. */
    val seriesNumber: Int = 0,
    val image: RegularIssueImage? = null,
    /** Il taglio con la sua finestra di anni, per salvare senza toccare le annate di altre serie. */
    val denomination: SeriesDenomination? = null,
    val items: List<RegularCollectionItem> = emptyList(),
)

/**
 * Dettaglio di UN taglio di UNA serie: `paese` + `ordineCronologico` (non `seriesKey` diretto,
 * per evitare il separatore `|` in un argomento di rotta) + `taglio` individuano la serie e
 * l'immagine dentro `RegularIssueRepository.series`, riletti reattivamente come
 * `RegularIssueCountryViewModel` (non un fetch singolo come `CoinDetailViewModel.getById`: qui
 * non serve un id stabile per riga, il taglio è già una chiave dentro la serie).
 */
class RegularDenominationDetailViewModel(
    private val repository: RegularIssueRepository,
    private val paese: String,
    private val ordineCronologico: Int,
    private val taglio: String,
) : ViewModel() {

    val uiState: StateFlow<RegularDenominationDetailUiState> = combine(
        repository.series,
        repository.collectionItems,
    ) { all, collection -> buildState(all, collection) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    private fun initialState(): RegularDenominationDetailUiState {
        val series = repository.seriesNow
        val collection = repository.collectionNow
        return if (series != null && collection != null) {
            buildState(series, collection)
        } else {
            RegularDenominationDetailUiState()
        }
    }

    private fun buildState(
        all: List<RegularIssueSeries>,
        collection: List<RegularCollectionItem>,
    ): RegularDenominationDetailUiState {
        val forCountry = all.filter { it.paese == paese }
        val index = forCountry.indexOfFirst { it.ordineCronologico == ordineCronologico }
        val viewed = forCountry.getOrNull(index)
        // Il taglio come lo vede la serie guardata: se è rimasto invariato, `series` è quella di
        // origine (chiave della collezione) e `image` è già ritagliata agli anni della serie guardata.
        val denomination = viewed?.let { denominationsOf(forCountry, it) }?.firstOrNull { it.image.taglio == taglio }
        val items = if (denomination != null) {
            val key = denomination.series.stableKey
            collection.filter { it.seriesKey == key && it.taglio == taglio && denomination.contains(it.anno) }
        } else {
            emptyList()
        }
        return RegularDenominationDetailUiState(
            countryName = forCountry.firstOrNull()?.displayCountry() ?: paese,
            series = denomination?.series,
            seriesNumber = index + 1,
            image = denomination?.image,
            denomination = denomination,
            items = items,
        )
    }

    fun onSaveCollection(entries: List<RegularCollectionEntry>) {
        val denomination = uiState.value.denomination ?: return
        val series = denomination.series
        viewModelScope.launch { repository.saveCollection(series.stableKey, taglio, series.paese, entries, window = denomination) }
    }
}
