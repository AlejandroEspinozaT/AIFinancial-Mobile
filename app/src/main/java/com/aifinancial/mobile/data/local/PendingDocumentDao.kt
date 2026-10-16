package com.aifinancial.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingDocumentDao {

    /**
     * Synchronous (suspend, but awaited before returning) by design — the
     * capture flow must have a durable row before it reports success to the
     * user, not after. See specs/phases/stage-1-device-pairing-and-offline-capture.md.
     */
    @Insert
    suspend fun insert(entity: PendingDocumentEntity)

    @Update
    suspend fun update(entity: PendingDocumentEntity)

    @Query("SELECT * FROM pending_documents ORDER BY capturedAtEpochMillis DESC")
    fun observeAll(): Flow<List<PendingDocumentEntity>>

    @Query("SELECT * FROM pending_documents WHERE status = :status")
    suspend fun getByStatus(status: String): List<PendingDocumentEntity>

    @Query("SELECT * FROM pending_documents WHERE localId = :localId")
    suspend fun getById(localId: String): PendingDocumentEntity?
}
