package com.michele.eurocoins.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Pager dei dettagli (commemorativa e Regular Issues): le pagine si sovrappongono a "mazzo" (vedi
 * [stackedPage]). `beyondViewportPageCount = 1` compone anche le pagine vicine: la foto della prossima è
 * già caricata quando entra, invece di comparire a metà scorrimento. [key] deve essere salvabile in un
 * Bundle (Long, String...), come le chiavi di qualunque lista lazy.
 */
@Composable
internal fun StackedPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    key: (Int) -> Any,
    content: @Composable (page: Int) -> Unit,
) {
    // Sfondo opaco: la pagina sotto non deve trasparire da quella che esce.
    val background = MaterialTheme.colorScheme.background
    HorizontalPager(
        state = state,
        modifier = modifier,
        beyondViewportPageCount = 1,
        key = key,
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .stackedPage(state, page, background)
                .background(background),
        ) {
            content(page)
        }
    }
}

/** Scala e opacità della pagina sotto a riposo-di-trascinamento (mockup B: 94% e 55%, poi crescono fino a 100%). */
private const val STACK_UNDER_SCALE = 0.94f
private const val STACK_UNDER_ALPHA = 0.55f

/** La pagina che esce (mockup D): inclinazione massima a uscita completa, alzata, ingrandimento, ombra e angoli mentre è trascinata. */
private const val STACK_TILT_DEGREES = 5f
private const val STACK_LIFT_DP = 8f
private const val STACK_LIFT_SCALE = 0.012f
private const val STACK_SHADOW_DP = 12f
private const val STACK_CORNER_DP = 22f
/** Frazione di pagina dopo cui alzata e ombra sono al massimo (prima crescono in modo continuo da 0). */
private const val STACK_LIFT_RAMP = 1f / 3f

/**
 * Effetto "mazzo": la pagina che esce (indice più basso) scorre sopra, quella dopo resta FERMA al centro
 * sotto di lei, un po' più piccola e spenta, e cresce fino a scala e opacità piene man mano che la
 * prima la scopre. Vale in entrambe le direzioni: andando indietro la precedente rientra da sinistra
 * sopra la corrente, che si rimpicciolisce. La pagina in cima inoltre si inclina (5°, perno sul bordo in basso),
 * si alza e proietta ombra mentre è trascinata (variante D del mockup: con la sola traslazione "sembrava
 * ancora uno scorrimento laterale"). A riposo la pagina centrale è identica a prima (scala 1,
 * nessuna traslazione). `offset` = distanza della pagina dalla posizione corrente del pager, in pagine:
 * 0 al centro, negativo a sinistra, positivo a destra. Lo `zIndex` dà la precedenza alle pagine con
 * indice più basso, che il pager disegnerebbe invece sotto.
 */
internal fun Modifier.stackedPage(state: PagerState, page: Int, fadeTo: Color): Modifier = this
    .zIndex(-page.toFloat())
    .graphicsLayer {
        val offset = page - (state.currentPage + state.currentPageOffsetFraction)
        if (offset > 0f) {
            // Il pager sposta la pagina di offset * larghezza: si annulla per tenerla ferma.
            translationX = -offset * size.width
            val scale = STACK_UNDER_SCALE + (1f - STACK_UNDER_SCALE) * (1f - offset.coerceAtMost(1f))
            scaleX = scale
            scaleY = scale
        } else {
            // La pagina in cima, mentre la si trascina: ruota attorno al bordo in basso nel verso del dito, si
            // alza un poco e ha ombra e angoli arrotondati. A riposo (offset 0) niente di tutto questo.
            val lift = (-offset / STACK_LIFT_RAMP).coerceIn(0f, 1f)
            if (lift > 0f) {
                transformOrigin = TransformOrigin(0.5f, 1f)
                rotationZ = STACK_TILT_DEGREES * offset.coerceAtLeast(-1f)
                translationY = -STACK_LIFT_DP.dp.toPx() * lift
                val scale = 1f + STACK_LIFT_SCALE * lift
                scaleX = scale
                scaleY = scale
                shadowElevation = STACK_SHADOW_DP.dp.toPx() * lift
                shape = RoundedCornerShape((STACK_CORNER_DP * lift).dp)
                clip = true
            }
        }
    }
    // "Spenta" con una velatura del colore di sfondo e non con `alpha` del layer: un alpha sotto 1 fa
    // disegnare l'intera pagina (foto comprese) in un buffer fuori schermo a ogni fotogramma, ed era
    // il sospetto principale dello scorrimento poco fluido. Sopra uno sfondo uguale rende lo stesso.
    .drawWithContent {
        drawContent()
        val offset = page - (state.currentPage + state.currentPageOffsetFraction)
        if (offset > 0f) {
            val reveal = 1f - offset.coerceAtMost(1f)
            drawRect(fadeTo, alpha = (1f - (STACK_UNDER_ALPHA + (1f - STACK_UNDER_ALPHA) * reveal)).coerceIn(0f, 1f))
        }
    }

/**
 * Posizione nella lista ("4 / 12"): pillola lilla come le altre etichette dell'app. Cifre
 * tabulari, così la larghezza non cambia scorrendo da 9 a 10.
 */
@Composable
internal fun PagePositionPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = 16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
