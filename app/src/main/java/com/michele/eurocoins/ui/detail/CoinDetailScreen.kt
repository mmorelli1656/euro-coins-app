package com.michele.eurocoins.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.displayTema
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
import com.michele.eurocoins.ui.theme.PurpleFieldLight
import com.michele.eurocoins.ui.theme.PurpleFieldFocusLight
import com.michele.eurocoins.ui.theme.VerdigrisDark
import com.michele.eurocoins.ui.theme.linkColor
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    viewModel: CoinDetailViewModel,
    onBack: () -> Unit,
) {
    val coin by viewModel.coin.collectAsState()
    val items by viewModel.items.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var viewport by remember { mutableStateOf<Rect?>(null) }

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
                .onGloballyPositioned { viewport = it.boundsInWindow() }
                .verticalScroll(scrollState),
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
                currentCoin.noteStoriche?.let { NotesCard(it, scrollState) { viewport } }
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
                    // Stessa icona dell'euro in tutti i casi senza foto (elenco compreso): "non ancora
                    // pubblicata" (dato) e "non caricata ora" (rete assente/link morto, vedi
                    // scripts/validate_image_links.py nella pipeline dati) si distinguono dal testo.
                    // Mentre la foto arriva, o se il caricamento resta in sospeso senza rete, l'icona
                    // senza testo evita un riquadro vuoto.
                    when (painter.state.value) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        is AsyncImagePainter.State.Error -> CoinHeroFallback(
                            icon = Icons.Filled.EuroSymbol,
                            message = "Couldn't load this image",
                        )
                        else -> CoinHeroFallback(icon = Icons.Filled.EuroSymbol, message = null)
                    }
                }
            } else {
                CoinHeroFallback(
                    icon = Icons.Filled.EuroSymbol,
                    message = "Image not yet published by the source",
                )
            }
        }
        // Titolo sempre per intero (anche su 3 righe): niente ellissi né espansione.
        Text(
            text = coin.displayTema(),
            style = sansTitleMedium(),
            fontWeight = FontWeight.Bold,
            color = InkLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun CoinHeroFallback(icon: ImageVector, message: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.aspectRatio(1f).fillMaxWidth(0.225f),
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = InkLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
            )
        }
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

/**
 * Tipografia di questa schermata: solo sans. `titleMedium` del tema è serif (titoli delle altre
 * schermate), qui lo si sostituisce localmente senza toccare il tema.
 */
@Composable
private fun sansTitleMedium(): TextStyle =
    MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Default)
/** Misure del testo delle note, scritte durante il layout (non sono stato osservabile). */
private class NotesMetrics {
    var natural = 0 // altezza naturale del testo con le righe attuali
    var fourLines = 0 // altezza delle prime 4 righe (dal layout a righe piene)
}

/**
 * Note storiche: 4 righe con ellissi; il pulsante "Show more ∨" / "Show less ∧" compare solo se
 * il testo è troncato.
 *
 * L'altezza del testo è animata a mano (non con `animateContentSize`): in chiusura il testo deve
 * restare a righe piene mentre il riquadro si accorcia, altrimenti le righe in più sparivano di
 * colpo e poi restava uno spazio vuoto che si restringeva (lo scatto). `showFull` tiene il
 * testo a righe piene per tutta l'animazione; a fine chiusura torna a 4 righe con ellissi
 * (stessa altezza, quindi senza salti). In espansione la pagina scorre in sincronia per
 * centrare la card.
 */
@Composable
private fun NotesCard(note: String, scrollState: ScrollState, viewport: () -> Rect?) {
    var expanded by rememberSaveable(note) { mutableStateOf(false) }
    var showFull by remember(note) { mutableStateOf(expanded) }
    var animating by remember(note) { mutableStateOf(false) }
    var truncated by remember(note) { mutableStateOf(false) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val metrics = remember(note) { NotesMetrics() }
    val height = remember(note) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val spec = tween<Float>(NotesExpandMillis, easing = FastOutSlowInEasing)

    // In espansione, a ogni fotogramma: porta il centro della card verso il centro della zona
    // visibile (se la card è più alta della zona, il bordo superiore resta a filo con la zona).
    LaunchedEffect(expanded) {
        if (!expanded) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            if ((now - start) / 1_000_000 > NotesExpandMillis + 100) break
            val card = coords?.takeIf { it.isAttached } ?: continue
            val visible = viewport() ?: continue
            val bounds = card.boundsInWindow()
            val delta = minOf(bounds.center.y - visible.center.y, bounds.top - visible.top)
            if (delta > 0f) scrollState.scrollBy(delta)
        }
    }

    fun toggle() {
        if (animating) return
        scope.launch {
            animating = true
            if (!expanded) {
                height.snapTo(metrics.natural.toFloat()) // altezza a 4 righe, senza salti
                val rest = metrics.natural
                showFull = true
                expanded = true
                repeat(6) { if (metrics.natural <= rest) withFrameNanos { } } // attende la misura piena
                height.animateTo(metrics.natural.toFloat(), spec)
            } else {
                expanded = false
                height.snapTo(metrics.natural.toFloat())
                height.animateTo(metrics.fourLines.toFloat(), spec)
                showFull = false
            }
            animating = false
        }
    }

    DetailCard(modifier = Modifier.onGloballyPositioned { coords = it }) {
        SectionLabel("HISTORICAL NOTES")
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Justify,
                lineHeight = 20.sp,
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto,
            ),
            maxLines = if (showFull) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = {
                if (showFull) metrics.fourLines = it.getLineBottom(minOf(3, it.lineCount - 1)).toInt()
                else truncated = it.hasVisualOverflow
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    metrics.natural = placeable.height
                    val h = if (animating) minOf(placeable.height, height.value.roundToInt()) else placeable.height
                    layout(placeable.width, h) { placeable.place(0, 0) }
                },
        )
        if (truncated || expanded) {
            TextButton(
                onClick = { toggle() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(if (expanded) "Show less" else "Show more")
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun MintageCard(coin: Coin) {
    DetailCard {
        SectionLabel("MINTAGES")
        Spacer(Modifier.height(10.dp))
        MintageSection(coin)
    }
}

/**
 * Tiratura in una griglia a 3 colonne Standard / BU / Proof (senza filetti): la cifra
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
    // Tre colonne di larghezza UGUALE (un terzo della card ciascuna), con cifra ed etichetta
    // centrate sull'asse della propria colonna: con colonne larghe quanto il contenuto
    // (SpaceEvenly) quella con la cifra più larga spostava il baricentro e il gruppo sembrava
    // decentrato. Le cifre hanno SEMPRE lo stesso stile, anche "12,600,000" ("tnum": larghezza
    // fissa); se una cifra eccedesse il terzo esce simmetrica da entrambi i lati.
    Row(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { (label, value) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value,
                    style = sansTitleMedium().copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
                )
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
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Default),
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
 * collection" pieno (l'unica azione piena della schermata). Posseduta: card bianca con bordo viola,
 * badge OWNED in alto a destra, una pillola lilla per finitura posseduta (nome a sinistra,
 * prezzo a destra) e "Edit collection" compatto (36 dp visivi, 48 dp di tocco) a destra.
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
    // Viola/lilla dell'app (gli stessi del pannello di modifica): bordo della card e righe.
    val accent = if (dark) PurpleFieldDark else PurpleFieldLight
    val inkColor = if (dark) PurpleFieldFocusDark else PurpleFieldFocusLight
    val pillColor = if (dark) PurpleFieldDark.copy(alpha = 0.25f) else LilacLight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(2.dp, accent, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("COLLECTION", modifier = Modifier.weight(1f))
            OwnedBadge(dark)
        }
        // Solo le finiture possedute: una pillola lilla unica per riga, nome a sinistra e
        // prezzo a destra (trattino se assente o 0,00, per tenere la colonna allineata).
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CoinQuality.entries.mapNotNull { q -> items.firstOrNull { it.quality == q } }.forEach { item ->
                val cents = item.priceCents?.takeIf { it > 0 }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(pillColor)
                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.quality.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = inkColor,
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
        Text(text = "OWNED", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Default), fontWeight = FontWeight.Medium, color = fg)
    }
}

private const val NotesExpandMillis = 495
