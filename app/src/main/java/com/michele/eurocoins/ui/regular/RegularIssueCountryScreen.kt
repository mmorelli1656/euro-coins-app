package com.michele.eurocoins.ui.regular

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.transformations
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.ui.components.RegularCollectionSheet
import com.michele.eurocoins.ui.theme.appBarColors

private val ThumbnailSize = 52.dp

/** Cerchio lilla un po' più piccolo della foto (50 dp contro 52): stesso motivo di `CoinListScreen`. */
private val PlaceholderSize = 50.dp

/**
 * Serie divisionali (1 cent - 2 euro) di un singolo paese. Con più di una serie (cambio di
 * ritratto/stemma: Belgio 3, Vaticano 6...) un selettore a chip in cima sceglie quale mostrare;
 * sotto, una card per taglio (stesso linguaggio di `CoinRow` in Commemorative — miniatura, stato
 * di possesso, casella), non più una riga orizzontale scorrevole dentro un'unica card di serie.
 * La casella apre `RegularCollectionSheet` per registrare le annate possedute di quel taglio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularIssueCountryScreen(
    viewModel: RegularIssueCountryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<DenominationUiState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text("${state.flag} ${state.countryName}".trim()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (state.seriesList.size > 1) {
                item { SeriesChipRow(state.seriesList, state.selectedIndex, viewModel::onSelectSeries) }
            }
            state.selectedSeries?.let { series ->
                item { SeriesHeader(series) }
            }
            items(state.denominations, key = { it.image.taglio }) { denom ->
                DenominationRow(denom = denom, onEditCollection = { editing = denom })
            }
        }
    }

    val current = editing
    val selectedSeries = state.selectedSeries
    if (current != null && selectedSeries != null) {
        RegularCollectionSheet(
            countryName = state.countryName,
            series = selectedSeries,
            denomination = current.image,
            currentItems = current.items,
            onSave = { entries ->
                viewModel.onSaveCollection(selectedSeries, current.image.taglio, entries)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun SeriesChipRow(seriesList: List<RegularIssueSeries>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        seriesList.forEachIndexed { i, series ->
            FilterChip(
                selected = i == selectedIndex,
                // seriesHeading() e non solo "Series N": lo stesso numero di serie può comparire
                // più di una volta per lo stesso paese (es. Belgio: 2002 e 2008 sono entrambe
                // "Series 1" nel dataset, un ritocco minore non classificato come nuova serie) e
                // senza l'anno i chip sarebbero indistinguibili.
                onClick = { onSelect(i) },
                label = { Text(seriesHeading(series)) },
            )
        }
    }
}

@Composable
private fun SeriesHeader(series: RegularIssueSeries) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            seriesHeading(series),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            series.descrizione,
            // Bordo destro regolare: stessa sillabazione di HISTORICAL NOTES nel dettaglio
            // commemorative (CoinDetailScreen.kt) — senza, il testo giustificabile ma non
            // sillabato lasciava spazi larghi e irregolari a fine riga.
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Justify,
                lineHeight = 20.sp,
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
    }
}

/** "Series 1 · 2002" quando c'è un'intestazione, altrimenti solo "Series N". */
private fun seriesHeading(series: RegularIssueSeries): String {
    val base = "Series ${series.numeroSerieIpotesi}"
    return series.intestazioneRaw?.let { "$base · $it" } ?: base
}

/** Una card per taglio, altezza fissa 72 dp: stessa struttura di `CoinRow` in `CoinListScreen.kt`. */
@Composable
private fun DenominationRow(denom: DenominationUiState, onEditCollection: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .height(72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(ThumbnailSize).clip(CircleShape), contentAlignment = Alignment.Center) {
            DenominationThumbnail(denom.image)
        }
        Column(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 2.dp, bottom = 2.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = if (denom.ownedYears == 0) "Not owned" else "${denom.ownedYears} ${if (denom.ownedYears == 1) "year" else "years"} owned",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                fontWeight = if (denom.ownedYears > 0) FontWeight.Bold else FontWeight.Normal,
                color = if (denom.ownedYears > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = denom.image.taglio,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        CollectionBox(owned = denom.ownedYears > 0, onClick = onEditCollection)
    }
}

/** Casella accanto al taglio: piena con spunta se posseduto in almeno un'annata; un tocco apre il pannello. Stesso disegno di `CollectionBox` in `CoinListScreen.kt` (duplicata qui, non condivisa — stesso approccio di `BrowseCard`). */
@Composable
private fun CollectionBox(owned: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (owned) primary else Color.Transparent)
                .border(
                    2.dp,
                    if (owned) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    RoundedCornerShape(7.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (owned) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "In your collection, tap to edit",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DenominationThumbnail(image: RegularIssueImage) {
    if (image.urlImmagineFonte == null) {
        DenominationPlaceholder()
    } else {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            // RegularIssueImageTrim: il margine attorno alla moneta non è uniforme da file a
            // file (vedi quella classe) — senza, la moneta appare più piccola del cerchio.
            model = ImageRequest.Builder(context)
                .data(image.urlImmagineFonte)
                .transformations(RegularIssueImageTrim)
                .build(),
            contentDescription = image.taglio,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
        ) {
            // MAI painter.state.value: e' uno StateFlow, .value non sottoscrive la ricomposizione
            // (vedi CLAUDE.md § Decisioni di prodotto, "Stato di SubcomposeAsyncImage").
            when (painter.state.collectAsState().value) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                is AsyncImagePainter.State.Error -> DenominationPlaceholder()
                else -> PlaceholderCircle()
            }
        }
    }
}

@Composable
private fun PlaceholderCircle(content: @Composable () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(PlaceholderSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DenominationPlaceholder() {
    PlaceholderCircle {
        Icon(
            imageVector = Icons.Filled.EuroSymbol,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(26.dp),
        )
    }
}
