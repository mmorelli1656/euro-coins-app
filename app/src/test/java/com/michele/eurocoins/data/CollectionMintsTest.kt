package com.michele.eurocoins.data

import com.michele.eurocoins.data.backup.BackupException
import com.michele.eurocoins.data.backup.BackupFile
import com.michele.eurocoins.data.backup.BackupStatus
import com.michele.eurocoins.data.backup.backupStatus
import com.michele.eurocoins.data.backup.toBackupItem
import com.michele.eurocoins.data.backup.toCollectionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * La zecca nella chiave delle voci di collezione (fase 3 delle zecche tedesche): il salvataggio che non
 * cancella le voci che il pannello non vede, il Quick add, il backup e il confronto con l'ultimo backup.
 */
class CollectionMintsTest {

    private fun item(quality: CoinQuality, mint: String = NO_MINT, price: Int? = null, addedAt: Long = 1L) =
        CollectionItem(
            coinKey = "k", quality = quality, mint = mint, priceCents = price,
            anno = 2006, paese = "Germania", tema = "Schleswig-Holstein", addedAt = addedAt,
        )

    private fun save(
        existing: List<CollectionItem>,
        entries: Map<MintFinish, FinishEntry>,
        editable: Set<String>? = null,
        now: Long = 99L,
    ) = planCommemorativeSave(existing, entries, editable, "k", 2006, "Germania", "Schleswig-Holstein", now)

    private val none = FinishEntry(priceCents = null, purchasedOn = null)

    // ---- commemorative: cosa si scrive ----

    @Test
    fun savingWithEveryMintEditableReplacesEverything() {
        val existing = listOf(item(CoinQuality.STANDARD), item(CoinQuality.STANDARD, "D"))
        val saved = save(existing, mapOf(MintFinish(NO_MINT, CoinQuality.STANDARD) to none))
        assertEquals(listOf(MintFinish(NO_MINT, CoinQuality.STANDARD)), saved.map { MintFinish(it.mint, it.quality) })
    }

    @Test
    fun aPanelThatOnlyEditsNoMintLeavesTheLetteredEntriesUntouched() {
        // Il caso del Pro scaduto: il pannello vede solo "No mint" e salva; le voci con zecca NON devono sparire.
        val lettered = listOf(item(CoinQuality.STANDARD, "D", price = 250, addedAt = 5L), item(CoinQuality.BU, "D", price = 1200))
        val saved = save(
            existing = listOf(item(CoinQuality.STANDARD)) + lettered,
            entries = mapOf(MintFinish(NO_MINT, CoinQuality.BU) to none),
            editable = setOf(NO_MINT),
        )
        assertEquals(lettered.toSet() + item(CoinQuality.BU, addedAt = 99L), saved.toSet())
    }

    @Test
    fun removingEverythingInScopeKeepsTheLetteredEntries() {
        val lettered = item(CoinQuality.STANDARD, "A")
        val saved = save(listOf(item(CoinQuality.STANDARD), lettered), emptyMap(), editable = setOf(NO_MINT))
        assertEquals(listOf(lettered), saved)
    }

    @Test
    fun entriesOutsideTheEditableScopeAreIgnoredNotDuplicated() {
        // Il pannello senza Pro rimanda anche le voci con zecca (o cambiate): contano le esistenti.
        val existing = listOf(item(CoinQuality.STANDARD, "D", price = 250))
        val saved = save(
            existing,
            mapOf(
                MintFinish("D", CoinQuality.STANDARD) to FinishEntry(priceCents = 9999, purchasedOn = null),
                MintFinish(NO_MINT, CoinQuality.STANDARD) to none,
            ),
            editable = setOf(NO_MINT),
        )
        assertEquals(2, saved.size)
        assertEquals(250, saved.single { it.mint == "D" }.priceCents)
    }

    @Test
    fun theAddedDateIsKeptPerMintAndFinish() {
        val existing = listOf(item(CoinQuality.STANDARD, "D", addedAt = 5L))
        val saved = save(
            existing,
            mapOf(MintFinish("D", CoinQuality.STANDARD) to none, MintFinish("A", CoinQuality.STANDARD) to none),
            now = 99L,
        )
        assertEquals(5L, saved.single { it.mint == "D" }.addedAt)
        assertEquals(99L, saved.single { it.mint == "A" }.addedAt)
    }

    // ---- Regular: cosa si scrive ----

    private fun regular(
        anno: Int,
        mint: String = NO_MINT,
        quality: CoinQuality = CoinQuality.STANDARD,
        key: String = "G|1",
        addedAt: Long = 1L,
    ) = RegularCollectionItem(
        seriesKey = key, taglio = "1 cent", anno = anno, quality = quality, mint = mint, paese = "Germania", addedAt = addedAt,
    )

    private fun entry(anno: Int, mint: String = NO_MINT, quality: CoinQuality = CoinQuality.STANDARD) =
        RegularCollectionEntry(anno = anno, quality = quality, priceCents = null, mint = mint)

    private fun plan(
        entries: List<RegularCollectionEntry>,
        atOrigin: List<RegularCollectionItem>,
        atViewed: List<RegularCollectionItem> = emptyList(),
        viewedKey: String = "G|1",
        window: IntRange? = null,
        editable: Set<String>? = null,
    ) = planRegularSave(
        seriesKey = "G|1", viewedKey = viewedKey, taglio = "1 cent", paese = "Germania", entries = entries,
        atOrigin = atOrigin, atViewed = atViewed, inWindow = window?.let { w -> { y: Int -> y in w } },
        editableMints = editable, now = 99L,
    )

    @Test
    fun yearsOutsideTheWindowAreKept() {
        val writes = plan(listOf(entry(2010)), atOrigin = listOf(regular(2005), regular(2010)), window = 2008..2012)
        assertEquals(1, writes.size)
        assertEquals(setOf(2005, 2010), writes.single().items.map { it.anno }.toSet())
    }

    @Test
    fun theNoYearEntryOfAnInheritedRowIsWrittenUnderTheViewedSeries() {
        val writes = plan(
            entries = listOf(entry(NO_YEAR)),
            atOrigin = listOf(regular(2010)),
            viewedKey = "G|2",
            window = 2008..2012,
        )
        assertEquals(listOf("G|1", "G|2"), writes.map { it.seriesKey })
        assertTrue(writes[0].items.none { it.anno == NO_YEAR })
        assertEquals(listOf(NO_YEAR), writes[1].items.map { it.anno })
    }

    @Test
    fun noSecondWriteWhenTheViewedSeriesHasNothingToChange() {
        val writes = plan(listOf(entry(2010)), atOrigin = emptyList(), viewedKey = "G|2", window = 2008..2012)
        assertEquals(1, writes.size)
    }

    @Test
    fun aRegularPanelThatOnlyEditsNoMintKeepsTheLetteredYears() {
        val lettered = regular(2010, "D", addedAt = 7L)
        val writes = plan(
            entries = listOf(entry(2010)),
            atOrigin = listOf(regular(2010), lettered),
            window = 2008..2012,
            editable = setOf(NO_MINT),
        )
        val items = writes.single().items
        assertTrue(lettered in items)
        assertEquals(2, items.size)
    }

    @Test
    fun removingTheNoYearEntryInScopeKeepsTheLetteredOneUnderTheViewedSeries() {
        val plain = regular(NO_YEAR, key = "G|2")
        val lettered = regular(NO_YEAR, "A", key = "G|2")
        val writes = plan(
            entries = emptyList(),
            atOrigin = emptyList(),
            atViewed = listOf(plain, lettered),
            viewedKey = "G|2",
            window = 2008..2012,
            editable = setOf(NO_MINT),
        )
        assertEquals(listOf(lettered), writes.single { it.seriesKey == "G|2" }.items)
    }

    @Test
    fun theRegularAddedDateIsKeptPerYearQualityVarietyAndMint() {
        val before = regular(2010, "D", addedAt = 7L)
        val writes = plan(listOf(entry(2010, "D"), entry(2010, "A")), atOrigin = listOf(before))
        val items = writes.single().items
        assertEquals(7L, items.single { it.mint == "D" }.addedAt)
        assertEquals(99L, items.single { it.mint == "A" }.addedAt)
    }

    // ---- Quick add ----

    @Test
    fun aMintMakesAnEntryADetailSoRemovingItAsksForConfirmation() {
        assertEquals(QuickTap.REMOVE, quickTapFor(listOf(item(CoinQuality.STANDARD))))
        assertEquals(QuickTap.CONFIRM_REMOVE, quickTapFor(listOf(item(CoinQuality.STANDARD, "D"))))
        val simple = regular(NO_YEAR)
        assertEquals(QuickTap.REMOVE, quickTapForRegular(listOf(simple)))
        assertEquals(QuickTap.CONFIRM_REMOVE, quickTapForRegular(listOf(simple.copy(mint = "A"))))
    }

    // ---- backup ----

    @Test
    fun theMintSurvivesABackupRoundTripOnBothKinds() {
        val file = BackupFile(
            schemaVersion = BackupFile.versionFor(hasMints = true),
            exportedAt = 5L,
            items = listOf(item(CoinQuality.STANDARD, "D", price = 250).toBackupItem()),
            regularItems = listOf(regular(2010, "J").toBackupItem()),
        )
        val back = BackupFile.decode(BackupFile.encode(file))
        assertEquals(5, back.schemaVersion)
        assertEquals("D", back.items.single().toCollectionItem()!!.mint)
        assertEquals("J", back.regularItems.single().toCollectionItem()!!.mint)
    }

    @Test
    fun theVersionIsFiveOnlyWhenSomeEntryHasAMint() {
        assertEquals(4, BackupFile.versionFor(hasMints = false))
        assertEquals(5, BackupFile.versionFor(hasMints = true))
    }

    @Test
    fun filesWithoutTheMintFieldReadAsNoMint() {
        val v4 = """{"schemaVersion":4,"exportedAt":5,"items":[{"coinKey":"k","quality":"BU","anno":2010,"paese":"Italia","tema":"T","addedAt":1}],"regularItems":[{"seriesKey":"Italia|1","taglio":"1 cent","anno":0,"quality":"STANDARD","paese":"Italia","addedAt":1}]}"""
        val file = BackupFile.decode(v4)
        assertEquals(NO_MINT, file.items.single().toCollectionItem()!!.mint)
        assertEquals(NO_MINT, file.regularItems.single().toCollectionItem()!!.mint)
    }

    @Test
    fun aNewerVersionThanTheCurrentOneIsRefused() {
        try {
            BackupFile.decode("""{"schemaVersion":7,"exportedAt":5,"items":[]}""") // la versione attuale è la 6 (obiettivi): la 7 si rifiuta come "più nuova"
            fail("doveva rifiutarla")
        } catch (_: BackupException) {
        }
    }

    // ---- stato del backup ----

    private fun snapshot(items: List<CollectionItem>, regularItems: List<RegularCollectionItem> = emptyList()) = BackupFile(
        schemaVersion = 4, exportedAt = 5L,
        items = items.map { it.toBackupItem() }, regularItems = regularItems.map { it.toBackupItem() },
    )

    @Test
    fun theSameCoinInAnotherMintIsAChangeNotAnOverwrite() {
        val plain = item(CoinQuality.STANDARD)
        val status = backupStatus(snapshot(listOf(plain)), listOf(plain, item(CoinQuality.STANDARD, "D")), emptyList())
        assertEquals(BackupStatus.Pending(1), status)
    }

    @Test
    fun anOldSnapshotWithoutMintsMatchesTheSameCollectionAfterTheUpdate() {
        val items = listOf(item(CoinQuality.STANDARD), item(CoinQuality.BU))
        val regulars = listOf(regular(2010), regular(NO_YEAR))
        assertEquals(BackupStatus.UpToDate, backupStatus(snapshot(items, regulars), items, regulars))
    }

    // ---- lettere per paese ----

    @Test
    fun onlyGermanyHasMintLetters() {
        assertEquals(listOf("A", "D", "F", "G", "J"), mintLettersFor("Germania"))
        assertTrue(mintLettersFor("Francia").isEmpty())
    }

    // ---- Save dei pannelli ----

    @Test
    fun saveIsOffUntilThereIsSomethingToSave() {
        // Moneta non ancora in collezione e nessuna spunta: non c'è niente da scrivere.
        assertEquals(false, canSaveCollection(hasSaved = false, anyChecked = false))
        // Una spunta (qualunque finitura, anno o zecca) basta.
        assertTrue(canSaveCollection(hasSaved = false, anyChecked = true))
        // Con qualcosa di salvato resta acceso anche a bozza vuota: tutto deselezionato + Save toglie.
        assertTrue(canSaveCollection(hasSaved = true, anyChecked = false))
    }
}
