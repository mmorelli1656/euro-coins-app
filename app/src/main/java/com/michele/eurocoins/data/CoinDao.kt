package com.michele.eurocoins.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CoinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(coins: List<Coin>)

    @Query("DELETE FROM coins")
    abstract suspend fun deleteAll()

    /** Sostituisce l'intero catalogo in un'unica transazione (aggiornamento del dataset bundlato). */
    @Transaction
    open suspend fun replaceAll(coins: List<Coin>) {
        deleteAll()
        insertAll(coins)
    }

    @Query("SELECT COUNT(*) FROM coins")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM coins ORDER BY anno DESC, paese ASC")
    abstract fun observeAll(): Flow<List<Coin>>

    @Query("SELECT * FROM coins WHERE id = :id")
    abstract suspend fun getById(id: Long): Coin?

    @Query("SELECT DISTINCT paese FROM coins ORDER BY paese ASC")
    abstract fun observePaesi(): Flow<List<String>>
}
