package com.michele.eurocoins.data

import java.util.Locale

/**
 * Le medaglie di Insights → Statistics (2026-11-01, mockup approvato): dodici "badge" con un progresso e quattro "just for
 * fun" che premiano un gesto. Si DERIVANO dalla collezione ogni volta, senza salvare niente: non vanno nel backup, non si
 * perdono ripristinando, non servono migrazioni.
 */
enum class BadgeId(val isFun: Boolean = false) {
    FIRST_COIN, COINS_100, COINS_250, COINS_500,
    FIRST_BU, FIRST_PROOF,
    JOINT_ISSUE, FIRST_COUNTRY, FIVE_COUNTRIES, FIRST_SERIES, TEN_SERIES, FIVE_MINTS,
    BIG_SPENDER(true), TRIPLE_THREAT(true), POCKET_CHANGE(true), LETTER_HUNTER(true),
}

/** Dove porta il tasto di una medaglia non ancora conquistata: le stesse destinazioni di Milestones e di "Most expensive". */
sealed interface BadgeTarget {
    data class Milestone(val target: MilestoneTarget) : BadgeTarget
    data class Spent(val target: SpentTarget) : BadgeTarget

    /** La lista di tutte le commemorative, già su "Missing". */
    data object AllMissing : BadgeTarget
}

/**
 * Una medaglia. [progress] su [target] è quanto manca alla barra (per i soldi sono centesimi); [note] è la riga dinamica sotto
 * la descrizione ("You own 288 coins.", "Italy is the closest: 3 to go."); [go]/[goLabel] il tasto, `null` dove non c'è
 * niente da aprire (BU, Proof, i due soldi).
 */
data class Badge(
    val id: BadgeId,
    val earned: Boolean,
    val progress: Int,
    val target: Int,
    val note: String? = null,
    val go: BadgeTarget? = null,
    val goLabel: String? = null,
)

private const val BIG_SPENDER_CENTS = 10_000

/** Il taglio di una riga Regular in centesimi ("2 euro" → 200, "50 cent" → 50); 0 se non riconosciuto. */
internal fun faceValueCents(taglio: String): Int {
    val match = Regex("""(\d+)\s*(euro|cent)""", RegexOption.IGNORE_CASE).find(taglio) ?: return 0
    val n = match.groupValues[1].toInt()
    return if (match.groupValues[2].equals("euro", ignoreCase = true)) n * 100 else n
}

/** Le commemorative sono tutte da 2 euro. */
private const val COMMEMORATIVE_FACE_CENTS = 200

/** Le serie Regular per le medaglie: i nomi di quelle complete e il progresso (righe con qualcosa su righe) della più vicina. */
internal class BadgeSeriesInfo(val completed: List<String>, val closest: Progress?)

private fun nameList(names: List<String>, shown: Int = 3): String {
    val head = names.take(shown).joinToString(", ")
    return if (names.size > shown) "$head +${names.size - shown}" else head
}

/**
 * Le sedici medaglie, nell'ordine in cui si mostrano (prima le dodici, poi le quattro "just for fun"), SEMPRE tutte: anche quelle
 * che dipendono da dati facoltativi (prezzi, lettere di zecca) restano visibili, con una nota che dice come arrivarci. Prima
 * comparivano solo dopo il primo uso (2026-11-01), ma il proprietario ne contava 15 invece di 16 e nasconderle toglieva anche
 * la scoperta della funzione.
 */
internal fun badgesOf(
    stats: CollectionStats,
    coins: List<Coin>,
    items: List<CollectionItem>,
    regularItems: List<RegularCollectionItem>,
    series: List<RegularIssueSeries>,
    seriesInfo: BadgeSeriesInfo,
): List<Badge> {
    val owned = stats.total.owned
    // Le soglie contano solo le commemorative (100, 250, 500 su 613): le Regular hanno il loro catalogo e non entrano in queste.
    val commemorativeOwned = stats.commemorative.owned
    val m = stats.milestones
    val out = mutableListOf<Badge>()

    fun count(id: BadgeId, have: Int, goal: Int, what: String) {
        val earned = have >= goal
        out += Badge(
            id, earned, have.coerceAtMost(goal), goal,
            note = if (earned) "You own $have $what." else "${goal - have} to go.",
            go = if (earned) null else BadgeTarget.AllMissing,
            goLabel = if (earned) null else "Browse the coins you're missing",
        )
    }
    count(BadgeId.FIRST_COIN, owned, 1, "coins")
    count(BadgeId.COINS_100, commemorativeOwned, 100, "commemorative coins")
    count(BadgeId.COINS_250, commemorativeOwned, 250, "commemorative coins")
    count(BadgeId.COINS_500, commemorativeOwned, 500, "commemorative coins")

    out += Badge(BadgeId.FIRST_BU, stats.finishes.bu >= 1, stats.finishes.bu.coerceAtMost(1), 1, note = finishNote(stats.finishes.bu, "BU"))
    out += Badge(BadgeId.FIRST_PROOF, stats.finishes.proof >= 1, stats.finishes.proof.coerceAtMost(1), 1, note = finishNote(stats.finishes.proof, "Proof"))

    // "Completa il primo X": la barra segue il candidato più vicino (37 / 40), non un vuoto 0 / 1.
    fun firstOf(
        id: BadgeId,
        completed: List<String>,
        closest: Progress?,
        next: MilestoneNext?,
        verb: String,
    ) {
        val earned = completed.isNotEmpty()
        out += Badge(
            id, earned,
            progress = if (earned) 1 else closest?.owned ?: 0,
            target = if (earned) 1 else closest?.total?.takeIf { it > 0 } ?: 1,
            note = if (earned) "Completed: ${nameList(completed)}." else next?.let { "${it.label} is the closest: ${it.remaining} to go." },
            go = if (earned) null else next?.let { BadgeTarget.Milestone(it.target) },
            goLabel = if (earned) null else next?.let { "$verb ${it.label}" },
        )
    }

    val completedCountries = stats.commemorativeCountries.filter { it.progress.total > 0 && it.progress.owned == it.progress.total }.map { it.name }
    val closestCountry = stats.commemorativeCountries.filter { it.progress.owned < it.progress.total }
        .minWithOrNull(compareBy({ it.progress.total - it.progress.owned }, { it.name }))?.progress
    val completedJoint = stats.yearRows.filter { it.joint && it.progress.total > 0 && it.progress.owned == it.progress.total }.map { it.anno.toString() }
    val closestJoint = stats.yearRows.filter { it.joint && it.progress.owned < it.progress.total }
        .minWithOrNull(compareBy({ it.progress.total - it.progress.owned }, { -it.anno }))?.progress

    firstOf(BadgeId.JOINT_ISSUE, completedJoint, closestJoint, m.nextJoint, "Show what's missing for")
    firstOf(BadgeId.FIRST_COUNTRY, completedCountries, closestCountry, m.nextCountry, "Show what's missing for")

    run {
        val earned = completedCountries.size >= 5
        out += Badge(
            BadgeId.FIVE_COUNTRIES, earned, completedCountries.size.coerceAtMost(5), 5,
            note = if (earned) "Completed: ${nameList(completedCountries)}." else "You have completed ${completedCountries.size} of 5.",
            go = if (earned) null else m.nextCountry?.let { BadgeTarget.Milestone(it.target) },
            goLabel = if (earned) null else m.nextCountry?.let { "Show what's missing for ${it.label}" },
        )
    }

    firstOf(BadgeId.FIRST_SERIES, seriesInfo.completed, seriesInfo.closest, m.nextSeries, "Show what's missing for")

    run {
        val earned = seriesInfo.completed.size >= 10
        out += Badge(
            BadgeId.TEN_SERIES, earned, seriesInfo.completed.size.coerceAtMost(10), 10,
            note = if (earned) "${seriesInfo.completed.size} series completed." else "You have completed ${seriesInfo.completed.size} of 10.",
            go = if (earned) null else m.nextSeries?.let { BadgeTarget.Milestone(it.target) },
            goLabel = if (earned) null else m.nextSeries?.let { "Show what's missing for ${it.label}" },
        )
    }

    out += fiveMints(coins, items, regularItems, series)

    // Just for fun.
    val spent = stats.spending?.totalCents ?: 0L
    val bigSpender = spent >= BIG_SPENDER_CENTS
    out += Badge(
        BadgeId.BIG_SPENDER, bigSpender, spent.coerceAtMost(BIG_SPENDER_CENTS.toLong()).toInt(), BIG_SPENDER_CENTS,
        note = when {
            bigSpender -> "You have spent ${euro(spent)}. Worth every cent."
            stats.spending == null -> "Add prices to your coins to get started."
            else -> "${euro(spent)} of ${euro(BIG_SPENDER_CENTS.toLong())} so far."
        },
    )
    out += tripleThreat(coins, items)
    val lucky = items.count { (it.priceCents ?: 0) in 1..COMMEMORATIVE_FACE_CENTS } +
            regularItems.count { r -> (r.priceCents ?: 0).let { it > 0 && it <= faceValueCents(r.taglio) } }
    out += Badge(
        BadgeId.POCKET_CHANGE, lucky > 0, if (lucky > 0) 1 else 0, 1,
        note = if (lucky > 0) "$lucky ${if (lucky == 1) "coin" else "coins"} at or under face value." else "Add a price of €2.00 or less to a coin.",
    )
    // La serie EFS intera: gli otto tagli della Grecia 2002 con la lettera nella stella (E, F, S), non una moneta sola.
    val efsCoins = greekEfsLetterGroups().sumOf { it.taglios.size }
    val efsHave = regularItems.filter { it.variety == VARIETY_EFS && it.paese == "Grecia" }.map { it.taglio }.toSet()
    val efsMissing = greekEfsLetterGroups().flatMap { it.taglios }.filter { it !in efsHave }
    val efsOwned = efsCoins - efsMissing.size
    val hunted = efsOwned >= efsCoins
    out += Badge(
        BadgeId.LETTER_HUNTER, hunted, efsOwned, efsCoins,
        note = if (hunted) "All $efsCoins Greek 2002 coins with a letter in the star." else "Still to find: ${efsMissing.joinToString(", ")}.",
        go = if (hunted) null else BadgeTarget.Milestone(MilestoneTarget.RegularCountry("Grecia", 1)),
        goLabel = if (hunted) null else "Show the Greek coins",
    )
    return out
}

private fun finishNote(count: Int, label: String) =
    if (count >= 1) "You own $count $label ${if (count == 1) "coin" else "coins"}." else null

/** La stessa moneta in tutte e tre le finiture: la barra segue la moneta che ne ha di più. */
private fun tripleThreat(coins: List<Coin>, items: List<CollectionItem>): Badge {
    val coinByKey = coins.associateBy { it.stableKey }
    val best = items.filter { it.coinKey in coinByKey }.groupBy { it.coinKey }
        .mapValues { (_, list) -> list.map { it.quality }.toSet().size }
        .entries.maxWithOrNull(compareBy<Map.Entry<String, Int>>({ it.value }, { coinByKey.getValue(it.key).anno }))
    val count = best?.value ?: 0
    val earned = count >= 3
    val coin = best?.let { coinByKey[it.key] }
    return Badge(
        BadgeId.TRIPLE_THREAT, earned, count.coerceAtMost(3), 3,
        note = when {
            earned -> "${coin?.displayCountry()} ${coin?.anno} in all three finishes."
            coin != null -> "${coin.displayCountry()} ${coin.anno} has $count of 3 finishes."
            else -> null
        },
        go = if (earned || coin == null) null else BadgeTarget.Spent(SpentTarget.Commemorative(coin.id)),
        goLabel = if (earned || coin == null) null else "Show ${coin.displayCountry()} ${coin.anno}",
    )
}

/** Una moneta con le cinque zecche tedesche: commemorativa (per chiave) o riga Regular (per taglio, annata e varietà). */
private fun fiveMints(
    coins: List<Coin>,
    items: List<CollectionItem>,
    regularItems: List<RegularCollectionItem>,
    series: List<RegularIssueSeries>,
): Badge {
    val coinByKey = coins.associateBy { it.stableKey }
    val seriesByKey = series.associateBy { it.stableKey }
    val commemorative = items.filter { it.mint != NO_MINT }.groupBy { it.coinKey }
        .map { (key, list) -> Triple(list.map { it.mint }.toSet().size, key, null as RegularCollectionItem?) }
    val regular = regularItems.filter { it.mint != NO_MINT }.groupBy { listOf(it.seriesKey, it.taglio, it.anno, it.variety) }
        .map { (_, list) -> Triple(list.map { it.mint }.toSet().size, list.first().seriesKey, list.first()) }
    val best = (commemorative + regular).maxByOrNull { it.first }
    val count = best?.first ?: 0
    val earned = count >= 5
    val target: SpentTarget? = best?.let { (_, key, regularItem) ->
        if (regularItem == null) coinByKey[key]?.let { SpentTarget.Commemorative(it.id) }
        else seriesByKey[key]?.let { viewedDenomination(series, it, regularItem) }
    }
    return Badge(
        BadgeId.FIVE_MINTS, earned, count.coerceAtMost(5), 5,
        note = when {
            earned -> "One coin from A, D, F, G and J."
            count == 0 -> "Record the mint of your German coins to get started."
            else -> "Best so far: $count of 5 mints."
        },
        go = when {
            earned -> null
            target != null -> BadgeTarget.Spent(target)
            else -> BadgeTarget.Milestone(MilestoneTarget.CommemorativeCountry("Germania"))
        },
        goLabel = when {
            earned -> null
            target != null -> "Show that coin"
            else -> "Show the German coins"
        },
    )
}

/** 10000 → "€100.00" (la card Spending usa lo stesso formato, qui senza dipendere dalla UI). */
private fun euro(cents: Long): String = "€" + String.format(Locale.US, "%.2f", cents / 100.0)
