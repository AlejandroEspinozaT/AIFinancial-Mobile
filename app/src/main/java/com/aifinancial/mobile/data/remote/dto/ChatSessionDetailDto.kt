package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches AIFinancial backend's `ChatSessionDetailDto`. */
@Serializable
data class ChatSessionDetailDto(
    val id: String,
    val title: String? = null,
    val createdAtUtc: String,
    val messages: List<ChatMessageDto>,
)
