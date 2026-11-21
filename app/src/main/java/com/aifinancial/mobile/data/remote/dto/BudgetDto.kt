package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches AIFinancial backend's `BudgetDto` (Application/Budgets/Dtos). */
@Serializable
data class BudgetDto(
    val id: String,
    val category: String,
    val monthlyLimit: Double,
    val amountSpent: Double,
    val percentUtilized: Double,
    val isExceeded: Boolean,
)
