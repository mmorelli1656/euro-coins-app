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
        val (first, later) = members.partition { it.coins == "1 Jan 2002" }
        assertEquals(12, first.size) // gli 11 del 1999 più la Grecia
        assertEquals(9, later.size)
        assertTrue(later.all { it.coins == it.joined })
        // Il 1999 non porta monete: la tappa del 2002 sta tra la Grecia e la Slovenia e non ha paesi.
        val coinsStep = EURO_ENTRIES.single { it.year == "2002" }
        assertTrue(coinsStep.members.isEmpty())
        assertTrue(EURO_ENTRIES.filter { it.members.isNotEmpty() }.all { it.year.toInt() != 2002 })
    }

    @Test
    fun the1999EntryDoesNotSuggestThatCoinsCirculateYet() {
        // "1999" da solo faceva pensare che le monete girassero già da allora: il sottotesto lo dice.
        assertTrue(EURO_ENTRIES.first { it.year == "1999" }.text.contains("Coins and notes come later"))
    }

    @Test
    fun everyEntryHasASubtextSoTheTimelineLooksTheSame() {
        assertTrue(EURO_ENTRIES.all { it.text.isNotBlank() })
    }

    @Test
    fun theOrdinalSubtextsMatchTheRunningCountOfCountries() {
        // Con le cifre ("12th" e non "twelfth"): si leggono più in fretta (scelta del 2026-10-25).
        val ordinals = listOf("12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th", "21st")
        var count = 0
        for (entry in EURO_ENTRIES) {
            val before = count
            count += entry.members.size
            if (before < 11) continue
            for (n in before + 1..count) {
                if (entry.members.isEmpty()) continue
                assertTrue("${entry.year}: manca '${ordinals[n - 12]}'", entry.text.contains(ordinals[n - 12]))
            }
        }
        assertEquals(21, count)
    }

    @Test
    fun theAgreementStatesShowTheSameInformationAsTheEuroAreaCountries() {
        for (a in EURO_AGREEMENTS) {
            assertTrue("${a.name}: since", a.since.isNotBlank())
            assertTrue("${a.name}: coins", a.coins.isNotBlank())
            assertTrue("${a.name}: replaced", a.replaced.isNotBlank())
            assertTrue("${a.name}: rate", a.rate.startsWith("€1 = "))
        }
        // I tassi sono quelli delle monete che sostituiscono: franco francese e lira italiana.
        assertTrue(EURO_AGREEMENTS.single { it.iso == "MC" }.rate.contains("6.55957 FRF"))
        assertTrue(EURO_AGREEMENTS.single { it.iso == "VA" }.rate.contains("1,936.27 ITL"))
        assertTrue(EURO_AGREEMENTS.single { it.iso == "SM" }.rate.contains("1,936.27 ITL"))
    }

    @Test
    fun theCommonSideMapsAreInTheYearOfTheFirstCountryToUseThem() {
        // La Slovenia, il 1° gennaio 2007, è la prima a usare il lato comune ridisegnato.
        val entry = EURO_ENTRIES.single { it.maps }
        assertEquals("2007", entry.year)
        assertEquals(listOf("SI"), entry.members.map { it.iso })
    }

    @Test
    fun theRoadEntryTextMatchesTheNumberOfSteps() {
        assertEquals(6, ROAD_STEPS.size)
        assertTrue(ROAD_TEXT.startsWith("6 steps"))
        assertEquals("1957–1998", ROAD_YEARS)
        assertEquals("1957", ROAD_STEPS.first().year)
        assertEquals("1998", ROAD_STEPS.last().year)
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
