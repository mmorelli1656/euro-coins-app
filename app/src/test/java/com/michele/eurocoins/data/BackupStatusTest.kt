package com.michele.eurocoins.data

import com.michele.eurocoins.data.backup.BackupFile
import com.michele.eurocoins.data.backup.BackupStatus
import com.michele.eurocoins.data.backup.backupStatus
import com.michele.eurocoins.data.backup.toBackupItem
import java.time.Duration
import java.time.Instant
import com.michele.eurocoins.data.backup.shouldRotatePrevious
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupStatusTest {

    private fun item(key: String, quality: CoinQuality = CoinQuality.STANDARD, price: Int? = null) =
        CollectionItem(coinKey = key, quality = quality, priceCents = price, anno = 2010, paese = "Italia", tema = "T", addedAt = 1L)

    private fun regular(taglio: String, anno: Int = 2010, variety: String = "") = RegularCollectionItem(
        seriesKey = "Italia|1", taglio = taglio, anno = anno, quality = CoinQuality.STANDARD, variety = variety, paese = "Italia", addedAt = 1L,
    )

    private fun snapshot(items: List<CollectionItem>, regularItems: List<RegularCollectionItem>, version: Int = 2) =
        BackupFile(schemaVersion = version, exportedAt = 5L, items = items.map { it.toBackupItem() }, regularItems = regularItems.map { it.toBackupItem() })

    @Test
    fun withoutASnapshotTheStatusIsUnknown() {
        assertEquals(BackupStatus.Unknown, backupStatus(null, listOf(item("a")), emptyList()))
    }

    @Test
    fun identicalCollectionsAreUpToDateRegardlessOfOrder() {
        val items = listOf(item("a"), item("b"))
        val regulars = listOf(regular("1 cent"), regular("2 euro"))
        val status = backupStatus(snapshot(items, regulars), items.reversed(), regulars.reversed())
        assertEquals(BackupStatus.UpToDate, status)
    }

    @Test
    fun addedRemovedAndModifiedEntriesAreEachOneChange() {
        val before = listOf(item("a"), item("b"), item("c", price = 100))
        // "a" tolta, "d" aggiunta, "c" con un altro prezzo: tre modifiche; "b" invariata.
        val now = listOf(item("b"), item("c", price = 250), item("d"))
        assertEquals(BackupStatus.Pending(3), backupStatus(snapshot(before, emptyList()), now, emptyList()))
    }

    @Test
    fun theSameCoinInAnotherQualityIsADifferentEntry() {
        val before = listOf(item("a", CoinQuality.STANDARD))
        val now = listOf(item("a", CoinQuality.STANDARD), item("a", CoinQuality.BU))
        assertEquals(BackupStatus.Pending(1), backupStatus(snapshot(before, emptyList()), now, emptyList()))
    }

    @Test
    fun regularEntriesAreKeyedByYearAndVariety() {
        val before = listOf(regular("2 euro", 2002))
        val now = listOf(regular("2 euro", 2002), regular("2 euro", 2002, variety = "EFS"), regular("2 euro", 2003))
        assertEquals(BackupStatus.Pending(2), backupStatus(snapshot(emptyList(), before), emptyList(), now))
    }

    @Test
    fun aResetShowsAsAllEntriesRemoved() {
        val before = listOf(item("a"), item("b"))
        assertEquals(BackupStatus.Pending(2), backupStatus(snapshot(before, emptyList()), emptyList(), emptyList()))
    }

    @Test
    fun aVersion1SnapshotNeverClaimsTheRegularEntriesAreSaved() {
        // Un backup v1 non conteneva Regular Issues: dire "aggiornato" sarebbe falso, su Drive non ci sono.
        val items = listOf(item("a"))
        val regulars = listOf(regular("1 cent"), regular("2 cent"))
        val status = backupStatus(snapshot(items, emptyList(), version = 1), items, regulars)
        assertEquals(BackupStatus.Pending(2), status)
        // E senza Regular locali un v1 identico e' davvero aggiornato.
        assertEquals(BackupStatus.UpToDate, backupStatus(snapshot(items, emptyList(), version = 1), items, emptyList()))
    }

    @Test
    fun theOldBackupIsKeptAsPreviousOnlyWhenItIsAtLeastADayOld() {
        val now = Instant.parse("2026-10-05T12:00:00Z")
        val day = Duration.ofHours(24)
        // Niente copia precedente: si crea subito, qualunque eta' abbia l'attuale.
        assertTrue(shouldRotatePrevious(now.minusSeconds(60), previousExists = false, now = now))
        // Data dell'attuale non nota: meglio una copia in piu'.
        assertTrue(shouldRotatePrevious(null, previousExists = true, now = now))
        // Attuale di poche ore fa: la precedente (di almeno un giorno prima) non si tocca.
        assertFalse(shouldRotatePrevious(now.minus(Duration.ofHours(3)), previousExists = true, now = now))
        assertFalse(shouldRotatePrevious(now.minus(day).plusSeconds(1), previousExists = true, now = now))
        // Attuale vecchio di un giorno o piu': diventa la precedente.
        assertTrue(shouldRotatePrevious(now.minus(day), previousExists = true, now = now))
        assertTrue(shouldRotatePrevious(now.minus(Duration.ofDays(9)), previousExists = true, now = now))
    }
}
