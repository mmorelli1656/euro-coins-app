package com.michele.eurocoins.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.michele.eurocoins.ui.theme.BackgroundDark
import com.michele.eurocoins.ui.theme.InkLight
import com.michele.eurocoins.ui.theme.LilacLight
import com.michele.eurocoins.ui.theme.PurpleFieldDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusLight
import com.michele.eurocoins.ui.theme.VerdigrisDark
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
            // Hero card bianca (foto + titolo), poi tre card con la stessa etichetta maiuscola
            // (OFFICIAL MINTAGES, COLLECTION, HISTORICAL NOTES) e i crediti in una riga in fondo.
            // Margini di 16 dp, 12 dp tra le card.
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CoinHero(currentCoin)
                MintageCard(currentCoin)
                CollectionCard(items = items, onEdit = { showSheet = true })
                currentCoin.noteStoriche?.let { NotesCard(it) }
                ImageCreditFooter(currentCoin)
            }
        }
    }
}

/**
 * Hero card: foto della moneta grande e titolo sotto, nello stesso riquadro bianco puro (angoli
 * 24 dp). Lo sfondo delle foto BCE è bianco puro (255,255,255), quindi la moneta sembra
 * appoggiata sulla card. Bianco FISSO, non del tema: il bianco crema mostrerebbe il bordo della
 * foto; testi scuri fissi di conseguenza anche nel tema scuro. Il titolo è sempre intero: le
 * righe in più allungano la card, che non ha altezza fissa.
 */
@Composable
private fun CoinHero(coin: Coin) {
    val hasImage = !coin.immaginePlaceholder && coin.urlImmagineFonte != null
    val shape = RoundedCornerShape(24.dp)
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
            if (hasImage) {
                SubcomposeAsyncImage(
                    model = coin.urlImmagineFonte,
                    contentDescription = coin.tema,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
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
        // Titolo sempre per intero (anche su 3 righe): niente ellissi né espansione.
        Text(
            text = coin.tema,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = InkLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
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

/** Etichetta maiuscola comune a tutte le card del dettaglio. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = linkColor(),
        modifier = modifier,
    )
}

@Composable
private fun NotesCard(note: String) {
    DetailCard {
        SectionLabel("HISTORICAL NOTES")
        Text(
            text = note,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MintageCard(coin: Coin) {
    DetailCard {
        SectionLabel("OFFICIAL MINTAGES")
        Spacer(Modifier.height(10.dp))
        MintageSection(coin)
    }
}

/**
 * Tiratura in una griglia a 3 colonne Standard / BU / Proof con filetti verticali: la cifra
 * in evidenza sopra, l'etichetta della finitura centrata sotto.
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
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) VerticalDivider(color = MaterialTheme.colorScheme.outline)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Una sola voce compatta: fonte · licenza · credito (l'attribuzione resta sempre visibile).
        val parts = listOfNotNull(
            "Data source: ${coin.fonteDati.uppercase(Locale.ENGLISH)}",
            "Image license: ${coin.displayImageLicense()}",
            coin.attribuzioneImmagineRaw?.let { "Credit: $it" },
        )
        // Il link alla fonte è un'icona discreta accanto ai crediti (tocco da 48 dp), non più un
        // testo sottolineato: l'attribuzione resta visibile e il link raggiungibile.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f, fill = false)) { FooterLine(parts.joinToString(" · ")) }
            coin.urlImmagineFonte?.let { url ->
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

@Composable
private fun FooterLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

/**
 * Card della collezione. Non posseduta: card neutra con messaggio centrato e "Add to
 * collection" pieno (l'unica azione piena della schermata). Posseduta: contenitore tinto di
 * verdigris, badge OWNED in alto a destra, una riga chiave-valore per finitura (nome a sinistra,
 * prezzo in viola a destra) e "Edit collection" compatto (36 dp visivi, 48 dp di tocco) a destra.
 * Lo stesso pannello dell'elenco si apre da entrambi i pulsanti.
 */
@Composable
private fun CollectionCard(items: List<CollectionItem>, onEdit: () -> Unit) {
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
    val shape = RoundedCornerShape(16.dp)
    val accent = colors.primary
    val priceColor = if (dark) PurpleFieldFocusDark else PurpleFieldFocusLight
    val pillColor = if (dark) PurpleFieldDark.copy(alpha = 0.25f) else LilacLight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.5.dp, accent, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("COLLECTION", modifier = Modifier.weight(1f))
            OwnedBadge(dark)
        }
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CoinQuality.entries.mapNotNull { q -> items.firstOrNull { it.quality == q } }.forEach { item ->
                val rowShape = RoundedCornerShape(12.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(rowShape)
                        .background(colors.surface)
                        .border(1.dp, colors.outline, rowShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.quality.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    // Prezzo assente o 0,00: trattino discreto, così la colonna resta allineata.
                    val cents = item.priceCents?.takeIf { it > 0 }
                    if (cents != null) {
                        Text(
                            text = "€${formatPrice(cents)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = priceColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(pillColor)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    } else {
                        Text(
                            text = NO_VALUE,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
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

/** Pillola "✓ OWNED": verde scuro con testo bianco (tema chiaro), invertita nello scuro. */
@Composable
private fun OwnedBadge(dark: Boolean) {
    val bg = if (dark) VerdigrisDark else Color(0xFF2F4A38)
    val fg = if (dark) BackgroundDark else Color.White
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = "OWNED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = fg)
    }
}
