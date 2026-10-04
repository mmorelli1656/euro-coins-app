package com.michele.eurocoins.data.backup

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ultimo errore del salvataggio automatico: [message] da mostrare, [at] (ms) per sapere se è più recente dell'ultimo backup riuscito. */
data class AutoBackupError(val message: String, val at: Long)

/**
 * Salvataggio automatico su Drive: quando l'utente esce dall'app ([onAppLeft]), se la collezione
 * è cambiata dall'ultimo backup, la carica in silenzio. Il file pesa pochi KB: parte e finisce in
 * un attimo, senza WorkManager (non è nella cache offline delle dipendenze, e non serve la
 * regolarità di un lavoro periodico).
 *
 * Regole di sicurezza ([shouldAutoBackup]): mai una collezione VUOTA (dopo un reset caricherebbe il
 * niente sopra un backup buono: il caso che ha reso necessario tutto questo), e solo dopo che
 * questo telefono ha già fatto un backup (altrimenti su un telefono nuovo con qualche moneta
 * sovrascriverebbe il backup di un altro telefono senza chiedere: lì decide l'utente col backup
 * manuale e la sua conferma). In più Drive tiene la versione precedente ([DriveBackupClient.upload]).
 */
class AutoBackup(
    context: Context,
    private val service: BackupService,
    private val accounts: GoogleAccountManager,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val storedEnabled = MutableStateFlow(if (prefs.contains(KEY_ENABLED)) prefs.getBoolean(KEY_ENABLED, false) else null)

    /**
     * Interruttore effettivo: finché l'utente non lo tocca è acceso per chi ha già fatto un backup da
     * questo telefono (c'è l'istantanea) e spento per gli altri ([resolveAutoBackupEnabled]).
     */
    val enabled: StateFlow<Boolean> = combine(storedEnabled, service.snapshots.snapshot) { stored, snapshot ->
        resolveAutoBackupEnabled(stored, hasSnapshot = snapshot != null)
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        resolveAutoBackupEnabled(storedEnabled.value, hasSnapshot = service.snapshots.snapshot.value != null),
    )

    private val _error = MutableStateFlow(loadError())

    /** Ultimo errore del salvataggio automatico; la UI lo mostra solo se è più recente dell'ultimo backup riuscito. */
    val error: StateFlow<AutoBackupError?> = _error.asStateFlow()

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        storedEnabled.value = value
    }

    /** Da chiamare quando l'app esce dallo schermo: parte un tentativo, se serve. */
    fun onAppLeft() {
        scope.launch { runIfNeeded() }
    }

    internal suspend fun runIfNeeded() {
        if (accounts.currentAccount() == null) return
        val now = System.currentTimeMillis()
        val proceed = shouldAutoBackup(
            enabled = enabled.value,
            signedIn = true,
            localEmpty = service.isLocalEmpty(),
            status = service.currentStatus(),
            lastAttemptAt = prefs.getLong(KEY_LAST_ATTEMPT, 0L),
            now = now,
        )
        if (!proceed) return
        prefs.edit().putLong(KEY_LAST_ATTEMPT, now).apply()
        try {
            val token = accounts.silentDriveToken(appContext)
                ?: throw BackupException("Google Drive needs your approval again. Open Settings and tap Back up now.")
            service.backup(token)
            setError(null)
        } catch (e: BackupException) {
            setError(AutoBackupError(e.message ?: GENERIC_ERROR, System.currentTimeMillis()))
        } catch (e: Exception) {
            setError(AutoBackupError(GENERIC_ERROR, System.currentTimeMillis()))
        }
    }

    private fun setError(error: AutoBackupError?) {
        prefs.edit().apply {
            if (error == null) remove(KEY_ERROR).remove(KEY_ERROR_AT) else putString(KEY_ERROR, error.message).putLong(KEY_ERROR_AT, error.at)
        }.apply()
        _error.value = error
    }

    private fun loadError(): AutoBackupError? =
        prefs.getString(KEY_ERROR, null)?.let { AutoBackupError(it, prefs.getLong(KEY_ERROR_AT, 0L)) }

    private companion object {
        const val PREFS_NAME = "backup_auto"
        const val KEY_ENABLED = "enabled"
        const val KEY_LAST_ATTEMPT = "last_attempt"
        const val KEY_ERROR = "error"
        const val KEY_ERROR_AT = "error_at"
        const val GENERIC_ERROR = "Something went wrong."
    }
}

/** Tra un tentativo e il successivo (anche dopo un errore) passa almeno questo tempo: il file è piccolo, ma niente raffiche. */
internal const val AUTO_BACKUP_MIN_INTERVAL_MS = 15 * 60 * 1000L

/**
 * Se il salvataggio automatico deve partire. Serve: interruttore acceso, account collegato, qualcosa
 * da salvare (mai una collezione vuota), modifiche non ancora salvate ([BackupStatus.Pending]: con
 * [BackupStatus.Unknown] questo telefono non ha mai fatto un backup e non si sovrascrive in silenzio)
 * e almeno [minIntervalMs] dall'ultimo tentativo.
 */
internal fun shouldAutoBackup(
    enabled: Boolean,
    signedIn: Boolean,
    localEmpty: Boolean,
    status: BackupStatus,
    lastAttemptAt: Long,
    now: Long,
    minIntervalMs: Long = AUTO_BACKUP_MIN_INTERVAL_MS,
): Boolean = enabled && signedIn && !localEmpty && status is BackupStatus.Pending && now - lastAttemptAt >= minIntervalMs

/** Valore dell'interruttore: la scelta dell'utente se c'è, altrimenti acceso solo per chi ha già un backup da questo telefono. */
internal fun resolveAutoBackupEnabled(stored: Boolean?, hasSnapshot: Boolean): Boolean = stored ?: hasSnapshot
