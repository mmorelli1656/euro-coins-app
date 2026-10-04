package com.michele.eurocoins.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.RegularCollectionEntry
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.YearOption
import com.michele.eurocoins.data.defaultYearOption
import com.michele.eurocoins.data.regularYearOptions
import com.michele.eurocoins.data.seriesTitle
import com.michele.eurocoins.data.stableKey
import com.michele.eurocoins.data.varietyFor
import java.time.Year

/** Una casella della bozza: un anno (con varietà) in una finitura. Chiave delle mappe di spunte e prezzi. */
private data class DraftKey(val year: Int, val variety: String, val quality: CoinQuality)

/**
 * Pannello per registrare una moneta circolante posseduta. **Stesso pannello delle commemorative**
 * ([CollectionSheet]: tre card Standard / BU / Proof con il prezzo a destra, riuso di
 * [FinishCard]) più UN selettore dell'anno in cima: nelle commemorative l'anno è nel dataset, qui
 * (una serie copre più anni con lo stesso disegno) lo sceglie l'utente. Prima c'era una lista di
 * righe anno + prezzo da digitare, con "Add year" e rimozione: troppo diversa dall'altro pannello,
 * lenta per chi vuole solo registrare una moneta, e l'anno scritto a mano permetteva anni
 * impossibili o doppi.
 *
 * - **L'anno si sceglie da una lista** ([regularYearOptions]: dal primo anno della serie fino
 *   all'ultimo o a oggi), mai scritto. Il 2002 greco ha due voci (normale ed EFS): sono due monete.
 * - **Default = il primo anno della serie** (scelta dell'utente; non prima del 2002), con Standard
 *   già spuntata alla prima apertura come nelle commemorative: aprire e premere Save sono due
 *   tocchi. Se qualcosa è già in collezione si apre sulla prima voce posseduta.
 * - **Bozza per anno + Save**: cambiare anno non perde le spunte e i prezzi inseriti, "Save" scrive
 *   tutti gli anni insieme, chiudere senza salvare non cambia nulla (come [CollectionSheet]). Gli
 *   anni con almeno una finitura in bozza hanno un ✓ nel menu e sono riassunti accanto al selettore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularCollectionSheet(
    countryName: String,
    series: RegularIssueSeries,
    /** Posizione (1-based) della serie nella lista del paese: vedi `seriesTitle` in `RegularIssueText.kt`. */
    seriesNumber: Int,
    denomination: RegularIssueImage,
    currentItems: List<RegularCollectionItem>,
    onSave: (List<RegularCollectionEntry>) -> Unit,
    onDismiss: () -> Unit,
) {
    val stateKey = "${series.stableKey}|${denomination.taglio}"
    val currentYear = remember { Year.now().value }
    val owned = remember(stateKey) { currentItems.map { YearOption(it.anno, it.variety) }.distinct() }
    val options = remember(stateKey) { regularYearOptions(series, denomination, owned, currentYear) }
    val initial = remember(stateKey) { defaultYearOption(options, owned) }
    var selected by remember(stateKey) { mutableStateOf(initial) }

    val checked = remember(stateKey) {
        mutableStateMapOf<DraftKey, Boolean>().apply {
            currentItems.forEach { this[DraftKey(it.anno, it.variety, it.quality)] = true }
            // prima apertura: Standard già spuntata sull'anno di default, come nelle commemorative
            if (currentItems.isEmpty()) this[DraftKey(initial.year, initial.variety, CoinQuality.STANDARD)] = true
        }
    }
    val prices = remember(stateKey) {
        mutableStateMapOf<DraftKey, String>().apply {
            currentItems.forEach { this[DraftKey(it.anno, it.variety, it.quality)] = formatPrice(it.priceCents) }
        }
    }
    val withData: Set<YearOption> = checked.filterValues { it }.keys.map { YearOption(it.year, it.variety) }.toSet()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            Text(
                text = "MY COLLECTION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = denomination.taglio,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = "$countryName · ${seriesTitle(seriesNumber)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            YearSelector(
                options = options,
                selected = selected,
                withData = withData,
                varietyDetail = { series.varietyFor(denomination.taglio, it.year)?.detail },
                onSelect = { selected = it },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinQuality.entries.forEach { quality ->
                    val key = DraftKey(selected.year, selected.variety, quality)
                    FinishCard(
                        quality = quality,
                        checked = checked[key] == true,
                        price = prices[key].orEmpty(),
                        onCheckedChange = { checked[key] = it },
                        onPriceChange = { prices[key] = it },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            checked.filterValues { it }.keys.map { key ->
                                RegularCollectionEntry(
                                    anno = key.year,
                                    quality = key.quality,
                                    priceCents = parsePriceCents(prices[key].orEmpty()),
                                    variety = key.variety,
                                )
                            },
                        )
                    },
                ) { Text("Save") }
            }
        }
    }
}

/**
 * Riga "Year [2008 ▾]  also: 2011, 2015": la pillola (un controllo azionabile, come gli altri
 * dell'app) apre [YearGridDialog]. A destra, gli ALTRI anni già in bozza in una riga sola con
 * ellissi: dice a colpo d'occhio cosa verrà salvato senza aprire la finestra.
 */
@Composable
private fun YearSelector(
    options: List<YearOption>,
    selected: YearOption,
    withData: Set<YearOption>,
    varietyDetail: (YearOption) -> String?,
    onSelect: (YearOption) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val others = withData.filter { it != selected }.sortedWith(compareBy({ it.year }, { it.variety }))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Text(text = "Year", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, colors.primary),
            onClick = { open = true },
            modifier = Modifier
                .height(40.dp)
                .semantics { contentDescription = "Year: ${selected.label}. Choose another year" },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 10.dp),
            ) {
                Text(text = selected.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        if (others.isNotEmpty()) {
            Text(
                text = "also: " + others.joinToString(", ") { it.label.replace(" · EFS variety", " EFS") },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (open) {
        YearGridDialog(
            options = options,
            selected = selected,
            withData = withData,
            varietyDetail = varietyDetail,
            onSelect = {
                onSelect(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** Colonne della griglia degli anni: 25 anni sono 5 righe piene, il Belgio (28 con le monete datate 1999-2001) una in più. */
private const val YearGridColumns = 5

/**
 * Scelta dell'anno: una finestra centrata (come gli altri dialog dell'app, stessa palette) con
 * TUTTI gli anni in una griglia a 5 colonne, senza scorrere una lista a colonna singola — il menu
 * a tendina standard di Material, scartato dopo averlo visto sul telefono: lungo, di un grigio
 * fuori palette, anni lontani irraggiungibili senza scorrere. Un tocco sceglie e chiude. L'anno
 * scelto è lilla (la selezione dell'app), quelli già in collezione hanno un puntino verde; la
 * varietà EFS è una cella a parte con "EFS" sotto l'anno. Celle con angoli morbidi, non pillole:
 * sono contenuto da scegliere, non un controllo azionabile a sé.
 */
@Composable
private fun YearGridDialog(
    options: List<YearOption>,
    selected: YearOption,
    withData: Set<YearOption>,
    varietyDetail: (YearOption) -> String?,
    onSelect: (YearOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { DialogTitle("Select year") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.chunked(YearGridColumns).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        row.forEach { option ->
                            YearCell(
                                option = option,
                                isSelected = option == selected,
                                hasData = option in withData,
                                description = varietyDetail(option)?.takeIf { option.variety.isNotEmpty() },
                                onClick = { onSelect(option) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // l'ultima riga incompleta tiene le celle della stessa larghezza delle altre
                        repeat(YearGridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (withData.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Box(Modifier.size(8.dp).background(colors.primary, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "marked as owned",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun YearCell(
    option: YearOption,
    isSelected: Boolean,
    hasData: Boolean,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (isSelected) colors.onSecondaryContainer.copy(alpha = 0.6f) else colors.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (isSelected) colors.secondaryContainer else Color.Transparent)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(option.label)
                    description?.let { append(", $it") }
                    if (hasData) append(", marked as owned")
                    if (isSelected) append(", selected")
                }
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = option.year.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) colors.onSecondaryContainer else colors.onSurface,
            )
            if (option.variety.isNotEmpty()) {
                Text(
                    text = "EFS",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                )
            }
        }
        if (hasData) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 5.dp)
                    .size(6.dp)
                    .background(colors.primary, CircleShape),
            )
        }
    }
}

