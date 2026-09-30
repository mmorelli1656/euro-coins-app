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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.luminance
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
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.displayImageLicense
import com.michele.eurocoins.ui.components.RegularCollectionSheet
import com.michele.eurocoins.ui.components.formatPrice
import com.michele.eurocoins.ui.detail.DetailCard
import com.michele.eurocoins.ui.detail.DetailsSection
import com.michele.eurocoins.ui.detail.FooterLine
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
import java.util.Locale

/**
 * Dettaglio di un taglio: stessa struttura del dettaglio Commemorative
 * (`CoinDetailScreen.kt`, building block `DetailCard`/`SectionLabel`/`OwnedBadge`/`FooterLine`/
 * `ValueLabel`/`DetailsSection` esportati da lì e riusati qui). **MINTAGES e DETAILS (zecca
 * fisica/incisore/disegnatore) restano nella stessa posizione della schermata Commemorative ma
 * SEMPRE vuote (`NO_VALUE`, "—")**: quei dati non esistono per taglio nel dataset Regular Issues
 * (solo a livello di serie c'è `zeccaEmittente`, che è il paese, non una zecca fisica) — vuote e
 * non omesse, per coerenza strutturale con Commemorative e per essere già pronte il giorno in cui
 * la pipeline aggiungesse questi dati anche qui. `descrizione` della serie sta a parte, in ABOUT
 * THIS SERIES. "Zoom" è la card foto grande di questa schermata stessa (come nel dettaglio
 * Commemorative, che non ha un dialog di ingrandimento separato — vedi CLAUDE.md § Dettaglio
 * moneta, "Tocco sulla foto per ingrandirla: non c'è nel dettaglio").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularDenominationDetailScreen(
    viewModel: RegularDenominationDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DenominationHero(image)
                RegularMintageCard()
                RegularCollectionCard(items = state.items, onEdit = { showSheet = true })
                AboutSeriesCard(series)
                DenominationCreditFooter(series, image)
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
 * MINTAGES + DETAILS: stessa card unica di `MintageCard` in `CoinDetailScreen.kt`, con la stessa
 * griglia a 3 colonne (Standard/BU/Proof) e lo stesso `DetailsSection` (Mint/Engraver/Designer),
 * ma tutti i valori sono `NO_VALUE` fissi — il dataset Regular Issues non ha tirature né
 * zecca fisica/incisore/disegnatore per taglio (vedi il commento in cima al file). Card presente
 * comunque, non omessa: stessa scelta di Commemorative per un campo mancante ("—" invece di far
 * sparire la riga, così non si confonde "dato non ancora arrivato" con "sezione che non esiste").
 */
@Composable
private fun RegularMintageCard() {
    DetailCard {
        SectionLabel("MINTAGES")
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            CoinQuality.entries.forEach { quality ->
                ValueLabel(value = NO_VALUE, label = quality.label, modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
        SectionLabel("DETAILS")
        Spacer(Modifier.height(10.dp))
        DetailsSection(mint = NO_VALUE, engraver = NO_VALUE, designer = NO_VALUE)
    }
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
 * Descrizione della serie, per intero: stesso testo (e stesso trattamento tipografico
 * giustificato con sillabazione) già mostrato nell'intestazione di `RegularIssueCountryScreen`,
 * ripetuto qui perché questa schermata può essere raggiunta direttamente. "ABOUT THIS SERIES" e
 * non "HISTORICAL NOTES": non è una nota sulla singola moneta (non esiste, l'anno lo sceglie
 * l'utente), è la descrizione dell'intero disegno di serie.
 */
@Composable
private fun AboutSeriesCard(series: RegularIssueSeries) {
    DetailCard {
        SectionLabel("ABOUT THIS SERIES")
        Text(
            text = series.descrizione,
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Justify,
                lineHeight = 20.sp,
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto,
            ),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Crediti: fonte del testo della serie e fonte dell'immagine possono differire (vedi
 * `RegularIssueImage.fonteDati` in `RegularIssue.kt`), quindi due voci distinte invece della sola
 * "Data source" delle commemorative.
 */
@Composable
private fun DenominationCreditFooter(series: RegularIssueSeries, image: RegularIssueImage) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val parts = listOfNotNull(
            "Text source: ${series.fonteDati.uppercase(Locale.ENGLISH)}",
            image.fonteDati.takeIf { it.isNotBlank() }?.let { "Image source: ${it.uppercase(Locale.ENGLISH)}" },
            "Image license: ${image.displayImageLicense()}",
            image.attribuzioneImmagineRaw?.let { "Credit: $it" },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f, fill = false)) { FooterLine(parts.joinToString(" · ")) }
            image.urlImmagineFonte?.let { url ->
                IconButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open source image",
                        tint = linkColor(),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
