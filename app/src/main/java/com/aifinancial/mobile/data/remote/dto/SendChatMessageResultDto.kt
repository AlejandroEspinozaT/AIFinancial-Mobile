package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches AIFinancial backend's `SendChatMessageResultDto`. */
@Serializable
data class SendChatMessageResultDto(
    val sessionId: String,
    val message: ChatMessageDto,
)
