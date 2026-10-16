package com.aifinancial.mobile.data.remote

import com.aifinancial.mobile.data.remote.dto.DeviceAuthResponseDto
import com.aifinancial.mobile.data.remote.dto.RedeemPairingCodeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DevicesApi {
    /**
     * The one unauthenticated call this app makes (see
     * specs/architecture/api-integration.md). Returns `Response<T>` rather
     * than throwing on a non-2xx so the repository can distinguish a `400`
     * (invalid/expired/already-redeemed code — all deliberately
     * indistinguishable, per the backend's own design) from a real network
     * failure, without exceptions standing in for expected control flow.
     */
    @POST("devices/redeem")
    suspend fun redeemPairingCode(@Body request: RedeemPairingCodeRequest): Response<DeviceAuthResponseDto>
}
