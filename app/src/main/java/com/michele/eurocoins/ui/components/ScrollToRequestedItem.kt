package com.michele.eurocoins.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Riga da evidenziare per un attimo dopo che la lista è scorsa da sola (vedi [ScrollToRequestedItem]).
 * Vale [key] per circa 2 secondi, poi torna nulla: se restasse impostata, una riga che esce e rientra
 * nella composizione di una lista lazy rifarebbe l'evidenziazione a ogni scorrimento.
 */
class ReturnHighlight<K : Any> {
    var key by mutableStateOf<K?>(null)
        internal set
}

@Composable
fun <K : Any> rememberReturnHighlight(): ReturnHighlight<K> = remember { ReturnHighlight() }

/**
 * Al ritorno dal dettaglio a pagine porta in vista la riga in cui si è finiti scorrendo tra le pagine
 * ([target], chiave della riga), se non lo è già: la centra; se è già tutta visibile la lista non si muove.
 * Aspetta il primo layout (prima `visibleItemsInfo` è vuoto e ogni ritorno sembrerebbe "riga fuori
 * vista"). [indexOf] dà l'indice dell'elemento della LazyColumn (intestazioni comprese) o -1 se la riga non
 * c'è più (filtro cambiato nel frattempo). [ready] è falso finché la lista è vuota perché in caricamento:
 * la richiesta resta in attesa invece di andare persa. [onHandled] azzera la richiesta. Se la lista ha
 * dovuto scorrere, la riga arrivata viene segnalata in [highlight] (le righe la usano con [returnHighlight]):
 * se era già visibile non si evidenzia niente, non c'è nessun salto da compensare.
 */
@Composable
fun <K : Any> ScrollToRequestedItem(
    target: K?,
    listState: LazyListState,
    ready: Boolean,
    rowHeight: Dp,
    indexOf: (K) -> Int,
    highlight: ReturnHighlight<K>,
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
                highlight.key = key
            }
        }
        onHandled()
    }
    LaunchedEffect(highlight.key) {
        if (highlight.key != null) {
            delay(HIGHLIGHT_LIFETIME_MS)
            highlight.key = null
        }
    }
}

private const val HIGHLIGHT_LIFETIME_MS = 2_000L
private const val HIGHLIGHT_DELAY_MS = 150L
private const val HIGHLIGHT_HOLD_MS = 500L
private const val HIGHLIGHT_FADE_MS = 1_000

/**
 * Contorno verdigris da 2 dp che compare quando [active] diventa vero, resta pieno mezzo secondo e sfuma in
 * un secondo (mockup B, dopo "com'è ora" e uno sfondo lilla scartato: il lilla è la selezione di un controllo,
 * una riga lilla sembrerebbe selezionata o posseduta). Parte 150 ms dopo, a lista già assestata. Disegnato
 * sopra il bordo da 1 dp della riga: non cambia il layout né i tocchi. [cornerRadius] è quello della card.
 */
@Composable
fun Modifier.returnHighlight(active: Boolean, cornerRadius: Dp = 14.dp): Modifier {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            delay(HIGHLIGHT_DELAY_MS)
            alpha.snapTo(1f)
            delay(HIGHLIGHT_HOLD_MS)
            alpha.animateTo(0f, tween(HIGHLIGHT_FADE_MS, easing = LinearOutSlowInEasing))
        } else {
            alpha.snapTo(0f)
        }
    }
    val color = MaterialTheme.colorScheme.primary
    return drawWithContent {
        drawContent()
        val a = alpha.value
        if (a > 0f) {
            val stroke = 2.dp.toPx()
            val inset = stroke / 2
            drawRoundRect(
                color = color.copy(alpha = a),
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(cornerRadius.toPx() - inset),
                style = Stroke(stroke),
            )
        }
    }
}
