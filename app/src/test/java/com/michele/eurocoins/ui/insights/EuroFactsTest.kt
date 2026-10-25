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
        for (m in members) {
            // Due lettere ISO → due "regional indicator", 2 caratteri UTF-16 ciascuno.
            assertEquals("bandiera di ${m.name}", 4, flagEmojiForIsoCode(m.iso).length)
            assertTrue("tasso di ${m.name}", m.rate.replace(",", "").toDoubleOrNull() != null)
            assertEquals("codice valuta di ${m.name}", 3, m.code.length)
        }
        for (a in EURO_AGREEMENTS) assertEquals("bandiera di ${a.name}", 4, flagEmojiForIsoCode(a.iso).length)
    }

    @Test
    fun theTwelveCountriesGetTheirCoinsInTwoThousandAndTwoAndTheLaterOnesTheSameDay() {
        val (first, later) = members.partition { it.onlyEuro == null }
        assertEquals(12, first.size) // gli 11 del 1999 più la Grecia
        assertTrue(first.all { it.coins == "1 Jan 2002" })
        assertTrue(later.all { it.coins == it.joined })
        // Il 1999 non porta monete: la tappa del 2002 sta tra la Grecia e la Slovenia e non ha paesi.
        val coinsStep = EURO_ENTRIES.single { it.year == "2002" }
        assertTrue(coinsStep.members.isEmpty())
        assertTrue(EURO_ENTRIES.filter { it.members.isNotEmpty() }.all { it.year.toInt() != 2002 })
    }

    @Test
    fun theEntriesThatNeedATitleHaveOne() {
        // Senza titolo "1999" faceva pensare che le monete girassero già da allora.
        assertEquals("The euro starts", EURO_ENTRIES.first { it.year == "1999" }.title)
        assertTrue(EURO_ENTRIES.first { it.year == "1999" }.note!!.contains("Coins and notes come later"))
    }

    @Test
    fun theAgreementStatesAreNotEuroAreaMembers() {
        assertEquals(4, EURO_AGREEMENTS.size)
        val euroIso = members.map { it.iso }.toSet()
        assertTrue(EURO_AGREEMENTS.none { it.iso in euroIso })
    }

    @Test
    fun theCoinSizesMatchTheDrawnCoinsAndEveryDenominationAppearsOnce() {
        val facts = COIN_MATERIALS
        assertEquals(REGULAR_DENOMINATIONS.toSet(), facts.map { it.taglio }.toSet())
        assertEquals(REGULAR_DENOMINATIONS.size, facts.size)
        for (fact in facts) {
            val spec = coinSpecFor(fact.taglio)!!
            assertEquals("diametro di ${fact.taglio}", spec.diameterMm, fact.diameter.toFloat(), 0.001f)
            assertTrue("peso di ${fact.taglio}", fact.weight.toDoubleOrNull() != null)
            assertTrue("spessore di ${fact.taglio}", fact.thickness.toDoubleOrNull() != null)
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
