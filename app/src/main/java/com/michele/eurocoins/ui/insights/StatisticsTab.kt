package com.michele.eurocoins.ui.insights

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michele.eurocoins.data.CollectionStats
import com.michele.eurocoins.data.CountryStat
import com.michele.eurocoins.data.FinishCounts
import com.michele.eurocoins.data.Milestones
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.SpendingStats
import com.michele.eurocoins.data.SpentTarget
import com.michele.eurocoins.data.YearStat
import com.michele.eurocoins.data.flagEmojiForCountry
import com.michele.eurocoins.ui.components.formatPrice
import com.michele.eurocoins.ui.detail.DetailCard
import com.michele.eurocoins.ui.detail.sansTitleMedium
import java.util.Locale

/**
 * Scheda "Statistics" di Insights (Pro). Mockup approvato il 2026-10-25 (la mappa a tessere dei paesi è stata scartata
 * per un elenco, "Added over time" tolta perché le monete si registrano a gruppi e il grafico sarebbe a scalini):
 * Your collection, By country (con i due cataloghi), Milestones, By year, By finish e, solo se almeno una voce ha un
 * prezzo, Spending. Da Free si vede un'anteprima sfocata di "Your collection" con l'invito al Pro. Niente valore di
 * mercato né classifica di rarità (nessuna fonte affidabile: vedi CLAUDE.md § Insights).
 */
@Composable
internal fun StatisticsTab(
    stats: CollectionStats?,
    isPro: Boolean,
    onGoPro: () -> Unit,
    onCommemorativeCountry: (String) -> Unit,
    onRegularCountry: (String) -> Unit,
    onSpentItem: (SpentTarget) -> Unit,
) {
    // Room non ha ancora risposto: niente da mostrare, ma nemmeno un invito al Pro fuori posto.
    if (stats == null) return
    if (!isPro) {
        LockedPreview(stats, onGoPro)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        YourCollectionCard(stats)
        ByCountryCard(stats, onCommemorativeCountry, onRegularCountry)
        MilestonesCard(stats.milestones)
        ByYearCard(stats.years)
        FinishCard(stats.finishes)
        stats.spending?.let { SpendingCard(it, onSpentItem) }
    }
}

/** Altezza della pila sfocata da Free: tre card, la terza che sfuma in fondo. */
private val LockedStackHeight = 700.dp

/** Sfocatura da Free: abbastanza forte da non far leggere nessun numero (richiesta del proprietario, 2026-10-25). */
private val LockedBlur = 12.dp

/**
 * Da Free: l'invito al Pro in CIMA (si vede senza scorrere) e sotto una pila di tre card sfocate (Your collection, By
 * country, Milestones) che sfuma in fondo, così la pagina non sembra vuota e si intuisce cosa c'è dietro il lucchetto
 * (mockup del 2026-10-25). La pila non si legge né si tocca (nessuna descrizione per TalkBack, callback vuote); sotto
 * Android 12 il blur non c'è e resta solo l'attenuazione. Prima c'era la sola prima card, e prima ancora l'invito
 * sovrapposto che la copriva quasi tutta.
 */
@Composable
private fun LockedPreview(stats: CollectionStats, onGoPro: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InviteCard(onGoPro)
        Box(modifier = Modifier.fillMaxWidth().height(LockedStackHeight).clipToBounds()) {
            Column(
                modifier = Modifier
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .blur(LockedBlur)
                    .alpha(0.85f)
                    .clearAndSetSemantics {},
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Milestones prima di By country: quest'ultima è lunga e, in fondo, è quella che sfuma.
                YourCollectionCard(stats)
                MilestonesCard(stats.milestones)
                ByCountryCard(stats, onCommemorativeCountry = {}, onRegularCountry = {})
            }
            // Dissolvenza verso il fondo della pagina: la pila finisce senza un taglio netto.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))),
            )
        }
    }
}

@Composable
private fun InviteCard(onGoPro: () -> Unit) {
    DetailCard {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text(
                text = "Statistics is part of Pro",
                style = sansTitleMedium(),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Progress by country, milestones, and what you've spent. No ads.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Button(onClick = onGoPro, shape = RoundedCornerShape(50)) { Text("Go Pro") }
        }
    }
}

/** La barra sottile di avanzamento di una riga, la stessa tinta ovunque. */
@Composable
private fun StatBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private fun percent(p: Progress): String = String.format(Locale.US, "%.1f%%", p.fraction * 100)

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun YourCollectionCard(stats: CollectionStats) {
    DetailCard {
        InsightsTitle("Your collection")
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = stats.total.owned.toString(),
                style = sansTitleMedium().copy(fontSize = 34.sp, fontWeight = FontWeight.Medium),
            )
            Text(
                text = " of ${stats.total.total} coins · ${percent(stats.total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }
        StatBar(stats.total.fraction, Modifier.padding(top = 6.dp))
        CatalogRow("Commemorative", stats.commemorative)
        CatalogRow("Regular Issues", stats.regular)
    }
}

@Composable
private fun CatalogRow(name: String, progress: Progress) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        // Conteggio e percentuale pesano uguale, separati dal puntino come nel totale qui sopra (scelta del proprietario).
        Text(
            text = "${progress.owned} / ${progress.total} · ${percent(progress)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    StatBar(progress.fraction)
}

/** Paesi completi in fondo; gli altri dal più vicino al completamento (frazione più alta), a parità per nome. */
private val closestFirst = compareBy<CountryStat>(
    { it.progress.total > 0 && it.progress.owned == it.progress.total },
    { -it.progress.fraction },
    { it.name },
)

private const val COUNTRIES_SHOWN = 6

@Composable
private fun ByCountryCard(
    stats: CollectionStats,
    onCommemorativeCountry: (String) -> Unit,
    onRegularCountry: (String) -> Unit,
) {
    var regular by rememberSaveable { mutableStateOf(false) }
    var byName by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val source = if (regular) stats.regularCountries else stats.commemorativeCountries
    val sorted = if (byName) source.sortedBy { it.name } else source.sortedWith(closestFirst)
    val shown = if (showAll) sorted else sorted.take(COUNTRIES_SHOWN)

    DetailCard {
        InsightsTitle("By country")
        Hint("How complete each country is. Tap one to see what's missing.", Modifier.padding(top = 4.dp))
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !regular, onClick = { regular = false; showAll = false }, label = { Text("Commemorative") })
            FilterChip(selected = regular, onClick = { regular = true; showAll = false }, label = { Text("Regular Issues") })
        }
        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !byName, onClick = { byName = false }, label = { Text("Closest to complete") })
            FilterChip(selected = byName, onClick = { byName = true }, label = { Text("A to Z") })
        }
        Column(modifier = Modifier.animateContentSize().padding(top = 6.dp)) {
            shown.forEach { country ->
                CountryRow(country, onClick = { if (regular) onRegularCountry(country.paese) else onCommemorativeCountry(country.paese) })
            }
        }
        if (sorted.size > COUNTRIES_SHOWN) {
            Text(
                text = if (showAll) "Show fewer" else "Show all ${sorted.size} countries",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showAll = !showAll }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun CountryRow(country: CountryStat, onClick: () -> Unit) {
    val p = country.progress
    val complete = p.total > 0 && p.owned == p.total
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(flagEmojiForCountry(country.paese), fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(country.name, style = MaterialTheme.typography.bodyMedium)
                Hint(if (complete) "${p.owned} / ${p.total} · complete" else "${p.owned} / ${p.total} · ${p.total - p.owned} missing")
            }
            StatBar(p.fraction, Modifier.padding(top = 4.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MilestonesCard(m: Milestones) {
    DetailCard {
        InsightsTitle("Milestones")
        Hint("Complete sets, the things collectors count.", Modifier.padding(top = 4.dp, bottom = 4.dp))
        MilestoneRow("Commemorative countries complete", m.countries)
        MilestoneRow("Commemorative years complete", m.years)
        MilestoneRow("Regular series complete", m.regularSeries)
        MilestoneRow("Joint 2 euro issues complete", m.jointIssues)
    }
}

@Composable
private fun MilestoneRow(label: String, progress: Progress) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(progress.owned.toString(), style = sansTitleMedium().copy(fontSize = 20.sp, fontWeight = FontWeight.Medium))
                Hint(" / ${progress.total}", Modifier.padding(bottom = 2.dp))
            }
        }
        StatBar(progress.fraction, Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ByYearCard(years: List<YearStat>) {
    if (years.isEmpty()) return
    DetailCard {
        InsightsTitle("By year")
        Hint("Commemorative coins you own, year by year. The empty part is what's missing.", Modifier.padding(top = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(96.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            years.forEach { y ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(y.progress.fraction.coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val labels = listOf(years.first().anno, years[years.size / 2].anno, years.last().anno).distinct()
            labels.forEach { Hint(it.toString()) }
        }
    }
}

@Composable
private fun FinishCard(f: FinishCounts) {
    DetailCard {
        InsightsTitle("By finish")
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            FinishColumn("Standard", f.standard, Modifier.weight(1f))
            FinishColumn("BU", f.bu, Modifier.weight(1f))
            FinishColumn("Proof", f.proof, Modifier.weight(1f))
        }
        Hint(
            "Commemorative coins. A coin can have more than one finish.",
            Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@Composable
private fun FinishColumn(label: String, count: Int, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = sansTitleMedium().copy(fontSize = 26.sp, fontWeight = FontWeight.Medium))
        Hint(label)
    }
}

internal fun euro(cents: Long): String = "€" + formatPrice(cents.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())

@Composable
private fun SpendingCard(s: SpendingStats, onSpentItem: (SpentTarget) -> Unit) {
    DetailCard {
        InsightsTitle("Spending")
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
            Hint(
                if (s.itemsWithPrice < s.totalItems) {
                    "Based on ${s.itemsWithPrice} of ${s.totalItems} items with a price. Add prices to see the full picture."
                } else {
                    "Based on all ${s.totalItems} items."
                },
            )
        }
        // IntrinsicSize.Min: con il font ingrandito "Average per item" va a capo e i due riquadri restano alti uguali.
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpendTile("Total spent", euro(s.totalCents), Modifier.weight(1f).fillMaxHeight())
            SpendTile("Average per item", euro(s.averageCents), Modifier.weight(1f).fillMaxHeight())
        }
        MostExpensive(s.ranking, onSpentItem)
        if (s.byYear.size >= 2) {
            Hint("Spent per year, from the ${s.itemsInYears} items with a price and a date.", Modifier.padding(top = 14.dp))
            val max = s.byYear.maxOf { it.second }.coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                s.byYear.forEach { (year, cents) ->
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight((cents.toFloat() / max).coerceIn(0.02f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        // L'anno sotto la sua colonna: con molti anni un elenco a parte non starebbe allineato.
                        Hint(year.toString(), Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Hint(label)
        Text(value, style = sansTitleMedium().copy(fontSize = 20.sp, fontWeight = FontWeight.Medium))
    }
}
