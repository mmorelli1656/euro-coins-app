package com.michele.eurocoins

import android.app.Application
import com.michele.eurocoins.data.CoinDatabase
import com.michele.eurocoins.data.CoinRepository

class EuroCoinsApplication : Application() {

    val repository: CoinRepository by lazy {
        val db = CoinDatabase.getInstance(this)
        CoinRepository(this, db.coinDao(), db.collectionDao())
    }
}
