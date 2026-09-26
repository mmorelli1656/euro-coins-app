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
    private val _rotateHomeCoins = MutableStateFlow(prefs.getBoolean(KEY_ROTATE_HOME_COINS, true))

    /** true = nasconde ovunque monete e card di Andorra, Monaco, San Marino e Città del Vaticano. */
    val hideMicrostates: StateFlow<Boolean> = _hideMicrostates.asStateFlow()

    /** Scheda di Commemorative che si apre per prima. */
    val defaultTab: StateFlow<BrowseMode> = _defaultTab.asStateFlow()

    /** true = le monete della Home cambiano ogni giorno; false = set fisso. Vale anche per le future Regular Issues. */
    val rotateHomeCoins: StateFlow<Boolean> = _rotateHomeCoins.asStateFlow()

    fun setHideMicrostates(value: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_MICROSTATES, value).apply()
        _hideMicrostates.value = value
    }

    /**
     * URL delle 4 foto dell'ultimo set della Home mostrato per intero con successo (le foto stanno già nella
     * cache su disco di Coil): ripiego per quando le foto del giorno non si caricano. Non è una
     * preferenza dell'utente ma vive qui per non aprire un altro file di SharedPreferences.
     */
    val lastShowcase: List<String>
        get() = prefs.getString(KEY_LAST_SHOWCASE, null).orEmpty().split(SHOWCASE_SEPARATOR).filter { it.isNotBlank() }

    /** Foto del set di un giorno (`LocalDate.toEpochDay()`), se già calcolato in una sessione precedente: permette alla Home di mostrare subito le monete giuste, senza aspettare il database. */
    fun showcaseUrlsFor(day: Long): List<String>? =
        prefs.getString(KEY_SHOWCASE_DAY_PREFIX + day, null)?.split(SHOWCASE_SEPARATOR)?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }

    /** Salva i set di oggi e domani, scartando quelli dei giorni passati. */
    fun saveShowcaseUrls(sets: Map<Long, List<String>>) {
        if (sets.all { (day, urls) -> showcaseUrlsFor(day) == urls }) return
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith(KEY_SHOWCASE_DAY_PREFIX) }.forEach { remove(it) }
            sets.forEach { (day, urls) -> putString(KEY_SHOWCASE_DAY_PREFIX + day, urls.joinToString(SHOWCASE_SEPARATOR)) }
        }.apply()
    }

    fun setLastShowcase(urls: List<String>) {
        if (urls != lastShowcase) prefs.edit().putString(KEY_LAST_SHOWCASE, urls.joinToString(SHOWCASE_SEPARATOR)).apply()
    }

    fun setRotateHomeCoins(value: Boolean) {
        prefs.edit().putBoolean(KEY_ROTATE_HOME_COINS, value).apply()
        _rotateHomeCoins.value = value
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
        const val KEY_ROTATE_HOME_COINS = "rotate_home_coins"
        const val KEY_LAST_SHOWCASE = "last_showcase"
        const val KEY_SHOWCASE_DAY_PREFIX = "showcase_day_"
        const val SHOWCASE_SEPARATOR = "\n"
    }
}
