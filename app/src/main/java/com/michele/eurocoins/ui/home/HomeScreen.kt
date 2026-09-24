package com.michele.eurocoins.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.michele.eurocoins.R
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.ui.components.CollectionProgressBar
import com.michele.eurocoins.ui.components.ProgressAnimation
import com.michele.eurocoins.ui.components.rememberProgressAnimation

/**
 * Schermata d'ingresso: due tile che si dividono l'altezza. Oggi solo le
 * commemorative sono navigabili (l'unico dataset che la pipeline produce);
 * la tile divisionale è tratteggiata e senza azione finché quella fonte non
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CommemorativeTile(
                state = state,
                progressAnimation = progressAnimation,
                onClick = onCommemorativeClick,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth(),
            )
            CirculationTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CommemorativeTile(
    state: HomeUiState,
    progressAnimation: ProgressAnimation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val onFill = MaterialTheme.colorScheme.onPrimary
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Showcase(state.showcase)
        }
        Spacer(Modifier.height(16.dp))

        Column {
            Text("Commemorative", style = MaterialTheme.typography.headlineMedium, color = onFill)
            Text(
                text = "${state.progress.total} coins · ${state.countries} countries",
                style = MaterialTheme.typography.bodyLarge,
                color = onFill,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (state.firstYear != null && state.lastYear != null) {
                Text(
                    text = "${state.firstYear} – ${state.lastYear}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onFill,
                )
            }
            Spacer(Modifier.height(16.dp))
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
        }
    }
}

/** Mosaico 3 colonne di monete reali (paesi diversi): riempie lo spazio sopra il titolo. */
@Composable
private fun Showcase(coins: List<Coin>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        coins.chunked(SHOWCASE_COLUMNS).forEach { rowCoins ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCoins.forEach { coin ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape),
                    ) {
                        // Nessun fondo sotto la foto: il 20% di onPrimary (bianco nel tema chiaro) sporgeva
                        // come sfrangiatura chiara sul bordo tondo. Lo zoom leggero taglia l'anello bianco
                        // che lo sfondo del JPEG BCE lascia tra la moneta e il cerchio.
                        AsyncImage(
                            model = coin.urlImmagineFonte,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = SHOWCASE_ZOOM, scaleY = SHOWCASE_ZOOM),
                        )
                    }
                }
                // Ultima riga incompleta: mantiene le monete della stessa dimensione.
                repeat(SHOWCASE_COLUMNS - rowCoins.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val SHOWCASE_COLUMNS = 3

/** Ingrandimento delle monete del mosaico dentro il cerchio (vedi [Showcase]). */
private const val SHOWCASE_ZOOM = 1.05f

@Composable
private fun CirculationTile(modifier: Modifier = Modifier) {
    // Tratteggio: nel tema scuro il 50% del colore del testo su fondo quasi nero risultava troppo
    // debole; al 75% si vede bene. Nel tema chiaro resta al 50%.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.75f else 0.5f)
    Column(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16.dp.toPx(), 10.dp.toPx())),
                    ),
                )
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text("Circulation", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "Coming soon",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
