package com.aifinancial.mobile.ui.session

import androidx.lifecycle.ViewModel
import com.aifinancial.mobile.auth.SessionExpiryNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow

/** Thin Compose-facing wrapper around [SessionExpiryNotifier] — see [AppNavHost]. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionExpiryNotifier: SessionExpiryNotifier,
) : ViewModel() {
    val expiryEvents: SharedFlow<Unit> = sessionExpiryNotifier.events
}
