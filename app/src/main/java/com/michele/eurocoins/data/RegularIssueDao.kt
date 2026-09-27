package com.michele.eurocoins.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RegularIssueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(series: List<RegularIssueSeries>)

    @Query("DELETE FROM regular_issue_series")
    abstract suspend fun deleteAll()

    /** Sostituisce l'intero dataset in un'unica transazione (aggiornamento dell'asset bundlato). */
    @Transaction
    open suspend fun replaceAll(series: List<RegularIssueSeries>) {
        deleteAll()
        insertAll(series)
    }

    @Query("SELECT COUNT(*) FROM regular_issue_series")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM regular_issue_series ORDER BY paese ASC, ordineCronologico ASC")
    abstract fun observeAll(): Flow<List<RegularIssueSeries>>
}
