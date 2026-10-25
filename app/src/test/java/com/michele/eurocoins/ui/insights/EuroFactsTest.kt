package com.michele.eurocoins.ui.insights

import com.michele.eurocoins.data.REGULAR_DENOMINATIONS
import com.michele.eurocoins.data.flagEmojiForIsoCode
import com.michele.eurocoins.ui.regular.coinSpecFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EuroFactsTest {

    private val members = EURO_ENTRIES.flatMap { it.members }

    @Test
    fun theEuroAreaHasTwentyOneCountriesAndNoneTwice() {
        assertEquals(21, members.size)
        assertEquals(21, members.map { it.iso }.toSet().size)
        assertEquals(11, EURO_ENTRIES.first().members.size)
    }

    @Test
    fun theYearsGoForwardAndThe1999StepComesFirst() {
        val years = EURO_ENTRIES.map { it.year.toInt() }
        assertEquals(1999, years.first())
        assertEquals(years.sorted(), years)
        assertEquals(years.distinct(), years)
    }

    @Test
    fun everyCountryHasAFlagAndARateThatIsANumber() {
        for (m in members + EURO_AGREEMENTS.map { EuroMember(it.iso, it.name, "", "", "1") }) {
            // Due lettere ISO → due "regional indicator", 2 caratteri UTF-16 ciascuno.
            assertEquals("bandiera di ${m.name}", 4, flagEmojiForIsoCode(m.iso).length)
            assertTrue("tasso di ${m.name}", m.rate.replace(",", "").toDoubleOrNull() != null)
            assertEquals("codice valuta di ${m.name}", true, m.code.length == 3 || m.code.isEmpty())
        }
    }

    @Test
    fun theAgreementStatesAreNotEuroAreaMembers() {
        assertEquals(4, EURO_AGREEMENTS.size)
        val euroIso = members.map { it.iso }.toSet()
        assertTrue(EURO_AGREEMENTS.none { it.iso in euroIso })
    }

    @Test
    fun theCoinSizesMatchTheDrawnCoinsAndEveryDenominationAppearsOnce() {
        val facts = MATERIAL_FAMILIES.flatMap { it.coins }
        assertEquals(REGULAR_DENOMINATIONS.toSet(), facts.map { it.taglio }.toSet())
        assertEquals(REGULAR_DENOMINATIONS.size, facts.size)
        for (fact in facts) {
            val spec = coinSpecFor(fact.taglio)!!
            assertEquals("diametro di ${fact.taglio}", spec.diameterMm, fact.diameter.toFloat(), 0.001f)
        }
    }

    @Test
    fun theRoadToTheEuroIsInOrderAndShort() {
        val years = ROAD_STEPS.map { it.year.toInt() }
        assertEquals(years.sorted(), years)
        assertTrue(ROAD_STEPS.size <= 8)
        assertTrue(years.last() < 1999)
    }
}
