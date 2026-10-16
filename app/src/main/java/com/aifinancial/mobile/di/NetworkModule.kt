package com.aifinancial.mobile.di

import com.aifinancial.mobile.BuildConfig
import com.aifinancial.mobile.data.remote.DevicesApi
import com.aifinancial.mobile.pairing.AuthTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /**
     * Attaches the stored access token when one exists; it's a no-op for the
     * one call made before pairing (`redeemPairingCode`), since there's no
     * token yet. Token *refresh* (the OkHttp `Authenticator` described in
     * specs/architecture/api-integration.md) is Stage 2 — this interceptor
     * alone is enough for Stage 1, which makes exactly one authenticated-
     * capable call path and it's unauthenticated.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(authTokenStore: AuthTokenStore): Interceptor = Interceptor { chain ->
        val token = authTokenStore.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // BODY logging is deliberately never enabled even in debug: a
            // pairing code or token must never appear in a log line (same
            // rigor as the backend's own rule — see the project-specific
            // code-review skill).
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideDevicesApi(retrofit: Retrofit): DevicesApi = retrofit.create(DevicesApi::class.java)
}
