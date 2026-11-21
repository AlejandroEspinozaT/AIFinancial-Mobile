package com.aifinancial.mobile.auth

import com.aifinancial.mobile.data.remote.AuthApi
import com.aifinancial.mobile.data.remote.dto.RefreshTokenRequest
import com.aifinancial.mobile.data.remote.dto.RefreshTokenResponseDto
import com.aifinancial.mobile.pairing.AuthTokenStore
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.Response as RetrofitResponse

/**
 * [TokenAuthenticator] implements OkHttp's synchronous `Authenticator` SPI
 * directly, so these construct real [Response]/[Request] objects rather than
 * going through a live HTTP call — exactly what `Authenticator.authenticate`
 * receives from OkHttp itself.
 */
class TokenAuthenticatorTest {

    private val authTokenStore = mockk<AuthTokenStore>(relaxed = true)
    private val authApi = mockk<AuthApi>()
    private val sessionExpiryNotifier = mockk<SessionExpiryNotifier>(relaxed = true)
    private val authenticator = TokenAuthenticator(authTokenStore, authApi, sessionExpiryNotifier)

    private fun sampleRequest(): Request = Request.Builder().url("http://10.0.2.2:8080/api/documents").build()

    private fun unauthorizedResponse(request: Request = sampleRequest(), prior: Response? = null): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody(null))
            .apply { if (prior != null) priorResponse(prior) }
            .build()

    @Test
    fun `successful refresh retries the request with the new access token`() {
        every { authTokenStore.getRefreshToken() } returns "refresh-1"
        val call = mockk<Call<RefreshTokenResponseDto>>()
        every { call.execute() } returns RetrofitResponse.success(RefreshTokenResponseDto("new-access", "new-refresh"))
        every { authApi.refresh(RefreshTokenRequest("refresh-1")) } returns call

        val result = authenticator.authenticate(null, unauthorizedResponse())

        assertThat(result?.header("Authorization")).isEqualTo("Bearer new-access")
        verify { authTokenStore.updateTokens("new-access", "new-refresh") }
    }

    @Test
    fun `a rejected refresh clears tokens and notifies session expiry`() {
        every { authTokenStore.getRefreshToken() } returns "refresh-1"
        val call = mockk<Call<RefreshTokenResponseDto>>()
        every { call.execute() } returns RetrofitResponse.error(400, "".toResponseBody(null))
        every { authApi.refresh(RefreshTokenRequest("refresh-1")) } returns call

        val result = authenticator.authenticate(null, unauthorizedResponse())

        assertThat(result).isNull()
        verify { authTokenStore.clear() }
        verify { sessionExpiryNotifier.notifyExpired() }
    }

    @Test
    fun `no refresh token stored means no attempt and no retry`() {
        every { authTokenStore.getRefreshToken() } returns null

        val result = authenticator.authenticate(null, unauthorizedResponse())

        assertThat(result).isNull()
        verify(exactly = 0) { authApi.refresh(any()) }
    }

    @Test
    fun `already retried once gives up without a second refresh attempt`() {
        val first = unauthorizedResponse()
        val second = unauthorizedResponse(prior = first)

        val result = authenticator.authenticate(null, second)

        assertThat(result).isNull()
        verify(exactly = 0) { authApi.refresh(any()) }
    }
}
