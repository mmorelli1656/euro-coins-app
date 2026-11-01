package com.michele.eurocoins.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.BadgeTarget
import com.michele.eurocoins.data.CollectionStats
import com.michele.eurocoins.data.Goal
import com.michele.eurocoins.data.GoalKind
import com.michele.eurocoins.data.GoalOption
import com.michele.eurocoins.data.GoalRow
import com.michele.eurocoins.data.MAX_GOALS
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.StatsCard
import com.michele.eurocoins.data.goalOptions
import com.michele.eurocoins.data.flagEmojiForCountry
import com.michele.eurocoins.data.suggestedGoal

/** Gli obiettivi personali visti dalla card: le righe di oggi e cosa fare quando si aggiunge o si toglie (vuoti da Free). */
internal class GoalsUi(
    val rows: List<GoalRow>,
    val goals: List<Goal>,
    val onAdd: (Goal) -> Unit,
    val onRemove: (String) -> Unit,
) {
    companion object {
        val Empty = GoalsUi(emptyList(), emptyList(), {}, {})
    }
}

/**
 * Milestones (2026-11-01, mockup approvato dopo tre giri): gli obiettivi li sceglie l'UTENTE, da elenchi chiusi generati dal
 * catalogo (vedi [Goal]); la card ne mostra uno per riga con un anello di avanzamento, il nome, "x / y · N to go" (o "Done"), la
 * freccia verso ciò che manca e una X per toglierlo. In fondo la pillola "New milestone" (al massimo [MAX_GOALS]). A card vuota
 * un invito con il suggerimento più vicino. Prima erano quattro traguardi fissi ("0 / 24 paesi"), a zero per quasi tutti e senza un
 * dopo.
 */
@Composable
internal fun MilestonesCard(stats: CollectionStats, ui: GoalsUi, onGo: (BadgeTarget) -> Unit) {
    var creating by rememberSaveable { mutableStateOf(false) }
    val suggestion = suggestedGoal(stats, ui.goals)
    StatCard(StatsCard.MILESTONES.title, StatsCard.MILESTONES.icon) {
        Column(modifier = Modifier.padding(top = 6.dp)) {
            if (ui.rows.isEmpty()) {
                Hint("Set a goal for yourself, like completing a country.", Modifier.padding(vertical = 8.dp))
                if (suggestion != null) {
                    SuggestionRow(suggestion) { ui.onAdd(Goal(GoalKind.COUNTRY, suggestion.key)) }
                }
            }
            ui.rows.forEach { row -> GoalRowView(row, onGo, onRemove = { ui.onRemove(row.goal.id) }) }
        }
        val full = ui.goals.size >= MAX_GOALS
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            OutlinedButton(onClick = { creating = true }, enabled = !full && !LocalLockedStats.current, shape = RoundedCornerShape(50)) {
                Text("New milestone")
            }
        }
        if (full) Hint("Up to $MAX_GOALS at a time. Remove one to add another.", Modifier.fillMaxWidth().padding(top = 6.dp))
    }
    if (creating) NewMilestoneSheet(stats, ui, onDismiss = { creating = false })
}

/** Il suggerimento della card vuota: si tocca e l'obiettivo è creato. */
@Composable
private fun SuggestionRow(option: GoalOption, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !LocalLockedStats.current, onClick = onTap)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProgressRing(option.progress)
        Column(modifier = Modifier.weight(1f)) {
            Text("Suggested: Complete ${option.label}", style = MaterialTheme.typography.bodyLarge)
            Hint("${option.progress.total - option.progress.owned} to go. Tap to add it.")
        }
    }
}

@Composable
private fun GoalRowView(row: GoalRow, onGo: (BadgeTarget) -> Unit, onRemove: () -> Unit) {
    val go = row.go
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = go != null) { go?.let(onGo) }
            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProgressRing(row.progress, done = row.done)
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, style = MaterialTheme.typography.bodyLarge)
            if (row.done) {
                Text("Done · well played", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                Hint("${row.progress.owned} / ${row.progress.total} · ${row.progress.total - row.progress.owned} to go")
            }
        }
        // La freccia c'è solo dove il tocco apre qualcosa; lo spazio resta, così le X restano allineate.
        Box(modifier = Modifier.size(24.dp)) {
            if (go != null) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp), enabled = !LocalLockedStats.current) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove ${row.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** L'anello di avanzamento: traccia tenue, arco pieno dall'alto e al centro il numero posseduto (una spunta se raggiunto). */
@Composable
internal fun ProgressRing(progress: Progress, done: Boolean = false) {
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val fill = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arc = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = stroke)
            val sweep = if (done) 1f else progress.fraction.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(fill, -90f, 360f * sweep, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = stroke)
            }
        }
        if (done) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = fill, modifier = Modifier.size(22.dp))
        } else {
            Text(progress.owned.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

private fun GoalKind.title(): String = when (this) {
    GoalKind.COUNTRY -> "Country"
    GoalKind.YEAR -> "Year"
    GoalKind.SERIES -> "Series"
    GoalKind.COINS -> "Coins"
}

/**
 * "New milestone" (2026-11-01, mockup approvato in due giri): UN solo pannello dal basso. In cima lo stesso selettore a segmenti di
 * Progress (Country, Year, Series, Coins), sotto l'elenco di quel tipo: niente secondo passo e quindi niente "Back" (c'era, in alto
 * a sinistra, il punto più lontano dal pollice, e il gesto indietro di sistema chiudeva tutto). Niente testo da scrivere: ogni elenco
 * contiene solo ciò che si può ancora completare, dal più vicino, con l'avanzamento accanto; quello che c'è già è spento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMilestoneSheet(stats: CollectionStats, ui: GoalsUi, onDismiss: () -> Unit) {
    var kindName by rememberSaveable { mutableStateOf(GoalKind.COUNTRY.name) }
    val kind = GoalKind.entries.firstOrNull { it.name == kindName } ?: GoalKind.COUNTRY
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(
                text = "New milestone",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Default),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                GoalKind.entries.forEachIndexed { index, k ->
                    SegmentedButton(
                        selected = k == kind,
                        onClick = { kindName = k.name },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = GoalKind.entries.size),
                        // Nessuna spunta: sposterebbe l'etichetta (come negli altri selettori dell'app).
                        icon = {},
                        label = { Text(k.title(), maxLines = 1) },
                    )
                }
            }
            Hint("Only what you can still complete.", Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp))
            // Altezza FISSA: cambiando tipo il pannello non deve ridimensionarsi (gli elenchi hanno lunghezze diverse; con solo un minimo il tipo Coins, di sei righe, restava più basso).
            Column(modifier = Modifier.height(440.dp).verticalScroll(rememberScrollState())) {
                val options = goalOptions(kind, stats, ui.goals)
                if (options.isEmpty()) Hint("Nothing left here to complete.", Modifier.padding(vertical = 16.dp))
                options.forEach { option ->
                    OptionRow(kind, option) {
                        ui.onAdd(Goal(kind, option.key))
                        onDismiss()
                    }
                }
                Box(modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}

/**
 * Una scelta. Davanti al nome: la bandiera per paesi e serie (come in Progress), la pila di monete per i numeri (la stessa delle
 * medaglie "100 / 250 / 500 coins", che cresce con l'obiettivo); gli ANNI restano solo il numero, che è già il suo simbolo (e
 * nemmeno le emissioni comuni hanno un segno: nell'elenco non c'è nessuna scritta, e il tocco su un anno le conta insieme).
 */
@Composable
private fun OptionRow(kind: GoalKind, option: GoalOption, onTap: () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    val alpha = if (option.taken) 0.45f else 1f
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !option.taken, onClick = onTap).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            option.paese != null -> Text(flagEmojiForCountry(option.paese), fontSize = 22.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
            kind == GoalKind.COINS -> {
                val tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(26.dp)) { drawBadgeArt(coinStackFor(option.key.toIntOrNull() ?: 0), tint) }
                }
            }
        }
        Text(
            option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.weight(1f),
        )
        Text(
            if (option.taken) "Already added" else "${option.progress.owned} / ${option.progress.total}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}

/** La pila di monete di un obiettivo: una colonna fino a 249, due fino a 499, tre da 500 (le stesse soglie delle medaglie). */
internal fun coinStackFor(target: Int): BadgeArt = when {
    target >= 500 -> BadgeArt.STACK_500
    target >= 250 -> BadgeArt.STACK_250
    else -> BadgeArt.STACK_100
}
