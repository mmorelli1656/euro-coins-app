package com.michele.eurocoins.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Coin::class, CollectionItem::class], version = 2, exportSchema = false)
abstract class CoinDatabase : RoomDatabase() {

    abstract fun coinDao(): CoinDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        /**
         * 1 -> 2: aggiunge la tabella della collezione utente. Migrazione
         * esplicita (mai fallbackToDestructiveMigration): distruggere il
         * database cancellerebbe anche i dati dell'utente.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `collection_items` (" +
                        "`coinKey` TEXT NOT NULL, `quality` TEXT NOT NULL, `priceCents` INTEGER, " +
                        "`anno` INTEGER NOT NULL, `paese` TEXT NOT NULL, `tema` TEXT NOT NULL, " +
                        "`addedAt` INTEGER NOT NULL, PRIMARY KEY(`coinKey`, `quality`))",
                )
            }
        }

        @Volatile private var instance: CoinDatabase? = null

        fun getInstance(context: Context): CoinDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoinDatabase::class.java,
                    "coins.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
