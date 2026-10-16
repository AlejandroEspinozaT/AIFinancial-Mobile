package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.core.pairing.PairingResult
import com.aifinancial.mobile.data.remote.DevicesApi
import com.aifinancial.mobile.data.remote.dto.RedeemPairingCodeRequest
import com.aifinancial.mobile.pairing.AuthTokenStore
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairingRepository @Inject constructor(
    private val devicesApi: DevicesApi,
    private val authTokenStore: AuthTokenStore,
) {

    fun isPaired(): Boolean = authTokenStore.isPaired()

    /**
     * Redeems a scanned pairing code. Per specs/architecture/api-integration.md,
     * an unknown/expired/already-used code, and a genuine network failure
     * during this one-time online step, are collapsed into the same
     * [PairingResult.InvalidOrExpired] from the UI's point of view — this app
     * does not try to guess *why* pairing failed beyond what the backend
     * itself is willing to say.
     */
    suspend fun redeem(code: String, deviceName: String): PairingResult {
        val response = try {
            devicesApi.redeemPairingCode(
                RedeemPairingCodeRequest(code = code, deviceName = deviceName, deviceType = "android"),
            )
        } catch (_: IOException) {
            return PairingResult.InvalidOrExpired
        }

        if (!response.isSuccessful) {
            return PairingResult.InvalidOrExpired
        }
        val body = response.body() ?: return PairingResult.InvalidOrExpired

        authTokenStore.saveTokens(
            deviceId = body.deviceId,
            accessToken = body.accessToken,
            refreshToken = body.refreshToken,
        )

        return PairingResult.Paired(
            deviceId = body.deviceId,
            accessToken = body.accessToken,
            accessTokenExpiresAtEpochSeconds = Instant.parse(body.expiresAtUtc).epochSecond,
            refreshToken = body.refreshToken,
        )
    }
}
