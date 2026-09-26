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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.ui.backup.BackupSection
import com.michele.eurocoins.ui.backup.BackupViewModel
import com.michele.eurocoins.ui.backup.SettingsCard
import com.michele.eurocoins.ui.browse.BrowseMode
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
    val hideMicrostates by settingsViewModel.hideMicrostates.collectAsState()
    val defaultTab by settingsViewModel.defaultTab.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val ownedCount by settingsViewModel.ownedCount.collectAsState()
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

            SectionHeader("Catalog and display")
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide microstates", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Andorra, Monaco, San Marino, Vatican City",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Colori espliciti: l'outline del tema scuro (34351F) è quasi uguale alla card, quindi da spento bordo e pallino usano onSurfaceVariant e la traccia lo sfondo.
                    Switch(
                        checked = hideMicrostates,
                        onCheckedChange = settingsViewModel::setHideMicrostates,
                        thumbContent = if (hideMicrostates) {
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Default tab", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Opens first in Commemorative",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SegmentedChoice(
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
                }
            }

            SectionHeader("Appearance")
            SettingsCard {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                // Ordine Auto, Light, Dark = ordine dell'enum; "Auto" segue il telefono.
                SegmentedChoice(
                    options = ThemeMode.entries,
                    selected = themeMode,
                    label = { it.label },
                    onSelect = settingsViewModel::setThemeMode,
                )
            }

            SectionHeader("Danger zone")
            ResetRow(ownedCount = ownedCount, onClick = { confirmReset = true })
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset collection?") },
            text = {
                Text(
                    "$ownedCount ${if (ownedCount == 1) "coin" else "coins"} will be removed from this device. " +
                        "Your Google Drive backup will not be deleted automatically, but performing a new " +
                        "backup after reset will overwrite it.",
                )
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
            onDismissRequest = { showProInfo = false },
            title = { Text("Euro Coins Pro") },
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
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        // Neutro: il colore primario è riservato agli elementi interattivi, i titoli non devono sembrarlo.
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = if (first) 4.dp else 24.dp, bottom = 8.dp),
    )
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
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                modifier = Modifier.weight(1f),
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
private fun ResetRow(ownedCount: Int, onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(14.dp)
    val enabled = ownedCount > 0
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
                if (enabled) "Removes $ownedCount ${if (ownedCount == 1) "coin" else "coins"} from local storage" else "Collection is currently empty",
                style = MaterialTheme.typography.bodyMedium,
                color = error,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = error)
    }
}
