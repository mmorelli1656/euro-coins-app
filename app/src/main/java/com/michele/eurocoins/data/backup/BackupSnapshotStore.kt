package com.michele.eurocoins.data.backup

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Copia locale dell'ultimo backup caricato su Drive (o ripristinato): è ciò che permette di dire
 * "aggiornato" o "12 modifiche non salvate" senza collegarsi a Drive ([backupStatus]). Un solo
 * file JSON nel formato del backup, in `filesDir`: non è una preferenza dell'utente e non contiene
 * niente che non sia già su Drive (sono le stesse voci).
 *
 * `exportedAt` dell'istantanea è anche la data dell'ultimo backup da questo telefono.
 */
class BackupSnapshotStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, FILE_NAME)

    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<BackupFile?> = _snapshot.asStateFlow()

    fun save(backup: BackupFile) {
        // Scrittura atomica: un'interruzione a metà lascia l'istantanea precedente, non un file troncato.
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        temp.writeText(BackupFile.encode(backup))
        if (!temp.renameTo(file)) {
            file.writeText(BackupFile.encode(backup))
            temp.delete()
        }
        _snapshot.value = backup
    }

    /** Si dimentica l'istantanea (disconnessione dall'account, ripristino della versione precedente): lo stato torna "non verificabile". */
    fun clear() {
        file.delete()
        _snapshot.value = null
    }

    private fun load(): BackupFile? =
        runCatching { if (file.exists()) BackupFile.decode(file.readText()) else null }.getOrNull()

    private companion object {
        const val FILE_NAME = "backup_snapshot.json"
    }
}
