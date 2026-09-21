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
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.michele.eurocoins.R
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.ui.components.CollectionProgressBar
import com.michele.eurocoins.ui.components.ThemeModePill
import com.michele.eurocoins.ui.theme.ThemeMode

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
    /** Iniziale dell'account Google collegato, null se non ha fatto l'accesso. */
    accountInitial: String?,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCommemorativeClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    ThemeModePill(mode = themeMode, onModeChange = onThemeModeChange)
                    ProfileButton(accountInitial, onProfileClick)
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

/** Icona profilo: cerchio con l'iniziale se l'utente ha fatto l'accesso, altrimenti l'icona generica. */
@Composable
private fun ProfileButton(initial: String?, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (initial == null) {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile and backup", modifier = Modifier.size(32.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CommemorativeTile(state: HomeUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                trackColor = onFill.copy(alpha = 0.3f),
                labelColor = onFill,
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                    ) {
                        AsyncImage(
                            model = coin.urlImmagineFonte,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
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

@Composable
private fun CirculationTile(modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
