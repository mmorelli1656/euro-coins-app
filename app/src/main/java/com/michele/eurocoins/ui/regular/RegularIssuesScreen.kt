package com.michele.eurocoins.ui.regular

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.michele.eurocoins.ui.components.ScrollToRequestedItem
import com.michele.eurocoins.ui.components.rememberReturnHighlight
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.DenominationRow
import com.michele.eurocoins.data.RegularAllGroup
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.seriesTitle
import com.michele.eurocoins.ui.browse.BrowseCard
import com.michele.eurocoins.ui.browse.CardFooter
import com.michele.eurocoins.ui.browse.CardGrid
import com.michele.eurocoins.ui.browse.CompletionFilter
import com.michele.eurocoins.ui.components.ChoiceSection
import com.michele.eurocoins.ui.components.CollectionProgressBar
import com.michele.eurocoins.ui.components.FilterGroupHeader
import com.michele.eurocoins.ui.components.FilterSheet
import com.michele.eurocoins.ui.components.FloatingSearchBar
import com.michele.eurocoins.ui.components.MultiChoiceSection
import com.michele.eurocoins.ui.components.RegularCollectionSheet
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.michele.eurocoins.ui.components.floatingBarClearance
import com.michele.eurocoins.ui.components.rememberFloatingBarState
import com.michele.eurocoins.ui.theme.appBarColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Catalogo Regular Issues: un selettore Countries / Denominations / All. Countries è la griglia dei paesi
 * (un tocco apre le serie), Denominations una card per taglio (un tocco apre l'elenco di quel taglio in
 * tutti i paesi), All l'elenco di tutte le righe (il tocco apre il dettaglio, la casella il pannello di
 * collezione). In basso la stessa barra flottante di ricerca + FILTER delle commemorative, con query,
 * ordine e filtro propri di ogni scheda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularIssuesScreen(
    viewModel: RegularIssuesViewModel,
    onCountryClick: (String) -> Unit,
    onDenominationClick: (String) -> Unit,
    /** Dettaglio di un taglio dalla scheda All: serie GUARDATA e taglio. */
    onRowClick: (series: RegularIssueSeries, taglio: String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }
    var showFilters by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DenominationRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text("Regular Issues") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        // Una barra per scheda: cambiando scheda lo stato riparte "visibile".
        val barState = rememberFloatingBarState(state.mode)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .nestedScroll(barState.connection),
        ) {
            Column {
                ModeSelector(
                    selected = state.mode,
                    onSelected = viewModel::onModeChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                when (state.mode) {
                    RegularBrowseMode.COUNTRIES -> Box {
                        CardGrid(
                            loaded = state.loaded,
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
                                    Text(
                                        "${card.seriesCount} series",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 10.dp),
                                    )
                                    // Stessa barra dritta di Years/Countries nelle commemorative: righe possedute, non annate.
                                    CollectionProgressBar(progress = card.progress, modifier = Modifier.padding(top = 10.dp))
                                }
                            }
                        }
                        if (state.loaded && state.countries.isEmpty()) {
                            NoMatch(
                                what = "countries",
                                hint = "Countries can be filtered by name only.",
                                query = state.prefs.countriesQuery.trim(),
                            )
                        }
                    }
                    RegularBrowseMode.DENOMINATIONS -> Box {
                        CardGrid(
                            loaded = state.loaded,
                            resetScrollKey = state.prefs.denominationsLargestFirst,
                            hazeState = hazeState,
                        ) {
                            items(state.denominations, key = { it.taglio }) { card ->
                                BrowseCard(onClick = { onDenominationClick(card.taglio) }) {
                                    // Nella posizione della bandiera delle card Countries: le due schede hanno la stessa grammatica.
                                    DenominationCoin(card.taglio)
                                    Text(
                                        card.taglio,
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                    CardFooter(card.progress)
                                }
                            }
                        }
                        if (state.loaded && state.denominations.isEmpty()) {
                            NoMatch(
                                what = "denominations",
                                hint = "Denominations can be filtered by value, like “2 euro” or “cent”.",
                                query = state.prefs.denominationsQuery.trim(),
                            )
                        }
                    }
                    RegularBrowseMode.ALL -> Box {
                        // key(...) e non un LaunchedEffect: lo scorrimento si ricrea nella stessa composizione,
                        // come nelle liste di Commemorative (un effetto arriverebbe un fotogramma dopo).
                        key(state.prefs.allOrder) {
                            val listState = rememberLazyListState()
                            val scrollTarget by viewModel.scrollTo.collectAsState()
                            val highlight = rememberReturnHighlight<RegularPageKey>()
                            ScrollToRequestedItem(
                                target = scrollTarget,
                                listState = listState,
                                ready = state.all.isNotEmpty(),
                                rowHeight = RowHeight,
                                indexOf = { key ->
                                    state.all.indexOfFirst {
                                        it.paese == key.paese && it.viewedSeries.ordineCronologico == key.ordine &&
                                            it.denomination.image.taglio == key.taglio
                                    }
                                },
                                highlight = highlight,
                                onHandled = viewModel::consumeScrollTo,
                            )
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
                                contentPadding = PaddingValues(top = 4.dp, bottom = floatingBarClearance()),
                            ) {
                                items(state.all, key = { "${it.paese}|${it.viewedSeries.ordineCronologico}|${it.denomination.image.taglio}" }) { row ->
                                    // Come CoinRow: riga piccola "Paese · …" in verde sopra, il taglio come titolo. La serie
                                    // serve: lo stesso 5 cent francese compare in tre serie con la stessa foto.
                                    RegularCoinRow(
                                        image = row.denomination.image,
                                        status = "${row.flag} ${row.countryName} · ${seriesTitle(row.seriesNumber)}".trim(),
                                        statusHighlight = true,
                                        title = row.denomination.image.taglio,
                                        owned = row.owned,
                                        onClick = { onRowClick(row.viewedSeries, row.denomination.image.taglio) },
                                        highlighted = highlight.key ==
                                            RegularPageKey(row.paese, row.viewedSeries.ordineCronologico, row.denomination.image.taglio),
                                        onEditCollection = { editing = row },
                                    )
                                }
                            }
                        }
                        if (state.loaded && state.all.isEmpty()) {
                            NoMatch(
                                what = "coins",
                                hint = "Search by country, value like “2 euro”, series or year.",
                                query = state.prefs.allQuery.trim(),
                            )
                        }
                    }
                }
            }

            when (state.mode) {
                RegularBrowseMode.COUNTRIES -> FloatingSearchBar(
                    query = viewModel.countriesQueryNow,
                    onQueryChange = viewModel::setCountriesQuery,
                    placeholder = "Filter by country…",
                    filterActive = state.prefs.countriesFilterActive,
                    onFilterClick = { showFilters = true },
                    hazeState = hazeState,
                    barState = barState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                RegularBrowseMode.DENOMINATIONS -> FloatingSearchBar(
                    query = viewModel.denominationsQueryNow,
                    onQueryChange = viewModel::setDenominationsQuery,
                    placeholder = "Filter by value…",
                    filterActive = state.prefs.denominationsFilterActive,
                    onFilterClick = { showFilters = true },
                    hazeState = hazeState,
                    barState = barState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                RegularBrowseMode.ALL -> FloatingSearchBar(
                    query = viewModel.allQueryNow,
                    onQueryChange = viewModel::setAllQuery,
                    placeholder = "Country, value, series…",
                    filterActive = state.prefs.allFilterActive,
                    onFilterClick = { showFilters = true },
                    hazeState = hazeState,
                    barState = barState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    if (showFilters) {
        when (state.mode) {
            RegularBrowseMode.COUNTRIES -> FilterSheet(
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
            RegularBrowseMode.DENOMINATIONS -> FilterSheet(
                onReset = viewModel::resetDenominations,
                onDismiss = { showFilters = false },
            ) {
                ChoiceSection(
                    title = "Sort by value",
                    options = listOf(true, false),
                    selected = state.prefs.denominationsLargestFirst,
                    label = { if (it) "Largest first" else "Smallest first" },
                    onSelect = viewModel::setDenominationsLargestFirst,
                )
                ChoiceSection(
                    title = "Collection",
                    options = CompletionFilter.entries,
                    selected = state.prefs.denominationsCompletion,
                    label = { it.label },
                    onSelect = viewModel::setDenominationsCompletion,
                )
            }
            RegularBrowseMode.ALL -> FilterSheet(
                onReset = viewModel::resetAll,
                onDismiss = { showFilters = false },
            ) {
                FilterGroupHeader("Sort")
                ChoiceSection(
                    title = "Group by",
                    options = RegularAllGroup.entries,
                    selected = state.prefs.allOrder.group,
                    label = { it.label },
                    onSelect = viewModel::setAllGroup,
                )
                ChoiceSection(
                    title = "Country order",
                    options = listOf(true, false),
                    selected = state.prefs.allOrder.countryAscending,
                    label = { if (it) "A → Z" else "Z → A" },
                    onSelect = viewModel::setAllCountryAscending,
                )
                ChoiceSection(
                    title = "Value order",
                    options = listOf(true, false),
                    selected = state.prefs.allOrder.largestFirst,
                    label = { if (it) "Largest first" else "Smallest first" },
                    onSelect = viewModel::setAllLargestFirst,
                )
                FilterGroupHeader("Filter")
                ChoiceSection(
                    title = "Collection",
                    options = OwnershipFilter.entries,
                    selected = state.prefs.allOwnership,
                    label = { it.label },
                    onSelect = viewModel::setAllOwnership,
                )
                if (state.prefs.allOwnership == OwnershipFilter.OWNED) {
                    MultiChoiceSection(
                        title = "Owned quality",
                        options = CoinQuality.entries,
                        selected = state.prefs.allQualities,
                        label = { it.label },
                        onToggle = viewModel::toggleAllQuality,
                    )
                }
            }
        }
    }

    val current = editing
    if (current != null) {
        // La serie del taglio (di origine) per la chiave, la serie GUARDATA per il titolo del pannello.
        RegularCollectionSheet(
            countryName = current.countryName,
            series = current.denomination.series,
            seriesNumber = current.seriesNumber,
            denomination = current.denomination.image,
            currentItems = current.items,
            onSave = { entries ->
                viewModel.onSaveCollection(current, entries)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selected: RegularBrowseMode,
    onSelected: (RegularBrowseMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = RegularBrowseMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                // Nessuna spunta: spostava l'etichetta fuori centro (come nei selettori di Impostazioni).
                icon = {},
                label = {
                    Text(
                        when (mode) {
                            RegularBrowseMode.COUNTRIES -> "Countries"
                            RegularBrowseMode.DENOMINATIONS -> "Values"
                            RegularBrowseMode.ALL -> "All"
                        },
                    )
                },
            )
        }
    }
}

/**
 * Nessuna card o riga corrisponde alla ricerca o al filtro. A differenza di Commemorative non c'è il
 * pulsante "Search all" sulle griglie: l'avviso resta semplice, la scheda All si sceglie dal selettore.
 */
@Composable
private fun NoMatch(what: String, hint: String, query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (query.isEmpty()) "No $what match this filter" else "No $what match “$query”",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
