package com.michele.eurocoins.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.michele.eurocoins.ui.theme.appBarColors
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.displayImageLicense
import com.michele.eurocoins.ui.components.CollectionSheet
import com.michele.eurocoins.ui.components.formatPrice
import com.michele.eurocoins.ui.theme.InkLight
import com.michele.eurocoins.ui.theme.linkColor
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    viewModel: CoinDetailViewModel,
    onBack: () -> Unit,
) {
    val coin by viewModel.coin.collectAsState()
    val items by viewModel.items.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(coin?.let { "${it.displayCountry()} · ${it.anno}" } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val currentCoin = coin
        if (currentCoin == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        if (showSheet) {
            CollectionSheet(
                coin = currentCoin,
                currentItems = items,
                onSave = { entries ->
                    viewModel.onSaveCollection(entries)
                    showSheet = false
                },
                onDismiss = { showSheet = false },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            CoinHero(currentCoin)

            // Una card con i dati della moneta, un banner per la collezione (l'unica azione) e in
            // fondo i crediti in piccolo: margini di 16 dp come la card della foto.
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CoinDetailsCard(currentCoin)
                CollectionBanner(items = items, onEdit = { showSheet = true })
                ImageCreditFooter(currentCoin)
            }
        }
    }
}

@Composable
private fun CoinHero(coin: Coin) {
    val hasImage = !coin.immaginePlaceholder && coin.urlImmagineFonte != null
    // Un solo riquadro: card bianca pura (angoli 24 dp, margine 16 dp), senza un secondo
    // riquadro dentro. Lo sfondo delle foto BCE è bianco puro (255,255,255), quindi la
    // moneta sembra appoggiata direttamente sulla card. Bianco FISSO, non del tema: il
    // bianco crema delle card mostrerebbe il bordo della foto; testi scuri fissi di
    // conseguenza anche nel tema scuro. Il bordo da 1 dp la stacca dal fondo chiaro.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage) {
            SubcomposeAsyncImage(
                model = coin.urlImmagineFonte,
                contentDescription = coin.tema,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(24.dp),
            ) {
                // Distinto da "immagine non ancora pubblicata" qui sotto:
                // qui il link c'era ma il caricamento è fallito ora
                // (rete/link morto) — vedi
                // scripts/validate_image_links.py nella pipeline dati.
                if (painter.state.value is AsyncImagePainter.State.Error) {
                    CoinHeroFallback(
                        icon = Icons.Filled.BrokenImage,
                        message = "Couldn't load this image",
                    )
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        } else {
            CoinHeroFallback(
                icon = Icons.Filled.MonetizationOn,
                message = "Image not yet published by the source",
            )
        }
    }
}

@Composable
private fun CoinHeroFallback(icon: ImageVector, message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.aspectRatio(1f).fillMaxWidth(0.25f),
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

/** Superficie delle card del dettaglio: colore del tema, bordo da 1 dp, angoli 16 dp. */
@Composable
private fun DetailCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(16.dp),
        content = content,
    )
}

/**
 * Card unica con i dati della moneta: titolo, mintage, divisore e note storiche. Il paese non
 * c'è: è già nell'header ("Andorra · 2025"). Lo spazio sopra e sotto il divisore è lo stesso
 * (16 dp). "Data source" è nel piè di pagina insieme agli altri crediti.
 */
@Composable
private fun CoinDetailsCard(coin: Coin) {
    DetailCard {
        Text(text = coin.tema, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))
        MintageSection(coin)

        coin.noteStoriche?.let { note ->
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(text = "Historical notes", style = MaterialTheme.typography.titleMedium)
            Text(
                text = note,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * Tiratura in una lista compatta Standard / BU / Proof: etichette e cifre in due colonne
 * vicine (cifre allineate a destra, così le migliaia si incolonnano).
 *
 * Standard/BU/Proof vengono da Numista (`tiraturaNumista*`), fonte indipendente da quella BCE
 * (`Coin.tiratura`) e mai fusa con essa. Il numero BCE va sulla riga Standard solo se Numista
 * non ha NESSUN dato per quella moneta (né standard, né BU, né proof): se invece Numista
 * distingue BU/proof ma non ha una riga standard, di solito è perché la moneta non ne ha una
 * (tiratura interamente divisa tra BU e proof) — cadere comunque sul numero BCE mostrerebbe
 * quella somma come una terza tiratura inventata. Non
 * mostrata una quarta riga "Other" (tirature Numista con commento non classificabile, es.
 * lotti in rotoli o coincard): a volte è la maggioranza della tiratura reale, ma per ora si
 * preferisce mostrare solo valori certi — vedi NOTES.md della pipeline.
 */
@Composable
private fun MintageSection(coin: Coin) {
    val numberFormat = NumberFormat.getIntegerInstance(Locale.ENGLISH)
    // Il ripiego sul numero BCE ha senso solo se Numista non sa nulla di questa moneta (né
    // standard, né BU, né proof): se invece Numista distingue BU/proof ma non ha una riga
    // "standard" a parte, di solito è perché quella moneta non ne ha una — l'intera tiratura è
    // divisa tra BU e proof (es. San Marino 2013: BU 110 000 + proof 5 000, BCE 115 000 totale,
    // nessuna finitura standard). Cadere comunque sul numero BCE in quel caso mostrerebbe quella
    // somma come se fosse una terza tiratura distinta — trovato confrontando i due dati a mano.
    val standardFromBce = coin.tiraturaNumistaStandard == null &&
        coin.tiraturaNumistaBu == null &&
        coin.tiraturaNumistaProof == null &&
        coin.tiratura != null
    val standard = if (standardFromBce) coin.tiratura else coin.tiraturaNumistaStandard
    val rows = listOf(
        CoinQuality.STANDARD.label to (standard?.let(numberFormat::format) ?: NO_VALUE),
        CoinQuality.BU.label to (coin.tiraturaNumistaBu?.let(numberFormat::format) ?: NO_VALUE),
        CoinQuality.PROOF.label to (coin.tiraturaNumistaProof?.let(numberFormat::format) ?: NO_VALUE),
    )
    Text(
        text = "MINTAGE",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column {
            rows.forEach { (label, _) ->
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            rows.forEach { (_, value) ->
                Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    // Il "contingente autorizzato" è un pattern osservato sul numero BCE (vedi NOTES.md):
    // si applica solo quando la riga Standard mostra davvero quel numero, non quando Numista
    // ha un proprio valore indipendente per la finitura standard.
    if (standardFromBce && coin.paese in PAESI_TIRATURA_SOSPETTA) {
        Text(
            text = "For ${coin.displayCountry()}, this figure is most likely the country's " +
                "authorized quota for the period, not the actual mintage of " +
                "this specific coin — see NOTES.md in the data pipeline.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private const val NO_VALUE = "—"

/**
 * Crediti in piccolo in fondo: fonte dei dati, licenza e credito dell'immagine, link alla
 * fonte. Sempre visibili (attribuzione). Testo in inchiostro (≈10:1 sul fondo chiaro); il link
 * usa [linkColor] e la sottolineatura, perché il verdigris del tema (4:1) non raggiunge il 4.5:1
 * WCAG AA per testo piccolo.
 */
@Composable
private fun ImageCreditFooter(coin: Coin) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FooterLine("Data source: ${coin.fonteDati.uppercase(Locale.ENGLISH)}")
        FooterLine("Image license: ${coin.displayImageLicense()}")
        coin.attribuzioneImmagineRaw?.let { FooterLine("Credit: $it") }

        coin.urlImmagineFonte?.let { url ->
            Text(
                text = "Open source image",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                color = linkColor(),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
            )
        }
    }
}

@Composable
private fun FooterLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
}

/**
 * Banner della collezione: l'unica azione della schermata, quindi tinto di verdigris (non
 * bianco come le card di dati) e con il pulsante pieno. "Add"/"Edit" apre lo stesso pannello
 * dell'elenco.
 */
@Composable
private fun CollectionBanner(items: List<CollectionItem>, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent, shape)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "My collection", style = MaterialTheme.typography.titleMedium)
            if (items.isEmpty()) {
                Text(
                    text = "Not in your collection yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            CoinQuality.entries.mapNotNull { quality -> items.firstOrNull { it.quality == quality } }.forEach { item ->
                val price = item.priceCents?.let { " · €${formatPrice(it)}" }.orEmpty()
                Text(text = item.quality.label + price, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Button(onClick = onEdit) { Text(if (items.isEmpty()) "Add" else "Edit") }
    }
}
