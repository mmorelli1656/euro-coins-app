package com.michele.eurocoins.data.backup

import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.RegularCollectionItem

/**
 * Rapporto tra la collezione locale e l'ultimo backup eseguito (o ripristinato) da questo telefono.
 * Si calcola in locale, senza interrogare Drive: l'app tiene l'istantanea dell'ultimo file
 * caricato/scaricato ([BackupSnapshotStore]) e la confronta con le voci attuali.
 */
sealed interface BackupStatus {
    /** Nessuna istantanea: non si può dire se il backup su Drive sia aggiornato (mai fatto un backup da questa versione). */
    data object Unknown : BackupStatus

    /** La collezione locale coincide con l'ultimo backup. */
    data object UpToDate : BackupStatus

    /** [changes] voci aggiunte, tolte o modificate dall'ultimo backup (una voce = una finitura, o un anno+finitura nelle Regular). */
    data class Pending(val changes: Int) : BackupStatus
}

/**
 * Stato del backup: confronta le voci locali con [snapshot], l'ultimo file caricato su Drive o
 * ripristinato. Un'istantanea della versione 1 non conteneva Regular Issues, quindi tutte le voci
 * Regular locali risultano "non salvate": dire "aggiornato" sarebbe falso, su Drive non ci sono.
 */
fun backupStatus(
    snapshot: BackupFile?,
    items: List<CollectionItem>,
    regularItems: List<RegularCollectionItem>,
): BackupStatus {
    snapshot ?: return BackupStatus.Unknown
    val commemorative = countChanges(
        old = snapshot.items.associateBy { it.coinKey to it.quality },
        new = items.map { it.toBackupItem() }.associateBy { it.coinKey to it.quality },
    )
    val regular = if (snapshot.includesRegularIssues) {
        countChanges(
            old = snapshot.regularItems.associateBy { listOf(it.seriesKey, it.taglio, it.anno, it.quality, it.variety) },
            new = regularItems.map { it.toBackupItem() }.associateBy { listOf(it.seriesKey, it.taglio, it.anno, it.quality, it.variety) },
        )
    } else {
        regularItems.size
    }
    val total = commemorative + regular
    return if (total == 0) BackupStatus.UpToDate else BackupStatus.Pending(total)
}

/** Voci presenti solo in uno dei due insiemi, o presenti in entrambi ma diverse. */
private fun <K, V> countChanges(old: Map<K, V>, new: Map<K, V>): Int =
    (old.keys + new.keys).count { old[it] != new[it] }
