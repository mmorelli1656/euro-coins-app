package com.michele.eurocoins.ui.components

import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.YearOption
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La spunta iniziale di Standard nel pannello di Regular Issues è una spunta vera della bozza: sta sull'anno di
 * partenza (e sulla sua varietà) e non si sposta né sparisce cambiando anno, quindi più annate dello stesso taglio
 * (2002 e 2003, Grecia 2002 e 2002 EFS) si registrano spuntando l'altra senza toccare niente due volte. Il resto
 * (non spostarla) è comportamento della UI: qui si fissa solo che cosa viene preselezionato.
 */
class RegularQuickPickTest {
    @Test
    fun theInitialTickIsStandardOnTheStartingYear() {
        assertEquals(DraftKey(2002, "", CoinQuality.STANDARD), initialDraftKey(YearOption(2002, "")))
    }

    @Test
    fun theVarietyOfTheStartingEntryIsKept() {
        // "2002 EFS" è una moneta a parte: se il pannello si apre lì, la spunta iniziale è quella.
        assertEquals(DraftKey(2002, "EFS", CoinQuality.STANDARD), initialDraftKey(YearOption(2002, "EFS")))
    }
}
