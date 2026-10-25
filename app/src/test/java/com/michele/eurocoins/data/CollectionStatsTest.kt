package com.michele.eurocoins.data

import java.io.File
import java.time.LocalDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** I conti della scheda Statistics di Insights, sul catalogo vero (commemorative e Regular Issues). */
class CollectionStatsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val coins: List<Coin> = json.decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }
    private val series: List<RegularIssueSeries> = json
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun stats(
        items: List<CollectionItem> = emptyList(),
        regular: List<RegularCollectionItem> = emptyList(),
        catalog: List<Coin> = coins,
    ) = collectionStats(catalog, items, series, regular)

    private fun item(
        coin: Coin,
        quality: CoinQuality = CoinQuality.STANDARD,
        priceCents: Int? = null,
        purchasedOn: Long? = null,
    ) = CollectionItem(
        coinKey = coin.stableKey, quality = quality, priceCents = priceCents, purchasedOn = purchasedOn,
        anno = coin.anno, paese = coin.paese, tema = coin.tema, addedAt = 0L,
    )

    private fun regularItem(s: RegularIssueSeries, taglio: String, anno: Int, priceCents: Int? = null) =
        RegularCollectionItem(
            seriesKey = s.stableKey, taglio = taglio, anno = anno, quality = CoinQuality.STANDARD, paese = s.paese,
            priceCents = priceCents, addedAt = 0L,
        )

    @Test
    fun anEmptyCollectionHasTheCatalogTotalsAndNothingOwned() {
        val s = stats()
        // I totali che la Home mostra già: tutte le commemorative e le righe Regular (328 = 41 serie x 8).
        assertEquals(Progress(0, coins.size), s.commemorative)
        assertEquals(Progress(0, 328), s.regular)
        assertEquals(Progress(0, coins.size + 328), s.total)
        assertEquals(coins.map { it.paese }.toSet().size, s.commemorativeCountries.size)
        assertEquals(25, s.regularCountries.size)
        assertEquals(Progress(0, 41), s.milestones.regularSeries)
        assertEquals(Progress(0, 5), s.milestones.jointIssues)
        assertEquals(FinishCounts(0, 0, 0), s.finishes)
        assertNull(s.spending)
    }

    @Test
    fun theCountryAndYearTotalsAddUpToTheCatalog() {
        val s = stats()
        assertEquals(coins.size, s.commemorativeCountries.sumOf { it.progress.total })
        assertEquals(coins.size, s.years.sumOf { it.progress.total })
        assertEquals(328, s.regularCountries.sumOf { it.progress.total })
        assertEquals(s.years.map { it.anno }.sorted(), s.years.map { it.anno })
    }

    @Test
    fun oneCoinMovesTheTotalTheCountryAndTheYear() {
        val coin = coins.first()
        val s = stats(items = listOf(item(coin), item(coin, CoinQuality.BU)))
        assertEquals(1, s.commemorative.owned) // due finiture della stessa moneta contano una volta
        assertEquals(1, s.commemorativeCountries.single { it.paese == coin.paese }.progress.owned)
        assertEquals(1, s.years.single { it.anno == coin.anno }.progress.owned)
        assertEquals(FinishCounts(standard = 1, bu = 1, proof = 0), s.finishes)
    }

    @Test
    fun aYearIsCompleteOnlyWhenEveryCoinOfThatYearIsOwned() {
        val year = coins.groupBy { it.anno }.minByOrNull { it.value.size }!!
        val almost = year.value.dropLast(1).map { item(it) }
        assertEquals(0, stats(items = almost).milestones.years.owned)
        val all = year.value.map { item(it) }
        val s = stats(items = all)
        assertEquals(1, s.milestones.years.owned)
        assertEquals(coins.map { it.anno }.toSet().size, s.milestones.years.total)
    }

    @Test
    fun aCountryIsCompleteWhenItsWholeCatalogIsOwned() {
        val country = coins.groupBy { it.paese }.minByOrNull { it.value.size }!!
        val s = stats(items = country.value.map { item(it) })
        assertEquals(1, s.milestones.countries.owned)
        assertEquals(country.value.size, s.commemorativeCountries.single { it.paese == country.key }.progress.owned)
    }

    @Test
    fun aJointIssueIsCompleteWhenTheCoinOfEveryCountryIsOwned() {
        val joint = coins.filter { it.emissioneComune }.groupBy { it.anno }
        assertEquals(5, joint.size)
        val year = joint.keys.first()
        val s = stats(items = joint.getValue(year).map { item(it) })
        assertEquals(1, s.milestones.jointIssues.owned)
        assertEquals(Progress(0, 5), stats().milestones.jointIssues)
    }

    @Test
    fun aRegularSeriesIsCompleteWhenAllItsRowsAreOwned() {
        val germany = series.single { it.paese == "Germania" }
        val rows = REGULAR_DENOMINATIONS.map { regularItem(germany, it, 2010) }
        val s = stats(regular = rows)
        assertEquals(1, s.milestones.regularSeries.owned)
        assertEquals(Progress(8, 8), s.regularCountries.single { it.paese == "Germania" }.progress)
        // Mancando un taglio non è completa.
        assertEquals(0, stats(regular = rows.drop(1)).milestones.regularSeries.owned)
    }

    @Test
    fun spendingCountsOnlyWhatHasAPriceAndSaysHowMuchThatIs() {
        val a = coins[0]
        val b = coins[1]
        val c = coins[2]
        val day2024 = LocalDate.of(2024, 5, 3).toEpochDay()
        val day2025 = LocalDate.of(2025, 2, 1).toEpochDay()
        val sp = stats(
            items = listOf(
                item(a, priceCents = 1000, purchasedOn = day2024),
                item(b, priceCents = 2500, purchasedOn = day2025),
                item(c, priceCents = 500), // senza data: conta nel totale, non nell'anno
                item(coins[3]), // senza prezzo
                item(coins[4], priceCents = 0), // prezzo 0 = non indicato
            ),
            regular = listOf(regularItem(series.first(), "1 cent", 2010, priceCents = 300)),
        ).spending
        assertNotNull(sp)
        sp!!
        assertEquals(4300L, sp.totalCents)
        assertEquals(4, sp.itemsWithPrice)
        assertEquals(6, sp.totalItems)
        assertEquals(1075L, sp.averageCents)
        assertEquals(listOf(2024 to 1000L, 2025 to 2500L), sp.byYear)
        assertEquals(2, sp.itemsInYears)
    }

    @Test
    fun hiddenCatalogCoinsAreLeftOutOfProgressButTheirMoneyIsStillCounted() {
        val hidden = coins.filter { it.isMicrostate }
        val visible = coins.filterNot { it.isMicrostate }
        assertTrue(hidden.isNotEmpty())
        val items = hidden.take(2).map { item(it, priceCents = 700) }
        val s = stats(items = items, catalog = visible)
        assertEquals(0, s.commemorative.owned)
        assertEquals(FinishCounts(0, 0, 0), s.finishes)
        assertEquals(1400L, s.spending!!.totalCents)
    }

    @Test
    fun theRankingListsEveryPricedEntryFromTheMostExpensiveWithTheirPhotos() {
        val withPhoto = coins.filter { !it.immaginePlaceholder && it.urlImmagineFonte != null }.take(3)
        val belgium = series.first { it.paese == "Belgio" }
        val twoEuro = belgium.immagini.first { it.taglio == "2 euro" }
        val s = stats(
            items = listOf(
                item(withPhoto[0], CoinQuality.BU, priceCents = 3500, purchasedOn = 20_000),
                // Stessa moneta, altra finitura: è un'altra voce della classifica.
                item(withPhoto[0], CoinQuality.STANDARD, priceCents = 500),
                item(withPhoto[1], priceCents = 1800),
                // Senza prezzo (o a 0): fuori dalla classifica.
                item(withPhoto[2]),
                item(withPhoto[2], CoinQuality.PROOF, priceCents = 0),
            ),
            regular = listOf(regularItem(belgium, "2 euro", 2005, priceCents = 900)),
        )
        val ranking = s.spending!!.ranking
        assertEquals(listOf(3500, 1800, 900, 500), ranking.map { it.priceCents })
        assertEquals(s.spending!!.itemsWithPrice, ranking.size)
        assertEquals("BU", ranking[0].finish)
        assertEquals(withPhoto[0].anno.toString(), ranking[0].what)
        assertEquals(withPhoto[0].urlImmagineFonte, ranking[0].imageUrl)
        assertEquals(20_000L, ranking[0].purchasedOn)
        val regular = ranking[2]
        assertTrue(regular.isRegular)
        assertEquals("2 euro · 2005", regular.what)
        assertEquals("Standard", regular.finish)
        assertEquals(twoEuro.urlImmagineFonte, regular.imageUrl)
    }

    @Test
    fun aHiddenCoinStillRanksButWithoutAPhoto() {
        val hidden = coins.first { it.isMicrostate }
        val visible = coins.filterNot { it.isMicrostate }
        val ranking = stats(items = listOf(item(hidden, priceCents = 700)), catalog = visible).spending!!.ranking
        assertEquals(1, ranking.size)
        assertNull(ranking[0].imageUrl)
        assertEquals(hidden.anno.toString(), ranking[0].what)
    }

    @Test
    fun mintsAndVarietiesShowInTheFinishLabel() {
        val belgium = series.first { it.paese == "Belgio" }
        val ranking = stats(
            regular = listOf(
                regularItem(belgium, "1 euro", 2008, priceCents = 400).copy(mint = "D", variety = VARIETY_EFS),
            ),
        ).spending!!.ranking
        assertEquals("Standard · EFS · D", ranking[0].finish)
    }

    @Test
    fun aRankedCoinOpensItsOwnDetail() {
        val coin = coins.first { !it.isMicrostate }
        val ranking = stats(items = listOf(item(coin, priceCents = 900))).spending!!.ranking
        assertEquals(SpentTarget.Commemorative(coin.id), ranking[0].target)

        // Fuori catalogo (microstato nascosto): niente destinazione, la riga non si tocca.
        val hidden = coins.first { it.isMicrostate }
        val visible = coins.filterNot { it.isMicrostate }
        val hiddenRanking = stats(items = listOf(item(hidden, priceCents = 700)), catalog = visible).spending!!.ranking
        assertNull(hiddenRanking[0].target)
    }

    @Test
    fun aRegularEntryOpensTheSeriesWhoseWindowHoldsItsYear() {
        // Il 5 cent francese è un'unica moneta salvata con la serie 1, ma ogni annata si vede in UNA sola serie:
        // 1999-2021 nella 1, 2022-2023 nella 2, dal 2024 nella 3. Si apre quella giusta, altrimenti la card COLLECTION
        // del dettaglio non mostrerebbe la voce.
        val france = series.filter { it.paese == "Francia" }.sortedBy { it.ordineCronologico }
        assertEquals(3, france.size)
        fun target(anno: Int) = stats(regular = listOf(regularItem(france[0], "5 cent", anno, priceCents = 100)))
            .spending!!.ranking[0].target as SpentTarget.Denomination
        assertEquals(france[0].ordineCronologico, target(2010).ordine)
        assertEquals(france[1].ordineCronologico, target(2023).ordine)
        assertEquals(france[2].ordineCronologico, target(2025).ordine)
        assertEquals("5 cent", target(2023).taglio)
        assertEquals(france[0].paese, target(2023).paese)
        // Senza anno la voce appartiene già alla serie guardata (è salvata con la sua chiave).
        assertEquals(france[0].ordineCronologico, target(NO_YEAR).ordine)
    }
}
