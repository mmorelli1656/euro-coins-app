package com.michele.eurocoins.ui.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.michele.eurocoins.ui.theme.appBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.ui.components.ChoiceSection
import com.michele.eurocoins.ui.components.CollectionProgressBar
import com.michele.eurocoins.ui.components.FilterSheet
import com.michele.eurocoins.ui.components.FloatingSearchBar
import com.michele.eurocoins.ui.components.floatingBarClearance
import com.michele.eurocoins.ui.list.CoinListContent
import com.michele.eurocoins.ui.list.CoinListSearchBar
import com.michele.eurocoins.ui.list.CoinListViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Catalogo commemorative: un selettore Years / Countries / All. Years e
 * Countries sono griglie di card (un tocco apre la lista filtrata), All è
 * l'elenco completo. In basso una barra flottante di ricerca + FILTER (che
 * contiene anche l'ordinamento) cambia contenuto con la scheda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    allCoinsViewModel: CoinListViewModel,
    onYearClick: (Int) -> Unit,
    onCountryClick: (String) -> Unit,
    onCoinClick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text("Commemorative") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            Column {
                ModeSelector(
                    selected = state.mode,
                    onSelected = viewModel::onModeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                when (state.mode) {
                    BrowseMode.YEARS -> CardGrid(
                        resetScrollKey = state.prefs.yearsAscending,
                        hazeState = hazeState,
                    ) {
                        items(state.years, key = { it.year }) { card ->
                            BrowseCard(onClick = { onYearClick(card.year) }) {
                                Text(card.year.toString(), style = MaterialTheme.typography.headlineMedium)
                                CardFooter(card.progress)
                            }
                        }
                    }
                    BrowseMode.COUNTRIES -> CardGrid(
                        resetScrollKey = state.prefs.countriesAscending,
                        hazeState = hazeState,
                    ) {
                        items(state.countries, key = { it.paese }) { card ->
                            BrowseCard(onClick = { onCountryClick(card.paese) }) {
                                Text(card.flag, fontSize = 34.sp)
                                Text(
                                    card.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                CardFooter(card.progress)
                            }
                        }
                    }
                    BrowseMode.ALL -> CoinListContent(
                        viewModel = allCoinsViewModel,
                        onCoinClick = onCoinClick,
                        hazeState = hazeState,
                    )
                }
            }

            // La scheda "All" ha la sua barra (query e filtri del suo ViewModel).
            when (state.mode) {
                BrowseMode.YEARS -> FloatingSearchBar(
                    query = state.prefs.yearsQuery,
                    onQueryChange = viewModel::setYearsQuery,
                    placeholder = "Search by year…",
                    filterActive = state.prefs.yearsFilterActive,
                    onFilterClick = { showFilters = true },
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                BrowseMode.COUNTRIES -> FloatingSearchBar(
                    query = state.prefs.countriesQuery,
                    onQueryChange = viewModel::setCountriesQuery,
                    placeholder = "Search countries…",
                    filterActive = state.prefs.countriesFilterActive,
                    onFilterClick = { showFilters = true },
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                BrowseMode.ALL -> CoinListSearchBar(
                    viewModel = allCoinsViewModel,
                    placeholder = "Search by year or theme…",
                    hazeState = hazeState,
                )
            }
        }
    }

    if (showFilters) {
        when (state.mode) {
            BrowseMode.YEARS -> FilterSheet(
                onReset = viewModel::resetYears,
                onDismiss = { showFilters = false },
            ) {
                ChoiceSection(
                    title = "Sort by year",
                    options = listOf(false, true),
                    selected = state.prefs.yearsAscending,
                    label = { if (it) "Oldest first" else "Newest first" },
                    onSelect = viewModel::setYearsAscending,
                )
                ChoiceSection(
                    title = "Collection",
                    options = CompletionFilter.entries,
                    selected = state.prefs.yearsCompletion,
                    label = { it.label },
                    onSelect = viewModel::setYearsCompletion,
                )
            }
            BrowseMode.COUNTRIES -> FilterSheet(
                onReset = viewModel::resetCountries,
                onDismiss = { showFilters = false },
            ) {
                ChoiceSection(
                    title = "Sort by name",
                    options = listOf(true, false),
                    selected = state.prefs.countriesAscending,
                    label = { if (it) "A → Z" else "Z → A" },
                    onSelect = viewModel::setCountriesAscending,
                )
                ChoiceSection(
                    title = "Collection",
                    options = CompletionFilter.entries,
                    selected = state.prefs.countriesCompletion,
                    label = { it.label },
                    onSelect = viewModel::setCountriesCompletion,
                )
            }
            BrowseMode.ALL -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selected: BrowseMode,
    onSelected: (BrowseMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = BrowseMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                label = {
                    Text(
                        when (mode) {
                            BrowseMode.YEARS -> "Years"
                            BrowseMode.COUNTRIES -> "Countries"
                            BrowseMode.ALL -> "All"
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun CardGrid(
    resetScrollKey: Any,
    hazeState: HazeState,
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit,
) {
    // Cambiando l'ordine le card mantengono la loro chiave, quindi la griglia
    // resterebbe scorsa "a metà" sulla nuova sequenza: si riparte dall'inizio.
    // Stato ricreato nella stessa composizione (non con un LaunchedEffect, che
    // arriva un frame dopo e lascia un fotogramma con l'ordine nuovo scorso a metà).
    val gridState = key(resetScrollKey) { rememberLazyGridState() }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().hazeSource(hazeState),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = floatingBarClearance()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun BrowseCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        // Bordo: nel tema chiaro fondo e card sono troppo vicini di tono per separarsi da soli.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun CardFooter(progress: Progress) {
    Text(
        text = "${progress.total} coins",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    CollectionProgressBar(progress = progress, modifier = Modifier.padding(top = 10.dp))
}
