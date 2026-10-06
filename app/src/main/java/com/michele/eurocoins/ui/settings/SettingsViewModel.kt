package com.michele.eurocoins.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.ui.browse.BrowseMode
import com.michele.eurocoins.ui.regular.RegularBrowseMode
import com.michele.eurocoins.ui.theme.ThemeMode
import com.michele.eurocoins.ui.theme.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Parte "catalogo, aspetto e reset" della schermata Impostazioni (account e backup: `BackupViewModel`). */
class SettingsViewModel(
    private val repository: CoinRepository,
    private val regularIssueRepository: RegularIssueRepository,
    private val settings: UserSettings,
    private val themePreference: ThemePreference,
) : ViewModel() {

    val hideCommemorativeMicrostates: StateFlow<Boolean> = settings.hideCommemorativeMicrostates
    val hideRegularMicrostates: StateFlow<Boolean> = settings.hideRegularMicrostates
    val defaultTab: StateFlow<BrowseMode> = settings.defaultTab
    val defaultRegularTab: StateFlow<RegularBrowseMode> = settings.defaultRegularTab
    val rotateHomeCoins: StateFlow<Boolean> = settings.rotateHomeCoins
    val themeMode: StateFlow<ThemeMode> = themePreference.mode

    /**
     * Monete possedute (commemorative in almeno una qualità, tagli di Regular Issues in almeno
     * un'annata), anche quelle di microstati nascosti: sono tutte quelle che il reset toglie.
     */
    val ownedCounts: StateFlow<OwnedCounts> = combine(repository.ownedCount, regularIssueRepository.ownedCount, ::OwnedCounts)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnedCounts(0, 0))

    fun setHideCommemorativeMicrostates(value: Boolean) = settings.setHideCommemorativeMicrostates(value)
    fun setHideRegularMicrostates(value: Boolean) = settings.setHideRegularMicrostates(value)
    fun setDefaultTab(mode: BrowseMode) = settings.setDefaultTab(mode)
    fun setDefaultRegularTab(mode: RegularBrowseMode) = settings.setDefaultRegularTab(mode)
    fun setRotateHomeCoins(value: Boolean) = settings.setRotateHomeCoins(value)
    fun setThemeMode(mode: ThemeMode) = themePreference.set(mode)

    /** Svuota entrambe le collezioni: commemorative e Regular Issues (il backup su Drive resta com'è). */
    fun resetCollection() {
        viewModelScope.launch {
            repository.resetCollection()
            regularIssueRepository.resetCollection()
        }
    }
}

/** Quanto possiede l'utente nelle due sezioni: serve al messaggio del reset. */
data class OwnedCounts(val commemorative: Int, val regular: Int) {
    val total: Int get() = commemorative + regular

    /**
     * Una voce per sezione non vuota, per l'elenco del dialog di Reset sotto "This removes the following coins:":
     * "85 Commemorative", "4 Regular Issues" (i nomi dei cataloghi come nel resto dell'app, senza "coins" in coda:
     * lo dice l'intestazione, e così non c'è nemmeno un plurale da accordare). Era una frase che andava a capo a
     * metà nome ("2 coins from Commemorative and 4 coins from Regular Issues"); con un elenco ogni riga è corta.
     */
    fun lines(): List<String> = listOfNotNull(
        commemorative.takeIf { it > 0 }?.let { "$it Commemorative" },
        regular.takeIf { it > 0 }?.let { "$it Regular Issues" },
    )
}
