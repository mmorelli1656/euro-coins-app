package com.michele.eurocoins.ui.regular

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.transformations
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.SeriesPeriod
import com.michele.eurocoins.data.displayDescription
import com.michele.eurocoins.data.displayTextSource
import com.michele.eurocoins.data.seriesChipLabel
import com.michele.eurocoins.data.seriesPeriod
import com.michele.eurocoins.data.seriesTextLabel
import com.michele.eurocoins.data.seriesTitle
import com.michele.eurocoins.ui.components.RegularCollectionSheet
import com.michele.eurocoins.ui.detail.NotesCard
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
    onDenominationClick: (RegularIssueSeries, String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<DenominationUiState?>(null) }
    val scrollState = rememberScrollState()
    var viewport by remember { mutableStateOf<Rect?>(null) }

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
        // Column scrollabile e non LazyColumn: `NotesCard` (descrizione della serie) scorre la pagina
        // per centrarsi in espansione e vuole uno `ScrollState`; i tagli sono al massimo 8 per
        // serie (una sola serie alla volta), la lista lazy non farebbe risparmiare nulla.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .onGloballyPositioned { viewport = it.boundsInWindow() }
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp),
        ) {
            if (state.seriesList.size > 1) {
                SeriesChipRow(state.seriesList, state.selectedIndex, viewModel::onSelectSeries)
            }
            state.selectedSeries?.let { series ->
                SeriesHeader(
                    series = series,
                    number = state.selectedIndex + 1,
                    period = seriesPeriod(state.seriesList, series),
                    textLabel = seriesTextLabel(state.seriesList, series),
                    scrollState = scrollState,
                    viewport = { viewport },
                )
            }
            for (denom in state.denominations) {
                key(denom.image.taglio) {
                    DenominationRow(
                        denom = denom,
                        onClick = { state.selectedSeries?.let { onDenominationClick(it, denom.image.taglio) } },
                        onEditCollection = { editing = denom },
                    )
                }
            }
        }
    }

    val current = editing
    if (current != null) {
        // La serie del taglio, non quella selezionata: un taglio rimasto invariato (es. Francia
        // 5 cent nella serie 2022) è la moneta della serie che l'ha introdotto, e si salva lì.
        RegularCollectionSheet(
            countryName = state.countryName,
            series = current.series,
            seriesNumber = state.selectedIndex + 1,
            denomination = current.image,
            currentItems = current.items,
            onSave = { entries ->
                viewModel.onSaveCollection(current.denomination, entries)
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
                onClick = { onSelect(i) },
                label = { Text(seriesChipLabel(i + 1, seriesPeriod(seriesList, series))) },
            )
        }
    }
}

/**
 * Titolo della serie, poi la descrizione in una card espandibile (`NotesCard`, la stessa di
 * HISTORICAL NOTES nel dettaglio commemorative: 4 righe + "Show more") invece del muro di testo
 * che spingeva i tagli fuori schermo. La fonte resta SEMPRE visibile sotto la card, anche chiusa:
 * l'attribuzione non deve dipendere da un tocco.
 */
@Composable
private fun SeriesHeader(
    series: RegularIssueSeries,
    number: Int,
    period: SeriesPeriod,
    textLabel: String,
    scrollState: ScrollState,
    viewport: () -> Rect?,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            seriesTitle(number),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = if (period.label == null) 4.dp else 0.dp),
        )
        // Periodo come dato secondario sotto il titolo (variante A scelta dopo mockup): il numero resta
        // il titolo, l'anno non compete con lui.
        period.label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )
        }
        val description = series.displayDescription()
        if (description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            NotesCard(description, scrollState, viewport, label = textLabel)
            // Fonte del testo sopra: EC per quasi tutte le serie, altrove la fonte che lo ha fornito
            // (BCL, CFN, Monaco Tribune). Qui e non nel dettaglio del taglio, dove questo testo non c'è.
            Text(
                text = "Series text: ${series.displayTextSource()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp),
            )
        }
    }
}

/**
 * Una card per taglio, altezza fissa 72 dp: stessa struttura di `CoinRow` in `CoinListScreen.kt`.
 * Il tocco sulla riga apre il dettaglio del taglio ([onClick]), come nell'elenco Commemorative;
 * la casella a destra resta un bersaglio separato per la collezione ([onEditCollection]).
 */
@Composable
private fun DenominationRow(denom: DenominationUiState, onClick: () -> Unit, onEditCollection: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .height(72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
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
