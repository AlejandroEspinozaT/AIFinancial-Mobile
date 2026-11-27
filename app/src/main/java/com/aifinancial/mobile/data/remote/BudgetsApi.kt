package com.aifinancial.mobile.data.remote

import com.aifinancial.mobile.data.remote.dto.BudgetDto
import retrofit2.Response
import retrofit2.http.GET

interface BudgetsApi {
    @GET("budgets")
    suspend fun getBudgets(): Response<List<BudgetDto>>
}
