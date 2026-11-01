package com.michele.eurocoins.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dove stanno gli obiettivi personali (vedi [Goal]): un file di SharedPreferences tutto loro (`goals`), non Room: sono poche voci
 * (al massimo [MAX_GOALS]) e non hanno bisogno di migrazioni. Sono nel backup su Drive dalla versione 6 del formato (scritto
 * solo se c'è almeno un obiettivo, come per le zecche): vedi [com.michele.eurocoins.data.backup.BackupFile].
 */
class GoalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _goals = MutableStateFlow(decodeGoals(prefs.getString(KEY_LIST, null)))

    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    fun add(goal: Goal) = save(_goals.value.added(goal))

    fun remove(id: String) = save(_goals.value.removed(id))

    fun markDone(ids: Set<String>, day: Long) = save(_goals.value.markedDone(ids, day))

    /** Il ripristino di un backup: sostituisce gli obiettivi (senza doppioni e nel massimo, anche se il file è scritto a mano). */
    fun replaceAll(goals: List<Goal>) = save(goals.distinctBy { it.id }.take(MAX_GOALS))

    private fun save(next: List<Goal>) {
        if (next == _goals.value) return
        prefs.edit().putString(KEY_LIST, encodeGoals(next)).apply()
        _goals.value = next
    }

    private companion object {
        const val PREFS_NAME = "goals"
        const val KEY_LIST = "list"
    }
}
