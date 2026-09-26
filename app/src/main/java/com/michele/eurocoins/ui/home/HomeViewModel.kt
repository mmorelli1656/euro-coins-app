package com.michele.eurocoins.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.progress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val progress: Progress = Progress(0, 0),
    val countries: Int = 0,
    val firstYear: Int? = null,
    val lastYear: Int? = null,
    /** Quattro monete con immagine, di paesi diversi, per la fascia della scheda. */
    val showcase: List<Coin> = emptyList(),
)

class HomeViewModel(private val repository: CoinRepository) : ViewModel() {

    /**
     * Ultimo valore mostrato dalla barra "x / y collected" (null = mai mostrata: prossimo avvio
     * = primo avvio). Stanno qui e non in un `remember` perché il ViewModel sopravvive quando si
     * va in un'altra schermata e si torna: la composizione di Home viene ricreata (un `remember`
     * ripartirebbe da zero e rifarebbe l'onda), il ViewModel no. Un riavvio vero dell'app crea un
     * ViewModel nuovo, quindi torna a essere un primo avvio.
     */
    var lastShownOwned: Int? = null

    val uiState: StateFlow<HomeUiState> = combine(repository.coins, repository.ownedKeys) { coins, ownedKeys ->
        HomeUiState(
            progress = coins.progress(ownedKeys),
            countries = coins.map { it.paese }.distinct().size,
            firstYear = coins.minOfOrNull { it.anno },
            lastYear = coins.maxOfOrNull { it.anno },
            showcase = coins
                .filter { !it.immaginePlaceholder && it.urlImmagineFonte != null }
                .distinctBy { it.paese }
                .take(4),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    // La home è la prima schermata: è qui che il database viene popolato al
    // primo avvio (ensureSeeded è protetto da mutex e idempotente).
    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }
}
