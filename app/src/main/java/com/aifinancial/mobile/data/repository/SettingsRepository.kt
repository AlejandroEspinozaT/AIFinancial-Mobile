package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.data.local.AppDatabase
import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.remote.DevicesApi
import com.aifinancial.mobile.pairing.AuthTokenStore
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface UnlinkResult {
    data object Success : UnlinkResult
    data object Failed : UnlinkResult
}

/**
 * Stage 4's device-management actions
 * (specs/phases/stage-4-device-management-and-polish.md). This app only
 * ever acts on its own device id — there is no "manage other devices" here,
 * that stays the web app's job.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val devicesApi: DevicesApi,
    private val authTokenStore: AuthTokenStore,
    private val appDatabase: AppDatabase,
    private val cacheMetadataDao: CacheMetadataDao,
) {

    fun deviceName(): String? = authTokenStore.getDeviceName()

    fun email(): String? = authTokenStore.getEmail()

    fun observeDocumentsLastSynced(): Flow<Instant?> =
        cacheMetadataDao.observe(DOCUMENTS_CACHE_KEY).map { it?.let { entity -> Instant.ofEpochMilli(entity.lastUpdatedEpochMillis) } }

    /**
     * Calls the backend's existing `DELETE /api/devices/{id}` for this
     * device's own id, then clears every local token and every cached/
     * queued row — unlinking must not leave this account's data sitting on
     * a phone that no longer has permission to hold it. A `404` (the device
     * was already removed, e.g. from the web app, between opening this
     * screen and tapping unlink) is treated the same as success: the end
     * state this app cares about — "not linked, nothing cached locally" —
     * is identical either way.
     */
    suspend fun unlinkDevice(): UnlinkResult {
        val deviceId = authTokenStore.getDeviceId()
        if (deviceId == null) {
            clearLocalState()
            return UnlinkResult.Success
        }

        return try {
            val response = devicesApi.unlink(deviceId)
            if (response.isSuccessful || response.code() == 404) {
                clearLocalState()
                UnlinkResult.Success
            } else {
                UnlinkResult.Failed
            }
        } catch (_: IOException) {
            UnlinkResult.Failed
        }
    }

    private suspend fun clearLocalState() {
        authTokenStore.clear()
        withContext(Dispatchers.IO) {
            appDatabase.clearAllTables()
        }
    }

    private companion object {
        const val DOCUMENTS_CACHE_KEY = "documents"
    }
}
