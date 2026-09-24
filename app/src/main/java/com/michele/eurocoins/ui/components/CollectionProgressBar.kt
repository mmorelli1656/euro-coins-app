package com.michele.eurocoins.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.Progress
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Durata dell'animazione al primo avvio: 1300 ms faceva attendere troppo, 800 ms risultava un
 * po' troppo veloce (e in parte coperta dall'animazione di apertura dell'app); qui in mezzo.
 */
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
 * Curva "Emphasized" di Material 3 (cubic-bezier 0.2, 0, 0, 1): parte più decisa e rallenta più a
 * lungo verso la fine di quanto faccia la FastOutSlowIn standard (0.4, 0, 0.2, 1) — pensata per
 * un movimento che si nota, non per una transizione qualunque dell'interfaccia.
 */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** "Ease-out" standard (cubic-bezier 0, 0, 0.58, 1): parte spedita e rallenta solo verso la fine
 * — per la dissolvenza finale dell'onda, che deve fondersi nella linea dritta senza scatti. */
private val EaseOutEasing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

// Geometria dell'onda: altezza del box che la contiene, spessore del tratto e ampiezza massima
// dell'oscillazione. Il giro precedente (8 dp di ampiezza, 3.5 creste) risultava un "rimbalzo"
// grosso e lento; qui l'ampiezza scende un po' e le creste aumentano, per un'onda più fitta e
// meno vistosa nel singolo balzo. Il box resta alto abbastanza da non tagliarla.
private val WaveBoxHeight = 20.dp
private val WaveStrokeWidth = 5.dp
private val WaveAmplitude = 4.dp

/**
 * Lunghezza d'onda FISSA in dp, non proporzionale alla larghezza della barra: con "quante creste
 * sull'intera barra" (il tentativo precedente) a inizio riempimento, quando la parte colorata è
 * ancora stretta (es. 43/499), nella parte piena ci stava meno di un'onda intera e sembrava un
 * singolo rigonfiamento invece di un'onda fitta. Una lunghezza fissa dà sempre la stessa densità
 * di creste, dal primo pixel riempito in poi — è anche come la definisce la specifica Material.
 */
private val WaveWavelength = 16.dp

/** Tempo per un ciclo completo della fase: 900 ms sembrava ancora "vibrare"; il doppio, per un
 * flusso lento e ipnotico invece che frenetico. */
private const val WAVE_PERIOD_MS = 1800

/** Passo di campionamento del percorso, in dp: più piccolo = curva più morbida, più punti da disegnare. */
private val WAVE_STEP = 3.dp

/**
 * Stato dell'animazione della barra "x / y collected":
 *
 * - **Primo avvio (cold start)**: [playIntro] vero e [lastShown] nullo → [shownOwned] sale da 0
 *   al valore vero; la parte riempita cresce da 0 di larghezza e, mentre cresce, ha una forma a
 *   onda sinuosa che scorre (stile "wavy progress" di Material You / Play Store), non un
 *   riempimento a bordo dritto.
 * - **Si torna alla schermata nello stesso processo**: [lastShown] arriva già valorizzato dal
 *   ViewModel, si riparte da lì e solo la differenza si anima.
 * - **Il valore cambia a schermata visibile**: stesso trattamento del caso precedente.
 *
 * Mentre il valore è in movimento l'onda è "gonfia" ([waveAmplitude] vicino a 1); appena
 * l'animazione finisce si appiattisce dolcemente verso una linea dritta (0) in mezzo secondo
 * circa — più lenta a calmarsi di quanto sia stata rapida a gonfiarsi. Alla fine di ogni
 * animazione un piccolo impulso ("pulse", scala 1 → 1.08 → 1) tocca barra e numero. "Resume da
 * RAM" non ha bisogno di codice dedicato: finché il processo resta vivo l'Activity non viene
 * distrutta e Compose conserva lo stato così com'era.
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
    val waveAmplitude = remember { Animatable(0f) }
    var filling by remember { mutableStateOf(false) }

    LaunchedEffect(owned, ready) {
        if (!ready) return@LaunchedEffect
        val isIntro = playIntro && lastShown == null
        if (isIntro || ownedAnim.value.roundToInt() != owned) {
            // Solo al primo avvio: lascia finire l'animazione di apertura dell'app prima di partire.
            // Se l'utente esce durante l'attesa il LaunchedEffect viene annullato prima di
            // onShown: al ritorno l'intro riparte, come deve.
            if (isIntro) delay(INTRO_START_DELAY_MS)
            filling = true
            try {
                // Curva Emphasized per l'allungamento della barra (un giro intermedio l'aveva
                // portata alla FastOutSlowIn standard, su richiesta di allora; qui si torna
                // all'Emphasized, richiesta esplicita più recente). La fase invece scorre a
                // velocità costante (Linear, più sotto): il movimento orizzontale non accelera
                // né rallenta mai, indipendentemente da come accelera il riempimento.
                ownedAnim.animateTo(
                    owned.toFloat(),
                    tween(if (isIntro) INTRO_DURATION_MS else UPDATE_DURATION_MS, easing = EmphasizedEasing),
                )
            } finally {
                filling = false
            }
            // Micro-pulse di conferma, solo quando è appena finito di riempirsi.
            pulse.animateTo(1.08f, tween(120, easing = EmphasizedEasing))
            pulse.animateTo(1f, tween(180, easing = EmphasizedEasing))
        }
        onShown(owned)
    }

    // Ampiezza separata dal riempimento: sale in fretta (250 ms, Emphasized) quando si inizia a
    // riempire, scende (300 ms, ease-out) quando ci si ferma: parte ancora decisa ma rallenta
    // verso zero, così le creste si fondono nella linea dritta senza uno scatto visibile alla
    // fine (un "ease-in-out" simmetrico, provato prima, partiva già lento e sembrava trascinarsi).
    LaunchedEffect(filling) {
        waveAmplitude.animateTo(if (filling) 1f else 0f, tween(if (filling) 250 else 300, easing = EaseOutEasing))
    }

    return remember(ownedAnim, pulse, waveAmplitude) { ProgressAnimation(ownedAnim, pulse, waveAmplitude) }
}

class ProgressAnimation internal constructor(
    private val ownedAnim: Animatable<Float, AnimationVector1D>,
    private val pulse: Animatable<Float, AnimationVector1D>,
    private val waveAmplitude: Animatable<Float, AnimationVector1D>,
) {
    val shownOwned: Float get() = ownedAnim.value
    val pulseScale: Float get() = pulse.value

    /** 0 = linea dritta, 1 = ampiezza massima dell'onda. */
    val waveAmplitudeFraction: Float get() = waveAmplitude.value
}

/**
 * Barra "x / y possedute" usata su home, card anno e card paese.
 *
 * [animation] (da [rememberProgressAnimation]) è opzionale: le card di Years/Countries non lo
 * passano (499 barre non hanno bisogno di animarsi una per una, tantomeno di ondeggiare tutte
 * insieme) e restano una barra dritta come prima. Il conteggio testuale è dietro un
 * `derivedStateOf`, che si ricompone solo quando il numero intero cambia.
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
        } else {
            WavyProgressBar(
                progress = progress,
                animation = animation,
                color = color,
                trackColor = trackColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "$shownOwned / ${progress.total} collected",
            style = labelStyle,
            color = labelColor,
            modifier = Modifier
                .padding(top = 4.dp)
                .graphicsLayer { scaleX = animation?.pulseScale ?: 1f; scaleY = animation?.pulseScale ?: 1f },
        )
    }
}

/**
 * Il tratto riempito è un percorso sinusoidale, non un rettangolo: ampiezza e fase animate danno
 * l'effetto "wavy" di Material You. La parte non ancora riempita resta una linea dritta in
 * [trackColor], come il resto vuoto di una barra normale.
 */
@Composable
private fun WavyProgressBar(
    progress: Progress,
    animation: ProgressAnimation,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val amplitudeFraction = animation.waveAmplitudeFraction
    // La fase scorre solo quando c'è ampiezza da mostrare: a barra piatta non serve animare nulla.
    val phase = if (amplitudeFraction > 0.001f) {
        val infinite = rememberInfiniteTransition(label = "wave_phase")
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(WAVE_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        p
    } else {
        0f
    }

    // Niente graphicsLayer/scaleY qui: scalare verticalmente il Canvas del pulse (come faceva
    // prima) stira l'onda in verticale per un istante subito dopo il riempimento — esattamente il
    // "salto"/"sobbalzo" percepito, proprio nel momento in cui l'onda dovrebbe placarsi e
    // scorrere solo in orizzontale. Il pulse resta solo sul numero (sotto).
    Canvas(modifier = modifier.height(WaveBoxHeight)) {
        val strokeWidthPx = WaveStrokeWidth.toPx()
        val amplitudePx = WaveAmplitude.toPx() * amplitudeFraction
        val centerY = size.height / 2f
        val fraction = if (progress.total == 0) 0f else (animation.shownOwned / progress.total).coerceIn(0f, 1f)
        val filledWidth = size.width * fraction

        if (filledWidth < size.width) {
            drawLine(
                color = trackColor,
                start = Offset(filledWidth, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }

        if (filledWidth > 0f) {
            val wavelengthPx = WaveWavelength.toPx()
            val stepPx = WAVE_STEP.toPx()
            val path = Path()
            var x = 0f
            var first = true
            fun yAt(px: Float) = centerY + amplitudePx * sin(2f * PI.toFloat() * (px / wavelengthPx - phase))
            while (x < filledWidth) {
                if (first) {
                    path.moveTo(x, yAt(x))
                    first = false
                } else {
                    path.lineTo(x, yAt(x))
                }
                x += stepPx
            }
            path.lineTo(filledWidth, yAt(filledWidth))
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
