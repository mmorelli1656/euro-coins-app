package com.michele.eurocoins.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CoinDetailViewModel(
    private val repository: CoinRepository,
    private val coinId: Long,
) : ViewModel() {

    private val _coin = MutableStateFlow<Coin?>(null)
    val coin: StateFlow<Coin?> = _coin.asStateFlow()

    init {
        viewModelScope.launch {
            _coin.value = repository.getById(coinId)
        }
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
