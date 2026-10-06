package com.michele.eurocoins.ui.components

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.Transformation

/** Lato della miniatura negli elenchi (monete commemorative e tagli Regular Issues). */
val ListThumbnailSize = 64.dp

/** Lato della miniatura in pixel, per [thumbnailRequest]. */
@Composable
fun rememberThumbnailPx(): Int = with(LocalDensity.current) { ListThumbnailSize.roundToPx() }

/**
 * Richiesta Coil di una miniatura d'elenco, costruita sempre qui: la riga e [PrefetchThumbnails]
 * devono produrre la STESSA chiave di cache in memoria (dati + dimensione + trasformazioni, in
 * quest'ordine), altrimenti il precaricamento scalda solo la cache su disco e la riga rifà lettura,
 * decodifica e nitidezza quando arriva. La dimensione è esplicita (non quella del layout): così
 * `AsyncImage` non aspetta la misura per partire e il precaricamento non deve indovinarla.
 */
fun thumbnailRequest(context: Context, url: String, sizePx: Int, transformations: List<Transformation>): ImageRequest =
    ImageRequest.Builder(context).data(url).size(sizePx).transformations(transformations).build()

/** Quante righe oltre l'ultima visibile precaricare mentre si scorre. */
private const val PREFETCH_AHEAD = 24

/**
 * Prepara in anticipo le miniature delle righe appena sotto quelle visibili: lettura dalla cache su
 * disco (o download), decodifica, trasformazioni, e il risultato finito sta nella cache in memoria di
 * Coil, così quando la riga arriva sullo schermo la foto c'è già. [urls] è l'elenco nell'ordine della
 * lista (null = riga senza foto), [firstIndex] quanti elementi della lista precedono la prima riga
 * (l'intestazione "N coins"). Ogni URL si accoda una volta sola.
 */
@Composable
fun PrefetchThumbnails(
    listState: LazyListState,
    urls: List<String?>,
    transformations: List<Transformation>,
    firstIndex: Int = 0,
) {
    val context = LocalContext.current
    val sizePx = rememberThumbnailPx()
    val requested = remember { HashSet<String>() }
    LaunchedEffect(listState, urls) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { lastItem ->
            val loader = SingletonImageLoader.get(context)
            val from = (lastItem - firstIndex).coerceAtLeast(0)
            val to = (from + PREFETCH_AHEAD).coerceAtMost(urls.size)
            for (i in from until to) {
                val url = urls[i] ?: continue
                if (requested.add(url)) loader.enqueue(thumbnailRequest(context, url, sizePx, transformations))
            }
        }
    }
}
