package com.aifinancial.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheMetadataDao {

    @Query("SELECT * FROM cache_metadata WHERE cacheKey = :cacheKey")
    fun observe(cacheKey: String): Flow<CacheMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CacheMetadataEntity)
}
