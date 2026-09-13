package com.michele.eurocoins

import android.app.Application
import com.michele.eurocoins.data.CoinDatabase
import com.michele.eurocoins.data.CoinRepository

class EuroCoinsApplication : Application() {

    val repository: CoinRepository by lazy {
        CoinRepository(this, CoinDatabase.getInstance(this).coinDao())
    }
}
