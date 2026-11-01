package com.michele.eurocoins.ui.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.michele.eurocoins.data.StatsCard
import com.michele.eurocoins.data.StatsLayout
import kotlin.math.roundToInt

/** Il titolo e il simbolo di ogni card: una sola fonte, usata dalle card e da questo pannello, così non divergono. */
internal val StatsCard.title: String
    get() = when (this) {
        StatsCard.COLLECTION -> "Your collection"
        StatsCard.PROGRESS -> "Progress"
        StatsCard.MILESTONES -> "Milestones"
        StatsCard.BADGES -> "Badges"
        StatsCard.SHINE -> "Shine"
        StatsCard.EXPENSES -> "Expenses"
    }

internal val StatsCard.icon: ImageVector
    get() = when (this) {
        StatsCard.COLLECTION -> Icons.Outlined.Inventory2
        StatsCard.PROGRESS -> Icons.Outlined.TrendingUp
        StatsCard.MILESTONES -> Icons.Outlined.Flag
        StatsCard.BADGES -> Icons.Outlined.MilitaryTech
        StatsCard.SHINE -> Icons.Outlined.Flare
        StatsCard.EXPENSES -> Icons.Outlined.Receipt
    }

private val RowHeight = 56.dp

/**
 * "Customize" (2026-11-01, mockup approvato): sceglie quali card di Statistics mostrare e in che ordine. Una riga per card con la
 * maniglia a tre linee (si trascina), il simbolo, il nome e l'interruttore; "Reset" in alto a destra rimette ordine e card come
 * erano (come nel pannello "Filter & sort") e si spegne finché non si è cambiato niente. Il riepilogo non si spegne.
 *
 * Il trascinamento passa SOLO dalla maniglia: il pannello si chiude trascinando dal bordo in alto, e una riga trascinata non deve
 * trascinarlo. Ogni spostamento oltre mezza riga scambia subito le due card e si salva (nessun "Save"), come un interruttore.
 * Per TalkBack ogni riga ha "Move up" e "Move down".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomizeSheet(layout: StatsLayout, onChange: (StatsLayout) -> Unit, onDismiss: () -> Unit) {
    // Il valore su cui si lavora subito: tra un gesto e la ricomposizione che arriva dalle preferenze passa qualche fotogramma, e
    // uno spostamento calcolato su un ordine vecchio spostava due volte.
    var working by remember { mutableStateOf(layout) }
    LaunchedEffect(layout) { working = layout }
    val apply: (StatsLayout) -> Unit = { next ->
        working = next
        onChange(next)
    }
    val currentApply by rememberUpdatedState(apply)
    val currentWorking by rememberUpdatedState(working)

    var dragging by remember { mutableStateOf<StatsCard?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f))
                Text(
                    text = "Customize",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Default),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(2f),
                )
                val canReset = !working.isDefault
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (canReset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = canReset) { apply(StatsLayout()) }
                        .padding(vertical = 8.dp),
                )
            }
            Text(
                text = "Show, hide and reorder the cards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
            )
            working.order.forEachIndexed { index, card ->
                // key: la riga che si trascina deve restare la stessa composizione anche quando cambia posizione nell'elenco.
                key(card) {
                    val isDragging = dragging == card
                    Column(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction("Move up") { currentApply(currentWorking.move(card, index - 1)); true },
                                    CustomAccessibilityAction("Move down") { currentApply(currentWorking.move(card, index + 1)); true },
                                )
                            },
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth().height(RowHeight),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .pointerInput(card) {
                                        detectDragGestures(
                                            onDragStart = { dragging = card; dragOffset = 0f },
                                            onDragEnd = { dragging = null; dragOffset = 0f },
                                            onDragCancel = { dragging = null; dragOffset = 0f },
                                        ) { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val from = currentWorking.order.indexOf(card)
                                            val steps = (dragOffset / rowHeightPx).roundToInt()
                                            val to = (from + steps).coerceIn(0, currentWorking.order.lastIndex)
                                            if (to != from) {
                                                currentApply(currentWorking.move(card, to))
                                                // La riga è ora nella posizione nuova: l'offset riparte da lì, così resta sotto il dito.
                                                dragOffset -= (to - from) * rowHeightPx
                                            }
                                        }
                                    },
                            ) {
                                Icon(Icons.Filled.Reorder, contentDescription = "Reorder ${card.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(card.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Text(card.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            val shown = card !in working.hidden
                            // Gli stessi colori degli altri interruttori dell'app (vedi SwitchRow nelle Impostazioni).
                            Switch(
                                checked = shown,
                                onCheckedChange = { apply(working.toggle(card)) },
                                enabled = card.canHide,
                                thumbContent = if (shown) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                } else {
                                    null
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedIconColor = MaterialTheme.colorScheme.primary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.background,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
            val hiddenCount = working.hidden.count { it.canHide }
            Text(
                text = if (hiddenCount > 0) "$hiddenCount hidden. Your data stays as it is." else " ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp),
            )
        }
    }
}
