package com.aifinancial.mobile.data.remote

import com.aifinancial.mobile.data.remote.dto.PagedResultDto
import com.aifinancial.mobile.data.remote.dto.TransactionDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TransactionsApi {
    @GET("transactions")
    suspend fun getTransactions(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<PagedResultDto<TransactionDto>>
}
