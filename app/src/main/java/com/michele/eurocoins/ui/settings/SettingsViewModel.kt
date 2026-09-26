package com.michele.eurocoins.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.ui.browse.BrowseMode
import com.michele.eurocoins.ui.theme.ThemeMode
import com.michele.eurocoins.ui.theme.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Parte "catalogo, aspetto e reset" della schermata Impostazioni (account e backup: `BackupViewModel`). */
class SettingsViewModel(
    private val repository: CoinRepository,
    private val settings: UserSettings,
    private val themePreference: ThemePreference,
) : ViewModel() {

    val hideMicrostates: StateFlow<Boolean> = settings.hideMicrostates
    val defaultTab: StateFlow<BrowseMode> = settings.defaultTab
    val rotateHomeCoins: StateFlow<Boolean> = settings.rotateHomeCoins
    val themeMode: StateFlow<ThemeMode> = themePreference.mode

    /** Monete possedute (in almeno una qualità), anche quelle di microstati nascosti: sono tutte quelle che il reset toglie. */
    val ownedCount: StateFlow<Int> = repository.ownedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setHideMicrostates(value: Boolean) = settings.setHideMicrostates(value)
    fun setDefaultTab(mode: BrowseMode) = settings.setDefaultTab(mode)
    fun setRotateHomeCoins(value: Boolean) = settings.setRotateHomeCoins(value)
    fun setThemeMode(mode: ThemeMode) = themePreference.set(mode)

    fun resetCollection() {
        viewModelScope.launch { repository.resetCollection() }
    }
}
