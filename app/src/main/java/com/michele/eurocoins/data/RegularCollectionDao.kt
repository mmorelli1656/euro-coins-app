package com.michele.eurocoins.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RegularCollectionDao {

    @Query("SELECT * FROM regular_collection_items")
    abstract fun observeAll(): Flow<List<RegularCollectionItem>>

    @Query("SELECT * FROM regular_collection_items WHERE seriesKey = :seriesKey AND taglio = :taglio")
    abstract suspend fun itemsFor(seriesKey: String, taglio: String): List<RegularCollectionItem>

    @Query("SELECT * FROM regular_collection_items")
    abstract suspend fun getAll(): List<RegularCollectionItem>

    @Query("DELETE FROM regular_collection_items")
    abstract suspend fun deleteAll()

    /** Sostituisce l'intera collezione (ripristino da backup) in un'unica transazione. */
    @Transaction
    open suspend fun replaceAll(items: List<RegularCollectionItem>) {
        deleteAll()
        insertAll(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<RegularCollectionItem>)

    @Query("DELETE FROM regular_collection_items WHERE seriesKey = :seriesKey AND taglio = :taglio")
    abstract suspend fun deleteForDenomination(seriesKey: String, taglio: String)

    /** Sostituisce in un'unica transazione tutte le annate/qualità possedute di un taglio. */
    @Transaction
    open suspend fun replaceForDenomination(seriesKey: String, taglio: String, items: List<RegularCollectionItem>) {
        deleteForDenomination(seriesKey, taglio)
        insertAll(items)
    }
}
