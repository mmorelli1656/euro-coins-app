package com.michele.eurocoins.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.stableKey
import com.michele.eurocoins.ui.components.CollectionSheet
import com.michele.eurocoins.ui.components.floatingBarClearance
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(
    viewModel: CoinListViewModel,
    filter: CoinFilter,
    onCoinClick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            CoinListContent(
                viewModel = viewModel,
                onCoinClick = onCoinClick,
                hazeState = hazeState,
            )
            CoinListSearchBar(
                viewModel = viewModel,
                placeholder = when (filter) {
                    CoinFilter.All -> "Search by year or theme…"
                    is CoinFilter.Year -> "Search by theme…"
                    is CoinFilter.Country -> "Search by year or theme…"
                },
                hazeState = hazeState,
            )
        }
    }
}

/**
 * Conteggio + elenco: riusato dalle liste filtrate (anno/paese) e dalla
 * scheda "All". La barra di ricerca è a parte ([CoinListSearchBar]) e
 * galleggia sopra: l'elenco è la sorgente del vetro ([hazeState]) e lascia
 * spazio in fondo perché l'ultima moneta non resti coperta.
 */
@Composable
fun CoinListContent(
    viewModel: CoinListViewModel,
    onCoinClick: (Long) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<Coin?>(null) }

    editing?.let { coin ->
        CollectionSheet(
            coin = coin,
            currentItems = state.collection[coin.stableKey].orEmpty(),
            onSave = { entries ->
                viewModel.onSaveCollection(coin, entries)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    val filtering = state.query.isNotBlank() || state.options.isActive
    LazyColumn(
        modifier = modifier.fillMaxSize().hazeSource(hazeState),
        contentPadding = PaddingValues(bottom = floatingBarClearance()),
    ) {
        item {
            Text(
                text = "${state.coins.size} coins" + if (filtering) " found" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(state.coins, key = { it.id }) { coin ->
            CoinRow(
                coin = coin,
                owned = coin.stableKey in state.collection,
                onClick = { onCoinClick(coin.id) },
                onEditCollection = { editing = coin },
            )
        }
    }
}

@Composable
private fun CoinRow(coin: Coin, owned: Boolean, onClick: () -> Unit, onEditCollection: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinThumbnail(coin)
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                text = "${coin.displayCountry()} · ${coin.anno}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = coin.tema,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CollectionBox(owned = owned, onClick = onEditCollection)
    }
}

/** Casella accanto alla moneta: piena con spunta se posseduta in almeno una qualità; un tocco apre il pannello. */
@Composable
private fun CollectionBox(owned: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        // Area di tocco 44dp, casella visibile 26dp.
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
private fun CoinThumbnail(coin: Coin) {
    val hasImage = !coin.immaginePlaceholder && coin.urlImmagineFonte != null
    Box(
        modifier = Modifier
            .size(52.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage) {
            SubcomposeAsyncImage(
                model = coin.urlImmagineFonte,
                contentDescription = coin.tema,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Distinto dal ramo "else" qui sotto: quello è "la fonte non
                // ha ancora pubblicato l'immagine" (dato), questo è "il link
                // c'era ma il caricamento è fallito ora" (rete/link morto) —
                // vedi scripts/validate_image_links.py nella pipeline dati.
                if (painter.state.value is AsyncImagePainter.State.Error) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        } else {
            Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
