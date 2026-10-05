package com.michele.eurocoins.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.michele.eurocoins.ui.components.DialogTitle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.ui.backup.BackupSection
import com.michele.eurocoins.data.backup.BackupStatus
import com.michele.eurocoins.ui.backup.BackupUiState
import com.michele.eurocoins.ui.backup.BackupViewModel
import com.michele.eurocoins.ui.backup.SettingsCard
import com.michele.eurocoins.ui.browse.BrowseMode
import com.michele.eurocoins.ui.regular.RegularBrowseMode
import com.michele.eurocoins.ui.theme.PurpleFieldDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusLight
import com.michele.eurocoins.ui.theme.ThemeMode
import com.michele.eurocoins.ui.theme.appBarColors

/**
 * Impostazioni: un'unica schermata per account e backup, catalogo, aspetto e reset. Raggiunta
 * dall'ingranaggio della home; sostituisce la vecchia schermata Backup e la pillola del tema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    backupViewModel: BackupViewModel,
    onBack: () -> Unit,
) {
    val hideCommemorativeMicrostates by settingsViewModel.hideCommemorativeMicrostates.collectAsState()
    val hideRegularMicrostates by settingsViewModel.hideRegularMicrostates.collectAsState()
    val defaultTab by settingsViewModel.defaultTab.collectAsState()
    val defaultRegularTab by settingsViewModel.defaultRegularTab.collectAsState()
    val rotateHomeCoins by settingsViewModel.rotateHomeCoins.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val owned by settingsViewModel.ownedCounts.collectAsState()
    val backupState by backupViewModel.state.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    var showProInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SectionHeader("Account and backup", first = true)
            BackupSection(backupViewModel)

            Spacer(Modifier.height(12.dp))
            ProBanner(onClick = { showProInfo = true })

            // Una sezione per catalogo, ciascuna con le sue due impostazioni (interruttore, poi selettore):
            // i microstati si nascondono in modo indipendente e la scheda iniziale è quella del catalogo.
            SectionHeader("Commemorative")
            CatalogCard(
                catalogName = "Commemorative",
                hideMicrostates = hideCommemorativeMicrostates,
                onHideMicrostatesChange = settingsViewModel::setHideCommemorativeMicrostates,
                options = BrowseMode.entries,
                selected = defaultTab,
                label = {
                    when (it) {
                        BrowseMode.YEARS -> "Years"
                        BrowseMode.COUNTRIES -> "Countries"
                        BrowseMode.ALL -> "All"
                    }
                },
                onSelect = settingsViewModel::setDefaultTab,
            )

            SectionHeader("Regular Issues")
            CatalogCard(
                catalogName = "Regular Issues",
                hideMicrostates = hideRegularMicrostates,
                onHideMicrostatesChange = settingsViewModel::setHideRegularMicrostates,
                options = RegularBrowseMode.entries,
                selected = defaultRegularTab,
                label = {
                    when (it) {
                        RegularBrowseMode.COUNTRIES -> "Countries"
                        RegularBrowseMode.DENOMINATIONS -> "Denominations"
                        RegularBrowseMode.ALL -> "All"
                    }
                },
                onSelect = settingsViewModel::setDefaultRegularTab,
            )

            SectionHeader("Appearance")
            SettingsCard {
                // Stesso ordine delle sezioni dei cataloghi: prima l'interruttore, poi il selettore a segmenti.
                SwitchRow(
                    title = "Rotate home coins",
                    subtitle = "Shows a different set of coins on the home screen every day.",
                    checked = rotateHomeCoins,
                    onCheckedChange = settingsViewModel::setRotateHomeCoins,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    // Ordine Auto, Light, Dark = ordine dell'enum; "Auto" segue il telefono.
                    SegmentedChoice(
                        options = ThemeMode.entries,
                        selected = themeMode,
                        label = { it.label },
                        onSelect = settingsViewModel::setThemeMode,
                    )
                }
            }

            SectionHeader("Danger zone")
            ResetRow(owned = owned, onClick = { confirmReset = true })
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { confirmReset = false },
            title = { DialogTitle("Reset collection?") },
            text = {
                Text("${owned.describe()} will be removed from this device. " + resetBackupNote(backupState))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    settingsViewModel.resetCollection()
                }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }

    // Segnaposto finché non c'è l'acquisto (Play Billing): quando ci sarà, il tocco avvia l'acquisto
    // e il banner sparisce per gli utenti Pro.
    if (showProInfo) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showProInfo = false },
            title = { DialogTitle("Euro Coins Pro") },
            text = { Text("Pro will remove ads and support the development of the app. It's coming soon.") },
            confirmButton = { TextButton(onClick = { showProInfo = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    first: Boolean = false,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
        // Neutro: il colore primario è riservato agli elementi interattivi, i titoli non devono sembrarlo.
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = if (first) 4.dp else 24.dp, bottom = 8.dp),
    )
}

/**
 * Card di un catalogo (Commemorative o Regular Issues): interruttore dei microstati e scheda che si
 * apre per prima. Le due card sono identiche nella forma, cambiano solo le opzioni del selettore.
 */
@Composable
private fun <T> CatalogCard(
    catalogName: String,
    hideMicrostates: Boolean,
    onHideMicrostatesChange: (Boolean) -> Unit,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    SettingsCard {
        SwitchRow(
            title = "Hide microstates",
            subtitle = "Andorra, Monaco, San Marino, Vatican City",
            checked = hideMicrostates,
            onCheckedChange = onHideMicrostatesChange,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Default tab", style = MaterialTheme.typography.titleMedium)
            Text(
                "Opens first in $catalogName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedChoice(options = options, selected = selected, label = label, onSelect = onSelect)
        }
    }
}

@Composable
private fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    // L'outline del tema scuro (34351F) è quasi uguale alla card: il bordo dei segmenti usa onSurfaceVariant attenuato.
    val border = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    // Larghezza di ogni segmento = larghezza MISURATA del testo (stile e dimensione carattere dell'utente) più il
    // padding del segmento, con un minimo di 64 dp di testo: con tre segmenti uguali "Denominations" andava a
    // capo, mentre i selettori con etichette corte (System / Light / Dark) restano pari. SIMMETRICA (il più
    // largo tra un segmento e il suo opposto): con pesi diversi sui due lati il segmento di mezzo non sta al
    // centro della barra. Contare i caratteri non bastava: "Countries" di mezzo andava a capo a sua volta.
    val measurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val textWidths = options.map { with(density) { measurer.measure(label(it), textStyle, maxLines = 1).size.width.toDp() } }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                modifier = Modifier.weight(
                    maxOf(textWidths[index], textWidths[options.lastIndex - index], 64.dp).value + 2 * 12f,
                ),
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                // Nessuna spunta sul segmento selezionato: spostava l'etichetta e rendeva i segmenti sbilanciati.
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    activeBorderColor = border,
                    inactiveContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveBorderColor = border,
                ),
                label = { Text(label(option)) },
            )
        }
    }
}

/** Invito a diventare Pro (rimozione pubblicità): card neutra come le altre, l'accento è solo la corona nel viola dei campi dell'app. */
@Composable
private fun ProBanner(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = if (dark) PurpleFieldDark else PurpleFieldFocusLight,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Go Pro", style = MaterialTheme.typography.titleMedium)
            Text(
                "Remove ads and support the app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Riga distruttiva: bordo e testo in colore d'errore; il tocco apre la conferma, non cancella. */
@Composable
private fun ResetRow(owned: OwnedCounts, onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(14.dp)
    val enabled = owned.total > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, error, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Delete, contentDescription = null, tint = error)
        Column(modifier = Modifier.weight(1f)) {
            Text("Reset collection", style = MaterialTheme.typography.titleMedium, color = error)
            Text(
                if (enabled) "Removes ${owned.describe()} from local storage" else "Collection is currently empty",
                style = MaterialTheme.typography.bodyMedium,
                color = error,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = error)
    }
}

/** Riga con titolo, spiegazione e interruttore (stessa resa per tutte le opzioni on/off). */
@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Colori espliciti: l'outline del tema scuro (34351F) è quasi uguale alla card, quindi da spento bordo e pallino usano onSurfaceVariant e la traccia lo sfondo.
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
            } else {
                null
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedIconColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.background,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

/**
 * Cosa dire sul backup nel dialog del Reset: se la collezione si potrà ripristinare e da quando.
 * Il Reset non tocca Drive, ma il suo effetto dipende da quanto il backup è aggiornato.
 */
internal fun resetBackupNote(state: BackupUiState): String {
    if (state.account == null) return "You're not signed in, so there is no backup to restore from."
    val last = state.lastBackup ?: state.lastBackupLocal
    val head = when {
        state.localStatus == BackupStatus.UpToDate ->
            "Your collection is backed up on Google Drive${last?.let { " ($it)" } ?: ""}, so you can restore it."
        last != null -> "Anything added since your last backup ($last) can't be restored."
        else -> "There is no backup on Google Drive yet, so this can't be undone."
    }
    return "$head Your backup isn't deleted by the reset, but backing up again replaces it (the previous version stays on Drive)."
}
