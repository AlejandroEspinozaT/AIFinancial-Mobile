package com.aifinancial.mobile.di

import javax.inject.Qualifier

/**
 * The [retrofit2.Retrofit]/[okhttp3.OkHttpClient] pair used only for
 * `POST /api/auth/refresh`. Deliberately separate from the main, authenticated
 * pair: the main `OkHttpClient` carries a [com.aifinancial.mobile.auth.TokenAuthenticator]
 * that itself calls this endpoint on a 401 — wiring the refresh call through
 * the *same* client would be a circular dependency (and, if it somehow built,
 * an infinite 401-retry loop the moment refresh itself ever returned 401).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshRetrofit
