package com.michele.eurocoins.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pannello dal basso del pulsante FILTER. Le scelte si applicano subito (si
 * vede l'elenco cambiare dietro): "Reset" riporta tutto al predefinito,
 * "Done" chiude. Titolo centrato con "Reset" a destra; l'altezza si anima quando una sezione
 * compare o sparisce (es. "Owned quality" solo con Collection = Owned), e scorre se non sta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Stesso fondo del pannello di modifica collezione: senza, Material usa il suo grigio-viola.
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Filter & sort", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Default))
                TextButton(onClick = onReset, modifier = Modifier.align(Alignment.CenterEnd)) { Text("Reset") }
            }
            content()
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

/** Intestazione di un gruppo di sezioni ("Sort", "Filter"): titolo piccolo centrato con un filetto per lato. */
@Composable
fun FilterGroupHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }
}

/** Titolo centrato + una riga di chip tutti della stessa larghezza (le sezioni hanno al massimo 3 opzioni). */
@Composable
private fun ChipSection(title: String, chips: @Composable RowScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // IntrinsicSize.Min + fillMaxHeight: se un'etichetta va a capo (font ingrandito) i chip restano alti uguali.
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = chips,
        )
    }
}

@Composable
private fun RowScope.EqualChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.fillMaxWidth()) },
        modifier = Modifier.weight(1f).fillMaxHeight(),
    )
}

/** Una sezione del pannello: titolo + chip a scelta singola, a larghezza uguale. */
@Composable
fun <T> ChoiceSection(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    ChipSection(title) {
        options.forEach { option ->
            EqualChip(selected = option == selected, text = label(option), onClick = { onSelect(option) })
        }
    }
}

/** Come [ChoiceSection] ma a scelta multipla (nessuna selezione = nessun vincolo). */
@Composable
fun <T> MultiChoiceSection(
    title: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
) {
    ChipSection(title) {
        options.forEach { option ->
            EqualChip(selected = option in selected, text = label(option), onClick = { onToggle(option) })
        }
    }
}
