package com.aifinancial.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// exportSchema = false for Stage 1 — there's only one schema version and no
// migration to track yet; turn this on (and configure a schema directory)
// once a real migration is needed.
@Database(entities = [PendingDocumentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingDocumentDao(): PendingDocumentDao
}
