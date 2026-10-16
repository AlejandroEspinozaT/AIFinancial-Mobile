package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches AIFinancial backend's `DeviceAuthResultDto` (Application/Devices/Dtos). */
@Serializable
data class DeviceAuthResponseDto(
    val deviceId: String,
    val email: String,
    val accessToken: String,
    val expiresAtUtc: String,
    val refreshToken: String,
    val refreshTokenExpiresAtUtc: String,
)
