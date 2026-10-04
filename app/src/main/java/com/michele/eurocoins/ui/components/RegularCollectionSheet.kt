package com.michele.eurocoins.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.RegularCollectionEntry
import com.michele.eurocoins.data.RegularCollectionItem
import com.michele.eurocoins.data.RegularIssueImage
import com.michele.eurocoins.data.RegularIssueSeries
import com.michele.eurocoins.data.RegularVariety
import com.michele.eurocoins.data.stableKey
import com.michele.eurocoins.data.varietyFor
import com.michele.eurocoins.ui.theme.PurpleFieldDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusLight
import com.michele.eurocoins.ui.theme.PurpleFieldLight

/** Sottotitolo di ogni finitura: stesso testo di `CollectionSheet`. */
private val CoinQuality.descriptor: String
    get() = when (this) {
        CoinQuality.STANDARD -> "Circulation"
        CoinQuality.BU -> "Brilliant Uncirculated"
        CoinQuality.PROOF -> "Mirror finish"
    }

private val YearFieldHeight = 40.dp
private val YearFieldWidth = 64.dp

private var nextEntryId = 0L
private fun newEntryId() = nextEntryId++

/** Una riga di annata in fase di modifica: identità stabile ([id]) per le chiavi di Compose, testo mutabile. */
private class YearEntry(val id: Long, year: String, price: String, variety: String = "") {
    var year by mutableStateOf(year)
    var price by mutableStateOf(price)
    /** Codice della varietà spuntata (`VARIETY_EFS`) o vuoto; vale solo se l'anno ne offre una. */
    var variety by mutableStateOf(variety)
}

/**
 * Pannello per registrare una moneta circolante posseduta: una card per qualità (Standard / BU /
 * Proof, riuso di [CoinQuality] come nelle commemorative), ma sotto ogni qualità spuntata una
 * LISTA di annate — a differenza delle commemorative, qui l'anno non è nel dataset (una
 * `RegularIssueSeries` copre più anni con lo stesso disegno): lo inserisce l'utente, e la stessa
 * qualità può avere più annate (es. Standard 2018 e Standard 2020).
 *
 * Stessa filosofia "bozza + Save" di `CollectionSheet`: chiudere senza salvare non cambia nulla.
 * Alla prima apertura (nessuna voce esistente) Standard è già spuntata con una riga vuota, come
 * nelle commemorative. Le righe con anno vuoto o incompleto (meno di 4 cifre) vengono ignorate al
 * salvataggio, invece di bloccare "Save" con un errore: un modo leggero di scartare bozze non
 * finite di scrivere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularCollectionSheet(
    countryName: String,
    series: RegularIssueSeries,
    /** Posizione (1-based) della serie nella lista del paese: vedi `seriesHeading` in `RegularIssueCountryScreen.kt`. */
    seriesNumber: Int,
    denomination: RegularIssueImage,
    currentItems: List<RegularCollectionItem>,
    onSave: (List<RegularCollectionEntry>) -> Unit,
    onDismiss: () -> Unit,
) {
    val stateKey = "${series.stableKey}|${denomination.taglio}"
    val itemsByQuality = remember(stateKey) { currentItems.groupBy { it.quality } }

    val checked = remember(stateKey) {
        mutableStateMapOf<CoinQuality, Boolean>().apply {
            CoinQuality.entries.forEach { quality ->
                this[quality] = itemsByQuality.containsKey(quality) ||
                    (currentItems.isEmpty() && quality == CoinQuality.STANDARD)
            }
        }
    }
    val years = remember(stateKey) {
        mutableStateMapOf<CoinQuality, SnapshotStateList<YearEntry>>().apply {
            CoinQuality.entries.forEach { quality ->
                val existing = itemsByQuality[quality].orEmpty()
                    .sortedWith(compareBy({ it.anno }, { it.variety }))
                    .map { YearEntry(newEntryId(), it.anno.toString(), formatPrice(it.priceCents), it.variety) }
                this[quality] = if (existing.isEmpty() && checked[quality] == true) {
                    mutableStateListOf(YearEntry(newEntryId(), "", ""))
                } else {
                    existing.toMutableStateList()
                }
            }
        }
    }

    // varietà offerta dall'anno scritto in una riga (oggi solo Grecia 2002, EFS), null altrimenti
    val varietyOf: (YearEntry) -> RegularVariety? = { entry ->
        entry.year.takeIf { it.length == 4 }?.toIntOrNull()?.let { series.varietyFor(denomination.taglio, it) }
    }

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
                text = "$countryName · ${seriesHeadingForSheet(series, seriesNumber)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinQuality.entries.forEach { quality ->
                    QualityCard(
                        quality = quality,
                        checked = checked[quality] == true,
                        years = years[quality].orEmpty(),
                        onCheckedChange = { isChecked ->
                            checked[quality] = isChecked
                            if (isChecked && years[quality].isNullOrEmpty()) {
                                years[quality] = mutableStateListOf(YearEntry(newEntryId(), "", ""))
                            }
                        },
                        varietyOf = varietyOf,
                        onVarietyChange = { entry, isOn -> entry.variety = if (isOn) varietyOf(entry)?.code.orEmpty() else "" },
                        onYearChange = { entry, value ->
                            entry.year = sanitizeYear(value)
                            // cambiando anno la varietà non vale più: niente EFS nascosto su un 2005
                            if (varietyOf(entry) == null) entry.variety = ""
                        },
                        onPriceChange = { entry, value -> entry.price = sanitizePrice(value) },
                        onAddYear = {
                            years.getOrPut(quality) { mutableStateListOf() }.add(YearEntry(newEntryId(), "", ""))
                        },
                        onRemoveYear = { entry -> years[quality]?.remove(entry) },
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
                        val entries = CoinQuality.entries
                            .filter { checked[it] == true }
                            .flatMap { quality ->
                                years[quality].orEmpty().mapNotNull { entry ->
                                    val year = entry.year.takeIf { it.length == 4 }?.toIntOrNull() ?: return@mapNotNull null
                                    RegularCollectionEntry(
                                        anno = year,
                                        quality = quality,
                                        priceCents = parsePriceCents(entry.price),
                                        // salvata solo se l'anno la offre (guardia in più al cambio anno)
                                        variety = if (varietyOf(entry) != null) entry.variety else "",
                                    )
                                }
                            }
                        onSave(entries)
                    },
                ) { Text("Save") }
            }
        }
    }
}

/** Card di una qualità: spunta + etichetta, e se spuntata la lista di annate sotto. */
@Composable
private fun QualityCard(
    quality: CoinQuality,
    checked: Boolean,
    years: List<YearEntry>,
    onCheckedChange: (Boolean) -> Unit,
    varietyOf: (YearEntry) -> RegularVariety?,
    onVarietyChange: (YearEntry, Boolean) -> Unit,
    onYearChange: (YearEntry, String) -> Unit,
    onPriceChange: (YearEntry, String) -> Unit,
    onAddYear: () -> Unit,
    onRemoveYear: (YearEntry) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    // Solo bordo, mai fondo pieno: stesso trattamento di FinishCard in CollectionSheet.kt (la
    // spunta verde è già il segnale di stato, un fondo lilla a tutta card era ridondante e
    // "pesante" secondo l'utente). Bordo pieno a 2.5 dp (non più 1.5 dp al 40% di opacità).
    val borderColor by animateColorAsState(
        targetValue = if (checked) (if (colors.surface.luminance() < 0.5f) PurpleFieldDark else PurpleFieldLight) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "qualityCardBorder",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(2.5.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = quality.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    text = quality.descriptor,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (checked) {
            Column(
                modifier = Modifier.padding(start = 40.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                years.forEach { entry ->
                    key(entry.id) {
                        YearRow(
                            entry = entry,
                            variety = varietyOf(entry),
                            onVarietyChange = { onVarietyChange(entry, it) },
                            onYearChange = { onYearChange(entry, it) },
                            onPriceChange = { onPriceChange(entry, it) },
                            onRemove = { onRemoveYear(entry) },
                        )
                    }
                }
                TextButton(onClick = onAddYear, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add year", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearRow(
    entry: YearEntry,
    /** Varietà offerta da questo anno (Grecia 2002: EFS), null per tutti gli altri. */
    variety: RegularVariety?,
    onVarietyChange: (Boolean) -> Unit,
    onYearChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            YearField(value = entry.year, onValueChange = onYearChange)
            PriceField(value = entry.price, onValueChange = onPriceChange, enabled = true, description = "Price paid (€)")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove this year",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        // compare solo quando l'anno scritto ha una varietà: negli altri casi la riga non cambia
        if (variety != null) {
            val selected = entry.variety == variety.code
            FilterChip(
                selected = selected,
                onClick = { onVarietyChange(!selected) },
                label = {
                    Text(variety.label, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        variety.detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Campo dell'anno: pillola a larghezza fissa (4 cifre), stesso linguaggio visivo di [PriceField]. */
@Composable
private fun YearField(value: String, onValueChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val fieldColor = if (dark) PurpleFieldDark else PurpleFieldLight
    val focusColor = if (dark) PurpleFieldFocusDark else PurpleFieldFocusLight
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val idleFill = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f)
    val borderColor by animateColorAsState(
        targetValue = if (focused) focusColor else fieldColor,
        animationSpec = tween(durationMillis = 150),
        label = "yearBorder",
    )
    val fillColor by animateColorAsState(
        targetValue = if (focused) colors.surface else idleFill,
        animationSpec = tween(durationMillis = 150),
        label = "yearFill",
    )
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = colors.onSurface,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = textStyle,
        cursorBrush = SolidColor(focusColor),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .width(YearFieldWidth)
            .height(YearFieldHeight)
            .clip(shape)
            .background(fillColor)
            .border(1.5.dp, borderColor, shape)
            .padding(horizontal = 8.dp)
            .semantics { contentDescription = "Year" },
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(text = "Year", style = textStyle, color = colors.onSurfaceVariant)
                }
                inner()
            }
        },
    )
}

/** Solo cifre, al massimo 4 (un anno a 4 cifre): nessun separatore, a differenza del prezzo. */
private fun sanitizeYear(input: String): String = input.filter(Char::isDigit).take(4)

/**
 * "Series N" + intestazione, come `seriesHeading` in `RegularIssueCountryScreen.kt`: [number] è
 * la posizione (1-based) nella lista del paese, non `series.numeroSerieIpotesi` (può ripetersi,
 * vedi quella funzione).
 */
private fun seriesHeadingForSheet(series: RegularIssueSeries, number: Int): String {
    val base = "Series $number"
    return series.intestazioneRaw?.let { "$base · $it" } ?: base
}
