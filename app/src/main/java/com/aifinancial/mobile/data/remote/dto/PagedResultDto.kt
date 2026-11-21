package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches AIFinancial backend's generic `PagedResult<T>` (Application/Common/Models). */
@Serializable
data class PagedResultDto<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
)
