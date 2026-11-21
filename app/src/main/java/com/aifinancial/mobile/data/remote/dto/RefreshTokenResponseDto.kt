package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Matches the subset of AIFinancial backend's `AuthResultDto` this app needs
 * from `POST /api/auth/refresh`. The backend rotates the refresh token on
 * every use (see `RefreshTokenCommandHandler`) — the old one is dead the
 * moment this response is received, so both tokens are always persisted
 * together, never just the access token.
 */
@Serializable
data class RefreshTokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
)
