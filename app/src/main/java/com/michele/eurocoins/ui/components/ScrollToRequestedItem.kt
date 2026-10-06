package com.michele.eurocoins.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.first

/**
 * Al ritorno dal dettaglio a pagine porta in vista la riga in cui si è finiti scorrendo tra le pagine
 * ([target], chiave della riga), se non lo è già: la centra; se è già tutta visibile la lista non si muove.
 * Aspetta il primo layout (prima `visibleItemsInfo` è vuoto e ogni ritorno sembrerebbe "riga fuori
 * vista"). [indexOf] dà l'indice dell'elemento della LazyColumn (intestazioni comprese) o -1 se la riga non
 * c'è più (filtro cambiato nel frattempo). [ready] è falso finché la lista è vuota perché in caricamento:
 * la richiesta resta in attesa invece di andare persa. [onHandled] azzera la richiesta.
 */
@Composable
fun <K : Any> ScrollToRequestedItem(
    target: K?,
    listState: LazyListState,
    ready: Boolean,
    rowHeight: Dp,
    indexOf: (K) -> Int,
    onHandled: () -> Unit,
) {
    val rowPx = with(LocalDensity.current) { rowHeight.roundToPx() }
    LaunchedEffect(target, ready) {
        val key = target ?: return@LaunchedEffect
        if (!ready) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        val index = indexOf(key)
        if (index >= 0) {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo.firstOrNull { it.index == index }
            val bottom = info.viewportEndOffset - info.afterContentPadding
            val fullyVisible = visible != null && visible.offset >= info.viewportStartOffset && visible.offset + visible.size <= bottom
            if (!fullyVisible) {
                listState.scrollToItem(index, scrollOffset = -((bottom - info.viewportStartOffset - rowPx) / 2))
            }
        }
        onHandled()
    }
}
