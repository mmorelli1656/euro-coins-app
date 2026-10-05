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
    version = 10,
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

        /**
         * 8 -> 9: `variety` entra nella chiave primaria di `regular_collection_items` (il 2002 greco
         * di Atene e quello EFS sono lo stesso anno e qualità). SQLite non cambia una chiave primaria
         * con ALTER: si ricostruisce la tabella copiando le voci esistenti, tutte con varietà `''`
         * (moneta normale). Dati dell'utente: nessuna voce deve andare persa. Colonna NOT NULL senza
         * DEFAULT, come l'entity (il valore vuoto lo mette la copia).
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `regular_collection_items_new` (" +
                        "`seriesKey` TEXT NOT NULL, `taglio` TEXT NOT NULL, `anno` INTEGER NOT NULL, " +
                        "`quality` TEXT NOT NULL, `variety` TEXT NOT NULL, `priceCents` INTEGER, " +
                        "`paese` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`seriesKey`, `taglio`, `anno`, `quality`, `variety`))",
                )
                db.execSQL(
                    "INSERT INTO `regular_collection_items_new` " +
                        "(`seriesKey`, `taglio`, `anno`, `quality`, `variety`, `priceCents`, `paese`, `addedAt`) " +
                        "SELECT `seriesKey`, `taglio`, `anno`, `quality`, '', `priceCents`, `paese`, `addedAt` " +
                        "FROM `regular_collection_items`",
                )
                db.execSQL("DROP TABLE `regular_collection_items`")
                db.execSQL("ALTER TABLE `regular_collection_items_new` RENAME TO `regular_collection_items`")
            }
        }

        // Data di acquisto facoltativa su entrambe le tabelle di collezione: colonne nuove, nullable,
        // senza DEFAULT (come le migrazioni 3->4 e 4->5): le voci esistenti restano senza data.
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `collection_items` ADD COLUMN `purchasedOn` INTEGER")
                db.execSQL("ALTER TABLE `regular_collection_items` ADD COLUMN `purchasedOn` INTEGER")
            }
        }

        @Volatile private var instance: CoinDatabase? = null

        fun getInstance(context: Context): CoinDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoinDatabase::class.java,
                    "coins.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10).build().also { instance = it }
            }
    }
}
