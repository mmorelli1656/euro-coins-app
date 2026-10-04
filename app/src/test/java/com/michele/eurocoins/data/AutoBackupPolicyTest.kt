package com.michele.eurocoins.data

import com.michele.eurocoins.data.backup.AUTO_BACKUP_MIN_INTERVAL_MS
import com.michele.eurocoins.data.backup.BackupStatus
import com.michele.eurocoins.data.backup.resolveAutoBackupEnabled
import com.michele.eurocoins.data.backup.shouldAutoBackup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupPolicyTest {

    private val now = 10_000_000_000L
    private val longAgo = 0L

    private fun decide(
        enabled: Boolean = true,
        signedIn: Boolean = true,
        localEmpty: Boolean = false,
        status: BackupStatus = BackupStatus.Pending(3),
        lastAttemptAt: Long = longAgo,
    ) = shouldAutoBackup(enabled, signedIn, localEmpty, status, lastAttemptAt, now)

    @Test
    fun backsUpWhenSomethingChanged() = assertTrue(decide())

    @Test
    fun neverUploadsAnEmptyCollection() {
        // Il caso che ha reso necessario il backup automatico sicuro: dopo un reset non deve caricare il niente sopra un backup buono.
        assertFalse(decide(localEmpty = true, status = BackupStatus.Pending(40)))
    }

    @Test
    fun doesNothingWhenNothingChangedOrTheSituationIsUnknown() {
        assertFalse(decide(status = BackupStatus.UpToDate))
        // Mai un backup da questo telefono: non si sovrascrive in silenzio quello di un altro telefono.
        assertFalse(decide(status = BackupStatus.Unknown))
    }

    @Test
    fun needsTheSwitchAndAnAccount() {
        assertFalse(decide(enabled = false))
        assertFalse(decide(signedIn = false))
    }

    @Test
    fun waitsBetweenAttempts() {
        assertFalse(decide(lastAttemptAt = now - AUTO_BACKUP_MIN_INTERVAL_MS + 1))
        assertTrue(decide(lastAttemptAt = now - AUTO_BACKUP_MIN_INTERVAL_MS))
    }

    @Test
    fun theSwitchIsOnByDefaultOnlyForWhoAlreadyBackedUp() {
        assertTrue(resolveAutoBackupEnabled(stored = null, hasSnapshot = true))
        assertFalse(resolveAutoBackupEnabled(stored = null, hasSnapshot = false))
        // La scelta dell'utente vince sempre.
        assertFalse(resolveAutoBackupEnabled(stored = false, hasSnapshot = true))
        assertTrue(resolveAutoBackupEnabled(stored = true, hasSnapshot = false))
    }
}
