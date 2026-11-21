package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches AIFinancial backend's `RefreshTokenRequest` (Api/Controllers/Requests). */
@Serializable
data class RefreshTokenRequest(val refreshToken: String)
