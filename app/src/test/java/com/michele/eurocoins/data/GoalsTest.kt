package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gli obiettivi personali di Milestones, sul catalogo vero: scelte da elenchi chiusi, mai testo libero. */
class GoalsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val coins: List<Coin> = json.decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }
    private val series: List<RegularIssueSeries> = json
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun item(coin: Coin) = CollectionItem(
        coinKey = coin.stableKey, quality = CoinQuality.STANDARD, anno = coin.anno, paese = coin.paese, tema = coin.tema, addedAt = 0L,
    )

    private fun stats(owned: List<Coin> = emptyList()) = collectionStats(coins, owned.map(::item), series, emptyList())

    @Test
    fun countryChoicesAreOnlyWhatCanStillBeCompletedClosestFirst() {
        val country = coins.groupBy { it.paese }.minByOrNull { it.value.size }!!
        val s = stats(country.value.dropLast(1))
        val options = goalOptions(GoalKind.COUNTRY, s, emptyList())
        // Il più vicino in cima (mancava una sola moneta).
        assertEquals(country.key, options.first().key)
        assertEquals(country.value.size - 1, options.first().progress.owned)
        // Un paese completo non si offre.
        val done = goalOptions(GoalKind.COUNTRY, stats(country.value), emptyList())
        assertTrue(done.none { it.key == country.key })
        // E l'ordine è per avanzamento decrescente.
        val fractions = options.map { it.progress.fraction }
        assertEquals(fractions.sortedDescending(), fractions)
    }

    @Test
    fun whatIsAlreadyAddedIsShownButTaken() {
        val s = stats()
        val first = goalOptions(GoalKind.COUNTRY, s, emptyList()).first()
        val options = goalOptions(GoalKind.COUNTRY, s, listOf(Goal(GoalKind.COUNTRY, first.key)))
        assertTrue(options.first { it.key == first.key }.taken)
        assertEquals(1, options.count { it.taken })
        // Il suggerimento della card vuota salta quello che c'è già.
        assertFalse(suggestedGoal(s, listOf(Goal(GoalKind.COUNTRY, first.key)))!!.key == first.key)
    }

    @Test
    fun coinTargetsStartAboveWhatYouOwnAndStopAtTheCatalog() {
        val owned = coins.take(93)
        val s = stats(owned)
        val options = goalOptions(GoalKind.COINS, s, emptyList())
        assertEquals(listOf(100, 125, 150, 175, 200, 225), options.map { it.key.toInt() })
        assertTrue(options.all { it.key.toInt() > 93 && it.key.toInt() <= coins.size })
        // Con quasi tutto il catalogo restano solo i valori che stanno dentro.
        val nearlyAll = goalOptions(GoalKind.COINS, stats(coins.take(coins.size - 10)), emptyList())
        assertTrue(nearlyAll.all { it.key.toInt() <= coins.size })
    }

    @Test
    fun aNumberOfCoinsGoalCountsOnlyCommemoratives() {
        val s = stats(coins.take(60))
        val row = goalRows(listOf(Goal(GoalKind.COINS, "100")), s).single()
        assertEquals(Progress(60, 100), row.progress)
        assertEquals("Reach 100 coins", row.label)
        assertEquals(BadgeTarget.AllMissing, row.go)
        assertFalse(row.done)
    }

    @Test
    fun rowsShowTodaysProgressAndWhereTheTapGoes() {
        val country = coins.groupBy { it.paese }.minByOrNull { it.value.size }!!
        val s = stats(country.value.take(2))
        val row = goalRows(listOf(Goal(GoalKind.COUNTRY, country.key)), s).single()
        assertEquals(Progress(2, country.value.size), row.progress)
        assertEquals(BadgeTarget.Milestone(MilestoneTarget.CommemorativeCountry(country.key)), row.go)
        assertTrue(row.label.startsWith("Complete "))
    }

    @Test
    fun aSeriesGoalUsesTheStableKeyAndOpensTheCountryOnThatSeries() {
        val s = stats()
        val one = s.seriesStats.first()
        val row = goalRows(listOf(Goal(GoalKind.SERIES, one.key)), s).single()
        assertEquals(BadgeTarget.Milestone(MilestoneTarget.RegularCountry(one.paese, one.number)), row.go)
        assertEquals(0, row.progress.owned)
        assertTrue(row.progress.total in 1..8)
    }

    @Test
    fun aGoalWhoseTargetIsGoneIsNotDrawnButStaysSaved() {
        val s = stats()
        val ghost = Goal(GoalKind.COUNTRY, "Atlantide")
        assertTrue(goalRows(listOf(ghost), s).isEmpty())
        // Resta salvato (e conta per il massimo): riappare se il bersaglio torna.
        assertEquals(listOf(ghost, Goal(GoalKind.YEAR, "2005")), listOf(ghost).added(Goal(GoalKind.YEAR, "2005")))
    }

    @Test
    fun aReachedGoalStaysReachedEvenIfTheCollectionShrinks() {
        val country = coins.groupBy { it.paese }.minByOrNull { it.value.size }!!
        val goals = listOf(Goal(GoalKind.COUNTRY, country.key))
        val complete = stats(country.value)
        assertEquals(setOf(goals.single().id), newlyDoneGoals(goals, complete))
        val marked = goals.markedDone(newlyDoneGoals(goals, complete), day = 20_000L)
        assertEquals(20_000L, marked.single().doneAt)
        // Si segna una volta sola: la data non cambia.
        assertEquals(20_000L, marked.markedDone(setOf(marked.single().id), day = 30_000L).single().doneAt)
        // Tolte monete: resta raggiunto, senza freccia.
        val shrunk = stats(country.value.take(1))
        val row = goalRows(marked, shrunk).single()
        assertTrue(row.done)
        assertNull(row.go)
        assertTrue(newlyDoneGoals(marked, shrunk).isEmpty())
    }

    @Test
    fun theListHasNoDuplicatesAndAtMostFiveGoals() {
        var goals = emptyList<Goal>()
        goals = goals.added(Goal(GoalKind.YEAR, "2005")).added(Goal(GoalKind.YEAR, "2005"))
        assertEquals(1, goals.size)
        listOf("2006", "2007", "2008", "2009", "2010").forEach { goals = goals.added(Goal(GoalKind.YEAR, it)) }
        assertEquals(MAX_GOALS, goals.size)
        assertEquals(goals, goals.added(Goal(GoalKind.COUNTRY, "Italia")))
        assertEquals(MAX_GOALS - 1, goals.removed("YEAR:2005").size)
    }

    @Test
    fun savingAndLoadingKeepsGoalsAndToleratesGarbage() {
        val goals = listOf(Goal(GoalKind.COUNTRY, "Italia"), Goal(GoalKind.COINS, "150", doneAt = 20_000L))
        assertEquals(goals, decodeGoals(encodeGoals(goals)))
        assertEquals(emptyList<Goal>(), decodeGoals(null))
        assertEquals(emptyList<Goal>(), decodeGoals(""))
        assertEquals(emptyList<Goal>(), decodeGoals("{non è json"))
        // Doppioni tolti e massimo rispettato anche su un file scritto a mano.
        val many = (1..9).map { Goal(GoalKind.COINS, (it * 25).toString()) } + Goal(GoalKind.COINS, "25")
        assertEquals(MAX_GOALS, decodeGoals(encodeGoals(many)).size)
        assertNotNull(decodeGoals(encodeGoals(many)).firstOrNull())
    }
}
