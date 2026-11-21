package com.aifinancial.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per cached read-model (currently "transactions" and "budgets"),
 * recording when it was last successfully refreshed from the backend. This
 * is specs/phases/stage-3-offline-read-cache.md's "staleness is visible, not
 * hidden" requirement — every cached screen reads this to show a real
 * "last updated" timestamp instead of implying the list is live.
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val cacheKey: String,
    val lastUpdatedEpochMillis: Long,
)
