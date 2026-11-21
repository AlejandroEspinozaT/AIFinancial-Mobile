package com.aifinancial.mobile.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The only bridge between [TokenAuthenticator] — which runs on an OkHttp
 * connection thread, not the main thread, and has no navigation context of
 * its own — and the UI's decision to route back to pairing when a refresh
 * fails. See specs/architecture/overview.md: the device's revocation state
 * is enforced server-side, and this app finds out only when a request
 * actually fails; this is that notification, decoupled from any one screen.
 */
@Singleton
class SessionExpiryNotifier @Inject constructor() {

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Safe to call from any thread, including an OkHttp dispatcher thread. */
    fun notifyExpired() {
        _events.tryEmit(Unit)
    }
}
