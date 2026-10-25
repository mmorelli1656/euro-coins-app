package com.michele.eurocoins.ui.insights

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.michele.eurocoins.data.flagEmojiForIsoCode
import com.michele.eurocoins.ui.detail.DetailCard
import com.michele.eurocoins.ui.detail.SectionLabel
import com.michele.eurocoins.ui.detail.SourceCredits
import com.michele.eurocoins.ui.detail.SourceItem
import com.michele.eurocoins.ui.detail.sansTitleMedium
import com.michele.eurocoins.ui.regular.DenominationCoin
import com.michele.eurocoins.ui.regular.coinScaleFor

/**
 * Segmento "The euro" di Insights, libero per tutti: come si è arrivati alla moneta unica e chi l'ha adottata, di
 * che metalli sono fatte le monete, cosa è cambiato nel lato comune nel 2007. Mockup approvato il 2026-10-25;
 * semplificato su richiesta ("troppo pesante e prolissa, con troppi indicatori"): una sola tinta, niente legenda,
 * niente cerchi con i conteggi, le tappe prima del 1999 chiuse finché non si aprono. Il riquadro con i dettagli di un
 * paese e la card che spiega il materiale di una moneta sono tornati dopo la prova sul telefono: piacevano.
 */
@Composable
internal fun EuroTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JoiningCard()
        MaterialsCard()
        CommonSideCard()
        SourceCredits(
            sources = EURO_SOURCES.map { (label, value, url) -> SourceItem(label, value, url) },
            licenseLine = "Photos: ECB",
        )
    }
}

@Composable
private fun JoiningCard() {
    var roadOpen by rememberSaveable { mutableStateOf(false) }
    // Il paese toccato (codice ISO): il suo riquadro compare sotto il suo gruppo.
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val toggle: (String) -> Unit = { iso -> selected = if (selected == iso) null else iso }

    DetailCard {
        SectionLabel("Joining the euro")
        Text(
            text = "From the Treaty of Rome to 21 countries. Tap a country for its dates and rate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

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
                    Text("The road to 1999", style = sansTitleMedium())
                    Text(
                        text = "${ROAD_STEPS.size} steps, from 1957",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (roadOpen) "Hide the steps" else "Show the steps",
                    modifier = Modifier.rotate(if (roadOpen) 180f else 0f),
                )
            }
            Column(modifier = Modifier.animateContentSize()) {
                if (roadOpen) {
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
        }

        EURO_ENTRIES.forEach { entry ->
            val items = entry.members.map { it.iso to it.name }
            val member = entry.members.firstOrNull { it.iso == selected }
            // Senza titolo e con uno o due paesi: anno e paesi sulla stessa riga.
            if (entry.title == null && items.size in 1..COMPACT_MAX) {
                TimelineRow(isLast = false, dotY = 20.dp, bottom = 8.dp) {
                    Row {
                        Text(entry.year, style = sansTitleMedium(), modifier = Modifier.width(56.dp).padding(top = 8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            items.forEach { (iso, name) ->
                                CountryItem(iso, name, selected = iso == selected, onClick = toggle, modifier = Modifier.fillMaxWidth())
                            }
                            entry.note?.let { Note(it) }
                            member?.let { CountryPanel(it) }
                        }
                    }
                }
            } else {
                TimelineRow(isLast = false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.year, style = sansTitleMedium())
                        entry.title?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                    entry.note?.let { Note(it) }
                    if (items.isNotEmpty()) CountryGrid(items = items, selected = selected, onClick = toggle)
                    member?.let { CountryPanel(it) }
                }
            }
        }

        TimelineRow(isLast = true, dashed = true) {
            Text("By agreement", style = sansTitleMedium())
            Text(
                text = "They use the euro and mint their own coins, but are not euro area members.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CountryGrid(
                items = EURO_AGREEMENTS.map { it.iso to it.name },
                selected = selected,
                onClick = toggle,
            )
            EURO_AGREEMENTS.firstOrNull { it.iso == selected }?.let { AgreementPanel(it) }
        }
    }
}

/** Una voce della timeline: pallino e linea a sinistra (una sola tinta), il contenuto a destra. */
@Composable
private fun TimelineRow(
    isLast: Boolean,
    dashed: Boolean = false,
    dotY: Dp = 12.dp,
    bottom: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val line = MaterialTheme.colorScheme.outline
    val dot = MaterialTheme.colorScheme.primary
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Spacer(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    if (!isLast) {
                        drawLine(
                            color = line,
                            start = Offset(x, dotY.toPx()),
                            end = Offset(x, size.height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null,
                        )
                    }
                    drawCircle(color = dot, radius = 5.dp.toPx(), center = Offset(x, dotY.toPx()))
                },
        )
        Column(
            modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = if (isLast) 0.dp else bottom),
            content = content,
        )
    }
}

/** Fino a questo numero di paesi, l'anno sta sulla stessa riga dei paesi. */
private const val COMPACT_MAX = 2

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

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp),
    )
}

/** Il riquadro che si apre toccando un paese: date, vecchia valuta e tasso (le righe della BCE). */
@Composable
private fun CountryPanel(member: EuroMember) {
    Panel(member.name) {
        PanelRow("Joined the euro", member.joined, first = true)
        PanelRow("Coins and notes", member.coins)
        member.onlyEuro?.let { PanelRow("Only euro from", it) }
        PanelRow("Replaced", "${member.former} (${member.code})")
        PanelRow("Rate", "€1 = ${member.rate} ${member.code}")
    }
}

@Composable
private fun AgreementPanel(state: AgreementState) {
    Panel(state.name) {
        PanelRow("Euro as official currency", state.official, first = true)
        PanelRow("Own euro coins from", state.coinsFrom)
        PanelRow("Current agreement", state.agreement)
        PanelRow("Status", "By agreement, not a member")
    }
}

/** Riquadro sul fondo dell'app dentro una card bianca: titolo e righe etichetta-valore. */
@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(title, style = sansTitleMedium(), modifier = Modifier.padding(bottom = 2.dp))
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
        SectionLabel("What the coins are made of")
        Text(
            text = "Tap a coin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
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
        Panel("${material.taglio}: ${material.title}") {
            Text(material.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            PanelRow("Diameter", "${material.diameter} mm", first = true)
            PanelRow("Weight", "${material.weight} g")
            PanelRow("Thickness", "${material.thickness} mm")
            PanelRow("Edge", material.edge)
            PanelRow("Magnet", material.magnet)
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

@Composable
private fun CommonSideCard() {
    DetailCard {
        SectionLabel("The common side changed")
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MapPhoto(COMMON_SIDE_OLD_URL, "Until 2006", Modifier.weight(1f))
            MapPhoto(COMMON_SIDE_NEW_URL, "From 2007", Modifier.weight(1f))
        }
        Fact("Before", "Each of the 15 EU countries is a separate piece, with gaps. Eastern Europe is missing.")
        Fact("After", "One continuous Europe, from Iberia to the east.")
        Fact("Why", "The EU grew on 1 May 2004.")
        Text(
            text = "Applies to 10, 20, 50 cent, 1 and 2 euro. Slovenia was first, on 1 January 2007.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

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

@Composable
private fun Fact(label: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(60.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
