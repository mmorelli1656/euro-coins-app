package com.michele.eurocoins.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `yearMintLabels()` con dati sintetici: quando le etichette compaiono e come si distinguono i livelli. */
class RegularYearMintsTest {

    private fun certain(anno: Int, vararg zecche: String) = RegularIssueYearMint(anno, zecche = zecche.toList())
    private fun probable(anno: Int, vararg zecche: String) = RegularIssueYearMint(anno, probabili = zecche.toList())

    @Test
    fun singleMintCountryGetsNoLabels() {
        val data = (2002..2005).map { certain(it, "Rome") }
        assertTrue(yearMintLabels(data, (2002..2005).toList()).isEmpty())
    }

    @Test
    fun germanFiveMintsCollapseToOneCountryAndStayUnlabelled() {
        val five = arrayOf("Berlin", "Munich", "Hamburg Mint", "State Mint of Stuttgart / State Mints of Baden-Württemberg", "State Mint of Karlsruhe / State Mints of Baden-Württemberg")
        val data = (2002..2004).map { certain(it, *five) }
        assertTrue(yearMintLabels(data, (2002..2004).toList()).isEmpty())
    }

    @Test
    fun noDataAtAllGetsNoLabels() {
        assertTrue(yearMintLabels(emptyList(), listOf(2002, 2003)).isEmpty())
    }

    @Test
    fun changingMintLabelsEveryYearWithItsLevel() {
        val data = listOf(certain(2007, "Mint of Finland"), probable(2008, "Royal Dutch Mint"))
        val labels = yearMintLabels(data, listOf(2007, 2008, 2009))
        assertEquals(YearMintLabel(MintLevel.CERTAIN, listOf("Finland")), labels[2007])
        assertEquals("Netherlands ?", labels[2008]!!.text)
        // anno senza voce: non nota, mai "nessuna zecca"
        assertEquals(MintLevel.UNKNOWN, labels[2009]!!.level)
        assertEquals("not known", labels[2009]!!.text)
    }

    @Test
    fun certainWinsOverProbableInTheSameYear() {
        val both = RegularIssueYearMint(2010, zecche = listOf("Kremnica"), probabili = listOf("Rome"))
        val labels = yearMintLabels(listOf(both, certain(2011, "Rome")), listOf(2010, 2011))
        assertEquals("Slovakia", labels[2010]!!.text)
    }

    @Test
    fun unknownYearsNextToASingleKnownCountryStillShowLabels() {
        val labels = yearMintLabels(listOf(certain(2014, "Mint of Finland")), listOf(2014, 2015))
        assertEquals(setOf(2014, 2015), labels.keys)
    }

    @Test
    fun allUnknownGetsNoLabels() {
        assertTrue(yearMintLabels(listOf(certain(2020)), listOf(2020, 2021)).isEmpty())
    }

    @Test
    fun multipleCountriesInOneYearAreListedAndCompareWithoutOrder() {
        val a = yearMintLabels(listOf(certain(2002, "Monnaie de Paris", "Royal Dutch Mint"), certain(2003, "Rome")), listOf(2002, 2003))
        assertEquals("France, Netherlands", a[2002]!!.text)
        val b = YearMintLabel(MintLevel.CERTAIN, listOf("Netherlands", "France"))
        assertEquals(a[2002]!!.sameAs, b.sameAs)
    }

    @Test
    fun certainAndProbableOfTheSameCountryAreDifferentPeriods() {
        val certainFinland = YearMintLabel(MintLevel.CERTAIN, listOf("Finland"))
        val probableFinland = YearMintLabel(MintLevel.PROBABLE, listOf("Finland"))
        assertTrue(certainFinland.sameAs != probableFinland.sameAs)
    }
}
