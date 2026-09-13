package com.michele.eurocoins.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.michele.eurocoins.data.Coin
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    viewModel: CoinDetailViewModel,
    onBack: () -> Unit,
) {
    val coin by viewModel.coin.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(coin?.paese?.let { "$it · ${coin?.anno}" } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            CoinHero(currentCoin)

            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = currentCoin.tema, style = MaterialTheme.typography.headlineMedium)

                VerticalGap()

                InfoGrid(currentCoin)

                currentCoin.noteStoriche?.let { note ->
                    VerticalGap()
                    Text(text = "Note storiche", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                VerticalGap()
                HorizontalDivider()
                VerticalGap()

                ImageCreditFooter(currentCoin)
            }
        }
    }
}

@Composable
private fun CoinHero(coin: Coin) {
    val hasImage = !coin.immaginePlaceholder && coin.urlImmagineFonte != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage) {
            AsyncImage(
                model = coin.urlImmagineFonte,
                contentDescription = coin.tema,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(24.dp),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.aspectRatio(1f).fillMaxWidth(0.25f),
                )
                Text(
                    text = "Immagine non ancora pubblicata dalla fonte",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoGrid(coin: Coin) {
    val tiraturaFormatted = coin.tiratura?.let {
        NumberFormat.getIntegerInstance(Locale.ITALY).format(it) + " monete"
    } ?: "Non nota"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InfoRow("Zecca emittente", coin.zeccaEmittente)
        InfoRow("Tiratura", tiraturaFormatted)
        if (coin.paese in PAESI_TIRATURA_SOSPETTA && coin.tiratura != null) {
            Text(
                text = "Per ${coin.paese} questo numero è probabilmente il " +
                    "contingente autorizzato del periodo, non la tiratura " +
                    "reale di questa moneta — vedi NOTES.md nella pipeline dati.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }
        InfoRow("Fonte dati", coin.fonteDati.uppercase(Locale.ITALY))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(Locale.ITALY),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ImageCreditFooter(coin: Coin) {
    val context = LocalContext.current
    Column {
        Text(
            text = "Licenza immagine",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = coin.licenzaImmagine, style = MaterialTheme.typography.bodyMedium)

        coin.attribuzioneImmagineRaw?.let {
            Text(
                text = "Credito: $it",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        coin.urlImmagineFonte?.let { url ->
            Text(
                text = "Apri l'immagine sorgente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
            )
        }
    }
}

@Composable
private fun VerticalGap() {
    Box(modifier = Modifier.padding(top = 16.dp))
}
