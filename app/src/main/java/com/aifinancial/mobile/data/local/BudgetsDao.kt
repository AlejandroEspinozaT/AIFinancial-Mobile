package com.aifinancial.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetsDao {

    @Query("SELECT * FROM cached_budgets ORDER BY category ASC")
    fun observeAll(): Flow<List<CachedBudgetEntity>>

    @Query("DELETE FROM cached_budgets")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(entities: List<CachedBudgetEntity>)

    /** See [TransactionsDao.replaceAll]'s KDoc — same atomicity requirement. */
    @Transaction
    suspend fun replaceAll(entities: List<CachedBudgetEntity>) {
        deleteAll()
        insertAll(entities)
    }
}
