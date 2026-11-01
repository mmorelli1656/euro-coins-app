package com.michele.eurocoins.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dove stanno gli obiettivi personali (vedi [Goal]): un file di SharedPreferences tutto loro (`goals`), non Room: sono poche voci
 * (al massimo [MAX_GOALS]) e non hanno bisogno di migrazioni. NON sono ancora nel backup su Drive (da fare: formato v6, scritto
 * solo se c'è almeno un obiettivo, come per le zecche); cambiando telefono per ora si perdono.
 */
class GoalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _goals = MutableStateFlow(decodeGoals(prefs.getString(KEY_LIST, null)))

    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    fun add(goal: Goal) = save(_goals.value.added(goal))

    fun remove(id: String) = save(_goals.value.removed(id))

    fun markDone(ids: Set<String>, day: Long) = save(_goals.value.markedDone(ids, day))

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
