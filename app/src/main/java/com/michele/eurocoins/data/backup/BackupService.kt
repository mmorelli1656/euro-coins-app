package com.michele.eurocoins.data.backup

import com.michele.eurocoins.data.CollectionDao
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.RegularCollectionDao
import com.michele.eurocoins.data.RegularCollectionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Messaggio quando non c'è niente da salvare: la collezione locale è vuota. */
const val EMPTY_COLLECTION_MESSAGE = "Your collection is empty, so there's nothing to back up."

/** Date (ISO-8601) del backup attuale e della versione precedente su Drive; null = non esiste. */
data class BackupInfo(val latest: String?, val previous: String?)

/**
 * Orchestra backup e ripristino della collezione dell'utente su Drive: commemorative e Regular
 * Issues nello stesso file. Un solo backup attuale per account (ogni salvataggio lo sovrascrive,
 * tenendo come "versione precedente" la copia di almeno un giorno prima, vedi [DriveBackupClient]),
 * e il ripristino SOSTITUISCE la collezione locale.
 *
 * Tiene in [snapshots] una copia dell'ultimo file caricato o ripristinato: è il riferimento con
 * cui [observeStatus] dice se la collezione locale è aggiornata. Backup manuale e automatico
 * passano da qui e si escludono a vicenda ([mutex]).
 */
class BackupService(
    private val collectionDao: CollectionDao,
    private val regularCollectionDao: RegularCollectionDao,
    private val drive: DriveBackupClient,
    val snapshots: BackupSnapshotStore,
) {
    private val mutex = Mutex()

    /** Carica la collezione locale su Drive; restituisce quante voci ha salvato (commemorative + Regular Issues). */
    suspend fun backup(token: String): Int = mutex.withLock {
        val items = collectionDao.getAll()
        val regularItems = regularCollectionDao.getAll()
        // Un backup vuoto non serve a niente e sovrascriverebbe quello buono: vale per il manuale come per l'automatico.
        if (items.isEmpty() && regularItems.isEmpty()) throw BackupException(EMPTY_COLLECTION_MESSAGE)
        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            items = items.map { it.toBackupItem() },
            regularItems = regularItems.map { it.toBackupItem() },
        )
        drive.upload(token, BackupFile.encode(file))
        snapshots.save(file)
        items.size + regularItems.size
    }

    /**
     * Scarica il backup e sostituisce la collezione locale; restituisce quante voci ha ripristinato.
     * Un backup della versione 1 (precedente a Regular Issues) non ha quei dati: la collezione
     * Regular locale resta com'è invece di essere azzerata.
     *
     * [usePrevious]: ripristina la versione precedente invece dell'attuale (la rete di sicurezza
     * contro un backup sovrascritto da una collezione sbagliata). Dopo questo ripristino lo stato
     * torna "non verificabile": la collezione locale non coincide con il backup attuale su Drive.
     */
    suspend fun restore(token: String, usePrevious: Boolean = false): Int = mutex.withLock {
        val info = (if (usePrevious) drive.findPrevious(token) else drive.find(token))
            ?: throw BackupException(if (usePrevious) "No previous version found on this Google account." else "No backup found on this Google account.")
        val file = BackupFile.decode(drive.download(token, info.id))
        val items = file.items.mapNotNull { it.toCollectionItem() }
        collectionDao.replaceAll(items)
        var restored = items.size
        if (file.includesRegularIssues) {
            val regularItems = file.regularItems.mapNotNull { it.toCollectionItem() }
            regularCollectionDao.replaceAll(regularItems)
            restored += regularItems.size
        }
        if (usePrevious) snapshots.clear() else snapshots.save(file)
        restored
    }

    /**
     * Monete distinte nella collezione locale (quelle che un backup salverebbe): commemorative più
     * tagli di Regular Issues, dove un taglio con più annate conta una volta sola.
     */
    suspend fun localCoinCount(): Int =
        collectionDao.getAll().map { it.coinKey }.distinct().size +
            regularCollectionDao.getAll().map { it.seriesKey to it.taglio }.distinct().size

    /** True se non c'è niente da salvare: il salvataggio automatico non carica mai una collezione vuota. */
    suspend fun isLocalEmpty(): Boolean = collectionDao.getAll().isEmpty() && regularCollectionDao.getAll().isEmpty()

    /** Come [isLocalEmpty] ma aggiornato a ogni modifica della collezione. */
    fun observeIsEmpty(): Flow<Boolean> = combine(
        collectionDao.observeAll(),
        regularCollectionDao.observeAll(),
    ) { items, regularItems -> items.isEmpty() && regularItems.isEmpty() }

    /** Rapporto attuale tra collezione locale e ultimo backup, calcolato una volta. */
    suspend fun currentStatus(): BackupStatus =
        backupStatus(snapshots.snapshot.value, collectionDao.getAll(), regularCollectionDao.getAll())

    /** Come [currentStatus] ma aggiornato a ogni modifica della collezione o del backup. */
    fun observeStatus(): Flow<BackupStatus> = combine(
        collectionDao.observeAll(),
        regularCollectionDao.observeAll(),
        snapshots.snapshot,
        ::statusOf,
    )

    private fun statusOf(
        items: List<CollectionItem>,
        regularItems: List<RegularCollectionItem>,
        snapshot: BackupFile?,
    ) = backupStatus(snapshot, items, regularItems)

    /** Si dimentica l'ultimo backup noto (cambio di account): lo stato torna "non verificabile". */
    fun forgetSnapshot() = snapshots.clear()

    /** Date del backup attuale e della versione precedente su Drive. */
    suspend fun backupInfo(token: String): BackupInfo = BackupInfo(
        latest = drive.find(token)?.modifiedTime,
        previous = drive.findPrevious(token)?.modifiedTime,
    )

    /** Data dell'ultimo backup (ISO-8601), null se non ne esiste uno. */
    suspend fun lastBackupTime(token: String): String? = drive.find(token)?.modifiedTime
}
