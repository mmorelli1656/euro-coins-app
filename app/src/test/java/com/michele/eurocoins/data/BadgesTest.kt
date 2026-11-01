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

/** Le medaglie di Insights → Statistics, sul catalogo vero: derivate dalla collezione, mai salvate. */
class BadgesTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val coins: List<Coin> = json.decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }
    private val series: List<RegularIssueSeries> = json
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun badges(items: List<CollectionItem> = emptyList(), regular: List<RegularCollectionItem> = emptyList()) =
        collectionStats(coins, items, series, regular).badges.associateBy { it.id }

    private fun item(coin: Coin, quality: CoinQuality = CoinQuality.STANDARD, priceCents: Int? = null, mint: String = NO_MINT) =
        CollectionItem(
            coinKey = coin.stableKey, quality = quality, priceCents = priceCents, mint = mint,
            anno = coin.anno, paese = coin.paese, tema = coin.tema, addedAt = 0L,
        )

    private fun regularItem(
        s: RegularIssueSeries, taglio: String, anno: Int, priceCents: Int? = null, variety: String = "", mint: String = NO_MINT,
    ) = RegularCollectionItem(
        seriesKey = s.stableKey, taglio = taglio, anno = anno, quality = CoinQuality.STANDARD, paese = s.paese,
        priceCents = priceCents, variety = variety, mint = mint, addedAt = 0L,
    )

    @Test
    fun anEmptyCollectionShowsAllSixteenBadgesLocked() {
        val b = badges()
        // Sempre tutte e sedici, anche senza prezzi né lettere di zecca (nascoste, ne mancava una e si perdeva la scoperta).
        assertEquals(BadgeId.entries.toSet(), b.keys)
        assertEquals(16, b.size)
        assertTrue(b.values.none { it.earned })
    }

    @Test
    fun theCoinThresholdsCountOnlyCommemorativeCoins() {
        val items = coins.take(60).map { item(it) }
        val b = badges(items)
        assertTrue(b.getValue(BadgeId.FIRST_COIN).earned)
        assertFalse(b.getValue(BadgeId.COINS_100).earned)
        assertEquals(60, b.getValue(BadgeId.COINS_100).progress)
        assertEquals(100, b.getValue(BadgeId.COINS_100).target)
        assertEquals(250, b.getValue(BadgeId.COINS_250).target)
        assertEquals(500, b.getValue(BadgeId.COINS_500).target)
        // Le Regular non fanno avanzare le soglie: sono un altro catalogo.
        val germany = series.first { it.paese == "Germania" }
        val rows = denominationsOf(listOf(germany), germany).map { regularItem(germany, it.image.taglio, 2005) }
        assertEquals(60, badges(items, rows).getValue(BadgeId.COINS_100).progress)
        // ...ma una sola Regular basta per la prima moneta.
        assertTrue(badges(regular = rows).getValue(BadgeId.FIRST_COIN).earned)
        assertEquals(0, badges(regular = rows).getValue(BadgeId.COINS_100).progress)
        // Cento commemorative conquistano la prima soglia.
        assertTrue(badges(coins.take(100).map { item(it) }).getValue(BadgeId.COINS_100).earned)
    }

    @Test
    fun theFinishBadgesNeedTheirFinish() {
        val coin = coins.first()
        assertFalse(badges(listOf(item(coin))).getValue(BadgeId.FIRST_BU).earned)
        val b = badges(listOf(item(coin, CoinQuality.BU), item(coin, CoinQuality.PROOF)))
        assertTrue(b.getValue(BadgeId.FIRST_BU).earned)
        assertTrue(b.getValue(BadgeId.FIRST_PROOF).earned)
    }

    @Test
    fun theClosestCountryDrivesTheFirstCountryBadgeBeforeAnyIsComplete() {
        val country = coins.groupBy { it.paese }.minByOrNull { it.value.size }!!
        val b = badges(country.value.dropLast(1).map { item(it) }).getValue(BadgeId.FIRST_COUNTRY)
        assertFalse(b.earned)
        assertEquals(country.value.size - 1, b.progress)
        assertEquals(country.value.size, b.target)
        assertEquals(BadgeTarget.Milestone(MilestoneTarget.CommemorativeCountry(country.key)), b.go)
        val done = badges(country.value.map { item(it) })
        assertTrue(done.getValue(BadgeId.FIRST_COUNTRY).earned)
        assertNull(done.getValue(BadgeId.FIRST_COUNTRY).go)
        assertEquals(1, done.getValue(BadgeId.FIVE_COUNTRIES).progress)
    }

    @Test
    fun aJointIssueBadgeNeedsTheCoinOfEveryCountry() {
        val joint = coins.filter { it.emissioneComune }.groupBy { it.anno }
        val year = joint.keys.first()
        assertFalse(badges(joint.getValue(year).dropLast(1).map { item(it) }).getValue(BadgeId.JOINT_ISSUE).earned)
        assertTrue(badges(joint.getValue(year).map { item(it) }).getValue(BadgeId.JOINT_ISSUE).earned)
    }

    @Test
    fun aCompleteRegularSeriesEarnsTheFirstSeriesBadge() {
        val germany = series.first { it.paese == "Germania" }
        val rows = denominationsOf(listOf(germany), germany)
        val almost = badges(regular = rows.drop(1).map { regularItem(germany, it.image.taglio, 2005) }).getValue(BadgeId.FIRST_SERIES)
        assertFalse(almost.earned)
        val all = badges(regular = rows.map { regularItem(germany, it.image.taglio, 2005) })
        assertTrue(all.getValue(BadgeId.FIRST_SERIES).earned)
        assertTrue(all.getValue(BadgeId.FIRST_SERIES).note!!.contains("Germany"))
    }

    @Test
    fun bigSpenderIsAlwaysThereAndEarnedAtOneHundredEuro() {
        val coin = coins.first()
        val none = badges().getValue(BadgeId.BIG_SPENDER)
        assertFalse(none.earned)
        assertEquals(0, none.progress)
        assertTrue(none.note!!.contains("prices"))
        val few = badges(listOf(item(coin, priceCents = 6_400))).getValue(BadgeId.BIG_SPENDER)
        assertFalse(few.earned)
        assertEquals(6_400, few.progress)
        assertEquals(10_000, few.target)
        val enough = badges(listOf(item(coin, priceCents = 6_400), item(coins[1], priceCents = 3_600))).getValue(BadgeId.BIG_SPENDER)
        assertTrue(enough.earned)
    }

    @Test
    fun pocketChangeIsAPricePaidAtOrUnderFaceValue() {
        val coin = coins.first()
        // Una moneta con prezzo sopra il facciale: la medaglia compare ma non è conquistata.
        assertFalse(badges(listOf(item(coin, priceCents = 350))).getValue(BadgeId.POCKET_CHANGE).earned)
        assertTrue(badges(listOf(item(coin, priceCents = 200))).getValue(BadgeId.POCKET_CHANGE).earned)
        // Una Regular: il facciale è quello del taglio (50 cent).
        val germany = series.first { it.paese == "Germania" }
        assertTrue(badges(regular = listOf(regularItem(germany, "50 cent", 2005, priceCents = 50))).getValue(BadgeId.POCKET_CHANGE).earned)
        assertFalse(badges(regular = listOf(regularItem(germany, "50 cent", 2005, priceCents = 51))).getValue(BadgeId.POCKET_CHANGE).earned)
    }

    @Test
    fun faceValueOfADenomination() {
        assertEquals(200, faceValueCents("2 euro"))
        assertEquals(100, faceValueCents("1 euro"))
        assertEquals(50, faceValueCents("50 cent"))
        assertEquals(1, faceValueCents("1 cent"))
        assertEquals(0, faceValueCents("???"))
    }

    @Test
    fun tripleThreatNeedsTheSameCoinInAllThreeFinishes() {
        val coin = coins.first()
        val two = badges(listOf(item(coin), item(coin, CoinQuality.BU))).getValue(BadgeId.TRIPLE_THREAT)
        assertFalse(two.earned)
        assertEquals(2, two.progress)
        assertEquals(BadgeTarget.Spent(SpentTarget.Commemorative(coin.id)), two.go)
        // Tre monete diverse, una finitura ciascuna, non bastano.
        val spread = badges(listOf(item(coins[0]), item(coins[1], CoinQuality.BU), item(coins[2], CoinQuality.PROOF)))
        assertFalse(spread.getValue(BadgeId.TRIPLE_THREAT).earned)
        val three = badges(CoinQuality.entries.map { item(coin, it) })
        assertTrue(three.getValue(BadgeId.TRIPLE_THREAT).earned)
    }

    @Test
    fun letterSpotterIsTheGreekEfsVariety() {
        val greece = series.first { it.paese == "Grecia" && it.ordineCronologico == 1 }
        val locked = badges().getValue(BadgeId.LETTER_SPOTTER)
        assertFalse(locked.earned)
        assertEquals(BadgeTarget.Milestone(MilestoneTarget.RegularCountry("Grecia", 1)), locked.go)
        // Una Grecia 2002 senza la varietà non conta.
        assertFalse(badges(regular = listOf(regularItem(greece, "2 euro", 2002))).getValue(BadgeId.LETTER_SPOTTER).earned)
        assertTrue(badges(regular = listOf(regularItem(greece, "2 euro", 2002, variety = VARIETY_EFS))).getValue(BadgeId.LETTER_SPOTTER).earned)
    }

    @Test
    fun fiveMintsIsAlwaysThereAndNeedsAllFiveOnOneCoin() {
        val germany = coins.first { it.paese == "Germania" }
        val none = badges(listOf(item(germany))).getValue(BadgeId.FIVE_MINTS)
        assertEquals(0, none.progress)
        assertEquals(BadgeTarget.Milestone(MilestoneTarget.CommemorativeCountry("Germania")), none.go)
        val three = badges(listOf("A", "D", "F").map { item(germany, mint = it) }).getValue(BadgeId.FIVE_MINTS)
        assertFalse(three.earned)
        assertEquals(3, three.progress)
        assertNotNull(three.go)
        // Le lettere su monete diverse non si sommano.
        val other = coins.first { it.paese == "Germania" && it.stableKey != germany.stableKey }
        val spread = badges(listOf(item(germany, mint = "A"), item(germany, mint = "D"), item(other, mint = "F"), item(other, mint = "G")))
        assertEquals(2, spread.getValue(BadgeId.FIVE_MINTS).progress)
        assertTrue(badges(listOf("A", "D", "F", "G", "J").map { item(germany, mint = it) }).getValue(BadgeId.FIVE_MINTS).earned)
    }

    @Test
    fun theFunBadgesCloseTheList() {
        val order = badges(listOf(item(coins.first(), priceCents = 500))).keys.toList()
        val firstFun = order.indexOfFirst { it.isFun }
        assertTrue(order.drop(firstFun).all { it.isFun })
    }
}
