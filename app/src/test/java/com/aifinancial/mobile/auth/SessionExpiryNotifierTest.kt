package com.aifinancial.mobile.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionExpiryNotifierTest {

    @Test
    fun `the revocation flag is false before any expiry`() {
        val notifier = SessionExpiryNotifier()

        assertThat(notifier.consumeRevocationFlag()).isFalse()
    }

    @Test
    fun `notifyExpired sets the flag, and consuming it clears it`() {
        val notifier = SessionExpiryNotifier()

        notifier.notifyExpired()

        assertThat(notifier.consumeRevocationFlag()).isTrue()
        // Stage 4: read-once, so a later unrelated pairing attempt never
        // resurfaces the old revocation message.
        assertThat(notifier.consumeRevocationFlag()).isFalse()
    }
}
