package com.michele.eurocoins.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.Progress
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import java.util.Locale
import kotlinx.coroutines.delay

/** Durata del riempimento al primo avvio. */
private const val INTRO_DURATION_MS = 1100

/**
 * Attesa prima di far partire l'animazione del primo avvio, contata da quando i dati sono
 * pronti: l'animazione di apertura dell'app (e la splash) dura ancora qualche centinaio di
 * millisecondi e ne copriva l'inizio, che quindi non si vedeva. Vale solo per il primo avvio:
 * gli aggiornamenti a schermata già mostrata partono subito.
 */
private const val INTRO_START_DELAY_MS = 450L

/** Durata quando il conteggio cambia a schermata già mostrata: solo la differenza. */
private const val UPDATE_DURATION_MS = 600

/**
 * Curva "Emphasized" di Material 3 (cubic-bezier 0.2, 0, 0, 1), usata solo per il pulse finale del
 * numero. Il riempimento usa la FastOutSlowIn standard di Compose (0.4, 0, 0.2, 1).
 */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Derivata massima della FastOutSlowIn (cubic-bezier 0.4, 0, 0.2, 1) rispetto al progresso lineare,
 * calcolata sul punto più ripido della curva (u ≈ 0.42 → ≈ 2.73). Serve a sapere quale sia la
 * velocità di picco di un riempimento senza doverla misurare a ogni fotogramma: v_picco =
 * PEAK_SLOPE × differenza / durata.
 */
private const val FAST_OUT_SLOW_IN_PEAK_SLOPE = 2.75f

// Geometria del "liquido": barra sottile (15 dp: 12 dp era stata ingrandita del 25% con tutta l'onda in
// proporzione; la pillola da 22 dp, provata prima, occupava troppo nella card),
// onda frontale piena e onda di sfondo più trasparente, più alta, sfasata di 90° e più lenta
// (parallasse). Le ampiezze sono in dp.
private val WaveBoxHeight = 15.dp

/** Angoli del contenitore: metà dell'altezza, cioè una pillola completa. */
private val WaveCornerRadius = 7.5.dp

/** Ampiezza a riposo: micro-oscillazione "viva" ma non invadente quando la barra è ferma. */
private val WaveAmplitudeRest = 1.25.dp

/**
 * Ampiezza massima, raggiunta solo al picco di velocità del riempimento: 1.9 dp su 15 dp di altezza,
 * in modo che le creste non tocchino il bordo alto o basso venendo tagliate. Poca distanza dal riposo
 * (1.25 dp): l'agitazione durante la salita è discreta, per scelta.
 */
private val WaveAmplitudeMax = 1.9.dp

/** Lunghezza d'onda FISSA in dp (non proporzionale alla larghezza: vedi CLAUDE.md, onda della home). */
private val WaveWavelength = 20.dp

/** Tempo per un ciclo completo della fase dell'onda frontale: lento e ipnotico, a velocità costante. */
private const val WAVE_PERIOD_MS = 1800

/** L'onda di sfondo scorre a 0.7× la velocità di quella frontale. */
private const val BACK_WAVE_SPEED = 0.7f

private const val BACK_WAVE_ALPHA = 0.38f
private const val BACK_WAVE_AMPLITUDE_SCALE = 0.85f

/** Sfasamento dell'onda di sfondo: 90° = un quarto di ciclo. */
private const val BACK_WAVE_PHASE_OFFSET = 0.25f

/** Linea d'acqua (dall'alto del contenitore) come frazione dell'altezza: sfondo un po' più in alto. */
private const val FRONT_WAVE_BASELINE = 0.34f
private const val BACK_WAVE_BASELINE = 0.26f

/**
 * Bordo destro del riempimento: invece di un taglio verticale, la superficie scende in una curva
 * di questa larghezza fino al fondo del contenitore, e nell'ultimo tratto ([WaveEdgeTaper]) l'ampiezza
 * si smorza, così la cresta non urta contro il bordo quando il valore è intermedio (es. 77/584).
 */
private val WaveEdgeSlope = 8.75.dp
private val WaveEdgeTaper = 12.5.dp

/** Passo di campionamento del percorso: più piccolo = curva più morbida, più punti da disegnare. */
private val WaveStep = 3.dp

/**
 * Stato dell'animazione della barra "x / y collected":
 *
 * - **Primo avvio (cold start)**: [playIntro] vero e [lastShown] nullo → [shownOwned] sale da 0
 *   al valore vero (dopo [INTRO_START_DELAY_MS]).
 * - **Si torna alla schermata nello stesso processo**: [lastShown] arriva già valorizzato dal
 *   ViewModel, si riparte da lì e solo la differenza si anima.
 * - **Il valore cambia a schermata visibile**: stesso trattamento del caso precedente.
 *
 * [agitation] (0..1) è la velocità istantanea del riempimento rapportata al suo picco: nulla a
 * barra ferma, massima nel punto più ripido della curva. La barra ne ricava l'ampiezza dell'onda:
 * mentre sale il liquido è "agitato", quando si ferma torna alla micro-oscillazione a riposo, in
 * modo continuo perché la FastOutSlowIn ha derivata nulla all'inizio e alla fine (niente scatti).
 * Alla fine di ogni animazione un piccolo impulso ("pulse") tocca il numero. "Resume da RAM" non
 * ha bisogno di codice dedicato: finché il processo resta vivo l'Activity non viene distrutta.
 */
@Composable
fun rememberProgressAnimation(
    owned: Int,
    ready: Boolean,
    playIntro: Boolean,
    lastShown: Int?,
    onShown: (Int) -> Unit,
): ProgressAnimation {
    val ownedAnim = remember { Animatable((lastShown ?: 0).toFloat()) }
    val pulse = remember { Animatable(1f) }
    val agitationState = remember { mutableFloatStateOf(0f) }
    var agitation by agitationState

    LaunchedEffect(owned, ready) {
        if (!ready) return@LaunchedEffect
        val isIntro = playIntro && lastShown == null
        if (isIntro || ownedAnim.value.roundToInt() != owned) {
            // Solo al primo avvio: lascia finire l'animazione di apertura dell'app prima di partire.
            // Se l'utente esce durante l'attesa il LaunchedEffect viene annullato prima di
            // onShown: al ritorno l'intro riparte, come deve.
            if (isIntro) delay(INTRO_START_DELAY_MS)
            val durationMs = if (isIntro) INTRO_DURATION_MS else UPDATE_DURATION_MS
            val delta = abs(owned - ownedAnim.value)
            // Velocità di picco attesa (monete al secondo): serve a normalizzare l'agitazione, così
            // un salto di 1 moneta e uno da 0 a 77 si agitano allo stesso modo relativo.
            val peakVelocity = (FAST_OUT_SLOW_IN_PEAK_SLOPE * delta / (durationMs / 1000f)).coerceAtLeast(1e-3f)
            try {
                ownedAnim.animateTo(
                    owned.toFloat(),
                    tween(durationMs, easing = FastOutSlowInEasing),
                ) {
                    agitation = (abs(velocity) / peakVelocity).coerceIn(0f, 1f)
                }
            } finally {
                agitation = 0f
            }
            // Micro-pulse di conferma sul numero, solo quando è appena finito di riempirsi.
            pulse.animateTo(1.08f, tween(120, easing = EmphasizedEasing))
            pulse.animateTo(1f, tween(180, easing = EmphasizedEasing))
        }
        onShown(owned)
    }

    return remember(ownedAnim, pulse, agitationState) { ProgressAnimation(ownedAnim, pulse, agitationState) }
}

class ProgressAnimation internal constructor(
    private val ownedAnim: Animatable<Float, AnimationVector1D>,
    private val pulse: Animatable<Float, AnimationVector1D>,
    private val agitationState: androidx.compose.runtime.FloatState,
) {
    val shownOwned: Float get() = ownedAnim.value
    val pulseScale: Float get() = pulse.value

    /** 0 = barra ferma, 1 = punto più ripido del riempimento. */
    val agitation: Float get() = agitationState.floatValue
}

/**
 * Barra "x / y possedute" usata su home, card anno e card paese.
 *
 * [animation] (da [rememberProgressAnimation]) è opzionale: le card di Years/Countries non lo
 * passano (centinaia di barre non hanno bisogno di animarsi una per una, tantomeno di ondeggiare
 * tutte insieme) e restano una barra dritta di Material. Il conteggio testuale è dietro un
 * `derivedStateOf`, che si ricompone solo quando il numero intero cambia.
 *
 * **Barra animata** (con [animation]): una riga di testo SOPRA la barra, con "x / y collected" a
 * sinistra e la percentuale a destra, e sotto la barra sottile tutta per il liquido. Il Canvas non
 * sovrappone mai il testo, quindi la leggibilità dipende solo dal colore del testo sullo sfondo della
 * card, qualunque sia il valore a cui si ferma l'onda. Il testo va in [labelColor]: sulla tile della
 * home è il colore "onPrimary" (5.9:1 nel tema chiaro, 6.2:1 nello scuro); `onSurface` darebbe 2.6:1 e
 * 2.0:1 perché la tile è colorata, non una superficie.
 *
 * **Barra normale** (senza animazione, card Years/Countries): barra dritta con il testo sotto, come
 * prima.
 */
@Composable
fun CollectionProgressBar(
    progress: Progress,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outline,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelStyle: TextStyle = MaterialTheme.typography.labelSmall,
    animation: ProgressAnimation? = null,
) {
    val shownOwned by remember(progress.owned, animation) {
        derivedStateOf { (animation?.shownOwned ?: progress.owned.toFloat()).roundToInt() }
    }
    // Percentuale in decimi di punto (77/584 → 132 → "13.2%"): si ricompone solo quando cambia il
    // decimo, non a ogni fotogramma.
    val percentTenths by remember(progress.total, animation) {
        derivedStateOf {
            val owned = animation?.shownOwned ?: progress.owned.toFloat()
            if (progress.total == 0) 0 else (owned / progress.total * 1000f).roundToInt()
        }
    }
    val pulseModifier = Modifier.graphicsLayer {
        scaleX = animation?.pulseScale ?: 1f
        scaleY = animation?.pulseScale ?: 1f
    }
    Column(modifier = modifier) {
        if (animation == null) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = trackColor,
            )
            Text(
                text = "$shownOwned / ${progress.total} collected",
                style = labelStyle,
                color = labelColor,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "$shownOwned / ${progress.total} collected",
                    style = labelStyle,
                    color = labelColor,
                    modifier = pulseModifier,
                )
                Text(
                    text = String.format(Locale.US, "%.1f%%", percentTenths / 10f),
                    style = labelStyle,
                    color = labelColor,
                    modifier = pulseModifier,
                )
            }
            LiquidProgressBar(
                progress = progress,
                animation = animation,
                color = color,
                trackColor = trackColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Barra "liquido": una pillola (la traccia) con dentro due onde sovrapposte — una di sfondo, più
 * trasparente, sfasata e più lenta, e una frontale piena — tagliate rigorosamente sulla forma della
 * pillola. Ogni onda è un percorso CHIUSO: parte dal fondo a sinistra, segue la superficie
 * sinusoidale e scende in una curva fino al fondo, a destra. Tutto ciò che varia a ogni fotogramma
 * (fasi, valore, agitazione) è letto dentro il blocco di disegno, non nella composizione: si
 * ridisegna soltanto, senza ricomporre.
 */
@Composable
private fun LiquidProgressBar(
    progress: Progress,
    animation: ProgressAnimation,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    // Le fasi girano sempre, anche a barra ferma: la micro-oscillazione a riposo è voluta. Con
    // l'app in background il frame clock si ferma da solo, e con "rimuovi animazioni" attivo
    // l'onda resta ferma.
    val infinite = rememberInfiniteTransition(label = "liquid_phase")
    val frontPhase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(WAVE_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "front_phase",
    )
    val backPhase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((WAVE_PERIOD_MS / BACK_WAVE_SPEED).toInt(), easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "back_phase",
    )

    // Un solo Path per tutta la vita del composable, azzerato con reset() a ogni disegno: allocarne
    // uno nuovo a ogni fotogramma (due onde × 60 al secondo) produrrebbe garbage inutile.
    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .height(WaveBoxHeight)
            // Il clip vale per tutto quello che il Canvas disegna, traccia vuota compresa: a sinistra
            // e a destra il liquido rispetta la sagoma arrotondata del contenitore.
            .clip(RoundedCornerShape(WaveCornerRadius)),
    ) {
        drawRect(color = trackColor)

        val owned = animation.shownOwned
        // 0%: nessun percorso (una cresta sul bordo sinistro vuoto sembrerebbe una sbavatura).
        if (progress.total == 0 || owned <= 0.001f) return@Canvas
        val fraction = (owned / progress.total).coerceIn(0f, 1f)
        // 100%: il riempimento occupa tutto il contenitore, angoli compresi (li tiene il clip).
        if (fraction >= 1f - 0.0005f) {
            drawRect(color = color)
            return@Canvas
        }
        val xEnd = size.width * fraction
        if (xEnd < 1f) return@Canvas

        val restPx = WaveAmplitudeRest.toPx()
        val amplitudePx = restPx + (WaveAmplitudeMax.toPx() - restPx) * animation.agitation

        drawLiquidWave(
            path = path,
            color = color.copy(alpha = BACK_WAVE_ALPHA),
            xEnd = xEnd,
            baselineY = size.height * BACK_WAVE_BASELINE,
            amplitudePx = amplitudePx * BACK_WAVE_AMPLITUDE_SCALE,
            phase = backPhase.value + BACK_WAVE_PHASE_OFFSET,
        )
        drawLiquidWave(
            path = path,
            color = color,
            xEnd = xEnd,
            baselineY = size.height * FRONT_WAVE_BASELINE,
            amplitudePx = amplitudePx,
            phase = frontPhase.value,
        )
    }
}

/**
 * Disegna una superficie d'onda piena da x = 0 a [xEnd]. [phase] è in cicli (1 = un giro completo).
 *
 * Il bordo destro non è un taglio verticale: negli ultimi [WaveEdgeSlope] la superficie scende con
 * una curva cubica fino al fondo (tangente orizzontale alla cresta, verticale sul fondo), e prima
 * di essa l'ampiezza si smorza fino al 35% ([WaveEdgeTaper]) per non far urtare la cresta contro
 * il bordo. Se il riempimento è più stretto della curva, questa si accorcia con lui.
 *
 * [path] è un'istanza riusata dal chiamante: qui viene solo azzerata con `reset()` e riempita, così
 * non si alloca nulla a ogni fotogramma.
 */
private fun DrawScope.drawLiquidWave(
    path: Path,
    color: Color,
    xEnd: Float,
    baselineY: Float,
    amplitudePx: Float,
    phase: Float,
) {
    val height = size.height
    val wavelengthPx = WaveWavelength.toPx()
    val taperPx = WaveEdgeTaper.toPx()
    val stepPx = WaveStep.toPx()

    fun surfaceY(x: Float): Float {
        val taper = ((xEnd - x) / taperPx).coerceIn(0f, 1f)
        val damping = 0.35f + 0.65f * taper
        return baselineY + amplitudePx * damping * sin(2f * PI.toFloat() * (x / wavelengthPx - phase))
    }

    val edge = min(WaveEdgeSlope.toPx(), xEnd)
    val edgeStartX = xEnd - edge
    val edgeStartY = surfaceY(edgeStartX)

    path.reset()
    path.moveTo(0f, height)
    path.lineTo(0f, surfaceY(0f))
    var x = stepPx
    while (x < edgeStartX) {
        path.lineTo(x, surfaceY(x))
        x += stepPx
    }
    path.lineTo(edgeStartX, edgeStartY)
    path.cubicTo(
        edgeStartX + edge * 0.55f, edgeStartY,
        xEnd, edgeStartY + (height - edgeStartY) * 0.35f,
        xEnd, height,
    )
    path.close()
    drawPath(path = path, color = color)
}
