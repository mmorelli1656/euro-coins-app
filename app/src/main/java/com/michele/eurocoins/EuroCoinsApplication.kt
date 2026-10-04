package com.michele.eurocoins

import android.app.Application
import com.michele.eurocoins.data.CoinDatabase
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.RegularIssueRepository
import com.michele.eurocoins.data.backup.BackupService
import com.michele.eurocoins.data.backup.DriveBackupClient
import com.michele.eurocoins.data.backup.GoogleAccountManager
import com.michele.eurocoins.ui.settings.UserSettings
import com.michele.eurocoins.ui.theme.ThemePreference

class EuroCoinsApplication : Application() {

    val repository: CoinRepository by lazy {
        val db = CoinDatabase.getInstance(this)
        CoinRepository(this, db.coinDao(), db.collectionDao(), userSettings.hideCommemorativeMicrostates)
    }

    val regularIssueRepository: RegularIssueRepository by lazy {
        val db = CoinDatabase.getInstance(this)
        RegularIssueRepository(this, db.regularIssueDao(), db.regularCollectionDao(), userSettings.hideRegularMicrostates)
    }

    val accountManager: GoogleAccountManager by lazy { GoogleAccountManager(this) }

    val themePreference: ThemePreference by lazy { ThemePreference(this) }

    val userSettings: UserSettings by lazy { UserSettings(this) }

    val backupService: BackupService by lazy {
        val db = CoinDatabase.getInstance(this)
        BackupService(db.collectionDao(), db.regularCollectionDao(), DriveBackupClient())
    }
}
