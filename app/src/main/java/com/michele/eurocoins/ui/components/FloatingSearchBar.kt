package com.michele.eurocoins.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val BarHeight = 72.dp
private val BarBottomMargin = 24.dp
private val BarSideMargin = 8.dp
private val BarDeadZone = 32.dp

/**
 * Spazio da lasciare in fondo a una lista/griglia perché l'ultimo elemento
 * non resti sotto la barra flottante (altezza + margine + barra di
 * navigazione, o tastiera quando è aperta).
 */
@Composable
fun floatingBarClearance(): Dp {
    val insets = WindowInsets.navigationBars.union(WindowInsets.ime)
    return BarHeight + BarBottomMargin + 16.dp + insets.asPaddingValues().calculateBottomPadding()
}

/**
 * Barra a pillola fissa in basso: ricerca a sinistra, divisore, pulsante
 * FILTER a destra. Lo sfondo è vetro smerigliato: [hazeState] deve essere lo
 * stesso passato con `hazeSource` al contenuto che scorre sotto. Sotto
 * Android 12 (niente RenderEffect) resta il solo fondo semitrasparente.
 *
 * Va posta in un Box a schermo intero con `Modifier.align(BottomCenter)`;
 * sale da sola sopra la tastiera.
 */
@Composable
fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    filterActive: Boolean,
    onFilterClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    // Tema scuro: fondo molto opaco (0.94), altrimenti il solo blur lascia leggibile il testo
    // chiaro che scorre sotto. Tema chiaro: la superficie quasi bianca sopra un fondo chiaro
    // non lasciava vedere né il blur né il bordo; meno opaco di quello scuro (0.88) il vetro si vede ma il
    // testo sotto non deve leggersi (0.72 era troppo trasparente); contorno scuro da 2 dp.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glassTint = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.94f else 0.88f)
    val borderColor = if (isDark) onSurface.copy(alpha = 0.15f) else onSurface.copy(alpha = 0.5f)
    val borderWidth = if (isDark) 1.dp else 2.dp
    val focusManager = LocalFocusManager.current

    // Sollevamento sopra barra di navigazione o tastiera. Usa direttamente gli
    // insets: il sistema li anima insieme alla tastiera, quindi la barra parte
    // e si muove con lei, senza ritardo.
    val density = LocalDensity.current
    val liftPx = maxOf(
        WindowInsets.navigationBars.getBottom(density),
        WindowInsets.ime.getBottom(density),
    )

    // La zona morta è il Box esterno: assorbe i tocchi nel margine attorno alla
    // pillola (larga tutto lo schermo) così non finiscono sulle monete vicine.
    Box(
        modifier = modifier
            .offset { IntOffset(0, -liftPx) }
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(start = BarSideMargin, end = BarSideMargin, top = BarDeadZone, bottom = BarBottomMargin),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .clip(CircleShape)
                .hazeEffect(state = hazeState) {
                    blurRadius = 64.dp
                    tints = listOf(HazeTint(glassTint))
                    noiseFactor = 0.06f
                }
                .border(BorderStroke(borderWidth, borderColor), CircleShape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .width(1.dp)
                    .background(onSurface.copy(alpha = 0.2f)),
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onFilterClick)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    "FILTER",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
                if (filterActive) {
                    // Pallino: c'è un filtro/ordinamento diverso dal predefinito.
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary),
                    )
                }
            }
        }
    }
}
