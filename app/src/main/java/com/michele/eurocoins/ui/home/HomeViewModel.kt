package com.michele.eurocoins.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.progress
import com.michele.eurocoins.data.regularProgress
import com.michele.eurocoins.ui.settings.UserSettings
import java.time.LocalDate
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
    /** Set di domani (vuoto con la rotazione spenta): la Home ne precarica le foto in Coil. */
    val nextShowcase: List<Coin> = emptyList(),
    /** URL dell'ultimo set mostrato per intero (ripiego per foto che non si caricano). */
    val lastShowcaseUrls: List<String> = emptyList(),
    /** Statistiche della scheda Regular Issues: righe dei tagli (8 per serie, vedi `regularProgress`), paesi e serie coperte. */
    val regularIssueCoinsCount: Int = 0,
    val regularIssueCountries: Int = 0,
    val regularIssueSeriesCount: Int = 0,
    /** Righe possedute (su [regularIssueCoinsCount]), per la barra "collected": vedi `regularProgress`. */
    val regularIssueOwnedCount: Int = 0,
    /** Una foto per fascia di taglio crescente (§ dataset), null dove manca, per la fascia della scheda Regular Issues. */
    val regularIssueShowcase: List<String?> = emptyList(),
    /** Set di domani per Regular Issues (vuoto con la rotazione spenta): la Home ne precarica le foto in Coil. */
    val regularIssueNextShowcase: List<String?> = emptyList(),
    /** Come [lastShowcaseUrls], per Regular Issues: posizioni preservate, vedi `UserSettings.regularIssueLastShowcase`. */
    val regularIssueLastShowcaseUrls: List<String?> = emptyList(),
)

class HomeViewModel(
    private val repository: CoinRepository,
    private val regularIssueRepository: RegularIssueRepository,
    private val settings: UserSettings,
) : ViewModel() {

    /**
     * Ultimo valore mostrato dalla barra "x / y collected" (null = mai mostrata: prossimo avvio
     * = primo avvio). Stanno qui e non in un `remember` perché il ViewModel sopravvive quando si
     * va in un'altra schermata e si torna: la composizione di Home viene ricreata (un `remember`
     * ripartirebbe da zero e rifarebbe l'onda), il ViewModel no. Un riavvio vero dell'app crea un
     * ViewModel nuovo, quindi torna a essere un primo avvio.
     */
    var lastShownOwned: Int? = null

    /** Stesso discorso di [lastShownOwned], per la barra "collected" della scheda Regular Issues. */
    var lastShownRegularOwned: Int? = null

    // Letto una volta: durante la sessione il ripiego non cambia (il nuovo set salvato serve dalla prossima apertura).
    // Foto note subito, senza il database: il set di oggi se già calcolato (ieri l'ha precaricato, oppure
    // un'apertura precedente di oggi), altrimenti l'ultimo mostrato. Sono le monete del primo fotogramma.
    private val lastShowcaseUrls: List<String> = firstFrameShowcaseUrls(settings)

    /** Stesso discorso di [lastShowcaseUrls], per la fascia di Regular Issues. */
    private val regularIssueLastShowcaseUrls: List<String?> = firstFrameRegularIssueShowcaseUrls(settings)

    fun saveLastShowcase(urls: List<String>) = settings.setLastShowcase(urls)

    fun saveLastRegularIssueShowcase(urls: List<String?>) = settings.setRegularIssueLastShowcase(urls)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.coins,
        repository.ownedKeys,
        settings.rotateHomeCoins,
        regularIssueRepository.series,
        regularIssueRepository.collectionItems,
    ) { coins, ownedKeys, rotate, regularIssueSeries, regularCollection ->
        val today = LocalDate.now().toEpochDay()
        val showcase = pickShowcase(coins, if (rotate) today else null)
        val nextShowcase = if (rotate) pickShowcase(coins, today + 1) else emptyList()
        if (rotate && showcase.isNotEmpty()) {
            settings.saveShowcaseUrls(mapOf(today to showcase.mapNotNull { it.urlImmagineFonte }, today + 1 to nextShowcase.mapNotNull { it.urlImmagineFonte }))
        }
        val regularIssueShowcase = pickRegularIssueShowcaseUrls(regularIssueSeries, if (rotate) today else null)
        val regularIssueNextShowcase = if (rotate) pickRegularIssueShowcaseUrls(regularIssueSeries, today + 1) else emptyList()
        if (rotate && regularIssueShowcase.any { it != null }) {
            // Niente mapNotNull qui: le posizioni contano (vedi UserSettings.saveRegularIssueShowcaseUrls).
            settings.saveRegularIssueShowcaseUrls(mapOf(today to regularIssueShowcase, today + 1 to regularIssueNextShowcase))
        }
        // Le righe delle serie (8 per serie, anche i tagli invariati) e quelle possedute: lo stesso conto
        // della barra di ogni paese, vedi `regularProgress` — e CLAUDE.md § Regular Issues.
        val regularIssueProgress = regularIssueSeries.regularProgress(regularCollection)
        HomeUiState(
            progress = coins.progress(ownedKeys),
            countries = coins.map { it.paese }.distinct().size,
            firstYear = coins.minOfOrNull { it.anno },
            lastYear = coins.maxOfOrNull { it.anno },
            showcase = showcase,
            nextShowcase = nextShowcase,
            lastShowcaseUrls = lastShowcaseUrls,
            regularIssueCoinsCount = regularIssueProgress.total,
            regularIssueCountries = regularIssueSeries.map { it.paese }.distinct().size,
            regularIssueSeriesCount = regularIssueSeries.size,
            regularIssueOwnedCount = regularIssueProgress.owned,
            regularIssueShowcase = regularIssueShowcase,
            regularIssueNextShowcase = regularIssueNextShowcase,
            regularIssueLastShowcaseUrls = regularIssueLastShowcaseUrls,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(lastShowcaseUrls = lastShowcaseUrls, regularIssueLastShowcaseUrls = regularIssueLastShowcaseUrls),
    )

    // La home è la prima schermata: è qui che i database vengono popolati al
    // primo avvio (ensureSeeded è protetto da mutex e idempotente).
    init {
        viewModelScope.launch { repository.ensureSeeded() }
        viewModelScope.launch { regularIssueRepository.ensureSeeded() }
    }
}
