package com.michele.eurocoins.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CollectionDao {

    @Query("SELECT * FROM collection_items")
    abstract fun observeAll(): Flow<List<CollectionItem>>

    @Query("SELECT * FROM collection_items WHERE coinKey = :coinKey")
    abstract suspend fun itemsFor(coinKey: String): List<CollectionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<CollectionItem>)

    @Query("DELETE FROM collection_items WHERE coinKey = :coinKey")
    abstract suspend fun deleteForCoin(coinKey: String)

    /** Sostituisce in un'unica transazione tutte le qualità possedute di una moneta. */
    @Transaction
    open suspend fun replaceForCoin(coinKey: String, items: List<CollectionItem>) {
        deleteForCoin(coinKey)
        insertAll(items)
    }
}
