package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches AIFinancial backend's `RedeemPairingCodeRequest` (Api/Controllers/Requests). */
@Serializable
data class RedeemPairingCodeRequest(
    val code: String,
    val deviceName: String,
    val deviceType: String? = null,
)
