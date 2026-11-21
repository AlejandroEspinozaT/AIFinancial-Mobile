package com.aifinancial.mobile.auth

import com.aifinancial.mobile.data.remote.AuthApi
import com.aifinancial.mobile.data.remote.dto.RefreshTokenRequest
import com.aifinancial.mobile.pairing.AuthTokenStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Implements specs/architecture/api-integration.md's token-refresh contract
 * as an OkHttp `Authenticator`: transparent to every call site, triggered
 * only on a real `401`. OkHttp calls [authenticate] synchronously from a
 * connection-pool thread (never the main thread), so a blocking
 * [retrofit2.Call.execute] here is the correct, documented way to do this —
 * not a workaround.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val authTokenStore: AuthTokenStore,
    private val authApi: AuthApi,
    private val sessionExpiryNotifier: SessionExpiryNotifier,
) : okhttp3.Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // OkHttp retries a 401 by calling this again on the new request's
        // own 401, if any — without this guard, a refresh that itself comes
        // back 401 (or a token the backend keeps rejecting) would loop
        // forever. One refresh attempt per original request, never more.
        if (responseChainLength(response) >= 2) {
            return null
        }

        val refreshToken = authTokenStore.getRefreshToken() ?: return null

        val refreshResponse = try {
            authApi.refresh(RefreshTokenRequest(refreshToken)).execute()
        } catch (_: IOException) {
            // A network failure *refreshing* is not the same as a revoked
            // device — leave tokens as-is so the next request gets a normal
            // chance to retry refresh too, rather than forcing re-pairing
            // over what might just be a flaky connection.
            return null
        }

        if (!refreshResponse.isSuccessful) {
            // The refresh token itself was rejected (expired, already
            // rotated, or the device was revoked) — per
            // specs/architecture/overview.md, this app does not try to
            // distinguish which; it just forces re-pairing.
            authTokenStore.clear()
            sessionExpiryNotifier.notifyExpired()
            return null
        }

        val body = refreshResponse.body() ?: return null
        authTokenStore.updateTokens(accessToken = body.accessToken, refreshToken = body.refreshToken)

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${body.accessToken}")
            .build()
    }

    private fun responseChainLength(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
