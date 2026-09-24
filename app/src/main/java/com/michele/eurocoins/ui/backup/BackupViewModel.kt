package com.michele.eurocoins.ui.backup

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michele.eurocoins.data.backup.BackupException
import com.michele.eurocoins.data.backup.BackupService
import com.michele.eurocoins.data.backup.DriveAuthorization
import com.michele.eurocoins.data.backup.GoogleAccount
import com.michele.eurocoins.data.backup.GoogleAccountManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BackupAction { BACKUP, RESTORE, INFO }

/** Richiesta di conferma prima di sovrascrivere un backup già presente: sua data (se leggibile) e monete locali che lo sostituirebbero. */
data class OverwritePrompt(val date: String?, val coins: Int)

data class BackupUiState(
    val configured: Boolean,
    val account: GoogleAccount? = null,
    val busy: Boolean = false,
    /** Ultimo esito da mostrare all'utente (successo o errore). */
    val message: String? = null,
    val lastBackup: String? = null,
    /** true quando Drive è stato davvero interrogato: senza, `lastBackup` null vuol dire "non so", non "nessun backup". */
    val backupChecked: Boolean = false,
    /** Non null = la UI deve chiedere conferma prima di sovrascrivere il backup esistente. */
    val overwritePrompt: OverwritePrompt? = null,
    /** Schermata di consenso Drive da lanciare (one-shot: la UI la consuma con [BackupViewModel.consentLaunched]). */
    val consent: IntentSender? = null,
)

class BackupViewModel(
    private val service: BackupService,
    private val accounts: GoogleAccountManager,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState(configured = accounts.isConfigured, account = accounts.currentAccount()))
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    /** Azione in attesa del consenso Drive, da riprendere quando l'utente risponde. */
    private var pendingAction: BackupAction? = null

    fun signIn(activity: Activity) = launchBusy {
        val account = accounts.signIn(activity) ?: return@launchBusy
        _state.update { it.copy(account = account, message = null) }
        runAction(BackupAction.INFO, activity)
    }

    fun signOut(activity: Activity) = launchBusy {
        accounts.signOut(activity)
        _state.update { it.copy(account = null, lastBackup = null, backupChecked = false, message = "Signed out. Your local collection is untouched.") }
    }

    /** Silenziosa per INFO: se il consenso Drive non è ancora stato dato non lo chiede solo per mostrare una data. */
    fun refreshInfo(activity: Activity) {
        if (_state.value.account != null) launchBusy { runAction(BackupAction.INFO, activity) }
    }

    fun backup(activity: Activity) = launchBusy { runAction(BackupAction.BACKUP, activity) }

    fun restore(activity: Activity) = launchBusy { runAction(BackupAction.RESTORE, activity) }

    /** L'utente ha confermato il dialog di sovrascrittura: rifà il backup saltando il controllo. */
    fun confirmOverwrite(activity: Activity) = launchBusy {
        _state.update { it.copy(overwritePrompt = null) }
        runAction(BackupAction.BACKUP, activity, confirmed = true)
    }

    fun dismissOverwrite() = _state.update { it.copy(overwritePrompt = null) }

    fun consentLaunched() = _state.update { it.copy(consent = null) }

    /** Chiamata con l'esito della schermata di consenso; [data] null = utente ha rifiutato. */
    fun onConsentResult(activity: Activity, data: Intent?) {
        val action = pendingAction ?: return
        pendingAction = null
        launchBusy {
            val token = accounts.tokenFromConsent(activity, data)
            execute(action, token)
        }
    }

    private suspend fun runAction(action: BackupAction, activity: Activity, confirmed: Boolean = false) {
        when (val auth = accounts.authorizeDrive(activity)) {
            is DriveAuthorization.Granted -> execute(action, auth.accessToken, confirmed)
            is DriveAuthorization.NeedsConsent -> {
                if (action == BackupAction.INFO) return
                pendingAction = action
                _state.update { it.copy(consent = auth.intentSender) }
            }
        }
    }

    private suspend fun execute(action: BackupAction, token: String, confirmed: Boolean = false) {
        when (action) {
            BackupAction.BACKUP -> {
                // Un backup già presente si sovrascrive solo dopo conferma: su un telefono nuovo la
                // collezione locale è vuota e cancellerebbe quella salvata. Il controllo sta qui, non
                // nella UI, perché solo qui c'è il token (e il consenso Drive potrebbe non esserci ancora).
                if (!confirmed) {
                    val existing = service.lastBackupTime(token)
                    if (existing != null) {
                        _state.update {
                            it.copy(
                                lastBackup = formatTime(existing),
                                backupChecked = true,
                                overwritePrompt = OverwritePrompt(formatTime(existing), service.localCoinCount()),
                            )
                        }
                        return
                    }
                }
                val count = service.backup(token)
                _state.update {
                    it.copy(message = "Backed up $count ${entries(count)} to Google Drive.", lastBackup = formatTime(service.lastBackupTime(token)), backupChecked = true)
                }
            }
            BackupAction.RESTORE -> {
                val count = service.restore(token)
                _state.update { it.copy(message = "Restored $count ${entries(count)} from Google Drive.") }
            }
            BackupAction.INFO -> _state.update { it.copy(lastBackup = formatTime(service.lastBackupTime(token)), backupChecked = true) }
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            try {
                block()
            } catch (e: BackupException) {
                _state.update { it.copy(message = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(message = "Unexpected error: ${e.message}") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun entries(count: Int) = if (count == 1) "entry" else "entries"

    private fun formatTime(iso: String?): String? = iso?.let {
        runCatching {
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(it))
        }.getOrNull()
    }
}
