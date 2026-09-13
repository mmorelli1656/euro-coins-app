package com.michele.eurocoins.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(coins: List<Coin>)

    @Query("SELECT COUNT(*) FROM coins")
    suspend fun count(): Int

    @Query("SELECT * FROM coins ORDER BY anno DESC, paese ASC")
    fun observeAll(): Flow<List<Coin>>

    @Query("SELECT * FROM coins WHERE id = :id")
    suspend fun getById(id: Long): Coin?

    @Query("SELECT DISTINCT paese FROM coins ORDER BY paese ASC")
    fun observePaesi(): Flow<List<String>>
}
