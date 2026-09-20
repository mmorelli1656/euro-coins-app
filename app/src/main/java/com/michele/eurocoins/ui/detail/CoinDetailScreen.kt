package com.michele.eurocoins.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.displayCountry
import java.math.BigDecimal
import java.math.RoundingMode
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

    Scaffold(
        topBar = {
            TopAppBar(
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

                VerticalGap()
                CollectionSection(
                    items = items,
                    onQualityToggled = viewModel::onQualityToggled,
                    onPriceChanged = viewModel::onPriceChanged,
                )

                currentCoin.noteStoriche?.let { note ->
                    VerticalGap()
                    Text(text = "Historical notes", style = MaterialTheme.typography.titleMedium)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
        )
    }
}

@Composable
private fun InfoGrid(coin: Coin) {
    val tiraturaFormatted = coin.tiratura?.let {
        NumberFormat.getIntegerInstance(Locale.ENGLISH).format(it) + " coins"
    } ?: "Unknown"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InfoRow("Issuing country", coin.displayCountry())
        InfoRow("Mintage", tiraturaFormatted)
        if (coin.paese in PAESI_TIRATURA_SOSPETTA && coin.tiratura != null) {
            Text(
                text = "For ${coin.displayCountry()}, this figure is most likely the country's " +
                    "authorized quota for the period, not the actual mintage of " +
                    "this specific coin — see NOTES.md in the data pipeline.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }
        InfoRow("Data source", coin.fonteDati.uppercase(Locale.ENGLISH))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(Locale.ENGLISH),
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
            text = "Image license",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = coin.licenzaImmagine, style = MaterialTheme.typography.bodyMedium)

        coin.attribuzioneImmagineRaw?.let {
            Text(
                text = "Credit: $it",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        coin.urlImmagineFonte?.let { url ->
            Text(
                text = "Open source image",
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

/**
 * "My collection": una chip per qualità (si può possedere la stessa moneta
 * in più qualità) e, per ciascuna posseduta, il prezzo pagato (facoltativo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionSection(
    items: List<CollectionItem>,
    onQualityToggled: (CoinQuality, Boolean) -> Unit,
    onPriceChanged: (CoinQuality, Int?) -> Unit,
) {
    val ownedByQuality = items.associateBy { it.quality }

    Column {
        Text(text = "My collection", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            CoinQuality.entries.forEach { quality ->
                val owned = quality in ownedByQuality
                FilterChip(
                    selected = owned,
                    onClick = { onQualityToggled(quality, !owned) },
                    label = { Text(quality.label) },
                    leadingIcon = if (owned) {
                        { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
        CoinQuality.entries.mapNotNull { ownedByQuality[it] }.forEach { item ->
            PriceField(item = item, onPriceChanged = { onPriceChanged(item.quality, it) })
        }
    }
}

@Composable
private fun PriceField(item: CollectionItem, onPriceChanged: (Int?) -> Unit) {
    // Il testo è uno stato locale, non riletto dal database a ogni tasto: se
    // no il valore riformattato dal salvataggio riscriverebbe quello che
    // l'utente sta digitando (es. "12," diventerebbe "12").
    var text by remember(item.coinKey, item.quality) { mutableStateOf(formatPrice(item.priceCents)) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onPriceChanged(parsePriceCents(it))
        },
        label = { Text("Price paid, ${item.quality.label} (€)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

private fun formatPrice(cents: Int?): String =
    cents?.let { BigDecimal(it).movePointLeft(2).setScale(2).toPlainString() }.orEmpty()

/** "12,50" o "12.50" -> 1250; vuoto, non numerico o negativo -> null (prezzo non indicato). */
private fun parsePriceCents(input: String): Int? {
    val value = input.trim().replace(',', '.')
    if (value.isEmpty()) return null
    return try {
        BigDecimal(value).takeIf { it.signum() >= 0 }?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toInt()
    } catch (_: NumberFormatException) {
        null
    }
}
