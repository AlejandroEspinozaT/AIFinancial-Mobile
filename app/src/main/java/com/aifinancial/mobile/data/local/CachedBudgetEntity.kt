package com.aifinancial.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A read-only mirror of `GET /api/budgets` — see [CachedTransactionEntity]'s KDoc. */
@Entity(tableName = "cached_budgets")
data class CachedBudgetEntity(
    @PrimaryKey val id: String,
    val category: String,
    val monthlyLimit: Double,
    val amountSpent: Double,
    val percentUtilized: Double,
    val isExceeded: Boolean,
)
