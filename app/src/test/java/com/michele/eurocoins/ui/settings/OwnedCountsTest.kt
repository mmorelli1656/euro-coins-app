package com.michele.eurocoins.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** Le voci dell'elenco del dialog di Reset: i nomi dei cataloghi come nel resto dell'app, la sezione vuota saltata. */
class OwnedCountsTest {

    @Test
    fun oneLinePerSectionNamedLikeTheCatalogs() {
        assertEquals(listOf("2 Commemorative", "4 Regular Issues"), OwnedCounts(2, 4).lines())
    }

    @Test
    fun emptySectionIsSkipped() {
        assertEquals(listOf("85 Commemorative"), OwnedCounts(85, 0).lines())
        assertEquals(listOf("4 Regular Issues"), OwnedCounts(0, 4).lines())
        assertEquals(emptyList<String>(), OwnedCounts(0, 0).lines())
    }
}
