package com.aifinancial.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionsDao {

    @Query("SELECT * FROM cached_transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<CachedTransactionEntity>>

    @Query("DELETE FROM cached_transactions")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(entities: List<CachedTransactionEntity>)

    /**
     * Delete-then-insert as one transaction: a refresh that fails partway
     * through must never leave [observeAll]'s `Flow` emitting a half-old,
     * half-new mix, and must never leave the cache empty either. Room runs
     * this whole default method body atomically because of [Transaction].
     */
    @Transaction
    suspend fun replaceAll(entities: List<CachedTransactionEntity>) {
        deleteAll()
        insertAll(entities)
    }
}
