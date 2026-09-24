package com.michele.eurocoins.ui.settings

import android.content.Context
import com.michele.eurocoins.ui.browse.BrowseMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferenze dell'utente sul catalogo, salvate in SharedPreferences (file `settings`).
 * Il tema ha la sua classe ([com.michele.eurocoins.ui.theme.ThemePreference]) perché
 * `MainActivity` lo legge prima di disegnare qualunque schermata.
 *
 * Gli stati sono [StateFlow] così il catalogo si aggiorna subito quando l'utente
 * cambia opzione dalla schermata Impostazioni, senza riaprire le schermate.
 */
class UserSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hideMicrostates = MutableStateFlow(prefs.getBoolean(KEY_HIDE_MICROSTATES, false))
    private val _defaultTab = MutableStateFlow(loadDefaultTab())

    /** true = nasconde ovunque monete e card di Andorra, Monaco, San Marino e Città del Vaticano. */
    val hideMicrostates: StateFlow<Boolean> = _hideMicrostates.asStateFlow()

    /** Scheda di Commemorative che si apre per prima. */
    val defaultTab: StateFlow<BrowseMode> = _defaultTab.asStateFlow()

    fun setHideMicrostates(value: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_MICROSTATES, value).apply()
        _hideMicrostates.value = value
    }

    fun setDefaultTab(mode: BrowseMode) {
        prefs.edit().putString(KEY_DEFAULT_TAB, mode.name).apply()
        _defaultTab.value = mode
    }

    // Valore sconosciuto o assente (prima installazione, enum rinominato): parte da Years.
    private fun loadDefaultTab(): BrowseMode =
        runCatching { BrowseMode.valueOf(prefs.getString(KEY_DEFAULT_TAB, null).orEmpty()) }.getOrDefault(BrowseMode.YEARS)

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_HIDE_MICROSTATES = "hide_microstates"
        const val KEY_DEFAULT_TAB = "default_tab"
    }
}
