package com.michele.eurocoins.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.ui.components.CollectionProgressBar
import com.michele.eurocoins.ui.list.CoinListContent
import com.michele.eurocoins.ui.list.CoinListViewModel

/**
 * Catalogo commemorative: un selettore Years / Countries / All. Years e
 * Countries sono griglie di card (un tocco apre la lista filtrata), All è
 * l'elenco completo con ricerca.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commemorative") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ModeSelector(
                selected = state.mode,
                onSelected = viewModel::onModeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when (state.mode) {
                BrowseMode.YEARS -> CardGrid {
                    items(state.years, key = { it.year }) { card ->
                        BrowseCard(onClick = { onYearClick(card.year) }) {
                            Text(card.year.toString(), style = MaterialTheme.typography.headlineMedium)
                            CardFooter(card.progress)
                        }
                    }
                }
                BrowseMode.COUNTRIES -> CardGrid {
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
                BrowseMode.ALL -> CoinListContent(viewModel = allCoinsViewModel, onCoinClick = onCoinClick)
            }
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
private fun CardGrid(content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
