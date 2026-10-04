package com.michele.eurocoins.ui.regular

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.DenominationRow
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.ui.components.ChoiceSection
import com.michele.eurocoins.ui.components.FilterSheet
import com.michele.eurocoins.ui.components.FloatingSearchBar
import com.michele.eurocoins.ui.components.RegularCollectionSheet
import com.michele.eurocoins.ui.components.floatingBarClearance
import com.michele.eurocoins.ui.theme.appBarColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Un taglio in tutti i paesi: una riga per serie (41), il PAESE come titolo e "Series 2 · 2008 – 2013"
 * sopra — il taglio è già nella barra in alto, ripeterlo in ogni riga sarebbe rumore. La riga è la
 * stessa [RegularCoinRow] della schermata del paese: il tocco apre il dettaglio del taglio, la casella
 * il pannello di collezione. In basso la barra flottante (ricerca per paese, serie o anno) con FILTER
 * Owned / Missing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularDenominationListScreen(
    taglio: String,
    viewModel: RegularDenominationListViewModel,
    onRowClick: (series: RegularIssueSeries) -> Unit,
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
                title = { Text(taglio) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
                contentPadding = PaddingValues(top = 4.dp, bottom = floatingBarClearance()),
            ) {
                items(state.rows, key = { "${it.paese}|${it.viewedSeries.ordineCronologico}" }) { row ->
                    RegularCoinRow(
                        image = row.denomination.image,
                        status = row.seriesLabel,
                        statusHighlight = false,
                        title = "${row.flag} ${row.countryName}".trim(),
                        owned = row.owned,
                        onClick = { onRowClick(row.viewedSeries) },
                        onEditCollection = { editing = row },
                    )
                }
            }
            if (!state.loading && state.rows.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (state.query.isBlank()) "No coins match this filter" else "No coins match “${state.query.trim()}”",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Search by country, series number or year.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            FloatingSearchBar(
                query = viewModel.queryNow,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Country, series, year…",
                filterActive = state.filterActive,
                onFilterClick = { showFilters = true },
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showFilters) {
        FilterSheet(onReset = viewModel::resetFilters, onDismiss = { showFilters = false }) {
            ChoiceSection(
                title = "Collection",
                options = OwnershipFilter.entries,
                selected = state.ownership,
                label = { it.label },
                onSelect = viewModel::onOwnershipChange,
            )
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
