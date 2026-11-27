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

    // Plain @Volatile, not a Flow: this is read exactly once, by
    // PairingViewModel's own init, to decide its *initial* state — it isn't
    // observed over time the way [events] is.
    @Volatile
    private var revoked = false

    /** Safe to call from any thread, including an OkHttp dispatcher thread. */
    fun notifyExpired() {
        revoked = true
        _events.tryEmit(Unit)
    }

    /**
     * Stage 4's "handle server-initiated revocation gracefully": `true`
     * only the first time this is called after a [notifyExpired] — read-once
     * so a later, unrelated pairing attempt never resurfaces the old
     * revocation message.
     */
    fun consumeRevocationFlag(): Boolean {
        val was = revoked
        revoked = false
        return was
    }
}
