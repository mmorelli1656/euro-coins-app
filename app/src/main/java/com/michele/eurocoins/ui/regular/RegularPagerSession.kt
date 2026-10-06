package com.michele.eurocoins.ui.regular

import androidx.lifecycle.ViewModel

/**
 * Una pagina del dettaglio di Regular Issues: la serie GUARDATA (`paese` + `ordine` = `ordineCronologico`,
 * come nella rotta) e il taglio. [id] è la chiave stabile e salvabile in un Bundle (String) per liste e
 * pager: una data class qualsiasi non lo sarebbe.
 */
data class RegularPageKey(val paese: String, val ordine: Int, val taglio: String) {
    val id: String get() = "$paese|$ordine|$taglio"
}

/**
 * Contesto del dettaglio a pagine di un taglio (stessa idea di [com.michele.eurocoins.ui.detail.CoinPagerSession]):
 * l'elenco ORDINATO di righe da cui si è arrivati e la funzione con cui dire alla lista in che riga si è
 * finiti. Le righe cambiano a seconda dell'origine: gli 8 tagli di una serie (schermata del paese), un
 * taglio in tutti i paesi (elenco di un taglio), tutte le righe (scheda All). Copia presa al tocco, non
 * riferimento vivo, e `ViewModel` dell'Activity per sopravvivere alla rotazione; dopo la morte del processo
 * il dettaglio ripiega su una pagina sola ([pagesFor]).
 */
class RegularPagerSession : ViewModel() {
    private var pages: List<RegularPageKey> = emptyList()
    private var onShown: ((RegularPageKey) -> Unit)? = null

    /** Da chiamare al tocco di una riga: [onShown] riceve la riga in cui si è finiti scorrendo. */
    fun open(pages: List<RegularPageKey>, onShown: (RegularPageKey) -> Unit) {
        this.pages = pages
        this.onShown = onShown
    }

    fun pagesFor(initial: RegularPageKey): List<RegularPageKey> = if (initial in pages) pages else listOf(initial)

    fun onPageShown(key: RegularPageKey) {
        onShown?.invoke(key)
    }

    override fun onCleared() {
        onShown = null
    }
}
