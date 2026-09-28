package com.michele.eurocoins.ui.regular

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.transformations
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.ui.theme.appBarColors

private val ThumbnailSize = 52.dp

/**
 * Serie divisionali (1 cent - 2 euro) di un singolo paese, ordinate per anzianità: una card per
 * serie con descrizione per intero (nessun troncamento in questa prima versione) e una riga
 * scorrevole con le immagini dei tagli presenti. Stesso trattamento di caricamento/fallback delle
 * commemorative — vedi `CoinThumbnail` in `CoinListScreen.kt`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularIssueCountryScreen(
    viewModel: RegularIssueCountryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.series, key = { it.id }) { series -> SeriesCard(series) }
        }
    }
}

@Composable
private fun SeriesCard(series: RegularIssueSeries) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                seriesHeading(series),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                series.descrizione,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            if (series.immagini.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    series.immagini.forEach { image -> DenominationThumbnail(image) }
                }
            }
        }
    }
}

/** "Series 1 · 2002" quando c'è un'intestazione, altrimenti solo "Series N". */
private fun seriesHeading(series: RegularIssueSeries): String {
    val base = "Series ${series.numeroSerieIpotesi}"
    return series.intestazioneRaw?.let { "$base · $it" } ?: base
}

@Composable
private fun DenominationThumbnail(image: RegularIssueImage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(ThumbnailSize).clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
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
                    modifier = Modifier.fillMaxSize(),
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
        Text(
            image.taglio,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun PlaceholderCircle(content: @Composable () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f)
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
