package com.michele.eurocoins.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection_items")
    fun observeAll(): Flow<List<CollectionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CollectionItem)

    @Query("DELETE FROM collection_items WHERE coinKey = :coinKey AND quality = :quality")
    suspend fun delete(coinKey: String, quality: CoinQuality)

    @Query("UPDATE collection_items SET priceCents = :priceCents WHERE coinKey = :coinKey AND quality = :quality")
    suspend fun setPrice(coinKey: String, quality: CoinQuality, priceCents: Int?)
}
