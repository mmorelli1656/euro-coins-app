package com.michele.eurocoins.ui.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import com.michele.eurocoins.ui.theme.appBarColors
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.backup.GoogleAccount

/**
 * Schermata Backup: senza accesso un invito a collegare Google, con l'accesso
 * lo stato del backup in evidenza (card centrale) e i due pulsanti d'azione.
 * "Sign out" è in fondo, in colore secondario (bronzo): rara, quindi discreta,
 * ma riconoscibile come pulsante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(viewModel: BackupViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as Activity
    var confirmRestore by remember { mutableStateOf(false) }
    var showProInfo by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                colors = appBarColors(),
                title = { Text("Backup") },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val account = state.account
                when {
                    !state.configured -> NotConfiguredCard()
                    account == null -> SignedOutContent(busy = state.busy, onSignIn = { viewModel.signIn(activity) })
                    else -> {
                        AccountRow(account)
                        StatusCard(lastBackup = state.lastBackup, busy = state.busy)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.backup(activity) },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                            ) { Text("Back up") }
                            OutlinedButton(
                                onClick = { confirmRestore = true },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                            ) { Text("Restore") }
                        }
                    }
                }
                state.message?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
                }
                ProBanner(onClick = { showProInfo = true })
            }

            if (state.account != null) {
                OutlinedButton(
                    onClick = { viewModel.signOut(activity) },
                    enabled = !state.busy,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 24.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Sign out", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
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

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Replace your collection?") },
            text = { Text("Restoring replaces the collection on this phone with the one saved in the backup. Anything added since that backup will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    viewModel.restore(activity)
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
}

/** Invito a diventare Pro (rimozione pubblicità): bronzo, l'accento secondario, per distinguerlo dalle azioni di backup. */
@Composable
private fun ProBanner(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Go Pro", style = MaterialTheme.typography.titleMedium)
            Text("Remove ads and support the app.", style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = accent)
    }
}

@Composable
private fun SignedOutContent(busy: Boolean, onSignIn: () -> Unit) {
    Card {
        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Text("Back up your collection", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
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
    Card {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Text("Google sign-in isn't set up", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = "Set google.webClientId in local.properties and rebuild the app.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AccountRow(account: GoogleAccount) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (account.displayName ?: account.email).firstOrNull()?.uppercase().orEmpty(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(account.email, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Stato del backup: icona, titolo, data; durante un'operazione mostra la barra di avanzamento. */
@Composable
private fun StatusCard(lastBackup: String?, busy: Boolean) {
    val saved = lastBackup != null
    Card {
        Icon(
            imageVector = if (saved) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
            contentDescription = null,
            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = if (saved) "Collection saved" else "Not backed up yet",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (saved) "Last backup: $lastBackup" else "Tap Back up to save your collection to Google Drive.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

/** Card centrata con bordo sottile, nello stile "superficie + outline" del tema. */
@Composable
private fun Card(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
