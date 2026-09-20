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
import com.michele.eurocoins.ui.components.FilterSheet
import com.michele.eurocoins.ui.components.FloatingSearchBar
import com.michele.eurocoins.ui.components.MultiChoiceSection
import dev.chrisbanes.haze.HazeState

enum class OwnershipFilter(val label: String) {
    ALL("All"),
    OWNED("Owned"),
    MISSING("Missing"),
}

/** [DEFAULT] = ordine del database (anno decrescente, poi paese). */
enum class CoinSort(val label: String) {
    DEFAULT("Default"),
    YEAR_DESC("Newest first"),
    YEAR_ASC("Oldest first"),
    COUNTRY_AZ("Country A → Z"),
    COUNTRY_ZA("Country Z → A"),
}

data class CoinListOptions(
    val ownership: OwnershipFilter = OwnershipFilter.ALL,
    /** Vuoto = nessun vincolo; altrimenti la moneta deve avere almeno una di queste qualità. */
    val qualities: Set<CoinQuality> = emptySet(),
    val sort: CoinSort = CoinSort.DEFAULT,
) {
    val isActive: Boolean get() = this != CoinListOptions()
}

/** Ordinamenti sensati per lista: in un anno o in un paese l'altro asse è costante. */
fun CoinFilter.sortChoices(): List<CoinSort> = when (this) {
    CoinFilter.All -> CoinSort.entries
    is CoinFilter.Year -> listOf(CoinSort.DEFAULT, CoinSort.COUNTRY_AZ, CoinSort.COUNTRY_ZA)
    is CoinFilter.Country -> listOf(CoinSort.DEFAULT, CoinSort.YEAR_DESC, CoinSort.YEAR_ASC)
}

/** Barra flottante + pannello filtri di una lista di monete, collegati al suo [viewModel]. */
@Composable
fun BoxScope.CoinListSearchBar(
    viewModel: CoinListViewModel,
    placeholder: String,
    hazeState: HazeState,
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    FloatingSearchBar(
        query = state.query,
        onQueryChange = viewModel::onQueryChange,
        placeholder = placeholder,
        filterActive = state.options.isActive,
        onFilterClick = { showFilters = true },
        hazeState = hazeState,
        modifier = Modifier.align(Alignment.BottomCenter),
    )

    if (showFilters) {
        val options = state.options
        FilterSheet(
            onReset = { viewModel.onOptionsChange(CoinListOptions()) },
            onDismiss = { showFilters = false },
        ) {
            ChoiceSection(
                title = "Sort by",
                options = viewModel.sortChoices,
                selected = options.sort,
                label = { it.label },
                onSelect = { viewModel.onOptionsChange(options.copy(sort = it)) },
            )
            ChoiceSection(
                title = "Collection",
                options = OwnershipFilter.entries,
                selected = options.ownership,
                label = { it.label },
                onSelect = { viewModel.onOptionsChange(options.copy(ownership = it)) },
            )
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
