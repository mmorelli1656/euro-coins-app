package com.michele.eurocoins.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Coin::class, CollectionItem::class, RegularIssueSeries::class, RegularCollectionItem::class],
    version = 8,
    exportSchema = false,
)
@TypeConverters(RegularIssueConverters::class)
abstract class CoinDatabase : RoomDatabase() {

    abstract fun coinDao(): CoinDao
    abstract fun collectionDao(): CollectionDao
    abstract fun regularIssueDao(): RegularIssueDao
    abstract fun regularCollectionDao(): RegularCollectionDao

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

        /** 4 -> 5: aggiunge incisore/disegnatore del disegno commemorativo a `coins` (colonne nullable, niente default). */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `coins` ADD COLUMN `incisoreRetroRaw` TEXT")
                db.execSQL("ALTER TABLE `coins` ADD COLUMN `disegnatoreRetroRaw` TEXT")
            }
        }

        /** 5 -> 6: aggiunge la tabella delle serie divisionali (1 cent - 2 euro), popolata da `RegularIssueRepository.ensureSeeded()`. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `regular_issue_series` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `paese` TEXT NOT NULL, " +
                        "`zeccaEmittente` TEXT NOT NULL, `zeccaRaw` TEXT NOT NULL, " +
                        "`ordineCronologico` INTEGER NOT NULL, `numeroSerieIpotesi` INTEGER NOT NULL, " +
                        "`intestazioneRaw` TEXT, `descrizione` TEXT NOT NULL, `anniCitati` TEXT NOT NULL, " +
                        "`immagini` TEXT NOT NULL, `possibileIncongruenza` INTEGER NOT NULL, `fonteDati` TEXT NOT NULL)",
                )
            }
        }

        /**
         * 6 -> 7: aggiunge la tabella della collezione utente sulle monete circolanti (Regular
         * Issues) — un taglio di una serie, in un'annata e una qualità inserite dall'utente (non
         * nel dataset, a differenza di `collection_items`: vedi [RegularCollectionItem]).
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `regular_collection_items` (" +
                        "`seriesKey` TEXT NOT NULL, `taglio` TEXT NOT NULL, `anno` INTEGER NOT NULL, " +
                        "`quality` TEXT NOT NULL, `priceCents` INTEGER, `paese` TEXT NOT NULL, " +
                        "`addedAt` INTEGER NOT NULL, PRIMARY KEY(`seriesKey`, `taglio`, `anno`, `quality`))",
                )
            }
        }

        /**
         * 7 -> 8: aggiunge `numistaId` a `coins` (colonna nullable, niente default) per i crediti
         * Numista nel dettaglio. L'asset coins.json NON cambia, quindi l'hash non farebbe ripopolare:
         * `CoinRepository.SEED_VERSION` forza il ripopolamento che riempie la colonna.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `coins` ADD COLUMN `numistaId` INTEGER")
            }
        }

        @Volatile private var instance: CoinDatabase? = null

        fun getInstance(context: Context): CoinDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoinDatabase::class.java,
                    "coins.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build().also { instance = it }
            }
    }
}
