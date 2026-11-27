package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.data.local.AppDatabase
import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.local.ChatSessionStore
import com.aifinancial.mobile.data.remote.DevicesApi
import com.aifinancial.mobile.pairing.AuthTokenStore
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

/**
 * Exercises specs/phases/stage-4-device-management-and-polish.md's unlink
 * flow: success, the "already removed" 404 (treated the same as success),
 * a definite rejection, and a network failure -- local state is only ever
 * cleared on an end state this app can be sure means "not linked anymore."
 */
class SettingsRepositoryTest {

    private val devicesApi = mockk<DevicesApi>()
    private val authTokenStore = mockk<AuthTokenStore>(relaxed = true)
    private val appDatabase = mockk<AppDatabase>(relaxed = true)
    private val cacheMetadataDao = mockk<CacheMetadataDao>(relaxed = true)
    private val chatSessionStore = mockk<ChatSessionStore>(relaxed = true)
    private val repository = SettingsRepository(devicesApi, authTokenStore, appDatabase, cacheMetadataDao, chatSessionStore)

    @Test
    fun `a successful unlink clears tokens and all local tables`() = runTest {
        every { authTokenStore.getDeviceId() } returns "device-1"
        coEvery { devicesApi.unlink("device-1") } returns Response.success<Void>(null)

        val result = repository.unlinkDevice()

        assertThat(result).isEqualTo(UnlinkResult.Success)
        verify { authTokenStore.clear() }
        verify { appDatabase.clearAllTables() }
        verify { chatSessionStore.clear() }
    }

    @Test
    fun `a 404 -- already removed server-side -- is treated the same as success`() = runTest {
        every { authTokenStore.getDeviceId() } returns "device-1"
        coEvery { devicesApi.unlink("device-1") } returns Response.error(404, "".toResponseBody(null))

        val result = repository.unlinkDevice()

        assertThat(result).isEqualTo(UnlinkResult.Success)
        verify { authTokenStore.clear() }
    }

    @Test
    fun `a definite rejection leaves local state untouched`() = runTest {
        every { authTokenStore.getDeviceId() } returns "device-1"
        coEvery { devicesApi.unlink("device-1") } returns Response.error(500, "".toResponseBody(null))

        val result = repository.unlinkDevice()

        assertThat(result).isEqualTo(UnlinkResult.Failed)
        verify(exactly = 0) { authTokenStore.clear() }
        verify(exactly = 0) { appDatabase.clearAllTables() }
    }

    @Test
    fun `a network exception leaves local state untouched`() = runTest {
        every { authTokenStore.getDeviceId() } returns "device-1"
        coEvery { devicesApi.unlink("device-1") } throws IOException("timeout")

        val result = repository.unlinkDevice()

        assertThat(result).isEqualTo(UnlinkResult.Failed)
        verify(exactly = 0) { authTokenStore.clear() }
    }

    @Test
    fun `no stored device id is already the end state, so it just clears`() = runTest {
        every { authTokenStore.getDeviceId() } returns null

        val result = repository.unlinkDevice()

        assertThat(result).isEqualTo(UnlinkResult.Success)
        verify { authTokenStore.clear() }
        coVerify(exactly = 0) { devicesApi.unlink(any()) }
    }
}
