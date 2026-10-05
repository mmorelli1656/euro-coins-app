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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlin.math.roundToInt

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

private val HideAfterScroll = 40.dp
private val ShowAfterScroll = 16.dp
private val BarEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Se la barra è nascosta: scorrere la lista verso il basso la fa uscire dal bordo, scorrere verso
 * l'alto (o tornare in cima) la riporta. Chi sfoglia in fretta non se la trova sopra le righe.
 * Le soglie evitano che un tremolio del dito la faccia lampeggiare; contano solo gli spostamenti
 * REALI della lista (`consumed`), quindi una lista che non scorre la lascia sempre visibile.
 *
 * Lo stato va creato dalla schermata ([rememberFloatingBarState]), messo con `nestedScroll` su un
 * antenato sia della lista sia della barra (il Box che li contiene) e passato alla barra.
 */
@Stable
class FloatingBarState internal constructor(private val hidePx: Float, private val showPx: Float) {
    var hidden by mutableStateOf(false)
        private set

    fun show() {
        hidden = false
    }

    // Spostamento accumulato nella direzione corrente: negativo = la lista scorre verso il basso.
    private var travel = 0f

    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val dy = consumed.y
            if (dy == 0f) return Offset.Zero
            if (dy * travel < 0f) travel = 0f
            travel += dy
            if (travel < -hidePx) hidden = true else if (travel > showPx) hidden = false
            return Offset.Zero
        }
    }
}

/** [keys] cambiati (es. scheda diversa di Browse) riportano la barra visibile. */
@Composable
fun rememberFloatingBarState(vararg keys: Any?): FloatingBarState {
    val density = LocalDensity.current
    return remember(density, *keys) {
        FloatingBarState(hidePx = with(density) { HideAfterScroll.toPx() }, showPx = with(density) { ShowAfterScroll.toPx() })
    }
}

/**
 * Barra a pillola fissa in basso: ricerca a sinistra, divisore, pulsante
 * FILTER a destra. Lo sfondo è vetro smerigliato: [hazeState] deve essere lo
 * stesso passato con `hazeSource` al contenuto che scorre sotto. Sotto
 * Android 12 (niente RenderEffect) resta il solo fondo semitrasparente.
 *
 * Va posta in un Box a schermo intero con `Modifier.align(BottomCenter)`;
 * sale da sola sopra la tastiera e scivola fuori scorrendo ([barState]); con
 * la tastiera aperta resta sempre visibile.
 */
@Composable
fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    filterActive: Boolean,
    onFilterClick: () -> Unit,
    hazeState: HazeState,
    barState: FloatingBarState,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    // Tema scuro: fondo molto opaco (0.84), altrimenti il solo blur lascia leggibile il testo
    // chiaro che scorre sotto. Tema chiaro: la superficie quasi bianca sopra un fondo chiaro
    // non lasciava vedere né il blur né il bordo; meno opaco di quello scuro (0.78) il vetro si vede ma il
    // testo sotto non deve leggersi (0.72 era troppo trasparente); contorno scuro da 2 dp.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glassTint = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.84f else 0.78f)
    val borderColor = if (isDark) onSurface.copy(alpha = 0.4f) else onSurface.copy(alpha = 0.5f)
    val borderWidth = if (isDark) 1.5.dp else 2.dp
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    // Testo e cursore restano QUI: il valore che torna da `query` passa da un StateFlow e arriva
    // con uno o più fotogrammi di ritardo, e un BasicTextField(String) che riceve un valore
    // vecchio riporta il testo e il cursore indietro (cursore dopo la terza lettera, caratteri persi).
    // `query` vale solo come valore iniziale: ogni barra ha il suo punto nella composizione
    // (Years, Countries, All, elenco), e la cancellazione passa da qui.
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    val keyboard = LocalSoftwareKeyboardController.current

    // Sollevamento sopra barra di navigazione o tastiera. Usa direttamente gli
    // insets: il sistema li anima insieme alla tastiera, quindi la barra parte
    // e si muove con lei, senza ritardo.
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val liftPx = maxOf(WindowInsets.navigationBars.getBottom(density), imeBottom)

    // Nascosta solo se la lista è stata scorsa verso il basso E la tastiera è chiusa. Si guarda la
    // tastiera e non il focus: il campo tiene il focus anche dopo averla chiusa col tasto indietro,
    // e la barra non si nasconderebbe più. Il fuoco sul campo la rimostra (vedi sotto), così dopo
    // aver scritto non scivola via davanti ai risultati.
    val hidden = barState.hidden && imeBottom == 0
    val hideProgress by animateFloatAsState(
        targetValue = if (hidden) 1f else 0f,
        animationSpec = tween(300, easing = BarEasing),
        label = "barHide",
    )
    // Scivola con `offset` (layout) e non con graphicsLayer: Haze legge la posizione dal layout, come
    // per il sollevamento sopra la tastiera. Percorso = altezza del Box (zona morta + pillola + margine).
    val hideTravelPx = with(density) { (BarDeadZone + BarHeight + BarBottomMargin).roundToPx() }

    // La zona morta è il Box esterno: assorbe i tocchi nel margine attorno alla
    // pillola (larga tutto lo schermo) così non finiscono sulle monete vicine.
    Box(
        modifier = modifier
            .offset { IntOffset(0, -liftPx + (hideProgress * (liftPx + hideTravelPx)).roundToInt()) }
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
            // Tutta la metà sinistra (lente, margini, altezza intera della pillola) porta il focus al campo:
            // il BasicTextField è alto quanto una riga di testo, e toccare fuori da quella striscia
            // (sulla lente, sopra o sotto) non apriva la tastiera.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                    .padding(start = 18.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (field.text.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            if (it.text != query) onQueryChange(it.text)
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) barState.show() },
                    )
                }
                if (field.text.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                field = TextFieldValue("")
                                onQueryChange("")
                            },
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
