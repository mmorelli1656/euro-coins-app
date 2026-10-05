package com.michele.eurocoins.data

import com.michele.eurocoins.data.backup.BackupException
import com.michele.eurocoins.data.backup.BackupFile
import com.michele.eurocoins.data.backup.BackupItem
import com.michele.eurocoins.data.backup.toBackupItem
import com.michele.eurocoins.data.backup.toCollectionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupFileTest {

    private val regular = RegularCollectionItem(
        seriesKey = "Greece|1",
        taglio = "2 euro",
        anno = 2002,
        quality = CoinQuality.BU,
        variety = VARIETY_EFS,
        priceCents = 1250,
        paese = "Grecia",
        addedAt = 1_700_000_000_000,
    )

    private val commemorative = BackupItem(
        coinKey = "ecb|2012|italia|x",
        quality = "STANDARD",
        priceCents = null,
        anno = 2012,
        paese = "Italia",
        tema = "X",
        addedAt = 1L,
    )

    @Test
    fun regularItemsSurviveARoundTripWithVarietyAndPrice() {
        val file = BackupFile(exportedAt = 5L, items = listOf(commemorative), regularItems = listOf(regular.toBackupItem()))
        val back = BackupFile.decode(BackupFile.encode(file))
        assertEquals(3, back.schemaVersion)
        assertEquals(listOf(regular), back.regularItems.map { it.toCollectionItem() })
        assertEquals(listOf(commemorative), back.items)
    }

    @Test
    fun thePurchaseDateSurvivesARoundTripOnBothKinds() {
        val withDate = regular.copy(purchasedOn = 20_000L)
        val item = CollectionItem(coinKey = "k", quality = CoinQuality.BU, priceCents = 900, purchasedOn = 20_100L, anno = 2025, paese = "Andorra", tema = "T", addedAt = 1L)
        val file = BackupFile(exportedAt = 5L, items = listOf(item.toBackupItem()), regularItems = listOf(withDate.toBackupItem()))
        val back = BackupFile.decode(BackupFile.encode(file))
        assertEquals(20_000L, back.regularItems.single().toCollectionItem()!!.purchasedOn)
        assertEquals(20_100L, back.items.single().toCollectionItem()!!.purchasedOn)
    }

    @Test
    fun aBackupWithoutDatesStillReadsAndTheDateIsEmpty() {
        // I backup v1 e v2 non hanno il campo: le voci tornano senza data, non falliscono.
        val v2 = """{"schemaVersion":2,"exportedAt":5,"items":[{"coinKey":"k","quality":"BU","anno":2010,"paese":"Italia","tema":"T","addedAt":1}],"regularItems":[{"seriesKey":"Italia|1","taglio":"1 cent","anno":2010,"quality":"STANDARD","paese":"Italia","addedAt":1}]}"""
        val file = BackupFile.decode(v2)
        assertNull(file.items.single().purchasedOn)
        assertNull(file.regularItems.single().purchasedOn)
        assertTrue(file.includesRegularIssues)
    }

    @Test
    fun aVersion1FileStillReadsButDoesNotCarryRegularIssues() {
        // Il ripristino di un backup precedente a Regular Issues non deve azzerare la collezione Regular locale.
        val old = """{"schemaVersion":1,"exportedAt":5,"items":[{"coinKey":"k","quality":"BU","anno":2010,"paese":"Italia","tema":"T","addedAt":1}]}"""
        val file = BackupFile.decode(old)
        assertEquals(1, file.items.size)
        assertTrue(file.regularItems.isEmpty())
        assertFalse(file.includesRegularIssues)
    }

    @Test
    fun aVersion2FileWithNoRegularItemsDoesCarryThem() {
        // Un backup v2 con la collezione Regular vuota è un'informazione vera (sostituisce, azzerando).
        val file = BackupFile.decode(BackupFile.encode(BackupFile(exportedAt = 1L, items = emptyList())))
        assertTrue(file.includesRegularIssues)
    }

    @Test
    fun anUnknownQualityIsSkippedInsteadOfFailingTheRestore() {
        val item = regular.toBackupItem().copy(quality = "GOLD")
        assertNull(item.toCollectionItem())
    }

    @Test
    fun aNewerSchemaIsRefused() {
        try {
            BackupFile.decode("""{"schemaVersion":99,"exportedAt":1,"items":[]}""")
            fail("doveva rifiutare un backup più nuovo")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("newer"))
        }
    }
}
