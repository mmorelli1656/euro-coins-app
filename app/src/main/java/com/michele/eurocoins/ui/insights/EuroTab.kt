package com.michele.eurocoins.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.michele.eurocoins.data.flagEmojiForIsoCode
import com.michele.eurocoins.ui.detail.DetailCard
import com.michele.eurocoins.ui.detail.SourceCredits
import com.michele.eurocoins.ui.detail.SourceItem
import com.michele.eurocoins.ui.detail.sansTitleMedium
import com.michele.eurocoins.ui.regular.DenominationCoin
import com.michele.eurocoins.ui.regular.coinScaleFor

/**
 * Scheda "History" di Insights, libera per tutti: come si è arrivati alla moneta unica e chi l'ha adottata (con la
 * mappa del lato comune al 2007) e gli stati che la usano per accordo. Mockup approvato il 2026-10-25; semplificata su
 * richiesta ("troppo pesante e prolissa, con troppi indicatori"): una sola tinta, niente legenda, niente cerchi con i
 * conteggi, le tappe prima del 1999 e le due foto del 2007 chiuse finché non si aprono. Ogni voce della storia è
 * un anno con un sottotesto. Il riquadro con i dettagli di un paese è tornato dopo la prova sul telefono: piaceva.
 */
@Composable
internal fun HistoryTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JoiningCard()
        AgreementCard()
        SourceCredits(
            sources = HISTORY_SOURCES.map { (label, value, url) -> SourceItem(label, value, url) },
            licenseLine = null,
        )
    }
}

/** Scheda "Coins" di Insights, libera per tutti: di che metalli sono fatte le monete (la card che spiega quella toccata). */
@Composable
internal fun CoinsTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MaterialsCard()
        SourceCredits(
            sources = COIN_SOURCES.map { (label, value, url) -> SourceItem(label, value, url) },
            licenseLine = null,
        )
    }
}

/** Titolo di una card: 20 sp Bold nel colore del testo, come i titoli di sezione delle Impostazioni. */
@Composable
internal fun InsightsTitle(text: String) {
    Text(text = text, style = sansTitleMedium().copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
}

@Composable
private fun JoiningCard() {
    var roadOpen by rememberSaveable { mutableStateOf(false) }
    // Il paese toccato (codice ISO): il suo riquadro compare sotto il suo gruppo.
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val toggle: (String) -> Unit = { iso -> selected = if (selected == iso) null else iso }

    DetailCard {
        InsightsTitle("The story of the Euro")
        Text(
            text = "From the Treaty of Rome to 21 countries.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        TapHint("a country for its dates and rate.", Modifier.padding(top = 2.dp, bottom = 14.dp))

        TimelineRow(isLast = false) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { roadOpen = !roadOpen }
                    .heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ROAD_YEARS, style = sansTitleMedium())
                    Body(ROAD_TEXT)
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (roadOpen) "Hide the steps" else "Show the steps",
                    modifier = Modifier.rotate(chevronAngle(roadOpen)),
                )
            }
            Reveal(roadOpen) {
                ROAD_STEPS.forEach { step ->
                    Row(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = step.year,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp),
                        )
                        Column {
                            Text(step.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = step.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        EURO_ENTRIES.forEachIndexed { index, entry ->
            val items = entry.members.map { it.iso to it.name }
            // Tutte le voci hanno la stessa forma: anno, sottotesto, paesi (se ce ne sono). L'ultima chiude la linea.
            TimelineRow(isLast = index == EURO_ENTRIES.lastIndex) {
                Text(entry.year, style = sansTitleMedium())
                Body(entry.text)
                if (items.isNotEmpty()) CountryGrid(items = items, selected = selected, onClick = toggle)
                entry.members.firstOrNull { it.iso == selected }?.let { CountryPanel(it) }
                if (entry.maps) CommonSideCompare()
            }
        }
    }
}

/**
 * Gli stati che usano l'euro per accordo: una card a parte, non l'ultima voce della timeline (2026-10-25: in fondo alla
 * cronologia sembrava un fatto avvenuto dopo il 2026, e invece non ha una data: Monaco, San Marino e il Vaticano lo
 * usano dal 1999, Andorra dal 2002).
 */
@Composable
private fun AgreementCard() {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    DetailCard {
        InsightsTitle("Also using the euro")
        Text(
            text = "4 countries use the euro and mint their own coins under agreements with the EU. " +
                "They are not euro area members. Tap one for its details.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        CountryGrid(
            items = EURO_AGREEMENTS.map { it.iso to it.name },
            selected = selected,
            onClick = { iso -> selected = if (selected == iso) null else iso },
        )
        EURO_AGREEMENTS.firstOrNull { it.iso == selected }?.let { AgreementPanel(it) }
    }
}

/**
 * Testo corrente giustificato e con la sillabazione, come le note del dettaglio (`NotesCard`): per i paragrafi lunghi,
 * come la spiegazione di un materiale o il testo sotto le due mappe, dove il bordo destro a bandiera risultava
 * irregolare. I sottotesti brevi della storia restano a bandiera.
 */
@Composable
private fun justifiedStyle(): androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium.copy(
    textAlign = TextAlign.Justify,
    lineBreak = LineBreak.Paragraph,
    hyphens = Hyphens.Auto,
)

/** Sottotesto di una voce della storia. */
@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
}

/** Una voce della timeline: pallino e linea a sinistra (una sola tinta), il contenuto a destra. */
@Composable
private fun TimelineRow(
    isLast: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dotY = 12.dp
    val line = MaterialTheme.colorScheme.outline
    val dot = MaterialTheme.colorScheme.primary
    // Linea e pallino si disegnano sulla riga stessa, NON in uno Spacer alto quanto la riga (`IntrinsicSize.Min`): le
    // dimensioni intrinseche vedono il contenuto già aperto e la riga saltava subito all'altezza finale, con il
    // contenuto che si animava dentro un riquadro già grande. Così l'altezza segue l'animazione di apertura e chiusura.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = 10.dp.toPx()
                if (!isLast) {
                    drawLine(
                        color = line,
                        start = Offset(x, dotY.toPx()),
                        end = Offset(x, size.height),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                drawCircle(color = dot, radius = 5.dp.toPx(), center = Offset(x, dotY.toPx()))
            },
    ) {
        Spacer(modifier = Modifier.width(20.dp))
        Column(
            modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = if (isLast) 0.dp else 16.dp),
            content = content,
        )
    }
}

/** Bandiera e nome per intero, due per riga; il tocco sceglie il paese. */
@Composable
private fun CountryGrid(items: List<Pair<String, String>>, selected: String?, onClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        items.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (iso, name) ->
                    CountryItem(iso, name, selected = iso == selected, onClick = onClick, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CountryItem(iso: String, name: String, selected: Boolean, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable { onClick(iso) }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(flagEmojiForIsoCode(iso), fontSize = 20.sp)
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Il confronto della mappa del lato comune, dentro la voce del 2007: una riga lilla (è un controllo) che apre le due
 * foto BCE e due frasi. Chiuso di default, e le foto si scaricano solo quando si apre (niente rete se non si guarda).
 */
@Composable
private fun CommonSideCompare() {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { open = !open }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Compare the old and the new map", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = if (open) "Hide the maps" else "Show the maps",
            modifier = Modifier.rotate(chevronAngle(open)),
        )
    }
    Reveal(open) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MapPhoto(COMMON_SIDE_OLD_URL, "Until 2006", Modifier.weight(1f))
            MapPhoto(COMMON_SIDE_NEW_URL, "From 2007", Modifier.weight(1f))
        }
        // Giustificato (vedi `justifiedStyle`): a bandiera, il bordo destro era irregolare accanto alle due foto,
        // che hanno i bordi dritti.
        Text(
            text = COMMON_SIDE_TEXT,
            style = justifiedStyle(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    }
}

/**
 * L'invito a toccare sotto il titolo di History e di Coins (2026-11-01, mockup approvato): bandiere e monete sembrano testo
 * fisso, e "Tap" in una frase grigia non si notava. Su una riga sua, con l'icona del dito e "Tap" in grassetto nel colore
 * primario, il resto in grigio come il sottotesto. Le bandiere e le monete restano senza riquadro (21 bordi sarebbero troppo).
 */
@Composable
private fun TapHint(rest: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append("Tap") }
                append(" ")
                append(rest)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Apertura e chiusura leggere dei contenuti a comparsa di History (2026-10-26): si apre e si chiude con la stessa curva,
 * un breve scorrimento in altezza con dissolvenza (220 ms in apertura, 180 in chiusura, la chiusura un poco più svelta).
 * Prima era un `animateContentSize` con una molla: in apertura il riquadro si riempiva a scatto, in chiusura il contenuto
 * spariva subito e restava solo il vuoto che si restringeva (la chiusura "non c'era").
 */
@Composable
private fun Reveal(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(
        visible = visible,
        // La dissolvenza segue l'altezza (LinearOutSlowIn sale subito): con quella di default restava trasparente finché
        // il riquadro era già tutto aperto, e si vedeva un vuoto che poi si riempiva.
        // Ancorata in alto (di default è in basso: il contenuto scorreva sotto l'intestazione e in chiusura il primo
        // elemento restava tagliato): si apre e si chiude come una fisarmonica, dal bordo della riga che si tocca.
        enter = expandVertically(tween(220, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) +
            fadeIn(tween(220, easing = LinearOutSlowInEasing)),
        exit = shrinkVertically(tween(180, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) +
            fadeOut(tween(140, easing = FastOutLinearInEasing)),
    ) {
        Column(content = content)
    }
}

/** La freccia gira con la stessa durata del contenuto che apre. */
@Composable
private fun chevronAngle(open: Boolean): Float {
    val angle by animateFloatAsState(if (open) 180f else 0f, tween(200, easing = FastOutSlowInEasing), label = "chevron")
    return angle
}

@Composable
private fun CountryPanel(member: EuroMember) {
    Panel(member.name) {
        PanelRow("Joined the euro", member.joined, first = true)
        PanelRow("Coins and notes", member.coins)
        PanelRow("Replaced", "${member.former} (${member.code})")
        PanelRow("Rate", "€1 = ${member.rate} ${member.code}")
    }
}

@Composable
private fun AgreementPanel(state: AgreementState) {
    // Le stesse righe di un paese dell'area euro, più le due che solo gli stati per accordo hanno.
    Panel(state.name) {
        PanelRow("Uses the euro since", state.since, first = true)
        PanelRow("Coins and notes", state.coins)
        PanelRow("Replaced", state.replaced)
        PanelRow("Rate", state.rate)
        PanelRow("Own euro coins from", state.ownCoinsFrom)
        PanelRow("Agreement", state.agreement)
    }
}

/** Riquadro sul fondo dell'app dentro una card bianca: titolo e righe etichetta-valore. */
@Composable
private fun Panel(title: String, trailing: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(title, style = sansTitleMedium(), modifier = Modifier.padding(bottom = 2.dp))
        // Sempre SOTTO il titolo (2026-10-26): accanto ci stava solo per alcuni materiali ("10 cent: Nordic gold") e per
        // gli altri andava a capo, e il proprietario voleva la stessa posizione ovunque.
        trailing?.let { Box(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) { it() } }
        content()
    }
}

@Composable
private fun PanelRow(label: String, value: String, first: Boolean = false) {
    if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
    }
}

/** Le otto monete in scala e, sotto, la card che spiega il materiale di quella toccata. */
@Composable
private fun MaterialsCard() {
    var selected by rememberSaveable { mutableStateOf("2 euro") }
    val material = COIN_MATERIALS.first { it.taglio == selected }

    DetailCard {
        InsightsTitle("What's in a coin?")
        TapHint("a coin.", Modifier.padding(top = 4.dp))
        COIN_MATERIALS.chunked(4).forEach { coins ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                coins.forEach { coin ->
                    CoinChoice(
                        taglio = coin.taglio,
                        selected = coin.taglio == selected,
                        onClick = { selected = coin.taglio },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Panel("${material.taglio}: ${material.title}", trailing = { MagnetPill(material.magnetism) }) {
            Text(
                text = material.text,
                style = justifiedStyle(),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
            )
            PanelRow("Diameter", "${material.diameter} mm", first = true)
            PanelRow("Weight", "${material.weight} g")
            PanelRow("Thickness", "${material.thickness} mm")
            PanelRow("Edge", material.edge)
        }
        Text(
            text = MATERIALS_NOTE,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** Una moneta disegnata in scala reale (la stessa della scheda Denominations) con il suo taglio sotto. */
@Composable
private fun CoinChoice(taglio: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.height(COIN_SIZE), contentAlignment = Alignment.Center) {
            DenominationCoin(taglio = taglio, maxSize = COIN_SIZE, modifier = Modifier.width(COIN_SIZE * (coinScaleFor(taglio) ?: 1f)))
        }
        Text(taglio, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}

private val COIN_SIZE = 40.dp

/**
 * Il dettaglio della mappa sul 50 cent: la foto BCE intera (540 px, moneta a filo) ritagliata sull'area della
 * mappa, che è dove cambia il disegno. Si ritaglia disegnando la foto più grande del riquadro e spostandola.
 */
@Composable
private fun MapPhoto(url: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = RoundedCornerShape(10.dp)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CROP_WIDTH / CROP_HEIGHT)
                .clip(shape)
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape),
        ) {
            // `requiredSize` più grande del riquadro lo CENTRA: lo spostamento parte da lì (metà dell'eccedenza)
            // e porta in alto a sinistra l'angolo dell'area della mappa.
            val imageWidth = maxWidth * (SOURCE_SIZE / CROP_WIDTH)
            val imageHeight = maxHeight * (SOURCE_SIZE / CROP_HEIGHT)
            AsyncImage(
                model = url,
                contentDescription = "$label: the map on the common side of the 50 cent coin",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(imageWidth, imageHeight)
                    .offset(
                        x = (imageWidth - maxWidth) / 2 - maxWidth * (CROP_LEFT / CROP_WIDTH),
                        y = (imageHeight - maxHeight) / 2 - maxHeight * (CROP_TOP / CROP_HEIGHT),
                    ),
            )
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}

// Area della mappa nella foto BCE da 540×540 px.
private const val SOURCE_SIZE = 540f
private const val CROP_LEFT = 20f
private const val CROP_TOP = 50f
private const val CROP_WIDTH = 240f
private const val CROP_HEIGHT = 332f
