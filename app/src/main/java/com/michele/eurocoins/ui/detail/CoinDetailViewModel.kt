package com.michele.eurocoins.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.stableKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CoinDetailViewModel(
    private val repository: CoinRepository,
    private val coinId: Long,
) : ViewModel() {

    private val _coin = MutableStateFlow<Coin?>(null)
    val coin: StateFlow<Coin?> = _coin.asStateFlow()

    /** Le voci di collezione di QUESTA moneta (una per qualità posseduta). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<CollectionItem>> = _coin
        .filterNotNull()
        .flatMapLatest { coin ->
            repository.collectionItems.map { all -> all.filter { it.coinKey == coin.stableKey } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _coin.value = repository.getById(coinId)
        }
    }

    fun onQualityToggled(quality: CoinQuality, owned: Boolean) {
        val coin = _coin.value ?: return
        viewModelScope.launch { repository.setOwned(coin, quality, owned) }
    }

    fun onPriceChanged(quality: CoinQuality, priceCents: Int?) {
        val coin = _coin.value ?: return
        viewModelScope.launch { repository.setPrice(coin, quality, priceCents) }
    }
}

/**
 * Paesi per cui abbiamo osservato che la tiratura BCE è quasi certamente il
 * contingente autorizzato del paese per il periodo, non la tiratura reale
 * della singola moneta — vedi NOTES.md nella pipeline dati per il dettaglio
 * dell'indagine. Va tenuto sincronizzato a mano con quel documento.
 */
val PAESI_TIRATURA_SOSPETTA = setOf(
    "Italia", "Slovacchia", "Slovenia", "Grecia", "Germania", "Lituania", "Lussemburgo",
)
