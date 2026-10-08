package com.michele.eurocoins.ui.detail

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/** Quante volte al massimo si mostra il rimbalzo che suggerisce lo scorrimento (scelta del proprietario: 3). */
internal const val SWIPE_HINT_MAX_SHOWS = 3

/**
 * Quando mostrare il suggerimento di scorrimento nel dettaglio: serve più di una pagina, l'utente non deve aver
 * mai scorso (chi lo ha fatto una volta ha capito) e non va superato il tetto di volte. Logica pura e testata.
 */
internal fun shouldShowSwipeHint(timesShown: Int, hasSwiped: Boolean, pageCount: Int): Boolean =
    pageCount > 1 && !hasSwiped && timesShown < SWIPE_HINT_MAX_SHOWS

/**
 * Stato del suggerimento "la card si scorre", salvato in SharedPreferences (`swipe_hint`) perché valga anche dopo
 * un riavvio: non si ripete a ogni avvio (diventerebbe fastidioso e chi ha capito smetterebbe di guardarlo).
 * Si mostra all'apertura del dettaglio finché l'utente non fa un vero scorrimento, al massimo
 * [SWIPE_HINT_MAX_SHOWS] volte. Vedi `StackedPager` per l'animazione.
 */
class SwipeHint(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("swipe_hint", Context.MODE_PRIVATE)
    private var timesShown = prefs.getInt(KEY_SHOWN, 0)
    private var hasSwiped = prefs.getBoolean(KEY_SWIPED, false)

    fun shouldShow(pageCount: Int): Boolean = shouldShowSwipeHint(timesShown, hasSwiped, pageCount)

    /** Il rimbalzo è partito davvero (non conta se è stato annullato prima, o se le animazioni sono spente). */
    fun onShown() {
        timesShown++
        prefs.edit().putInt(KEY_SHOWN, timesShown).apply()
    }

    /** L'utente ha cambiato pagina scorrendo: il suggerimento non serve più. */
    fun onSwiped() {
        if (hasSwiped) return
        hasSwiped = true
        prefs.edit().putBoolean(KEY_SWIPED, true).apply()
    }

    private companion object {
        const val KEY_SHOWN = "times_shown"
        const val KEY_SWIPED = "has_swiped"
    }
}

/** Il suggerimento, fornito da `MainActivity`; `null` (anteprime, test) = nessun suggerimento. */
internal val LocalSwipeHint = staticCompositionLocalOf<SwipeHint?> { null }
