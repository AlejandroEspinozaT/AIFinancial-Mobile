package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Matches AIFinancial backend's `TransactionDto` (Application/Transactions/
 * Dtos). `date` stays a plain ISO-8601 string (`DateOnly`'s default JSON
 * form) — see `data/local/CachedTransactionEntity.kt`'s KDoc for why this
 * app never parses it into a richer type.
 */
@Serializable
data class TransactionDto(
    val id: String,
    val documentId: String,
    val date: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val type: String,
    val category: String,
)
