package com.michele.eurocoins.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** L'ordine e le card nascoste di Statistics: preferenza di visualizzazione, salvata come testo. */
class StatsLayoutTest {

    @Test
    fun theDefaultShowsEverythingInTheOriginalOrder() {
        val layout = StatsLayout()
        assertEquals(StatsCard.entries.toList(), layout.visible)
        assertTrue(layout.isDefault)
    }

    @Test
    fun hiddenCardsLeaveTheVisibleList() {
        val layout = StatsLayout().toggle(StatsCard.SHINE).toggle(StatsCard.PROGRESS)
        assertEquals(listOf(StatsCard.COLLECTION, StatsCard.MILESTONES, StatsCard.BADGES, StatsCard.EXPENSES), layout.visible)
        assertFalse(layout.isDefault)
        // Un secondo tocco la riaccende.
        assertTrue(StatsCard.SHINE in layout.toggle(StatsCard.SHINE).visible)
    }

    @Test
    fun everyCardCanBeHiddenIncludingTheCollectionSummary() {
        assertTrue(StatsCard.COLLECTION !in StatsLayout().toggle(StatsCard.COLLECTION).visible)
        assertTrue(StatsCard.COLLECTION in StatsLayout.decode("collection,progress;collection").hidden)
        // Anche tutte insieme: la pagina resta vuota e lo dice, non è un errore.
        val none = StatsCard.entries.fold(StatsLayout()) { layout, card -> layout.toggle(card) }
        assertTrue(none.visible.isEmpty())
        assertEquals(none, StatsLayout.decode(none.encode()))
    }

    @Test
    fun movingACardReordersTheOthers() {
        val layout = StatsLayout().move(StatsCard.BADGES, 1)
        assertEquals(
            listOf(StatsCard.COLLECTION, StatsCard.BADGES, StatsCard.PROGRESS, StatsCard.MILESTONES, StatsCard.SHINE, StatsCard.EXPENSES),
            layout.order,
        )
        // Fuori dai limiti si ferma sul bordo, e spostare dove già sta non cambia niente.
        assertEquals(StatsCard.COLLECTION, StatsLayout().move(StatsCard.COLLECTION, 99).order.last())
        assertEquals(StatsLayout(), StatsLayout().move(StatsCard.PROGRESS, 1))
    }

    @Test
    fun savingAndLoadingKeepsOrderAndHiddenCards() {
        val layout = StatsLayout().move(StatsCard.EXPENSES, 0).toggle(StatsCard.SHINE).toggle(StatsCard.BADGES)
        assertEquals(layout, StatsLayout.decode(layout.encode()))
        assertEquals(StatsLayout(), StatsLayout.decode(StatsLayout().encode()))
    }

    @Test
    fun nothingSavedOrGarbageGivesTheDefault() {
        assertEquals(StatsLayout(), StatsLayout.decode(null))
        assertEquals(StatsLayout(), StatsLayout.decode(""))
        assertEquals(StatsLayout(), StatsLayout.decode("???;!!!"))
    }

    @Test
    fun unknownNamesAreIgnoredAndMissingCardsGoLast() {
        // "futuro" non esiste più o non esiste ancora; BADGES, SHINE ed EXPENSES mancano dal salvataggio (card nuove).
        val layout = StatsLayout.decode("progress,futuro,collection,progress,milestones;futuro,shine")
        assertEquals(
            listOf(StatsCard.PROGRESS, StatsCard.COLLECTION, StatsCard.MILESTONES, StatsCard.BADGES, StatsCard.SHINE, StatsCard.EXPENSES),
            layout.order,
        )
        assertEquals(setOf(StatsCard.SHINE), layout.hidden)
    }
}
