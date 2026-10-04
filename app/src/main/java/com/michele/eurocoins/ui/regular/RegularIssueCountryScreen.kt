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
                SeriesHeader(number = state.selectedIndex + 1, period = seriesPeriod(state.seriesList, series))
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
            // La descrizione sta SOTTO i tagli: la lista (e le caselle della collezione) e' cio' che si usa
            // qui, il testo e' contesto da leggere una volta, come ABOUT THIS COIN nel dettaglio.
            state.selectedSeries?.let { series ->
                SeriesAbout(
                    series = series,
                    textLabel = seriesTextLabel(state.seriesList, series),
                    scrollState = scrollState,
                    viewport = { viewport },
                )
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
 * Titolo della serie (il numero) con il periodo sotto, come dato secondario. La descrizione e' in
 * [SeriesAbout], sotto i tagli.
 */
@Composable
private fun SeriesHeader(number: Int, period: SeriesPeriod) {
    // Titolo 20 sp Bold (come i titoli di sezione delle Impostazioni) e periodo 15 sp: a 16/13 sp il titolo
    // non staccava dal testo delle card sotto (14/13 sp). 8 dp prima della prima card.
    Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)) {
        Text(
            seriesTitle(number),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = if (period.label == null) 4.dp else 0.dp),
        )
        // Periodo come dato secondario sotto il titolo (variante A scelta dopo mockup): il numero resta
        // il titolo, l'anno non compete con lui.
        period.label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
            )
        }
    }
}

/**
 * La descrizione della serie in una card espandibile (`NotesCard`, la stessa di ABOUT THIS COIN
 * nel dettaglio commemorative: 4 righe + "Show more"), sotto i tagli. La fonte resta SEMPRE visibile
 * sotto la card, anche chiusa: l'attribuzione non deve dipendere da un tocco. Senza descrizione
 * non compare niente.
 */
@Composable
private fun SeriesAbout(
    series: RegularIssueSeries,
    textLabel: String,
    scrollState: ScrollState,
    viewport: () -> Rect?,
) {
    val description = series.displayDescription()
    if (description.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)) {
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

/** La riga di un taglio nella serie: stato di possesso sopra, il taglio come titolo. */
@Composable
private fun DenominationRow(denom: DenominationUiState, onClick: () -> Unit, onEditCollection: () -> Unit) {
    RegularCoinRow(
        image = denom.image,
        status = if (denom.ownedYears == 0) "Not owned" else "${denom.ownedYears} ${if (denom.ownedYears == 1) "year" else "years"} owned",
        statusHighlight = denom.ownedYears > 0,
        title = denom.image.taglio,
        owned = denom.ownedYears > 0,
        onClick = onClick,
        onEditCollection = onEditCollection,
    )
}
