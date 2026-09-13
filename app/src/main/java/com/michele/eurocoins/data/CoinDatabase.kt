package com.michele.eurocoins.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Coin::class], version = 1, exportSchema = false)
abstract class CoinDatabase : RoomDatabase() {

    abstract fun coinDao(): CoinDao

    companion object {
        @Volatile private var instance: CoinDatabase? = null

        fun getInstance(context: Context): CoinDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoinDatabase::class.java,
                    "coins.db",
                ).build().also { instance = it }
            }
    }
}
