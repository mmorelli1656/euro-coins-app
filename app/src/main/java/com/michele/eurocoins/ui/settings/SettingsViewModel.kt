package com.michele.eurocoins.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.ui.browse.BrowseMode
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

    val hideMicrostates: StateFlow<Boolean> = settings.hideMicrostates
    val defaultTab: StateFlow<BrowseMode> = settings.defaultTab
    val rotateHomeCoins: StateFlow<Boolean> = settings.rotateHomeCoins
    val themeMode: StateFlow<ThemeMode> = themePreference.mode

    /**
     * Monete possedute (commemorative in almeno una qualità, tagli di Regular Issues in almeno
     * un'annata), anche quelle di microstati nascosti: sono tutte quelle che il reset toglie.
     */
    val ownedCounts: StateFlow<OwnedCounts> = combine(repository.ownedCount, regularIssueRepository.ownedCount, ::OwnedCounts)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnedCounts(0, 0))

    fun setHideMicrostates(value: Boolean) = settings.setHideMicrostates(value)
    fun setDefaultTab(mode: BrowseMode) = settings.setDefaultTab(mode)
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

    /** "85 commemorative coins and 4 Regular Issues coins", saltando la sezione vuota. */
    fun describe(): String = listOfNotNull(
        commemorative.takeIf { it > 0 }?.let { "$it commemorative ${coinWord(it)}" },
        regular.takeIf { it > 0 }?.let { "$it Regular Issues ${coinWord(it)}" },
    ).joinToString(" and ")

    private fun coinWord(n: Int) = if (n == 1) "coin" else "coins"
}
