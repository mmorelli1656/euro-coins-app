package com.michele.eurocoins.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Gli obiettivi personali di Statistics → Milestones (2026-11-01, mockup approvato dopo tre giri). Prima erano quattro traguardi
 * fissi ("0 / 24 paesi") a zero per quasi tutti e senza un dopo; ora li sceglie l'utente.
 *
 * **L'utente non scrive mai niente**: un obiettivo si crea scegliendo da elenchi che l'app genera dal catalogo e dalla collezione
 * ([goalOptions]), quindi non può essere una sciocchezza né irraggiungibile (solo ciò che si può ancora completare; i numeri di
 * monete partono sopra quelli posseduti e si fermano al catalogo). Niente soldi e niente scadenze, di proposito: spingerebbero a
 * spendere o metterebbero fretta. Al massimo [MAX_GOALS] attivi. Un obiettivo raggiunto resta raggiunto per sempre
 * ([Goal.doneAt]) anche se il catalogo cresce o si tolgono monete: è un traguardo, non uno stato.
 */
enum class GoalKind { COUNTRY, YEAR, SERIES, COINS }

/**
 * [key] è la chiave NATURALE, mai un id che cambia a ogni reseed: il `paese` stabile, l'anno, `RegularIssueSeries.stableKey` o il
 * numero di monete. [doneAt] = giorno (`LocalDate.toEpochDay()`) in cui è stato raggiunto, `null` finché è aperto.
 */
@Serializable
data class Goal(val kind: GoalKind, val key: String, val doneAt: Long? = null) {
    val id: String get() = "${kind.name}:$key"
}

const val MAX_GOALS = 5

/** Una riga della card: l'obiettivo con il suo avanzamento di oggi. [go] = dove porta il tocco (`null` se è raggiunto). */
data class GoalRow(val goal: Goal, val label: String, val progress: Progress, val done: Boolean, val go: BadgeTarget?)

/** Una scelta del pannello "New milestone": [taken] = c'è già (si mostra spenta). */
data class GoalOption(
    val key: String,
    val label: String,
    val progress: Progress,
    val taken: Boolean,
    /** Il paese stabile, per la bandiera davanti al nome (paesi e serie); `null` per anni e numeri di monete. */
    val paese: String? = null,
)

/** Aggiunge [goal] se non c'è già e se non si è al massimo; altrimenti restituisce la stessa lista. */
fun List<Goal>.added(goal: Goal): List<Goal> =
    if (size >= MAX_GOALS || any { it.id == goal.id }) this else this + goal

fun List<Goal>.removed(id: String): List<Goal> = filterNot { it.id == id }

/** Segna come raggiunti, a [day], gli obiettivi con questi [ids] che non lo sono ancora. */
fun List<Goal>.markedDone(ids: Set<String>, day: Long): List<Goal> =
    map { if (it.id in ids && it.doneAt == null) it.copy(doneAt = day) else it }

private fun Progress.isComplete() = total > 0 && owned >= total

/**
 * Le righe della card, nell'ordine di creazione. Un obiettivo il cui bersaglio non c'è nel catalogo mostrato (un paese nascosto
 * dalle Impostazioni, una serie sparita) non si disegna ma resta salvato: riappare se il bersaglio torna.
 */
fun goalRows(goals: List<Goal>, stats: CollectionStats): List<GoalRow> = goals.mapNotNull { goal ->
    val target = goalTarget(goal, stats) ?: return@mapNotNull null
    val done = goal.doneAt != null || target.progress.isComplete()
    GoalRow(goal, target.label, target.progress, done, if (done) null else target.go)
}

/** Gli obiettivi non ancora segnati che, con la collezione di oggi, sono raggiunti: il ViewModel li segna una volta per sempre. */
fun newlyDoneGoals(goals: List<Goal>, stats: CollectionStats): Set<String> =
    goalRows(goals, stats).filter { it.goal.doneAt == null && it.progress.isComplete() }.map { it.goal.id }.toSet()

private class GoalTarget(val label: String, val progress: Progress, val go: BadgeTarget?)

private fun goalTarget(goal: Goal, stats: CollectionStats): GoalTarget? = when (goal.kind) {
    GoalKind.COUNTRY -> stats.commemorativeCountries.firstOrNull { it.paese == goal.key }?.let {
        GoalTarget("Complete ${it.name}", it.progress, BadgeTarget.Milestone(MilestoneTarget.CommemorativeCountry(it.paese)))
    }
    GoalKind.YEAR -> goal.key.toIntOrNull()?.let { year ->
        stats.years.firstOrNull { it.anno == year }?.let {
            GoalTarget("Complete $year", it.progress, BadgeTarget.Milestone(MilestoneTarget.Year(year, jointOnly(stats, year))))
        }
    }
    GoalKind.SERIES -> stats.seriesStats.firstOrNull { it.key == goal.key }?.let {
        GoalTarget("Complete ${it.label}", it.progress, BadgeTarget.Milestone(MilestoneTarget.RegularCountry(it.paese, it.number)))
    }
    // Le monete di un obiettivo sono le commemorative possedute (le Regular hanno il loro catalogo).
    GoalKind.COINS -> goal.key.toIntOrNull()?.let { target ->
        GoalTarget("Reach $target coins", Progress(stats.commemorative.owned.coerceAtMost(target), target), BadgeTarget.AllMissing)
    }
}

/** Il tocco su un anno apre la lista che ha ancora monete mancanti: quella dei singoli paesi o, se è l'unica rimasta, l'emissione comune. */
private fun jointOnly(stats: CollectionStats, year: Int): Boolean {
    val single = stats.yearRows.firstOrNull { it.anno == year && !it.joint }
    return single == null || single.progress.isComplete()
}

/**
 * Le scelte per un tipo di obiettivo: solo ciò che si può ANCORA completare, dalla più vicina alla più lontana (come Progress).
 * Per le monete, una scala di 25 in 25 sopra quelle possedute e non oltre il catalogo (al massimo [COIN_CHOICES] valori).
 */
fun goalOptions(kind: GoalKind, stats: CollectionStats, existing: List<Goal>): List<GoalOption> {
    val taken = existing.filter { it.kind == kind }.map { it.key }.toSet()
    fun option(key: String, label: String, progress: Progress, paese: String? = null) = GoalOption(key, label, progress, key in taken, paese)
    return when (kind) {
        GoalKind.COUNTRY -> stats.commemorativeCountries.filter { !it.progress.isComplete() }
            .sortedWith(compareBy({ -it.progress.fraction }, { it.name }))
            .map { option(it.paese, it.name, it.progress, paese = it.paese) }
        GoalKind.YEAR -> stats.years.filter { !it.progress.isComplete() }
            .sortedWith(compareBy({ -it.progress.fraction }, { -it.anno }))
            .map { option(it.anno.toString(), it.anno.toString(), it.progress) }
        GoalKind.SERIES -> stats.seriesStats.filter { !it.progress.isComplete() }
            .sortedWith(compareBy({ -it.progress.fraction }, { it.label }))
            .map { option(it.key, it.label, it.progress, paese = it.paese) }
        GoalKind.COINS -> {
            val owned = stats.commemorative.owned
            val total = stats.commemorative.total
            generateSequence((owned / COIN_STEP + 1) * COIN_STEP) { it + COIN_STEP }
                .takeWhile { it <= total }.take(COIN_CHOICES).toList()
                .map { option(it.toString(), "$it coins", Progress(owned, it)) }
        }
    }
}

private const val COIN_STEP = 25
private const val COIN_CHOICES = 6

/** Il suggerimento della card vuota: l'obiettivo più vicino da completare tra i paesi (la prima scelta dell\'elenco). */
fun suggestedGoal(stats: CollectionStats, existing: List<Goal>): GoalOption? =
    goalOptions(GoalKind.COUNTRY, stats, existing).firstOrNull { !it.taken }

private val goalsJson = Json { ignoreUnknownKeys = true }

fun encodeGoals(goals: List<Goal>): String = goalsJson.encodeToString(ListSerializer(Goal.serializer()), goals)

/** Tollerante: un testo rovinato o vuoto dà nessun obiettivo, i doppioni si tolgono e si rispetta il massimo. */
fun decodeGoals(text: String?): List<Goal> {
    if (text.isNullOrBlank()) return emptyList()
    val list = runCatching { goalsJson.decodeFromString(ListSerializer(Goal.serializer()), text) }.getOrDefault(emptyList())
    return list.distinctBy { it.id }.take(MAX_GOALS)
}
