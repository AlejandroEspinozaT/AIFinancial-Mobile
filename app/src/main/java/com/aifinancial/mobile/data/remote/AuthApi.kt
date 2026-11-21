package com.aifinancial.mobile.data.remote

import com.aifinancial.mobile.data.remote.dto.RefreshTokenRequest
import com.aifinancial.mobile.data.remote.dto.RefreshTokenResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    /**
     * A plain blocking [Call], not `suspend`: this is called from
     * [com.aifinancial.mobile.auth.TokenAuthenticator], which implements
     * OkHttp's synchronous `Authenticator` SPI — there is no coroutine scope
     * available at that call site, and OkHttp itself expects this to block
     * the connection thread, not launch concurrent work.
     */
    @POST("auth/refresh")
    fun refresh(@Body request: RefreshTokenRequest): Call<RefreshTokenResponseDto>
}
