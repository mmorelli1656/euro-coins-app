package com.michele.eurocoins.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tema scelto dall'utente: [SYSTEM] segue il telefono, gli altri due lo forzano. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/**
 * Scelta del tema, salvata in SharedPreferences (file `theme`). Lo stato è un
 * [StateFlow] così `MainActivity` ridisegna l'app appena l'utente cambia
 * opzione, senza riavviare l'activity.
 */
class ThemePreference(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(load())

    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    // Valore sconosciuto o assente (prima installazione, enum rinominato): torna a "System".
    private fun load(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_MODE, null).orEmpty()) }.getOrDefault(ThemeMode.SYSTEM)

    private companion object {
        const val PREFS_NAME = "theme"
        const val KEY_MODE = "mode"
    }
}
