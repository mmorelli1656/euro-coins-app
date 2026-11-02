package com.michele.eurocoins.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.michele.eurocoins.data.SpentItem
import com.michele.eurocoins.data.SpentTarget
import com.michele.eurocoins.data.formatPurchaseDate
import com.michele.eurocoins.ui.components.CoinThumbnailRing
import com.michele.eurocoins.ui.components.ThumbnailColorFilter
import com.michele.eurocoins.ui.components.ThumbnailFilterQuality
import com.michele.eurocoins.ui.components.ThumbnailSharpen
import com.michele.eurocoins.ui.components.thumbnailRequest
import com.michele.eurocoins.ui.regular.RegularThumbnailTransformations

/** Con meno di tre voci con prezzo il podio non si fa: le righe stanno già tutte nella card. */
private const val PODIUM_MIN_ITEMS = 3

/**
 * "Most expensive" nella card Spending (2026-10-25, dopo mockup): con almeno tre voci con prezzo un podio a tre
 * gradini (2° · 1° · 3°, il primo al centro e più alto) con la foto del lato nazionale di ciascuna; sotto, "See all N"
 * apre il pannello con tutte le voci, dalla più cara. Con una o due voci solo le righe, senza podio e senza pulsante.
 * Le altezze dei gradini sono fisse, non proporzionali al prezzo: con 35 € e 10 € il terzo si schiaccerebbe.
 */
@Composable
internal fun MostExpensive(ranking: List<SpentItem>, onOpen: (SpentTarget) -> Unit) {
    if (ranking.isEmpty()) return
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    // Da Free la pagina è sfocata e senza tocchi: il pannello mostrerebbe la classifica leggibile.
    val locked = LocalLockedStats.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text(
            text = "Most expensive",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (ranking.size >= PODIUM_MIN_ITEMS) {
            Podium(ranking.take(PODIUM_MIN_ITEMS), onOpen)
            Text(
                text = "See all ${ranking.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !locked) { sheetOpen = true }
                    .padding(vertical = 12.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                ranking.forEachIndexed { index, item -> SpentRow(rank = index + 1, item = item, showDivider = index > 0, onOpen = onOpen) }
            }
        }
    }
    if (sheetOpen && !locked) MostExpensiveSheet(ranking, onOpen = onOpen, onDismiss = { sheetOpen = false })
}

private val FirstCoin = 60.dp
private val OtherCoin = 52.dp

@Composable
private fun Podium(top: List<SpentItem>, onOpen: (SpentTarget) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumColumn(top[1], rank = 2, coin = OtherCoin, stepHeight = 56.dp, stepAlpha = 0.88f, onOpen = onOpen, modifier = Modifier.weight(1f))
        PodiumColumn(top[0], rank = 1, coin = FirstCoin, stepHeight = 80.dp, stepAlpha = 1f, onOpen = onOpen, modifier = Modifier.weight(1f))
        PodiumColumn(top[2], rank = 3, coin = OtherCoin, stepHeight = 46.dp, stepAlpha = 0.8f, onOpen = onOpen, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumColumn(
    item: SpentItem,
    rank: Int,
    coin: Dp,
    stepHeight: Dp,
    stepAlpha: Float,
    onOpen: (SpentTarget) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.openOnTap(item, onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpentCoin(item, coin)
        Text(
            text = item.country,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = item.what,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = item.finish,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // I gradini crescono con il font di sistema: con il testo ingrandito il numero del terzo, il più basso, si tagliava.
                .height(stepHeight * LocalDensity.current.fontScale.coerceAtLeast(1f))
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = stepAlpha))
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = euro(item.priceCents.toLong()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
            )
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun SpentRow(rank: Int, item: SpentItem, showDivider: Boolean, onOpen: (SpentTarget) -> Unit) {
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    Row(
        modifier = Modifier.fillMaxWidth().openOnTap(item, onOpen).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(width = 22.dp, height = 20.dp),
        )
        SpentCoin(item, 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.country} · ${item.what}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.purchasedOn?.let { "${item.finish} · ${formatPurchaseDate(it)}" } ?: item.finish,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = euro(item.priceCents.toLong()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * Il tocco apre il dettaglio della moneta (una pagina sola: nessuna lista da scorrere). Senza destinazione, cioè per un
 * microstato nascosto dalle Impostazioni, la riga non è cliccabile.
 */
private fun Modifier.openOnTap(item: SpentItem, onOpen: (SpentTarget) -> Unit): Modifier =
    item.target?.let { target -> clip(RoundedCornerShape(10.dp)).clickable { onOpen(target) } } ?: this

/** La foto del lato nazionale in un cerchio, con lo stesso trattamento (nitidezza, anello, segnaposto) degli elenchi. */
@Composable
private fun SpentCoin(item: SpentItem, size: Dp) {
    Box(modifier = Modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        val url = item.imageUrl
        if (url == null) {
            SpentPlaceholder(withEuro = true)
        } else {
            val context = LocalContext.current
            val sizePx = with(LocalDensity.current) { size.roundToPx() }
            val request = remember(url, sizePx, item.isRegular) {
                val transformations = if (item.isRegular) RegularThumbnailTransformations else listOf(ThumbnailSharpen)
                thumbnailRequest(context, url, sizePx, transformations)
            }
            SubcomposeAsyncImage(
                model = request,
                contentDescription = "${item.country} ${item.what}",
                contentScale = ContentScale.Crop,
                filterQuality = ThumbnailFilterQuality,
                colorFilter = ThumbnailColorFilter,
                modifier = Modifier.fillMaxSize(),
            ) {
                // MAI painter.state.value (è uno StateFlow): vedi CLAUDE.md § "Stato di SubcomposeAsyncImage".
                when (painter.state.collectAsState().value) {
                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent()
                        CoinThumbnailRing()
                    }
                    is AsyncImagePainter.State.Error -> SpentPlaceholder(withEuro = true)
                    else -> SpentPlaceholder(withEuro = false)
                }
            }
        }
    }
}

@Composable
private fun SpentPlaceholder(withEuro: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (withEuro) {
            Icon(
                imageVector = Icons.Filled.EuroSymbol,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }
}

/** Pannello di sola lettura (come quello delle tirature per anno): tutte le voci con un prezzo, dalla più cara. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MostExpensiveSheet(ranking: List<SpentItem>, onOpen: (SpentTarget) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(
                text = "Most expensive",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Default),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${ranking.size} items with a price, highest first",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(ranking) { index, item -> SpentRow(rank = index + 1, item = item, showDivider = index > 0, onOpen = onOpen) }
            }
        }
    }
}
