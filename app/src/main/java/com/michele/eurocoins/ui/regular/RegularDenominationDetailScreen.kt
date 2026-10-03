package com.michele.eurocoins.ui.regular

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.transformations
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.displayDesigner
import com.michele.eurocoins.data.displayEngraver
import com.michele.eurocoins.data.displayImageLicense
import com.michele.eurocoins.data.displayMint
import com.michele.eurocoins.data.displaySourceName
import com.michele.eurocoins.data.numistaUrl
import com.michele.eurocoins.data.formatApproxTotal
import com.michele.eurocoins.data.groupMintagesByYear
import com.michele.eurocoins.data.summarizeMintages
import com.michele.eurocoins.ui.components.RegularCollectionSheet
import com.michele.eurocoins.ui.components.formatPrice
import com.michele.eurocoins.ui.detail.DetailCard
import com.michele.eurocoins.ui.detail.DetailsSection
import com.michele.eurocoins.ui.detail.SourceCredits
import com.michele.eurocoins.ui.detail.SourceItem
import com.michele.eurocoins.ui.detail.NotesCard
import com.michele.eurocoins.ui.detail.NO_VALUE
import com.michele.eurocoins.ui.detail.OwnedBadge
import com.michele.eurocoins.ui.detail.SectionLabel
import com.michele.eurocoins.ui.detail.ValueLabel
import com.michele.eurocoins.ui.detail.sansTitleMedium
import com.michele.eurocoins.ui.theme.PurpleFieldDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusLight
import com.michele.eurocoins.ui.theme.InkLight
import com.michele.eurocoins.ui.theme.PurpleFieldLight
import com.michele.eurocoins.ui.theme.appBarColors
import com.michele.eurocoins.ui.theme.linkColor
import java.text.NumberFormat
import java.util.Locale

/**
 * Dettaglio di un taglio: stessa struttura del dettaglio Commemorative
 * (`CoinDetailScreen.kt`, building block `DetailCard`/`SectionLabel`/`OwnedBadge`/`FooterLine`/
 * `ValueLabel`/`DetailsSection` esportati da lì e riusati qui). **DETAILS (zecca fisica/incisore/
 * disegnatore) viene da Numista per (serie, taglio)** (`RegularIssueImage.zeccaFisicaRaw`/...):
 * "—" dove Numista non ha il campo (il disegnatore manca quasi sempre) o non ha il type
 * (Bulgaria). La descrizione del singolo taglio ("ABOUT THIS COIN", `NotesCard` riusata da
 * Commemorative) sta dopo COLLECTION; compare solo se esiste. La descrizione della SERIE non è
 * qui: sta solo nella schermata del paese (`RegularIssueCountryScreen`), scelta dell'utente.
 *
 * **MINTAGES è diversa da Commemorative**: lì un anno = una tiratura per qualità; qui una serie
 * copre più anni, quindi ogni qualità può avere una tiratura DIVERSA per anno (vedi
 * `RegularIssueImage.tirature`, da Numista abbinate alla serie per anni — vuota solo dove Numista
 * non ha il type). La card compatta mostra la SOMMA per qualità (con didascalia "all years", o l'anno
 * stesso se ce n'è uno solo — mai una somma spacciata per la tiratura di un anno), e un pulsante
 * "View by year" (solo se c'è più di un anno) apre `RegularMintageHistorySheet`: una tabella,
 * un anno per riga in ordine crescente, tre colonne Standard/BU/Proof affiancate come nella card
 * compatta — non tre elenchi separati per qualità (scartato: con molti anni per Standard si
 * sarebbe dovuto scorrere oltre tutti quegli anni prima di arrivare a BU/Proof). Un bottom sheet
 * (non un pannello che si espande in pagina) perché è modale: l'unica superficie che scorre
 * mentre è aperto è lui stesso, senza l'ambiguità di due scroll attivi insieme (la pagina sotto e
 * un riquadro con altezza fissa dentro) — scelta dopo un giro di mockup con l'utente.
 *
 * "Zoom" è la card foto grande di
 * questa schermata stessa (come nel dettaglio Commemorative, che non ha un dialog di
 * ingrandimento separato — vedi CLAUDE.md § Dettaglio moneta, "Tocco sulla foto per ingrandirla:
 * non c'è nel dettaglio").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularDenominationDetailScreen(
    viewModel: RegularDenominationDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var showMintageHistory by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var viewport by remember { mutableStateOf<Rect?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(state.series?.let { "${state.countryName} · Series ${state.seriesNumber}" } ?: state.countryName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val series = state.series
        val image = state.image
        if (series == null || image == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        if (showSheet) {
            RegularCollectionSheet(
                countryName = state.countryName,
                series = series,
                seriesNumber = state.seriesNumber,
                denomination = image,
                currentItems = state.items,
                onSave = { entries ->
                    viewModel.onSaveCollection(entries)
                    showSheet = false
                },
                onDismiss = { showSheet = false },
            )
        }
        if (showMintageHistory) {
            RegularMintageHistorySheet(
                countryName = state.countryName,
                seriesNumber = state.seriesNumber,
                denomination = image,
                onDismiss = { showMintageHistory = false },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onGloballyPositioned { viewport = it.boundsInWindow() }
                .verticalScroll(scrollState),
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DenominationHero(image)
                RegularMintageCard(image = image, onViewByYear = { showMintageHistory = true })
                RegularCollectionCard(items = state.items, onEdit = { showSheet = true })
                image.descrizione?.let { NotesCard(it, scrollState, { viewport }, label = "ABOUT THIS COIN") }
                DenominationCreditFooter(image)
            }
        }
    }
}

/** Hero card bianca: foto grande del taglio (o segnaposto) + etichetta sotto. Stessa forma di `CoinHero`. */
@Composable
private fun DenominationHero(image: RegularIssueImage) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (image.urlImmagineFonte == null) {
                DenominationHeroFallback(message = "Image not yet published by the source")
            } else {
                val context = LocalContext.current
                SubcomposeAsyncImage(
                    // Stesso ritaglio del margine delle miniature (RegularIssueImageTrim): senza,
                    // la moneta risulterebbe più piccola del riquadro, con l'alone bianco attorno.
                    model = ImageRequest.Builder(context)
                        .data(image.urlImmagineFonte)
                        .transformations(RegularIssueImageTrim)
                        .build(),
                    contentDescription = image.taglio,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (painter.state.collectAsState().value) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        is AsyncImagePainter.State.Error -> DenominationHeroFallback(message = "Couldn't load this image")
                        else -> Unit
                    }
                }
            }
        }
        Text(
            text = image.taglio,
            style = sansTitleMedium(),
            fontWeight = FontWeight.Bold,
            color = InkLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun DenominationHeroFallback(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.EuroSymbol,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(0.4f).aspectRatio(1f),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            color = InkLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
        )
    }
}

/**
 * MINTAGES + DETAILS: stessa card unica di `MintageCard` in `CoinDetailScreen.kt`. DETAILS
 * (Mint/Engraver/Designer, `DetailsSection` riusata) legge i campi Numista del taglio, "—" se mancano.
 * MINTAGES invece è dinamica su `image.tirature` (vedi il commento in
 * cima al file per il perché somma+sheet invece di un numero solo): ogni colonna mostra la somma
 * della qualità con una didascalia piccola sotto ("all years" con più annate complete, "N of M years" se mancano anni, l'anno stesso con
 * una sola, nulla se la qualità non ha alcun dato — coerente col trattino della card). "View by
 * year" compare solo se ci sono almeno due anni distinti in totale: con un solo anno la somma
 * coincide già col dato di quell'anno, un pulsante per aprire una tabella da una riga sola
 * sarebbe solo attrito.
 */
@Composable
private fun RegularMintageCard(image: RegularIssueImage, onViewByYear: () -> Unit) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.ENGLISH) }
    val distinctYears = remember(image) { image.tirature.map { it.anno }.distinct().size }
    DetailCard {
        SectionLabel("MINTAGES")
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            CoinQuality.entries.forEach { quality ->
                val summary = remember(image, quality) { summarizeMintages(image.tirature, quality) }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ValueLabel(
                        value = summary.total?.let { if (summary.isSum) formatApproxTotal(it) else numberFormat.format(it) } ?: NO_VALUE,
                        label = quality.label,
                        shrinkToFit = true,
                    )
                    summary.caption?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
        if (distinctYears > 1) {
            TextButton(
                onClick = onViewByYear,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
            ) {
                Text("View by year")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
        SectionLabel("DETAILS")
        Spacer(Modifier.height(10.dp))
        DetailsSection(
            mint = image.displayMint() ?: NO_VALUE,
            engraver = image.displayEngraver() ?: NO_VALUE,
            designer = image.displayDesigner() ?: NO_VALUE,
        )
    }
}

/**
 * Tabella di sola lettura delle tirature per anno: un anno per riga in ordine crescente, tre
 * colonne Standard/BU/Proof come nella card compatta ("—" dove quella qualità non ha un dato
 * quell'anno). Bottom sheet come `RegularCollectionSheet`, ma senza bozza/Save: qui non si scrive
 * nulla, solo si legge — vedi il commento in cima al file per il perché di uno sheet invece di un
 * pannello in pagina. Include solo gli anni con almeno un dato: non serve un range esplicito di
 * inizio/fine serie, una serie 2009-2015 semplicemente non ha righe fuori da quell'intervallo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegularMintageHistorySheet(
    countryName: String,
    seriesNumber: Int,
    denomination: RegularIssueImage,
    onDismiss: () -> Unit,
) {
    val rows = remember(denomination) { groupMintagesByYear(denomination.tirature) }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.ENGLISH) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Text(
                text = "MINTAGES BY YEAR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = denomination.taglio,
                style = sansTitleMedium(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = "$countryName · Series $seriesNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                MintageHeaderCell("Year", modifier = Modifier.weight(0.6f), center = false)
                CoinQuality.entries.forEach { MintageHeaderCell(it.label, modifier = Modifier.weight(it.columnWeight()), center = true) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(rows, key = { it.first }) { (year, values) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(0.6f),
                        )
                        CoinQuality.entries.forEach { quality ->
                            Text(
                                text = values[quality]?.let(numberFormat::format) ?: NO_VALUE,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(quality.columnWeight()),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun MintageHeaderCell(text: String, modifier: Modifier = Modifier, center: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = modifier,
    )
}

/**
 * Card COLLECTION: stessa forma di `CollectionCard` in `CoinDetailScreen.kt`, ma le righe sono
 * una per (qualità, anno) invece che una per qualità — qui la stessa qualità può avere più
 * annate, a differenza delle commemorative dove l'anno è fisso dal dataset.
 */
@Composable
private fun RegularCollectionCard(items: List<RegularCollectionItem>, onEdit: () -> Unit) {
    if (items.isEmpty()) {
        DetailCard {
            SectionLabel("COLLECTION")
            Text(
                text = "Not in your collection yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add to collection")
            }
        }
        return
    }

    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(14.dp)
    val accent = if (dark) PurpleFieldDark else PurpleFieldLight
    val inkColor = if (dark) PurpleFieldFocusDark else PurpleFieldFocusLight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(2.dp, colors.primary, shape)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("COLLECTION", modifier = Modifier.weight(1f))
            OwnedBadge(dark)
        }
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.sortedWith(compareBy({ it.quality.ordinal }, { it.anno })).forEach { item ->
                val cents = item.priceCents?.takeIf { it > 0 }
                val rowShape = RoundedCornerShape(14.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(rowShape)
                        .background(colors.surface)
                        .border(2.dp, accent, rowShape)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${item.quality.label} · ${item.anno}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = cents?.let { "€${formatPrice(it)}" } ?: NO_VALUE,
                        style = sansTitleMedium(),
                        fontWeight = FontWeight.Bold,
                        color = if (cents != null) inkColor else inkColor.copy(alpha = 0.7f),
                    )
                }
            }
        }
        TextButton(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit collection")
        }
    }
}


/**
 * Crediti nel formato comune ([SourceCredits]). Testo del taglio e immagine possono avere fonti
 * diverse (vedi `RegularIssueImage.fonteDati`): se coincidono (testo BCE di ripiego + immagine
 * BCE) una voce sola, altrimenti due. "Data: Numista N#…" con il link al type compare ogni volta
 * che esiste un type: copre testo, tirature, zecca, incisore e disegnatore, ed è il minimo che
 * chiedono i Termini API (§4) — non va tolto. Il testo introduttivo della serie (EC) non è in
 * questa schermata: è accreditato dove compare, in `RegularIssueCountryScreen`.
 */
@Composable
private fun DenominationCreditFooter(image: RegularIssueImage) {
    val imageSource = image.fonteDati.takeIf { it.isNotBlank() }
    val textSource = image.descrizioneFonte?.takeIf { it == "ecb" }
    val sources = buildList {
        when {
            imageSource != null && textSource == imageSource ->
                add(SourceItem("Source", displaySourceName(imageSource), image.urlImmagineFonte))
            else -> {
                textSource?.let { add(SourceItem("Text", displaySourceName(it))) }
                imageSource?.let { add(SourceItem("Image", displaySourceName(it), image.urlImmagineFonte)) }
            }
        }
        image.numistaId?.let { add(SourceItem("Data", "Numista N#$it", numistaUrl(it))) }
    }
    val license = listOfNotNull(
        "License: ${image.displayImageLicense()}",
        image.attribuzioneImmagineRaw?.let { "Credit: $it" },
    ).joinToString(" · ")
    SourceCredits(sources = sources, licenseLine = license)
}

/**
 * Larghezza relativa delle colonne del pannello per anno: Standard più larga perché è l'unica che
 * arriva ai miliardi (4.000.000.000 della Germania 2002, 13 caratteri); BU e Proof per anno
 * restano sotto il milione. Con tre colonne uguali la cifra andava a capo a metà.
 */
private fun CoinQuality.columnWeight(): Float = if (this == CoinQuality.STANDARD) 1.5f else 1f
