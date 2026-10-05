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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import com.michele.eurocoins.data.anyPurchaseDate
import com.michele.eurocoins.ui.components.PurchaseDateLine
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.displayDesigner
import com.michele.eurocoins.data.displayEngraver
import com.michele.eurocoins.data.displayImageLicense
import com.michele.eurocoins.data.displaySourceName
import com.michele.eurocoins.data.numistaUrl
import com.michele.eurocoins.data.displayMint
import com.michele.eurocoins.ui.components.CollectionSheet
import com.michele.eurocoins.ui.components.formatPrice
import com.michele.eurocoins.ui.theme.BackgroundDark
import com.michele.eurocoins.ui.theme.InkLight
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
            // (MINTAGES, COLLECTION, ABOUT THIS COIN) e i crediti in una riga in fondo.
            // Margini di 16 dp, 12 dp tra le card.
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CoinHero(currentCoin)
                MintageCard(currentCoin)
                CollectionCard(items = items, onEdit = { showSheet = true })
                currentCoin.noteStoriche?.let { NotesCard(it, scrollState, viewport = { viewport }) }
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
            if (hasImage) {
                SubcomposeAsyncImage(
                    model = coin.urlImmagineFonte,
                    contentDescription = coin.tema,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Icona dell'euro solo se la foto non c'è o non si carica ("non ancora pubblicata" e
                    // "non caricata ora" si distinguono dal testo, vedi scripts/validate_image_links.py
                    // nella pipeline dati). Mentre la foto arriva resta la sola card bianca.
                    when (painter.state.collectAsState().value) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        is AsyncImagePainter.State.Error -> CoinHeroFallback(
                            icon = Icons.Filled.EuroSymbol,
                            message = "Couldn't load this image",
                        )
                        else -> Unit
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
            modifier = Modifier.fillMaxWidth(0.4f).aspectRatio(1f),
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

/**
 * Superficie delle card del dettaglio: colore del tema, bordo da 1 dp, angoli 16 dp.
 * Esportata (non più `private`): riusata da `RegularDenominationDetailScreen`, stesso linguaggio
 * visivo per il dettaglio dei tagli di Regular Issues.
 */
@Composable
fun DetailCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
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

/** Etichetta maiuscola comune a tutte le card del dettaglio. Esportata, vedi [DetailCard]. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
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
fun sansTitleMedium(): TextStyle =
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
 *
 * Esportata e con etichetta parametrica: `RegularDenominationDetailScreen` la riusa tale e quale
 * per la descrizione del singolo taglio ("ABOUT THIS COIN") — stesso componente e stesso
 * comportamento, non una copia.
 */
@Composable
fun NotesCard(
    note: String,
    scrollState: ScrollState,
    viewport: () -> Rect?,
    label: String = "ABOUT THIS COIN",
) {
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
        SectionLabel(label)
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

/**
 * Una sola card per le informazioni tecniche: MINTAGES sopra, un filetto leggero, DETAILS
 * (zecca fisica e incisore) sotto — non due card separate (risparmia bordo/padding e riusa lo
 * stesso posto in cui MINTAGES ha già un blocco opzionale, l'avviso sul contingente autorizzato).
 */
@Composable
private fun MintageCard(coin: Coin) {
    DetailCard {
        SectionLabel("MINTAGES")
        Spacer(Modifier.height(10.dp))
        MintageSection(coin)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
        SectionLabel("DETAILS")
        Spacer(Modifier.height(10.dp))
        DetailsSection(coin.displayMint() ?: NO_VALUE, coin.displayEngraver() ?: NO_VALUE, coin.displayDesigner() ?: NO_VALUE)
    }
}

/**
 * Zecca fisica, incisore e disegnatore — mai nascosti (stesso criterio di MINTAGES: "—" se il
 * dato manca, non si distingue da "il dato non esiste"). Engraver e Designer sono ruoli distinti,
 * non l'uno il ripiego dell'altro — 25 monete su 584 li hanno entrambi valorizzati con persone
 * diverse, vedi [displayEngraver]. **Non una griglia a 3 colonne pari** (scartata dopo un
 * mockup): la zecca era spesso un nome istituzionale lungo (es. "State Mint of Stuttgart / State
 * Mints of Baden-Württemberg" per la Lettonia, che usa zecche tedesche in subappalto) e si
 * schiacciava in un terzo di card; ora è il nome del paese della zecca ("Germany", vedi
 * `displayMint`), ma la struttura resta. **Mint su una riga intera** (va a capo leggibile su tutta la
 * larghezza), **Engraver/Designer affiancati sotto** in 2 colonne (nomi di persona, quasi sempre
 * corti): una sola riga in più rispetto alla griglia a 3, non il triplo come la variante a righe
 * impilate scartata per lo stesso motivo (allungava troppo la card).
 *
 * Presi come stringhe già pronte (non un `Coin`) ed esportata (non più `private`): riusata da
 * `RegularDenominationDetailScreen` con tre `NO_VALUE` fissi, perché il dataset Regular Issues
 * non ha questi campi per taglio — vedi CLAUDE.md § Regular Issues.
 */
@Composable
fun DetailsSection(mint: String, engraver: String, designer: String) {
    ValueLabel(
        value = mint,
        // "Mints" con più paesi ("Finland, Netherlands"): sono le zecche usate nel tempo, non
        // quelle di ogni singola moneta — nessun nome di paese contiene una virgola
        label = if (mint.contains(", ")) "Mints" else "Mint",
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(14.dp))
    // altezza comune alle due colonne (IntrinsicSize.Min = quella della colonna più alta, di
    // solito quella con un nome su 2 righe): senza, "Engraver"/"Designer" finivano a quote
    // diverse quando uno dei due mancava (un trattino su una riga contro un nome su due) e il
    // trattino restava incollato in alto invece che centrato accanto al nome presente.
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        listOf(
            "Engraver" to engraver,
            "Designer" to designer,
        ).forEach { (label, value) ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = value,
                        style = sansTitleMedium(),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Valore in evidenza sopra, etichetta piccola sotto, entrambi centrati — usato da MINTAGES e
 * DETAILS. Esportata, vedi [DetailsSection].
 */
@Composable
fun ValueLabel(value: String, label: String, modifier: Modifier = Modifier, shrinkToFit: Boolean = false) {
    // `shrinkToFit`: per i numeri in colonne di larghezza fissa (MINTAGES delle Regular Issues: la
    // somma di Standard può arrivare a 12.475.760.000, 14 caratteri). Una riga sola, corpo ridotto
    // in proporzione oltre i 12 caratteri che stanno a corpo pieno in una colonna da un terzo:
    // andare a capo spezzava la cifra a metà ("12,475,760,0" / "00").
    val baseStyle = sansTitleMedium()
    // "≈ 7.87 B": il simbolo a sinistra spostava le CIFRE a destra dell'asse su cui sono centrate le
    // etichette sotto (Standard/BU/Proof). Si bilancia con lo stesso "≈ " in coda, trasparente: la
    // stringa resta simmetrica e le cifre cadono sull'asse. Il testo letto da TalkBack è l'originale.
    val display = if (value.startsWith("≈ ")) {
        buildAnnotatedString {
            append(value)
            withStyle(SpanStyle(color = Color.Transparent)) { append(" ≈") }
        }
    } else {
        AnnotatedString(value)
    }
    val style = if (shrinkToFit && display.length > ShrinkAfterChars) {
        baseStyle.copy(fontSize = baseStyle.fontSize * (ShrinkAfterChars.toFloat() / display.length))
    } else {
        baseStyle
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = display,
            style = style,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = if (shrinkToFit) 1 else 2,
            softWrap = !shrinkToFit,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = value },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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

const val NO_VALUE = "—"

/**
 * Crediti in piccolo in fondo, nel formato comune a tutti i dettagli ([SourceCredits]): testo e
 * immagine dalla stessa fonte (BCE) in una voce sola con il link all'immagine, "Data: Numista
 * N#…" con il link al type (tirature, zecca, incisore: l'attribuzione con N# la chiedono i
 * Termini API e prima mancava qui), e sotto la licenza e il credito dell'immagine. Sempre
 * visibili (attribuzione).
 */
@Composable
private fun ImageCreditFooter(coin: Coin) {
    val sources = buildList {
        add(SourceItem("Source", displaySourceName(coin.fonteDati), coin.urlImmagineFonte))
        coin.numistaId?.let { add(SourceItem("Data", "Numista N#$it", numistaUrl(it))) }
    }
    val license = listOfNotNull(
        "License: ${coin.displayImageLicense()}",
        coin.attribuzioneImmagineRaw?.let { "Credit: $it" },
    ).joinToString(" · ")
    SourceCredits(sources = sources, licenseLine = license)
}

@Composable
fun FooterLine(text: String) {
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
    val shape = RoundedCornerShape(14.dp)
    // Bordo verdigris (stato "posseduta", come badge OWNED e spunte); righe e pillole lilla/viola (finiture).
    val accent = if (dark) PurpleFieldDark else PurpleFieldLight
    val inkColor = if (dark) PurpleFieldFocusDark else PurpleFieldFocusLight
    val anyDate = anyPurchaseDate(items.map { it.purchasedOn })
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
        // Solo le finiture possedute: una pillola per riga, nome a sinistra e prezzo a destra
        // (trattino se assente o 0,00, per tenere la colonna allineata). Solo bordo viola, mai
        // fondo pieno (era LilacLight): la card ha già il bordo verde "posseduta" e il badge
        // OWNED, un terzo blocco di colore pieno competeva con quei due segnali. Bordo pieno
        // (non più al 40% di opacità) e più spesso (2 dp, era 1): su fondo bianco un bordo
        // sottile e sbiadito si vedeva poco. Etichetta nel colore normale del testo, prezzo
        // resta viola: è l'unico dato numerico della riga e mantenerlo colorato lo fa leggere
        // a colpo d'occhio anche col fondo bianco.
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CoinQuality.entries.mapNotNull { q -> items.firstOrNull { it.quality == q } }.forEach { item ->
                val cents = item.priceCents?.takeIf { it > 0 }
                // Rettangolo a 14 dp come le altre card del dettaglio e la FinishCard del pannello
                // di modifica, non più una pillola ovale: la riga non è cliccabile (solo "Edit
                // collection" apre il pannello), e in tutta l'app la pillola è riservata a
                // controlli azionabili (pulsanti, filtri, selettori) o etichette/badge — usarla
                // qui per un dato statico suggeriva "si tocca" quando non è così.
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
                            text = item.quality.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface,
                        )
                        // La data di QUESTA finitura. Se almeno una finitura ne ha una, tutte le righe hanno la
                        // seconda riga ("No date" in grigio dove manca), così le card hanno la stessa altezza;
                        // se nessuna ha la data la card resta com'era, senza "No date" ripetuto per niente.
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
            onClick = onEdit,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit collection")
        }
    }
}

/** Pillola "✓ OWNED": verde scuro con testo bianco (tema chiaro), invertita nello scuro. Esportata, vedi [DetailCard]. */
@Composable
fun OwnedBadge(dark: Boolean) {
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

/** Caratteri oltre i quali [ValueLabel] con `shrinkToFit` riduce il corpo per restare su una riga. */
private const val ShrinkAfterChars = 12
