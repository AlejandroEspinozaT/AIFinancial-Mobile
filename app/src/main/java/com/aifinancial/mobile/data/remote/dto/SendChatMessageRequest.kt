package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches AIFinancial backend's `SendChatMessageRequest` (Api/Controllers/Requests). */
@Serializable
data class SendChatMessageRequest(
    val message: String,
    val sessionId: String? = null,
)
