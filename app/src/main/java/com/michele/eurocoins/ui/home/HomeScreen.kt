package com.michele.eurocoins.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.R
import com.michele.eurocoins.ui.components.CollectionProgressBar
import com.michele.eurocoins.ui.components.ProgressAnimation
import com.michele.eurocoins.ui.components.rememberProgressAnimation

/**
 * Schermata d'ingresso: due schede di pari peso che si dividono l'altezza. Oggi solo le
 * commemorative sono navigabili (l'unico dataset che la pipeline produce);
 * "Regular Issues" è tratteggiata e senza azione finché quella fonte non
 * esiste — vedi CLAUDE.md § Scopo del progetto nel repo della pipeline dati.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCommemorativeClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    // Animazione della barra: onda + conteggio da 0 solo al primo avvio del processo
    // (lastShownOwned del ViewModel ancora nullo); tornando da un'altra schermata o se il
    // valore cambia a schermata aperta, anima solo la differenza dal valore mostrato l'ultima
    // volta. "playIntro" congela la scelta "primo avvio o no" alla prima composizione: se nel
    // frattempo l'animazione segna il ViewModel, non deve cambiare a metà.
    val playIntro = remember { viewModel.lastShownOwned == null }
    val progressAnimation = rememberProgressAnimation(
        owned = state.progress.owned,
        ready = state.progress.total > 0,
        playIntro = playIntro,
        lastShown = viewModel.lastShownOwned,
        onShown = { viewModel.lastShownOwned = it },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CommemorativeCard(
                state = state,
                progressAnimation = progressAnimation,
                onClick = onCommemorativeClick,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            RegularIssuesCard(modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

private val CardShape = RoundedCornerShape(22.dp)

/** Ingrandimento delle foto dentro il cerchio: taglia l'anello bianco dello sfondo del JPEG BCE. */
private const val PHOTO_ZOOM = 1.05f

/** Sovrapposizione tra monete vicine nella fascia. */
private val BandOverlap = 8.dp

/** Proporzioni dei diametri delle 4 monete: le centrali più grandi. */
private val BandRatios = listOf(0.8f, 1f, 1f, 0.8f)

/**
 * Fascia di monete a bordo scheda, sopra un velo leggero. Prende tutta l'altezza che avanza
 * (la scheda ha altezza fissa, testi a parte): le monete crescono con la fascia, ma senza
 * uscire in larghezza. Ogni moneta è composta da [coin].
 */
@Composable
private fun CoinBand(veil: Color, modifier: Modifier = Modifier, coin: @Composable (index: Int, size: Dp) -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().background(veil), contentAlignment = Alignment.Center) {
        val fitWidth = (maxWidth - 24.dp + BandOverlap * (BandRatios.size - 1)) / BandRatios.sum()
        val base = minOf(maxHeight * 0.82f, fitWidth)
        Row(
            horizontalArrangement = Arrangement.spacedBy(-BandOverlap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BandRatios.forEachIndexed { i, ratio -> coin(i, base * ratio) }
        }
    }
}

/** Una statistica centrata sulla propria colonna: numero grande sopra, etichetta sotto. */
@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Le tre colonne di statistiche: stesse larghezze in entrambe le schede (la terza, per l'intervallo di anni, è più larga). */
@Composable
private fun StatsRow(color: Color, coins: String, countries: String, years: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Stat(coins, "coins", color, Modifier.weight(1f))
        Stat(countries, "countries", color, Modifier.weight(1f))
        Stat(years, "years", color, Modifier.weight(1.3f))
    }
}

/** Struttura condivisa: fascia di monete (occupa lo spazio che avanza), titolo, statistiche, footer. */
@Composable
private fun CardContent(
    band: @Composable (Modifier) -> Unit,
    title: String,
    titleColor: Color,
    titleTrailing: @Composable () -> Unit,
    stats: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        band(Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    modifier = Modifier.weight(1f),
                )
                titleTrailing()
            }
            stats()
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp), contentAlignment = Alignment.CenterStart) { footer() }
        }
    }
}

@Composable
private fun CommemorativeCard(
    state: HomeUiState,
    progressAnimation: ProgressAnimation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onFill = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
    ) {
        CardContent(
            band = { bandModifier ->
                CoinBand(veil = onFill.copy(alpha = 0.14f), modifier = bandModifier) { i, size ->
                    Box(modifier = Modifier.size(size).clip(CircleShape)) {
                        val url = state.showcase.getOrNull(i)?.urlImmagineFonte
                        if (url != null) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = PHOTO_ZOOM, scaleY = PHOTO_ZOOM),
                            )
                        }
                    }
                }
            },
            title = "Commemorative",
            titleColor = onFill,
            titleTrailing = {},
            stats = {
                val range = if (state.firstYear != null && state.lastYear != null) "${state.firstYear}–${state.lastYear}" else "—"
                StatsRow(onFill, "${state.progress.total}", "${state.countries}", range)
            },
            footer = {
                CollectionProgressBar(
                    progress = state.progress,
                    color = onFill,
                    // Track e etichetta più leggibili (tema scuro: tile salvia con testo scuro): track al
                    // 45% invece di 30%, etichetta 12 sp SemiBold invece di 11 sp Normal.
                    trackColor = onFill.copy(alpha = 0.45f),
                    labelColor = onFill,
                    labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    animation = progressAnimation,
                )
            },
        )
    }
}

/** Tagli mostrati (disegnati, non foto) nella fascia di Regular Issues, in ordine crescente. */
private class DrawnCoin(val label: String, val outer: Color, val inner: Color?)

private val RegularCoins = listOf(
    DrawnCoin("1c", Color(0xFFC08A6B), null), // rame
    DrawnCoin("10c", Color(0xFFD3B56C), null), // oro nordico
    DrawnCoin("1€", Color(0xFFD9DBD9), Color(0xFFD3B56C)), // bimetallica: anello argento, centro oro
    DrawnCoin("2€", Color(0xFFD3B56C), Color(0xFFD9DBD9)), // bimetallica: anello oro, centro argento
)

/**
 * Moneta disegnata (segnaposto delle serie divisionali finché non ci sono foto): colori dei metalli
 * veri ma attenuati (alpha), con bordo rilevato e taglio in mezzo.
 */
@Composable
private fun RegularCoin(coin: DrawnCoin, size: Dp) {
    val ink = Color(0xFF3B4A42)
    Box(
        modifier = Modifier
            .size(size)
            .alpha(0.62f)
            .clip(CircleShape)
            .background(coin.outer)
            .border(size * 0.03f, ink.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Cerchio interno sottile: bordo rilevato delle monete monometalliche, centro delle bimetalliche.
        Box(
            modifier = Modifier
                .size(size * 0.66f)
                .clip(CircleShape)
                .background(coin.inner ?: coin.outer)
                .border(size * 0.02f, ink.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                coin.label,
                color = ink,
                fontWeight = FontWeight.Bold,
                fontSize = with(LocalDensity.current) { (size * 0.24f).toSp() },
            )
        }
    }
}

@Composable
private fun RegularIssuesCard(modifier: Modifier = Modifier) {
    // Tratteggio: nel tema scuro il 50% del colore del testo su fondo quasi nero risultava troppo
    // debole; al 75% si vede bene. Nel tema chiaro resta al 50%.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = muted.copy(alpha = if (isDark) 0.75f else 0.5f)
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(muted.copy(alpha = 0.10f))
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16.dp.toPx(), 10.dp.toPx())),
                    ),
                )
            },
    ) {
        CardContent(
            // Monete disegnate e desaturate (finché non ci sono foto): un disco per taglio, con l'etichetta.
            band = { bandModifier ->
                CoinBand(veil = muted.copy(alpha = 0.07f), modifier = bandModifier) { i, size ->
                    RegularCoin(RegularCoins[i], size)
                }
            },
            title = "Regular Issues",
            titleColor = muted,
            // Pillola a contorno e a basso contrasto: non compete con la scheda attiva.
            titleTrailing = {
                Text(
                    "Coming soon",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = muted,
                    modifier = Modifier
                        .border(1.dp, outline, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            },
            // Stessi campi di Commemorative, senza dati finché la pipeline non produce il dataset.
            stats = { StatsRow(muted, "—", "—", "—") },
            // Specchio della barra di Commemorative (stessa altezza di testo e traccia), vuoto e senza dati.
            footer = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val label = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("— / — collected", style = label, color = muted)
                        Text("—", style = label, color = muted)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(15.dp)
                            .clip(RoundedCornerShape(7.5.dp))
                            .background(muted.copy(alpha = 0.22f)),
                    )
                }
            },
        )
    }
}
