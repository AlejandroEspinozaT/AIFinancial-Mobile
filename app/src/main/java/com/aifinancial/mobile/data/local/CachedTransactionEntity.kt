package com.aifinancial.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A bounded, read-only mirror of the subset of `GET /api/transactions`
 * needed for a simple list view (specs/phases/stage-3-offline-read-cache.md)
 * — not a full replica of every field the web app shows. `date` is kept as
 * the backend's own ISO-8601 string (`DateOnly` serializes that way) rather
 * than parsed into a richer local type: this cache only ever displays it,
 * never computes with it.
 */
@Entity(tableName = "cached_transactions")
data class CachedTransactionEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val date: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val type: String,
    val category: String,
)
