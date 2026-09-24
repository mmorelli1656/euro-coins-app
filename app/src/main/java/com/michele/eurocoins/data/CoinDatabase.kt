package com.michele.eurocoins.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Coin::class, CollectionItem::class], version = 4, exportSchema = false)
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

        /**
         * 2 -> 3: aggiunge `emissioneComune` a `coins`. Il valore reale arriva
         * subito dopo dal ripopolamento di `ensureSeeded()` (l'hash dell'asset
         * cambia insieme allo schema), quindi qui basta un default che soddisfi
         * il vincolo NOT NULL.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `coins` ADD COLUMN `emissioneComune` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** 3 -> 4: aggiunge le tirature per finitura da Numista a `coins` (colonne nullable, niente default). */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `coins` ADD COLUMN `tiraturaNumistaStandard` INTEGER")
                db.execSQL("ALTER TABLE `coins` ADD COLUMN `tiraturaNumistaBu` INTEGER")
                db.execSQL("ALTER TABLE `coins` ADD COLUMN `tiraturaNumistaProof` INTEGER")
            }
        }

        @Volatile private var instance: CoinDatabase? = null

        fun getInstance(context: Context): CoinDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoinDatabase::class.java,
                    "coins.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
