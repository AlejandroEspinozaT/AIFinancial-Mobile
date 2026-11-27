package com.aifinancial.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// exportSchema = false — this app has not shipped yet, so there's no
// installed base whose data needs a real migration path; see
// DatabaseModule's fallbackToDestructiveMigration for the Stage 3 schema
// bump. Turn exportSchema on (with a schema directory) once this ships and
// migrations start mattering for real.
@Database(
    entities = [
        PendingDocumentEntity::class,
        CachedTransactionEntity::class,
        CachedBudgetEntity::class,
        CacheMetadataEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingDocumentDao(): PendingDocumentDao
    abstract fun transactionsDao(): TransactionsDao
    abstract fun budgetsDao(): BudgetsDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
}
