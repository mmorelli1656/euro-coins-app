package com.michele.eurocoins.ui.regular

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.michele.eurocoins.ui.components.EfsHint
import com.michele.eurocoins.ui.components.EfsInfoDialog
import com.michele.eurocoins.data.NO_YEAR
import com.michele.eurocoins.data.YearOption
import com.michele.eurocoins.data.chipLabel
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.pager.rememberPagerState
import com.michele.eurocoins.ui.detail.PagePositionPill
import com.michele.eurocoins.ui.detail.StackedPager
import kotlinx.coroutines.flow.drop
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.transformations
import com.michele.eurocoins.ui.components.DetailSharpen
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.MintLetterTable
import com.michele.eurocoins.data.formatCompactMintage
import com.michele.eurocoins.data.mintLetterCity
import com.michele.eurocoins.data.mintLetterTable
import com.michele.eurocoins.ui.components.LocalMintLetters
import com.michele.eurocoins.ui.components.keepScrollInside
import com.michele.eurocoins.ui.components.protectFromSheetDrag
import com.michele.eurocoins.ui.components.LocalProAccess
import com.michele.eurocoins.ui.components.LocalShowMints
import com.michele.eurocoins.ui.components.MintLetterBadges
import com.michele.eurocoins.data.NO_MINT
import com.michele.eurocoins.data.mintLettersFor
import com.michele.eurocoins.ui.components.MintLettersProPrompt
import com.michele.eurocoins.ui.components.ProLockedRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.michele.eurocoins.data.MintLevel
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.anyPurchaseDate
import com.michele.eurocoins.ui.components.InfoNote
import com.michele.eurocoins.ui.components.PurchaseDateLine
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.displayCoinDescription
import com.michele.eurocoins.data.designerCredit
import com.michele.eurocoins.data.displayImageLicense
import com.michele.eurocoins.data.displayMint
import com.michele.eurocoins.data.displaySourceName
import com.michele.eurocoins.data.YearMintLabel
import com.michele.eurocoins.data.YearMintPart
import com.michele.eurocoins.data.numistaUrl
import com.michele.eurocoins.data.yearMintLabels
import com.michele.eurocoins.data.yearMintParts
import com.michele.eurocoins.data.formatCardMintage
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
import com.michele.eurocoins.ui.detail.HeroOwnedBadge
import com.michele.eurocoins.ui.detail.HeroPlaceholder
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
/**
 * Dettaglio a pagine: scorrendo a sinistra e a destra si passa alla riga successiva/precedente della lista da cui
 * si è arrivati ([pages], nell'ordine di quella lista: gli 8 tagli di una serie, un taglio in tutti i paesi o
 * tutta la scheda All), senza aggiungere voci alla cronologia. Stesso pager, stessa animazione "mazzo" e stesso
 * contatore "4 / 12" del dettaglio commemorativo (`CoinDetailScreen`, `DetailPager.kt`); barra del titolo e
 * contatore seguono la pagina corrente. [viewModelFor] dà il ViewModel di una pagina (stessa chiave = stessa
 * istanza, per la pagina e per il titolo); [onPageShown] è chiamata quando si ferma una pagina diversa da quella
 * di partenza, per far scorrere la lista di origine su quella riga al ritorno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularDenominationDetailScreen(
    pages: List<RegularPageKey>,
    initialPage: RegularPageKey,
    viewModelFor: @Composable (RegularPageKey) -> RegularDenominationDetailViewModel,
    onPageShown: (RegularPageKey) -> Unit,
    onBack: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = pages.indexOf(initialPage).coerceAtLeast(0)) { pages.size }
    val currentState by viewModelFor(pages[pagerState.currentPage]).uiState.collectAsState()
    val currentOnPageShown by rememberUpdatedState(onPageShown)
    LaunchedEffect(pagerState) {
        // drop(1): la prima emissione è la pagina di partenza, la lista è già lì.
        snapshotFlow { pagerState.settledPage }.drop(1).collect { currentOnPageShown(pages[it]) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = {
                    Text(
                        currentState.series?.let { "${currentState.countryName} · Series ${currentState.seriesNumber}" }
                            ?: currentState.countryName,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pages.size > 1) PagePositionPill("${pagerState.currentPage + 1} / ${pages.size}")
                },
            )
        },
    ) { padding ->
        StackedPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            key = { pages[it].id },
        ) { page ->
            RegularDenominationDetailPage(viewModelFor(pages[page]))
        }
    }
}

/** Una pagina del dettaglio: il taglio di [viewModel], con il proprio scorrimento verticale e i propri pannelli. */
@Composable
private fun RegularDenominationDetailPage(viewModel: RegularDenominationDetailViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    // Anno scelto nella card COLLECTION quando si apre "Edit collection": il pannello si apre su quello.
    var sheetYear by remember { mutableStateOf<YearOption?>(null) }
    var showMintageHistory by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var viewport by remember { mutableStateOf<Rect?>(null) }

    val series = state.series
    val image = state.image
    if (series == null || image == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    if (showSheet) {
        RegularCollectionSheet(
            countryName = state.countryName,
            series = series,
            seriesNumber = state.seriesNumber,
            denomination = image,
            currentItems = state.items,
            onSave = { entries, editableMints ->
                viewModel.onSaveCollection(entries, editableMints)
                showSheet = false
            },
            onDismiss = { showSheet = false },
            initialYear = sheetYear,
        )
    }
    if (showMintageHistory) {
        RegularMintageHistorySheet(
            countryName = state.countryName,
            paese = series.paese,
            seriesNumber = state.seriesNumber,
            denomination = image,
            onDismiss = { showMintageHistory = false },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { viewport = it.boundsInWindow() }
            .verticalScroll(scrollState),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DenominationHero(image, owned = state.items.isNotEmpty())
            RegularMintageCard(image = image, onViewByYear = { showMintageHistory = true })
            RegularCollectionCard(
                items = state.items,
                onEdit = { year ->
                    sheetYear = year
                    showSheet = true
                },
            )
            image.displayCoinDescription()?.let { NotesCard(it, scrollState, { viewport }) }
            DenominationCreditFooter(image)
        }
    }
}

/** Hero card bianca: foto grande del taglio (o segnaposto) + etichetta sotto. Stessa forma di `CoinHero`. */
@Composable
private fun DenominationHero(image: RegularIssueImage, owned: Boolean) {
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
                HeroPlaceholder("Image not yet published by the source")
            } else {
                val context = LocalContext.current
                SubcomposeAsyncImage(
                    // Stesso ritaglio del margine delle miniature (RegularIssueImageTrim): senza,
                    // la moneta risulterebbe più piccola del riquadro, con l'alone bianco attorno.
                    model = ImageRequest.Builder(context)
                        .data(image.urlImmagineFonte)
                        .transformations(RegularIssueImageTrim, DetailSharpen)
                        .build(),
                    contentDescription = image.taglio,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (painter.state.collectAsState().value) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        is AsyncImagePainter.State.Error -> HeroPlaceholder("Couldn't load this image")
                        else -> Unit
                    }
                }
            }
            if (owned) HeroOwnedBadge(modifier = Modifier.align(Alignment.TopEnd))
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
    val distinctYears = remember(image) { image.tirature.map { it.anno }.distinct().size }
    // Compatta ("14.2 B", "10.1 M", "19.0 k") solo se "View by year" esiste: lì i numeri sono esatti (vedi formatCardMintage).
    val compact = distinctYears > 1
    DetailCard {
        SectionLabel("MINTAGES")
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            CoinQuality.entries.forEach { quality ->
                val summary = remember(image, quality) { summarizeMintages(image.tirature, quality) }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ValueLabel(
                        value = summary.total?.let { formatCardMintage(it, compact) } ?: NO_VALUE,
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
        // Nessuna tiratura in nessuna qualità (Bulgaria, che Numista non ha, e le serie senza dati): i tre trattini
        // da soli sembrano un errore, la nota dice che è un dato che manca.
        if (image.tirature.isEmpty()) {
            InfoNote(
                text = "Mintage data isn't available yet.",
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
            )
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
        val designer = image.designerCredit()
        DetailsSection(
            mint = image.displayMint() ?: NO_VALUE,
            designer = designer?.name ?: NO_VALUE,
            designerProbable = designer?.probable == true,
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
    paese: String,
    seriesNumber: Int,
    denomination: RegularIssueImage,
    onDismiss: () -> Unit,
) {
    val rows = remember(denomination) { groupMintagesByYear(denomination.tirature) }
    // etichetta "Mint · …" a ogni cambio di zecca; mappa vuota (nessuna etichetta) per i tagli a
    // zecca unica o senza dati — vedi yearMintLabels
    val mintLabels = remember(denomination) {
        yearMintLabels(denomination.zecchePerAnno, rows.map { it.first })
    }
    // anni divisi tra zecche di paesi diversi (oggi Grecia 2002): il totale resta nella riga
    // dell'anno, le parti sotto — vedi yearMintParts
    val parts = remember(denomination) {
        denomination.zecchePerAnno.associate { it.anno to yearMintParts(it) }.filterValues { it.isNotEmpty() }
    }
    val entries = remember(rows, mintLabels, parts) {
        buildList {
            var previous: YearMintLabel? = null
            rows.forEach { (year, values) ->
                val label = mintLabels[year]
                if (label != null && label.sameAs != previous?.sameAs) add(YearTableEntry.Mint(year, label))
                previous = label
                add(YearTableEntry.Row(year, values))
                parts[year]?.forEach { add(YearTableEntry.Part(year, it)) }
            }
        }
    }
    val hasProbable = mintLabels.values.any { it.level == MintLevel.PROBABLE }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.ENGLISH) }

    // Zecche affiancate (Pro, oggi solo la Germania: variante C del mockup del 2026-10-11): una colonna per
    // lettera, una qualità alla volta. Dove il taglio non ha lettere `letterTable` è nulla e vale la tabella dei
    // totali di sempre; senza Pro la tabella dei totali resta e sopra c'è la riga che invita al Pro.
    val access = LocalProAccess.current
    val mintLetters = LocalMintLetters.current
    var letterQuality by remember { mutableStateOf(CoinQuality.STANDARD) }
    var showProPrompt by remember { mutableStateOf(false) }
    // "German mints" spento: nessuna tabella a zecche affiancate e nemmeno la riga che invita al Pro.
    val showMints = LocalShowMints.current
    val letterTable = remember(mintLetters, denomination, rows, letterQuality, showMints) {
        if (showMints) mintLetterTable(mintLetters, paese, denomination.taglio, rows.map { it.first }, letterQuality) else null
    }
    val matrix = if (access.isPro) letterTable else null
    if (showProPrompt) {
        MintLettersProPrompt(onDismiss = { showProPrompt = false }, onGoPro = access.onGoPro)
    }

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
            // Dal titolo in giù il pannello non si trascina: la zona da cui si chiude trascinando e' solo la
            // maniglia con titolo e sottotitolo (prima partiva dalla riga "Year", dove il dito arrivava facilmente).
            Column(modifier = Modifier.protectFromSheetDrag()) {
                if (letterTable != null && !access.isPro) {
                    ProLockedRow(
                        text = "Go Pro to split by mint (${letterTable.letters.joinToString(", ")})",
                        onClick = { showProPrompt = true },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                if (matrix == null) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        MintageHeaderCell("Year", modifier = Modifier.weight(0.6f), center = false)
                        CoinQuality.entries.forEach { MintageHeaderCell(it.label, modifier = Modifier.weight(it.columnWeight()), center = true) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    LazyColumn(modifier = Modifier.keepScrollInside().heightIn(max = 360.dp)) {
                        items(entries, key = { it.key }) { entry ->
                            when (entry) {
                                is YearTableEntry.Mint -> MintPeriodLabel(entry.label)
                                is YearTableEntry.Row -> {
                                    MintageValuesRow(
                                        label = entry.year.toString(),
                                        values = entry.values,
                                        numberFormat = numberFormat,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                }
                                // parte di un anno diviso: più piccola e spenta, su fondo leggermente
                                // diverso, così si legge come dettaglio del totale della riga sopra
                                is YearTableEntry.Part -> {
                                    MintageValuesRow(
                                        label = entry.part.country,
                                        values = entry.part.values,
                                        numberFormat = numberFormat,
                                        small = true,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                            .padding(vertical = 5.dp),
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                    if (parts.isNotEmpty()) {
                        Text(
                            text = "Split by mint where the source reports it. BU pieces made for sets may be counted with the national mint.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (hasProbable) {
                        Text(
                            text = "? Probable: from Wikipedia or inferred from official data, not confirmed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    MintLetterMatrix(table = matrix, paese = paese, quality = letterQuality, onQuality = { letterQuality = it })
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * Le zecche di un taglio affiancate (Pro): una riga per anno, una colonna per lettera (A D F G J), i valori di UNA
 * qualità scelta con il selettore in cima. Pensata per rispondere a "quale delle cinque è la più rara quest'anno?":
 * i numeri sono compatti e sempre a UNA cifra decimale ("119.4 M", "960.0 M", "85.0 k": coerenza e più spazio tra
 * le colonne, scelta del proprietario) perché si confrontano tra loro; il totale dell'anno è l'ultima colonna.
 * Nessuna evidenziazione della più bassa: provata in bronzo nel mockup e scartata (colore nuovo che nell'app non
 * esiste per i dati). Nessun segmento "Sets" (i pezzi venduti solo in set, bucket `altro`): proposto e scartato dal
 * proprietario il 2026-10-11, la tabella resta Standard / BU / Proof come il resto dell'app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MintLetterMatrix(
    table: MintLetterTable,
    paese: String,
    quality: CoinQuality,
    onQuality: (CoinQuality) -> Unit,
) {
    val qualities = CoinQuality.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        qualities.forEachIndexed { index, option ->
            SegmentedButton(
                selected = quality == option,
                onClick = { onQuality(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = qualities.size),
                // Nessuna spunta: sposterebbe l'etichetta fuori centro (come negli altri selettori dell'app).
                icon = {},
                label = { Text(option.label) },
            )
        }
    }
    // La griglia resta a font NORMALE (2026-11-02): ha colonne a larghezza fissa e con il font al 130% "2002" andava a capo ("200" / "2") e
    // i numeri si tagliavano ("367.9 N"). Il selettore sopra e la legenda sotto continuano a seguire il font.
    val gridDensity = LocalDensity.current.let { Density(it.density, fontScale = 1f) }
    CompositionLocalProvider(LocalDensity provides gridDensity) {
    MatrixLine(
        modifier = Modifier.padding(bottom = 2.dp),
        verticalPadding = 4.dp,
        year = { MintageHeaderCell("Year", modifier = Modifier, center = false) },
        cells = table.letters.map { letter -> { MintageHeaderCell(letter, modifier = Modifier, center = true) } },
        total = { MintageHeaderCell("Total", modifier = Modifier, center = true) },
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    LazyColumn(modifier = Modifier.keepScrollInside().heightIn(max = 360.dp)) {
        items(table.rows, key = { it.anno }) { row ->
            MatrixLine(
                verticalPadding = 8.dp,
                year = {
                    Text(
                        text = row.anno.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                cells = row.values.map { value -> { MatrixNumber(value, FontWeight.Normal) } },
                // Il totale dell'anno è l'ultima colonna, su un velo chiaro (mockup del 2026-10-11, variante C):
                // sotto l'anno "stonava" come elemento diverso dalla griglia. È la somma delle zecche che hanno
                // il dato (i "—" accanto dicono quali): per questo non serve dire "N di 5".
                total = { MatrixNumber(row.total, FontWeight.Medium) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
    }
    // Legenda: la lettera è sulla moneta, la città no.
    Text(
        text = table.letters.joinToString(" · ") { letter -> mintLetterCity(paese, letter)?.let { "$letter $it" } ?: letter },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    // Sotto Standard il "—" non è un dato mancante: la lista degli ordini di conio del Ministero delle finanze
    // (Bundesbank, 2025) non ha nessun ordine per quei tagli e anni, cioè non furono coniati per la circolazione
    // (la Germania aveva scorte; in quegli anni solo set da collezione). Verificato su tutti i 62 anni vuoti.
    if (quality == CoinQuality.STANDARD) {
        Text(
            text = "— means none were struck for circulation.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// 11 sp e anno a 38 dp: a 12 sp "800.0 M" riempiva la colonna e toccava i filetti (visto sul telefono, 375 dp).
private val MATRIX_NUMBER_SIZE = 11.sp
private val MATRIX_YEAR_WIDTH = 38.dp
private val MATRIX_RULE_WIDTH = 0.5.dp

/** Righe della tabella "by year": una per anno, più l'etichetta di zecca dove cambia. */
private sealed interface YearTableEntry {
    val key: String

    class Mint(val year: Int, val label: YearMintLabel) : YearTableEntry {
        override val key get() = "m$year"
    }

    class Row(val year: Int, val values: Map<CoinQuality, Long>) : YearTableEntry {
        override val key get() = "y$year"
    }

    class Part(val year: Int, val part: YearMintPart) : YearTableEntry {
        override val key get() = "p$year-${part.country}"
    }
}

/**
 * Riga della tabella "by year": etichetta (anno, o paese della zecca per le parti) a sinistra e
 * tre colonne Standard/BU/Proof. Le parti ([small]) hanno il testo più piccolo, spento e rientrato.
 */
@Composable
private fun MintageValuesRow(
    label: String,
    values: Map<CoinQuality, Long>,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    val base = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    val color = if (small) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = base,
            color = color,
            fontWeight = if (small) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f).padding(start = if (small) 8.dp else 0.dp),
        )
        CoinQuality.entries.forEach { quality ->
            Text(
                text = values[quality]?.let(numberFormat::format) ?: NO_VALUE,
                style = base.copy(fontFeatureSettings = "tnum"),
                color = color,
                fontWeight = if (small) FontWeight.Normal else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(quality.columnWeight()),
            )
        }
    }
}

/**
 * "Mint · Finland" sopra il primo anno di ogni periodo con la stessa zecca. Pillola (etichetta,
 * non azione) nel colore `primary` come le etichette di sezione; il probabile resta in corsivo
 * col "?" (mai uguale al certo), il non noto è spento con solo il bordo.
 */
@Composable
private fun MintPeriodLabel(label: YearMintLabel) {
    val colors = MaterialTheme.colorScheme
    val unknown = label.level == MintLevel.UNKNOWN
    val textColor = if (unknown) colors.onSurfaceVariant else colors.primary
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 2.dp)
            .clip(shape)
            .then(
                if (unknown) Modifier.border(1.dp, colors.outline.copy(alpha = 0.6f), shape)
                else Modifier.background(colors.primary.copy(alpha = 0.14f)),
            )
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = "Mint · ",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
        Text(
            text = label.text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            fontStyle = if (label.level == MintLevel.PROBABLE) FontStyle.Italic else FontStyle.Normal,
            color = textColor,
        )
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
 * Card COLLECTION: stessa forma di `CollectionCard` in `CoinDetailScreen.kt` (righe per finitura con
 * data e prezzo), ma qui la stessa moneta può essere in più ANNI (e nella varietà EFS), a differenza
 * delle commemorative dove l'anno è fisso dal dataset. Con un solo anno (o varietà) è identica alla
 * card delle commemorative, con l'anno nell'etichetta di ogni riga ("Standard · 2008"). Con più anni
 * (scelta dell'utente dopo quattro mockup: la lista piatta di finitura × anno × varietà arrivava a 30
 * righe, ~1.700 dp, con 10 anni e 3 finiture) in cima ci sono gli ANNI come chip, nello stesso stile
 * del selettore dell'anno del pannello, e sotto le righe di sempre solo per l'anno scelto: l'altezza
 * non cresce con il numero di anni e date e prezzi restano visibili. Anni in ordine cronologico, si apre sull'ultimo aggiunto;
 * oltre i primi 8 gli anni stanno dietro "+N". "Edit collection" apre il pannello su
 * quell'anno.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegularCollectionCard(items: List<RegularCollectionItem>, onEdit: (YearOption?) -> Unit) {
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
                onClick = { onEdit(null) },
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
    // Altezza delle righe uguale per TUTTI gli anni (se una sola data esiste in qualunque anno): cambiando
    // anno la card non deve saltare.
    val anyDate = anyPurchaseDate(items.map { it.purchasedOn })
    // Anni (e varietà) in collezione in ordine CRONOLOGICO, dal più vecchio: come la griglia degli anni del
    // pannello e il "also: …" accanto, e prevedibile (un selettore di anni non deve avere un ordine casuale).
    val years = remember(items) {
        items.map { YearOption(it.anno, it.variety) }.distinct()
            .sortedWith(compareBy<YearOption> { it.year }.thenBy { it.variety })
    }
    // L'anno scelto all'apertura è l'ULTIMO AGGIUNTO (`addedAt` più recente; a parità, l'anno più recente: un
    // salvataggio con più anni nuovi li marca tutti con lo stesso istante): salvando un anno, il dettaglio
    // mostra subito quello. Si ricalcola solo se cambia l'insieme degli anni, quindi modificare un anno già
    // presente non sposta la scelta.
    var selected by remember(years) {
        val latest = items.maxWith(compareBy<RegularCollectionItem> { it.addedAt }.thenBy { it.anno })
        mutableStateOf(YearOption(latest.anno, latest.variety))
    }
    // Se l'anno scelto sparisce (tolto dal pannello) si torna al più recente invece di mostrare niente.
    val current = selected.takeIf { it in years } ?: years.first()
    val showYears = years.size > 1
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
        // La riga di aiuto dell'EFS compare solo con la varietà scelta (anche se è l'unico anno, senza chip): la stessa
        // riga e la stessa spiegazione del pannello di collezione.
        var showVarietyInfo by remember { mutableStateOf(false) }
        if (showVarietyInfo) EfsInfoDialog(taglio = items.first().taglio, onDismiss = { showVarietyInfo = false })
        if (showYears) {
            // Con tanti anni i primi [YearChipsCollapsed] e un chip "+N" per mostrarli tutti ("Less" per
            // richiuderli). Numero fisso e non calcolato dal layout: `FlowRowOverflow` legge `shownItemCount`
            // fuori dalla fase di disegno e fa crashare l'app. Un anno scelto oltre i primi è sempre visibile.
            // Stesso stile dei chip del selettore dell'anno e del pannello FILTER: rettangoli con angoli
            // morbidi, scelto in lilla.
            var expanded by remember { mutableStateOf(false) }
            val collapsible = years.size > YearChipsCollapsed + 1
            val showAll = !collapsible || expanded || years.indexOf(current) >= YearChipsCollapsed
            val shown = if (showAll) years else years.take(YearChipsCollapsed)
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shown.forEach { year ->
                    FilterChip(
                        selected = year == current,
                        onClick = { selected = year },
                        label = { Text(year.chipLabel()) },
                    )
                }
                if (collapsible) {
                    AssistChip(
                        onClick = {
                            if (showAll) {
                                expanded = false
                                selected = years.first()
                            } else {
                                expanded = true
                            }
                        },
                        label = { Text(if (showAll) "Less" else "+${years.size - shown.size}") },
                    )
                }
            }
        }
        if (current.variety.isNotEmpty()) {
            EfsHint(
                taglio = items.first().taglio,
                onClick = { showVarietyInfo = true },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        // Le cinque zecche dell'anno scelto (oggi la Germania): cerchi pieni dove la moneta è posseduta. Si vedono
        // con il Pro, o dove l'utente ha già registrato una lettera (un Pro scaduto non le nasconde).
        val paese = items.first().paese
        val mintLetters = remember(paese) { mintLettersFor(paese) }
        if (LocalShowMints.current && mintLetters.isNotEmpty() && (LocalProAccess.current.isPro || items.any { it.mint != NO_MINT })) {
            MintLetterBadges(
                paese = paese,
                letters = mintLetters,
                owned = items.filter { YearOption(it.anno, it.variety) == current && it.mint != NO_MINT }.map { it.mint }.toSet(),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Column(
            modifier = Modifier.padding(top = 12.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items
                .filter { !showYears || YearOption(it.anno, it.variety) == current }
                .sortedWith(compareBy({ it.quality.ordinal }, { it.anno }, { it.variety }, { it.mint }))
                .forEach { item ->
                    val cents = item.priceCents?.takeIf { it > 0 }
                    val rowShape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (anyDate) 58.dp else 44.dp)
                            .clip(rowShape)
                            .background(colors.surface)
                            .border(2.dp, accent, rowShape)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                // L'anno è già nel chip scelto: nell'etichetta solo con un anno solo, dove i chip non ci sono.
                                text = buildString {
                                    append(item.quality.label)
                                    if (!showYears) append(" · ${YearOption(item.anno, item.variety).chipLabel()}")
                                    if (item.mint != NO_MINT) append(" · ${item.mint}")
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface,
                            )
                            // La data di QUESTA finitura di QUESTA annata: come nelle commemorative, la seconda riga
                            // c'è su tutte le righe se almeno una ha la data ("No date" in grigio dove manca).
                            if (anyDate) PurchaseDateLine(item.purchasedOn)
                        }
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
            onClick = { onEdit(current) },
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit collection")
        }
    }
}

/** Chip degli anni mostrati prima del "+N": circa due righe su un telefono da 375 dp. */
private const val YearChipsCollapsed = 8

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
    // Un taglio senza immagine (serie 2026 del Vaticano) non ha licenza da mostrare.
    val license = if (image.licenzaImmagine.isBlank()) null else listOfNotNull(
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

/**
 * Una riga della tabella a zecche affiancate (anche l'intestazione): anno, una cella per lettera e il totale, con
 * un filetto verticale tra le colonne delle zecche e tra queste e il totale, che sta su un velo chiaro (la stessa
 * tinta delle righe "parte" della Grecia). Variante C del mockup del 2026-10-11, scelta dal proprietario perché la
 * separazione tra le colonne "era ancora poco netta". Tra anno e prima zecca non c'è filetto: l'anno è l'etichetta
 * della riga, non una colonna di dati.
 */
@Composable
private fun MatrixLine(
    verticalPadding: Dp,
    year: @Composable () -> Unit,
    cells: List<@Composable () -> Unit>,
    total: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rule = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier.width(MATRIX_YEAR_WIDTH).fillMaxHeight().padding(vertical = verticalPadding),
            contentAlignment = Alignment.CenterStart,
        ) { year() }
        cells.forEachIndexed { index, cell ->
            if (index > 0) VerticalDivider(thickness = MATRIX_RULE_WIDTH, color = rule)
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = verticalPadding),
                contentAlignment = Alignment.Center,
            ) { cell() }
        }
        VerticalDivider(thickness = MATRIX_RULE_WIDTH, color = rule)
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().background(tint).padding(vertical = verticalPadding),
            contentAlignment = Alignment.Center,
        ) { total() }
    }
}

@Composable
private fun MatrixNumber(value: Long?, weight: FontWeight) {
    Text(
        text = value?.let(::formatCompactMintage) ?: NO_VALUE,
        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum", fontSize = MATRIX_NUMBER_SIZE),
        fontWeight = weight,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
    )
}
