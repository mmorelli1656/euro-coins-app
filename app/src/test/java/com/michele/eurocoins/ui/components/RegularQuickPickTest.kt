package com.michele.eurocoins.ui.components

import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.YearOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La scelta rapida di Standard nel pannello di Regular Issues vale solo finché si resta sull'anno di partenza:
 * al primo cambio di anno sparisce, senza spostarsi (lo spostamento obbligava a deselezionare e riselezionare
 * per registrare due annate, es. Grecia 2002 e 2002 EFS).
 */
class RegularQuickPickTest {
    private fun standard(year: Int, variety: String = "") = DraftKey(year, variety, CoinQuality.STANDARD)
    private fun ticked(map: Map<DraftKey, Boolean>) = map.filterValues { it }.keys

    @Test
    fun changingYearDropsTheQuickPickInsteadOfLeavingItBehindOrMovingIt() {
        val checked = mutableMapOf(standard(2014) to true)
        val pick = dropQuickPickOnYearChange(checked, standard(2014), YearOption(2017, ""))
        // Né il 2014 resta spuntato di nascosto, né il 2017 viene preselezionato: si spunta a mano.
        assertTrue(ticked(checked).isEmpty())
        assertNull(pick)
    }

    @Test
    fun afterTheFirstChangeNothingIsPreselectedAnymore() {
        val checked = mutableMapOf(standard(2014) to true)
        var pick: DraftKey? = standard(2014)
        for (year in listOf(2015, 2017, 2016, 2014)) pick = dropQuickPickOnYearChange(checked, pick, YearOption(year, ""))
        assertTrue(ticked(checked).isEmpty())
        assertNull(pick)
    }

    @Test
    fun greece2002AndEfsCanBothBeTickedWithoutUntickingAnything() {
        val checked = mutableMapOf(standard(2002) to true)
        var pick: DraftKey? = standard(2002)
        // Apre su 2002 (spunta rapida), passa a "2002 EFS" e la spunta a mano, torna a 2002 e la spunta a mano.
        pick = dropQuickPickOnYearChange(checked, pick, YearOption(2002, "EFS"))
        checked[standard(2002, "EFS")] = true
        pick = dropQuickPickOnYearChange(checked, pick, YearOption(2002, ""))
        assertNull(pick)
        // "2002 EFS" resta spuntata: tornare su 2002 non la tocca.
        assertEquals(setOf(standard(2002, "EFS")), ticked(checked))
        checked[standard(2002)] = true
        assertEquals(setOf(standard(2002), standard(2002, "EFS")), ticked(checked))
    }

    @Test
    fun afterTheUserTouchesAFinishNothingChanges() {
        // quickPick = null: l'utente ha già spuntato qualcosa, le sue scelte restano dove sono.
        val checked = mutableMapOf(standard(2014) to true, DraftKey(2014, "", CoinQuality.BU) to true)
        val pick = dropQuickPickOnYearChange(checked, null, YearOption(2017, ""))
        assertNull(pick)
        assertEquals(setOf(standard(2014), DraftKey(2014, "", CoinQuality.BU)), ticked(checked))
    }

    @Test
    fun reselectingTheSameYearChangesNothing() {
        val checked = mutableMapOf(standard(2014) to true)
        val pick = dropQuickPickOnYearChange(checked, standard(2014), YearOption(2014, ""))
        assertEquals(standard(2014), pick)
        assertEquals(setOf(standard(2014)), ticked(checked))
    }
}
