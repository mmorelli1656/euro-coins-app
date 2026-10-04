package com.michele.eurocoins.ui.regular

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.transformations
import com.michele.eurocoins.data.RegularIssueImage

private val ThumbnailSize = 52.dp

/** Cerchio lilla un po' più piccolo della foto (50 dp contro 52): stesso motivo di `CoinListScreen`. */
private val PlaceholderSize = 50.dp

/** Stessa altezza di `CoinRow` in `CoinListScreen.kt`. */
private val RowHeight = 80.dp

/**
 * Una card per taglio, altezza fissa 80 dp: stessa struttura di `CoinRow` in `CoinListScreen.kt`.
 * Il tocco sulla riga apre il dettaglio del taglio ([onClick]); la casella a destra resta un bersaglio
 * separato per la collezione ([onEditCollection]). Usata dalla schermata del paese (stato = "Not owned" /
 * "N years owned", titolo = il taglio) e dall'elenco di un taglio (stato = "Series 2 · 2008 – 2013",
 * titolo = il paese): [status] è la riga piccola sopra il titolo, in verde grassetto se [statusHighlight].
 */
@Composable
internal fun RegularCoinRow(
    image: RegularIssueImage,
    status: String,
    statusHighlight: Boolean,
    title: String,
    owned: Boolean,
    onClick: () -> Unit,
    onEditCollection: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .height(RowHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(ThumbnailSize).clip(CircleShape), contentAlignment = Alignment.Center) {
            DenominationThumbnail(image)
        }
        Column(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 2.dp, bottom = 2.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                fontWeight = if (statusHighlight) FontWeight.Bold else FontWeight.Normal,
                color = if (statusHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CollectionBox(owned = owned, onClick = onEditCollection)
    }
}

/** Casella accanto al taglio: piena con spunta se posseduto in almeno un'annata; un tocco apre il pannello. Stesso disegno di `CollectionBox` in `CoinListScreen.kt` (duplicata qui, non condivisa — stesso approccio di `BrowseCard`). */
@Composable
private fun CollectionBox(owned: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (owned) primary else Color.Transparent)
                .border(
                    2.dp,
                    if (owned) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    RoundedCornerShape(7.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (owned) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "In your collection, tap to edit",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DenominationThumbnail(image: RegularIssueImage) {
    if (image.urlImmagineFonte == null) {
        DenominationPlaceholder()
    } else {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            // RegularIssueImageTrim: il margine attorno alla moneta non è uniforme da file a
            // file (vedi quella classe) — senza, la moneta appare più piccola del cerchio.
            model = ImageRequest.Builder(context)
                .data(image.urlImmagineFonte)
                .transformations(RegularIssueImageTrim)
                .build(),
            contentDescription = image.taglio,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
        ) {
            // MAI painter.state.value: e' uno StateFlow, .value non sottoscrive la ricomposizione
            // (vedi CLAUDE.md § Decisioni di prodotto, "Stato di SubcomposeAsyncImage").
            when (painter.state.collectAsState().value) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                is AsyncImagePainter.State.Error -> DenominationPlaceholder()
                else -> PlaceholderCircle()
            }
        }
    }
}

@Composable
private fun PlaceholderCircle(content: @Composable () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(PlaceholderSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DenominationPlaceholder() {
    PlaceholderCircle {
        Icon(
            imageVector = Icons.Filled.EuroSymbol,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(26.dp),
        )
    }
}
