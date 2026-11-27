package com.aifinancial.mobile.data.remote

import com.aifinancial.mobile.data.remote.dto.ChatSessionDetailDto
import com.aifinancial.mobile.data.remote.dto.SendChatMessageRequest
import com.aifinancial.mobile.data.remote.dto.SendChatMessageResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {
    @POST("chat")
    suspend fun sendMessage(@Body request: SendChatMessageRequest): Response<SendChatMessageResultDto>

    @GET("chat/sessions/{id}")
    suspend fun getSessionById(@Path("id") id: String): Response<ChatSessionDetailDto>
}
