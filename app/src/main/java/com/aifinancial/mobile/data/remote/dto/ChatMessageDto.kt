package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Matches AIFinancial backend's `ChatMessageDto`. `role` is the raw
 * `ChatMessageRole` enum's `.ToString()` -- always exactly `"User"` or
 * `"Assistant"` (see that repo's `ChatMappingExtensions.ToDto`).
 */
@Serializable
data class ChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val createdAtUtc: String,
)
