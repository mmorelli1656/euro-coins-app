package com.michele.eurocoins.data.backup

import com.michele.eurocoins.data.CollectionDao
import com.michele.eurocoins.data.RegularCollectionDao

/**
 * Orchestra backup e ripristino della collezione dell'utente su Drive: commemorative e Regular
 * Issues nello stesso file. Un solo backup per account: ogni salvataggio sovrascrive il
 * precedente (snapshot completo), e il ripristino SOSTITUISCE la collezione locale.
 */
class BackupService(
    private val collectionDao: CollectionDao,
    private val regularCollectionDao: RegularCollectionDao,
    private val drive: DriveBackupClient,
) {

    /** Carica la collezione locale su Drive; restituisce quante voci ha salvato (commemorative + Regular Issues). */
    suspend fun backup(token: String): Int {
        val items = collectionDao.getAll()
        val regularItems = regularCollectionDao.getAll()
        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            items = items.map { it.toBackupItem() },
            regularItems = regularItems.map { it.toBackupItem() },
        )
        drive.upload(token, BackupFile.encode(file))
        return items.size + regularItems.size
    }

    /**
     * Scarica il backup e sostituisce la collezione locale; restituisce quante voci ha ripristinato.
     * Un backup della versione 1 (precedente a Regular Issues) non ha quei dati: la collezione
     * Regular locale resta com'è invece di essere azzerata.
     */
    suspend fun restore(token: String): Int {
        val info = drive.find(token) ?: throw BackupException("No backup found on this Google account.")
        val file = BackupFile.decode(drive.download(token, info.id))
        val items = file.items.mapNotNull { it.toCollectionItem() }
        collectionDao.replaceAll(items)
        if (!file.includesRegularIssues) return items.size
        val regularItems = file.regularItems.mapNotNull { it.toCollectionItem() }
        regularCollectionDao.replaceAll(regularItems)
        return items.size + regularItems.size
    }

    /**
     * Monete distinte nella collezione locale (quelle che un backup salverebbe): commemorative più
     * tagli di Regular Issues, dove un taglio con più annate conta una volta sola.
     */
    suspend fun localCoinCount(): Int =
        collectionDao.getAll().map { it.coinKey }.distinct().size +
            regularCollectionDao.getAll().map { it.seriesKey to it.taglio }.distinct().size

    /** Data dell'ultimo backup (ISO-8601), null se non ne esiste uno. */
    suspend fun lastBackupTime(token: String): String? = drive.find(token)?.modifiedTime
}
