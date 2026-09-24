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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.key
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Public

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.material3.TopAppBar
import com.michele.eurocoins.ui.theme.appBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
                colors = appBarColors(),
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

    var zoomed by remember { mutableStateOf<Coin?>(null) }
    zoomed?.let { coin ->
        CoinImageDialog(
            coin = coin,
            onDismiss = { zoomed = null },
            onDetails = {
                zoomed = null
                onCoinClick(coin.id)
            },
        )
    }

    val filtering = state.query.isNotBlank() || state.options.isActive
    // Stato nuovo a ogni cambio d'ordinamento: con le chiavi stabili la lista
    // altrimenti "segue" la moneta ancorata nella nuova sequenza e salta.
    val listState = key(state.options.sort) { rememberLazyListState() }
    PrefetchThumbnails(listState = listState, coins = state.coins)
    // Alla prima apertura la lista arriva dopo la schermata (state.loading): senza il fade si vedeva
    // "0 coins" e poi le righe di colpo.
    val alpha by animateFloatAsState(if (state.loading) 0f else 1f, tween(220), label = "listFade")
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.hazeSource(hazeState),
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
                onImageClick = { zoomed = coin },
                onEditCollection = { editing = coin },
            )
        }
    }
}

/** Lato della miniatura nell'elenco; il precaricamento usa la stessa misura. */
private val ThumbnailSize = 52.dp

/** Foto pubblicata dalla fonte (non placeholder e con URL): distinta dal caso "caricamento fallito a runtime". */
private fun Coin.hasImage() = !immaginePlaceholder && urlImmagineFonte != null

/** Quante monete oltre l'ultima visibile precaricare mentre si scorre. */
private const val PREFETCH_AHEAD = 24

/**
 * Scarica in anticipo le immagini delle monete appena sotto quelle visibili,
 * così quando la riga arriva sullo schermo la foto è già nella cache su disco
 * di Coil. Stessa dimensione della miniatura ([ThumbnailSize]) per riusare
 * anche la cache in memoria. Ogni moneta si accoda una volta sola.
 */
@Composable
private fun PrefetchThumbnails(listState: LazyListState, coins: List<Coin>) {
    val context = LocalContext.current
    val thumbPx = with(LocalDensity.current) { ThumbnailSize.roundToPx() }
    val requested = remember { HashSet<Long>() }
    LaunchedEffect(listState, coins) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            // Indice 0 della lista è l'intestazione "N coins": la moneta i è l'elemento i + 1.
            (visible.lastOrNull()?.index ?: 0)
        }.collect { lastItem ->
            val loader = SingletonImageLoader.get(context)
            val from = (lastItem - 1).coerceAtLeast(0)
            val to = (from + PREFETCH_AHEAD).coerceAtMost(coins.size)
            for (i in from until to) {
                val coin = coins[i]
                val url = coin.urlImmagineFonte
                if (coin.immaginePlaceholder || url == null || !requested.add(coin.id)) continue
                loader.enqueue(ImageRequest.Builder(context).data(url).size(thumbPx).build())
            }
        }
    }
}

@Composable
private fun CoinRow(
    coin: Coin,
    owned: Boolean,
    onClick: () -> Unit,
    onImageClick: () -> Unit,
    onEditCollection: () -> Unit,
) {
    val hasImage = coin.hasImage()
    // Una card per moneta (bianca, bordo da 1 dp, angoli 14 dp, 8 dp tra una e l'altra): senza
    // card le righe stavano direttamente sul fondo e l'elenco risultava piatto, a differenza
    // delle card di Years/Countries. Il tocco sulla card apre il dettaglio.
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Area di tocco 60dp attorno alla miniatura da 52dp: ingrandisce la foto.
        // Senza foto (non ancora pubblicata) non c'è nulla da ingrandire: il tocco
        // resta quello della riga.
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .then(if (hasImage) Modifier.clickable(onClick = onImageClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            CoinThumbnail(coin)
        }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${coin.displayCountry()} · ${coin.anno}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (coin.emissioneComune) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = "Common issue, minted jointly by all Eurozone countries",
                        // Stesso colore della riga "Paese · Anno" accanto: si legge come parte
                        // dell'etichetta, non come un nuovo accento aggiunto solo qui.
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp).size(14.dp),
                    )
                }
            }
            Text(
                text = coin.tema,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
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
    val hasImage = coin.hasImage()
    Box(
        modifier = Modifier
            .size(ThumbnailSize)
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
