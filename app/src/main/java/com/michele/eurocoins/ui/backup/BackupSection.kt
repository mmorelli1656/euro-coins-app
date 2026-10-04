package com.michele.eurocoins.ui.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.michele.eurocoins.ui.components.DialogTitle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.backup.BackupStatus
import com.michele.eurocoins.data.backup.GoogleAccount
import com.michele.eurocoins.ui.settings.SwitchRow

/**
 * Sezione "Account and backup" della schermata Impostazioni: senza accesso un invito a
 * collegare Google; con l'accesso email + "Sign out", stato del backup in evidenza e i due
 * pulsanti d'azione (Back up now pieno, Restore a contorno: è quello che sovrascrive).
 */
@Composable
fun BackupSection(viewModel: BackupViewModel) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as Activity
    var confirmRestore by remember { mutableStateOf(false) }
    // Restore serve solo se esiste un backup: disabilitato quando Drive è stato interrogato e non ne ha.
    // Se non è ancora stato interrogato (consenso Drive mancante, es. telefono nuovo) resta attivo:
    // la data null non significa "nessun backup".
    val hasBackup = state.lastBackup != null || !state.backupChecked

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onConsentResult(activity, result.data)
    }
    // La schermata di consenso Drive è un evento una tantum dello stato: si lancia e si consuma.
    LaunchedEffect(state.consent) {
        state.consent?.let {
            consentLauncher.launch(IntentSenderRequest.Builder(it).build())
            viewModel.consentLaunched()
        }
    }
    LaunchedEffect(state.account) { viewModel.refreshInfo(activity) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val account = state.account
        when {
            !state.configured -> NotConfiguredCard()
            account == null -> SignedOutContent(busy = state.busy, onSignIn = { viewModel.signIn(activity) })
            else -> SettingsCard {
                AccountRow(account, signOutEnabled = !state.busy, onSignOut = { viewModel.signOut(activity) })
                StatusBox(
                    state = state,
                    checking = state.busy && !state.backupChecked && state.lastBackup == null && !state.hasSnapshot,
                )
                state.message?.let { InlineNotice(it, isError = state.messageIsError()) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.backup(activity) },
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier.weight(2f),
                    ) { Text("Back up now") }
                    OutlinedButton(
                        onClick = { confirmRestore = true },
                        enabled = !state.busy && hasBackup,
                        colors = ButtonDefaults.outlinedButtonColors(
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (!state.busy && hasBackup) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text("Restore", maxLines = 1, softWrap = false) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                SwitchRow(
                    title = "Back up automatically",
                    subtitle = if (state.hasSnapshot) "When you leave the app" else "Starts after your first backup",
                    checked = state.autoBackup,
                    onCheckedChange = viewModel::setAutoBackup,
                )
            }
        }
        // Con l'account l'esito sta dentro la card (InlineNotice); qui solo per gli stati senza card d'account.
        if (account == null) state.message?.let { InlineNotice(it, isError = state.messageIsError()) }
    }

    state.overwritePrompt?.let { prompt ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = viewModel::dismissOverwrite,
            title = { DialogTitle("Overwrite existing backup?") },
            text = {
                Text(
                    "A backup" + (prompt.date?.let { " from $it" } ?: "") + " already exists on Google Drive. " +
                        "Backing up now will overwrite it with your current local collection " +
                        "(${prompt.coins} ${if (prompt.coins == 1) "coin" else "coins"}).",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmOverwrite(activity) }) {
                    Text("Overwrite", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissOverwrite) { Text("Cancel") } },
        )
    }

    if (confirmRestore) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { confirmRestore = false },
            title = { DialogTitle("Replace your collection?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your current collection on this phone will be replaced by the backup saved on Google Drive" +
                            (state.lastBackup?.let { " ($it)" } ?: "") + ". Anything added since will be lost.",
                    )
                    // La rete di sicurezza: la copia di almeno un giorno prima, per un backup sovrascritto da una collezione sbagliata.
                    state.previousBackup?.let { previous ->
                        TextButton(
                            onClick = {
                                confirmRestore = false
                                viewModel.restorePrevious(activity)
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp),
                        ) { Text("Restore the previous version ($previous) instead") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    viewModel.restore(activity)
                }) { Text("Replace", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SignedOutContent(busy: Boolean, onSignIn: () -> Unit) {
    CenteredCard {
        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Text("Back up your collection", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Default), textAlign = TextAlign.Center)
        Text(
            text = "Sign in with Google to save your collection and restore it on a new phone. " +
                "It's stored in a private folder of this app on Google Drive: the app can't see your other files.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
    Button(onClick = onSignIn, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Sign in with Google") }
    if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}

@Composable
private fun NotConfiguredCard() {
    CenteredCard {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Text("Google sign-in isn't set up", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Default), textAlign = TextAlign.Center)
        Text(
            text = "Set google.webClientId in local.properties and rebuild the app.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Avatar, email (una riga, con puntini se lunga) e "Sign out" compatto a destra: azione rara, non deve pesare. */
@Composable
private fun AccountRow(account: GoogleAccount, signOutEnabled: Boolean, onSignOut: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (account.displayName ?: account.email).firstOrNull()?.uppercase().orEmpty(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = account.email,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onSignOut,
            enabled = signOutEnabled,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("Sign out", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Stato del backup: icona, titolo, data; durante un'operazione mostra la barra di avanzamento.
 * Il titolo dice la verità che l'app conosce: "Up to date" e "N changes not backed up" solo se
 * questo telefono ha un ultimo backup con cui confrontarsi ([BackupStatus]); altrimenti si limita a
 * dire che esiste un backup su Drive e di quando.
 */
@Composable
private fun StatusBox(state: BackupUiState, checking: Boolean) {
    val colors = MaterialTheme.colorScheme
    val last = state.lastBackup ?: state.lastBackupLocal
    var icon = Icons.Filled.CloudDone
    var tint = colors.primary
    val title: String
    val subtitle: String
    when (val status = state.localStatus) {
        BackupStatus.UpToDate -> {
            title = "Up to date"
            subtitle = "Last backup: ${last ?: "just now"}"
        }
        is BackupStatus.Pending -> {
            title = "${status.changes} ${if (status.changes == 1) "change" else "changes"} not backed up"
            val failed = state.autoError
            if (failed != null) {
                icon = Icons.Filled.CloudOff
                tint = colors.error
                subtitle = "Automatic backup failed: $failed"
            } else {
                icon = Icons.Filled.CloudUpload
                tint = colors.secondary
                subtitle = last?.let { "Last backup: $it" } ?: "Tap Back up now to save them to Google Drive."
            }
        }
        BackupStatus.Unknown -> if (last != null) {
            title = "Backup found"
            subtitle = "Last backup: $last"
        } else {
            icon = Icons.Filled.CloudOff
            tint = colors.secondary
            title = "Not backed up yet"
            subtitle = "Tap Back up now to save your collection to Google Drive."
        }
    }
    if (checking) {
        // Controllo iniziale su Drive: messaggio neutro e piccolo indicatore, niente titolo "Not backed up yet" che poi cambia.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            Column {
                Text("Checking backup status…", style = MaterialTheme.typography.titleMedium)
                Text("Looking for a backup on Google Drive.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

private fun BackupUiState.messageIsError() =
    message?.let { !(it.startsWith("Backed up") || it.startsWith("Restored") || it.startsWith("Signed out")) } ?: false

/** Avviso integrato nella card: errori in tinta d'errore con icona, esiti positivi neutri. */
@Composable
private fun InlineNotice(text: String, isError: Boolean) {
    val tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = if (isError) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Card con bordo sottile, nello stile "superficie + outline" del tema; contenuto allineato a sinistra. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

/** Card centrata (inviti e avvisi della sezione, senza account). */
@Composable
private fun CenteredCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
