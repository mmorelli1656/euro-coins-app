package com.michele.eurocoins

import android.app.Application
import com.michele.eurocoins.data.CoinDatabase
import com.michele.eurocoins.data.CoinRepository
import com.michele.eurocoins.data.backup.BackupService
import com.michele.eurocoins.data.backup.DriveBackupClient
import com.michele.eurocoins.data.backup.GoogleAccountManager

class EuroCoinsApplication : Application() {

    val repository: CoinRepository by lazy {
        val db = CoinDatabase.getInstance(this)
        CoinRepository(this, db.coinDao(), db.collectionDao())
    }

    val accountManager: GoogleAccountManager by lazy { GoogleAccountManager(this) }

    val backupService: BackupService by lazy {
        BackupService(CoinDatabase.getInstance(this).collectionDao(), DriveBackupClient())
    }
}
