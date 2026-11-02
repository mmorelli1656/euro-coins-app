package com.michele.eurocoins.data

import com.michele.eurocoins.data.backup.BackupException
import com.michele.eurocoins.data.backup.BackupFile
import com.michele.eurocoins.data.backup.BackupGoal
import com.michele.eurocoins.data.backup.BackupStatus
import com.michele.eurocoins.data.backup.backupStatus
import com.michele.eurocoins.data.backup.toBackupGoal
import com.michele.eurocoins.data.backup.toGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Gli obiettivi personali nel backup (formato v6): scritti solo se ci sono, mai cancellati da un file che non li porta. */
class BackupGoalsTest {

    private val italy = Goal(GoalKind.COUNTRY, "Italia")
    private val coins = Goal(GoalKind.COINS, "150")

    private fun snapshot(goals: List<Goal>, version: Int = BackupFile.SCHEMA_VERSION) =
        BackupFile(schemaVersion = version, exportedAt = 1L, items = emptyList(), goals = goals.map { it.toBackupGoal() })

    @Test
    fun theVersionIsSixOnlyWithGoalsFiveOnlyWithMintsAndFourOtherwise() {
        assertEquals(4, BackupFile.versionFor(hasMints = false, hasGoals = false))
        assertEquals(5, BackupFile.versionFor(hasMints = true, hasGoals = false))
        assertEquals(6, BackupFile.versionFor(hasMints = false, hasGoals = true))
        assertEquals(6, BackupFile.versionFor(hasMints = true, hasGoals = true))
        // La firma di prima (solo le zecche) continua a dare 4 e 5.
        assertEquals(4, BackupFile.versionFor(hasMints = false))
        assertEquals(5, BackupFile.versionFor(hasMints = true))
    }

    @Test
    fun goalsSurviveWritingAndReadingTheFile() {
        val done = coins.copy(doneAt = 20_000L)
        val back = BackupFile.decode(BackupFile.encode(snapshot(listOf(italy, done))))
        assertTrue(back.includesGoals)
        assertEquals(listOf(italy, done), back.goals.mapNotNull { it.toGoal() })
    }

    @Test
    fun anOlderFileHasNoGoalsAndDoesNotClaimToCarryThem() {
        // Un v5 e un v4 si leggono ancora, senza obiettivi: ripristinarli non deve toccare quelli locali.
        val v5 = BackupFile.decode(BackupFile.encode(snapshot(emptyList(), version = 5)))
        assertFalse(v5.includesGoals)
        assertTrue(v5.goals.isEmpty())
        assertFalse(snapshot(emptyList(), version = 4).includesGoals)
        // Anche un testo scritto da un\'app che non conosce il campo "goals".
        val old = BackupFile.decode("""{"schemaVersion":5,"exportedAt":1,"items":[]}""")
        assertFalse(old.includesGoals)
        assertTrue(old.goals.isEmpty())
    }

    @Test
    fun aNewerFileIsStillRefused() {
        try {
            BackupFile.decode(BackupFile.encode(snapshot(emptyList(), version = BackupFile.SCHEMA_VERSION + 1)))
            fail("un file piu' nuovo dell'app deve essere rifiutato")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("newer"))
        }
    }

    @Test
    fun aGoalKindNobodyKnowsIsSkippedNotFatal() {
        assertNull(BackupGoal(kind = "GALAXY", key = "x").toGoal())
        val file = BackupFile.decode(
            """{"schemaVersion":6,"exportedAt":1,"items":[],"goals":[{"kind":"GALAXY","key":"x"},{"kind":"COUNTRY","key":"Italia"}]}""",
        )
        assertEquals(listOf(italy), file.goals.mapNotNull { it.toGoal() })
    }

    @Test
    fun theStatusCountsGoalsAddedRemovedOrReached() {
        val base = snapshot(listOf(italy))
        assertEquals(BackupStatus.UpToDate, backupStatus(base, emptyList(), emptyList(), listOf(italy)))
        assertEquals(BackupStatus.Pending(1), backupStatus(base, emptyList(), emptyList(), listOf(italy, coins)))
        assertEquals(BackupStatus.Pending(1), backupStatus(base, emptyList(), emptyList(), emptyList()))
        // Raggiunto dall'ultimo backup (la data cambia): è una modifica.
        assertEquals(BackupStatus.Pending(1), backupStatus(base, emptyList(), emptyList(), listOf(italy.copy(doneAt = 20_000L))))
    }

    @Test
    fun aSnapshotThatPredatesGoalsMakesTheLocalOnesPending() {
        // Un'istantanea v5 non porta gli obiettivi: dire "aggiornato" per quelli locali sarebbe falso.
        val old = snapshot(emptyList(), version = 5)
        assertEquals(BackupStatus.Pending(2), backupStatus(old, emptyList(), emptyList(), listOf(italy, coins)))
        // Senza obiettivi locali resta aggiornato (non obbliga nessuno a un backup in piu').
        assertEquals(BackupStatus.UpToDate, backupStatus(old, emptyList(), emptyList(), emptyList()))
    }
}
