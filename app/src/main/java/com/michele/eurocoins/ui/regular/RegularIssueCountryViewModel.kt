package com.michele.eurocoins.ui.regular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.RegularCollectionEntry
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.REGULAR_DENOMINATIONS
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.SeriesDenomination
import com.michele.eurocoins.data.denominationsOf
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.flagEmojiForCountry
import com.michele.eurocoins.data.stableKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Un taglio della serie selezionata più quante annate distinte l'utente possiede (0 = non posseduto).
 * [denomination] porta la serie a cui il taglio APPARTIENE (diversa da quella selezionata per i tagli
 * che non hanno cambiato disegno) e l'immagine già ritagliata alla finestra della serie guardata, vedi
 * [denominationsOf]; [items] sono solo le annate DENTRO quella finestra.
 */
data class DenominationUiState(
    val denomination: SeriesDenomination,
    val ownedYears: Int,
    val items: List<RegularCollectionItem>,
) {
    val series: RegularIssueSeries get() = denomination.series
    val image: RegularIssueImage get() = denomination.image
}

data class RegularIssueCountryUiState(
    val countryName: String = "",
    val flag: String = "",
    /** Tutte le serie del paese: il selettore in cima compare solo se ce n'è più di una. */
    val seriesList: List<RegularIssueSeries> = emptyList(),
    val selectedIndex: Int = 0,
    /** Tagli della serie selezionata, uno per card (non più una riga orizzontale unica). */
    val denominations: List<DenominationUiState> = emptyList(),
) {
    val selectedSeries: RegularIssueSeries? get() = seriesList.getOrNull(selectedIndex)
}

/**
 * Le serie di un singolo paese (ordine già garantito da `ordineCronologico`, vedi
 * `RegularIssueDao.observeAll`) più la collezione utente sulle monete circolanti. Con più di una
 * serie (Belgio 3, Vaticano 6...) l'utente sceglie quale vedere con un selettore a chip; la lista
 * sotto è una card per taglio (stesso linguaggio di `CoinRow` in Commemorative), non più una riga
 * orizzontale dentro un'unica card di serie.
 */
class RegularIssueCountryViewModel(
    private val repository: RegularIssueRepository,
    private val paese: String,
) : ViewModel() {

    private val selectedIndex = MutableStateFlow(0)

    val uiState: StateFlow<RegularIssueCountryUiState> = combine(
        repository.series,
        repository.collectionItems,
        selectedIndex,
    ) { all, collection, index -> buildState(all, collection, index) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    private fun initialState(): RegularIssueCountryUiState {
        val series = repository.seriesNow
        val collection = repository.collectionNow
        return if (series != null && collection != null) {
            buildState(series, collection, selectedIndex.value)
        } else {
            RegularIssueCountryUiState()
        }
    }

    private fun buildState(
        all: List<RegularIssueSeries>,
        collection: List<RegularCollectionItem>,
        index: Int,
    ): RegularIssueCountryUiState {
        val forCountry = all.filter { it.paese == paese }
        val safeIndex = index.coerceIn(0, (forCountry.size - 1).coerceAtLeast(0))
        val selected = forCountry.getOrNull(safeIndex)
        val denominations = selected?.let { series ->
            denominationsOf(forCountry, series).sortedByDescending { denominationRank(it.image.taglio) }.map { denomination ->
                val key = denomination.series.stableKey
                val items = collection.filter {
                    it.seriesKey == key && it.taglio == denomination.image.taglio && denomination.contains(it.anno)
                }
                DenominationUiState(
                    denomination = denomination,
                    ownedYears = items.map { it.anno }.distinct().size,
                    items = items,
                )
            }
        }.orEmpty()
        return RegularIssueCountryUiState(
            countryName = forCountry.firstOrNull()?.displayCountry() ?: paese,
            flag = flagEmojiForCountry(paese),
            seriesList = forCountry,
            selectedIndex = safeIndex,
            denominations = denominations,
        )
    }

    fun onSelectSeries(index: Int) {
        selectedIndex.value = index
    }

    fun onSaveCollection(denomination: SeriesDenomination, entries: List<RegularCollectionEntry>) {
        val series = denomination.series
        viewModelScope.launch {
            repository.saveCollection(series.stableKey, denomination.image.taglio, series.paese, entries, window = denomination)
        }
    }
}

/**
 * Posizione di un taglio nell'elenco canonico (1 cent = 0 ... 2 euro = 7), da ordinare in modo
 * DECRESCENTE: la schermata di una serie parte dal taglio più grande (2 euro) e scende fino all'1 cent,
 * perché di solito i tagli grandi hanno i disegni più belli (richiesta dell'utente; non c'è un FILTER
 * con l'ordinamento in questa schermata). `RegularIssueSeries.immagini` non è garantita in un ordine: i
 * tagli aggiunti dall'arricchimento BCE (es. Andorra 1 euro, mai pubblicato dalla fonte EC) finiscono in
 * coda alla lista originale. Un taglio non riconosciuto vale -1: in ordine decrescente finisce in fondo,
 * invece di far fallire l'ordinamento.
 */
private fun denominationRank(taglio: String): Int = REGULAR_DENOMINATIONS.indexOf(taglio)
