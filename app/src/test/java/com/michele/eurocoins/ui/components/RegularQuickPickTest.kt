package com.michele.eurocoins.ui.components

import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.YearOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La scelta rapida di Standard nel pannello di Regular Issues segue l'anno finché l'utente non tocca niente. */
class RegularQuickPickTest {
    private fun standard(year: Int, variety: String = "") = DraftKey(year, variety, CoinQuality.STANDARD)
    private fun ticked(map: Map<DraftKey, Boolean>) = map.filterValues { it }.keys

    @Test
    fun changingYearMovesTheQuickPickInsteadOfLeavingItBehind() {
        val checked = mutableMapOf(standard(2014) to true)
        val pick = moveQuickPick(checked, standard(2014), YearOption(2017, ""))
        // Il 2014 non resta spuntato di nascosto, il 2017 è già preselezionato.
        assertEquals(setOf(standard(2017)), ticked(checked))
        assertEquals(standard(2017), pick)
    }

    @Test
    fun itKeepsFollowingAcrossSeveralYearChanges() {
        val checked = mutableMapOf(standard(2014) to true)
        var pick: DraftKey? = standard(2014)
        for (year in listOf(2015, 2017, 2016, 2014)) pick = moveQuickPick(checked, pick, YearOption(year, ""))
        assertEquals(setOf(standard(2014)), ticked(checked))
        assertEquals(standard(2014), pick)
    }

    @Test
    fun afterTheUserTouchesAFinishNothingMovesAnymore() {
        // quickPick = null: l'utente ha già spuntato qualcosa, le sue scelte restano dove sono.
        val checked = mutableMapOf(standard(2014) to true, DraftKey(2014, "", CoinQuality.BU) to true)
        val pick = moveQuickPick(checked, null, YearOption(2017, ""))
        assertNull(pick)
        assertEquals(setOf(standard(2014), DraftKey(2014, "", CoinQuality.BU)), ticked(checked))
    }

    @Test
    fun reselectingTheSameYearChangesNothing() {
        val checked = mutableMapOf(standard(2014) to true)
        val pick = moveQuickPick(checked, standard(2014), YearOption(2014, ""))
        assertEquals(standard(2014), pick)
        assertEquals(setOf(standard(2014)), ticked(checked))
    }

    @Test
    fun theVarietyOfTheNewEntryIsKept() {
        // Grecia 2002: "2002 EFS" è una moneta a parte, la scelta rapida si sposta anche lì.
        val checked = mutableMapOf(standard(2002) to true)
        val pick = moveQuickPick(checked, standard(2002), YearOption(2002, "EFS"))
        assertEquals(standard(2002, "EFS"), pick)
        assertTrue(standard(2002) !in ticked(checked))
    }
}
