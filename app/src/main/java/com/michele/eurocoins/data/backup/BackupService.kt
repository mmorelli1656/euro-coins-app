package com.michele.eurocoins.data.backup

import com.michele.eurocoins.data.CollectionDao

/**
 * Orchestra backup e ripristino della collezione dell'utente su Drive.
 * Un solo backup per account: ogni salvataggio sovrascrive il precedente
 * (snapshot completo), e il ripristino SOSTITUISCE la collezione locale.
 */
class BackupService(
    private val collectionDao: CollectionDao,
    private val drive: DriveBackupClient,
) {

    /** Carica la collezione locale su Drive; restituisce quante voci ha salvato. */
    suspend fun backup(token: String): Int {
        val items = collectionDao.getAll()
        val file = BackupFile(exportedAt = System.currentTimeMillis(), items = items.map { it.toBackupItem() })
        drive.upload(token, BackupFile.encode(file))
        return items.size
    }

    /** Scarica il backup e sostituisce la collezione locale; restituisce quante voci ha ripristinato. */
    suspend fun restore(token: String): Int {
        val info = drive.find(token) ?: throw BackupException("No backup found on this Google account.")
        val file = BackupFile.decode(drive.download(token, info.id))
        val items = file.items.mapNotNull { it.toCollectionItem() }
        collectionDao.replaceAll(items)
        return items.size
    }

    /** Monete distinte nella collezione locale (quelle che un backup salverebbe). */
    suspend fun localCoinCount(): Int = collectionDao.getAll().map { it.coinKey }.distinct().size

    /** Data dell'ultimo backup (ISO-8601), null se non ne esiste uno. */
    suspend fun lastBackupTime(token: String): String? = drive.find(token)?.modifiedTime
}
