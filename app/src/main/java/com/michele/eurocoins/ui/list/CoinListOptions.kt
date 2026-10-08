package com.michele.eurocoins.ui.list

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.ui.components.ChoiceSection
import com.michele.eurocoins.ui.components.FilterGroupHeader
import com.michele.eurocoins.ui.components.FilterSheet
import com.michele.eurocoins.ui.components.FloatingBarState
import com.michele.eurocoins.ui.components.FloatingSearchBar
import com.michele.eurocoins.ui.components.MultiChoiceSection
import dev.chrisbanes.haze.HazeState

enum class OwnershipFilter(val label: String) {
    ALL("All"),
    OWNED("Owned"),
    MISSING("Missing"),
}

/**
 * Ordine delle liste di un anno o di un paese (una sola scelta: l'altro asse è costante).
 * [DEFAULT] = ordine del database (anno decrescente, poi nome del paese mostrato): NON è più una scelta mostrata
 * nel pannello (2026-10-08: il pulsante "Default" era inutile, coincide con la prima voce: "Country A → Z" in una
 * lista di anno, "Newest first" in una di paese). Resta come valore interno "l'utente non ha scelto", così `CoinListOptions()`
 * resta il predefinito, il Reset funziona e il pallino del FILTER non si accende per una scelta che non cambia nulla.
 */
enum class CoinSort(val label: String) {
    DEFAULT("Default"),
    YEAR_DESC("Newest first"),
    YEAR_ASC("Oldest first"),
    COUNTRY_AZ("Country A → Z"),
    COUNTRY_ZA("Country Z → A"),
}

/** Cosa viene prima nell'elenco "All": le monete si raggruppano per anno o per paese. */
enum class CoinGroup(val label: String) {
    YEAR("Year"),
    COUNTRY("Country"),
}

data class CoinListOptions(
    val ownership: OwnershipFilter = OwnershipFilter.ALL,
    /** Vuoto = nessun vincolo; altrimenti la moneta deve avere almeno una di queste qualità. */
    val qualities: Set<CoinQuality> = emptySet(),
    /** Liste di un anno o di un paese. */
    val sort: CoinSort = CoinSort.DEFAULT,
    /** Elenco "All": tre scelte indipendenti, il predefinito coincide con l'ordine del database (anno decrescente, poi paese A → Z). */
    val group: CoinGroup = CoinGroup.YEAR,
    val countryAscending: Boolean = true,
    val newestFirst: Boolean = true,
) {
    val isActive: Boolean get() = this != CoinListOptions()

    /** Cambia con qualunque scelta di ordine: la lista torna in cima (stato di scorrimento nuovo). */
    val orderKey: List<Any> get() = listOf(sort, group, countryAscending, newestFirst)

    /** true se l'ordine dell'elenco "All" è quello del database. */
    val isDefaultAllOrder: Boolean get() = group == CoinGroup.YEAR && countryAscending && newestFirst
}

/** Ordinamenti sensati per le liste di un anno o di un paese (la prima voce è il predefinito); "All" ha invece le tre scelte di [CoinListOptions]. */
fun CoinFilter.sortChoices(): List<CoinSort> = when (this) {
    CoinFilter.All -> emptyList()
    is CoinFilter.Year -> listOf(CoinSort.COUNTRY_AZ, CoinSort.COUNTRY_ZA)
    is CoinFilter.Country -> listOf(CoinSort.YEAR_DESC, CoinSort.YEAR_ASC)
}

/**
 * La voce di [sortChoices] che coincide con l'ordine di base ([CoinSort.DEFAULT]): selezionata finché non si sceglie altro,
 * e scegliendola di nuovo si torna a [CoinSort.DEFAULT] (stessa lista, filtro non attivo).
 */
fun CoinFilter.defaultSort(): CoinSort = sortChoices().firstOrNull() ?: CoinSort.DEFAULT

/** Barra flottante + pannello filtri di una lista di monete, collegati al suo [viewModel]. */
@Composable
fun BoxScope.CoinListSearchBar(
    viewModel: CoinListViewModel,
    placeholder: String,
    hazeState: HazeState,
    barState: FloatingBarState,
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    FloatingSearchBar(
        query = viewModel.currentQuery,
        onQueryChange = viewModel::onQueryChange,
        placeholder = placeholder,
        filterActive = state.options.isActive,
        onFilterClick = { showFilters = true },
        hazeState = hazeState,
        barState = barState,
        modifier = Modifier.align(Alignment.BottomCenter),
    )

    if (showFilters) {
        val options = state.options
        FilterSheet(
            onReset = { viewModel.onOptionsChange(CoinListOptions()) },
            onDismiss = { showFilters = false },
        ) {
            if (viewModel.sortChoices.isEmpty()) {
                // "All": due assi (paese e anno), quindi tre scelte indipendenti invece di un solo ordine.
                FilterGroupHeader("Sort")
                ChoiceSection(
                    title = "Sort first by",
                    options = CoinGroup.entries,
                    selected = options.group,
                    label = { it.label },
                    onSelect = { viewModel.onOptionsChange(options.copy(group = it)) },
                )
                ChoiceSection(
                    title = "Country order",
                    options = listOf(true, false),
                    selected = options.countryAscending,
                    label = { if (it) "A → Z" else "Z → A" },
                    onSelect = { viewModel.onOptionsChange(options.copy(countryAscending = it)) },
                )
                ChoiceSection(
                    title = "Year order",
                    options = listOf(true, false),
                    selected = options.newestFirst,
                    label = { if (it) "Newest first" else "Oldest first" },
                    onSelect = { viewModel.onOptionsChange(options.copy(newestFirst = it)) },
                )
                FilterGroupHeader("Filter")
            } else {
                ChoiceSection(
                    title = "Sort by",
                    options = viewModel.sortChoices,
                    selected = if (options.sort == CoinSort.DEFAULT) viewModel.defaultSort else options.sort,
                    label = { it.label },
                    onSelect = { viewModel.onOptionsChange(options.copy(sort = if (it == viewModel.defaultSort) CoinSort.DEFAULT else it)) },
                )
            }
            ChoiceSection(
                title = "Collection",
                options = OwnershipFilter.entries,
                selected = options.ownership,
                label = { it.label },
                // "Owned quality" esiste solo con Collection = Owned: uscendone le qualità spuntate si
                // azzerano, non restano attive di nascosto.
                onSelect = {
                    viewModel.onOptionsChange(
                        options.copy(
                            ownership = it,
                            qualities = if (it == OwnershipFilter.OWNED) options.qualities else emptySet(),
                        ),
                    )
                },
            )
            if (options.ownership == OwnershipFilter.OWNED) {
                MultiChoiceSection(
                    title = "Owned quality",
                    options = CoinQuality.entries,
                    selected = options.qualities,
                    label = { it.label },
                    onToggle = {
                        val next = if (it in options.qualities) options.qualities - it else options.qualities + it
                        viewModel.onOptionsChange(options.copy(qualities = next))
                    },
                )
            }
        }
    }
}
