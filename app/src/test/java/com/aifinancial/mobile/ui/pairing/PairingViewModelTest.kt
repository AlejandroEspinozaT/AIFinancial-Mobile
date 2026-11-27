package com.aifinancial.mobile.ui.pairing

import com.aifinancial.mobile.auth.SessionExpiryNotifier
import com.aifinancial.mobile.data.repository.PairingRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers Stage 4's "handle server-initiated revocation gracefully": this
 * screen's *initial* state depends on whether it was reached because
 * [SessionExpiryNotifier] just forced re-pairing, not just the redeem flow
 * itself (already covered implicitly by [PairingRepository]'s own tests).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private val pairingRepository = mockk<PairingRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a normal launch starts Idle, with no revocation message`() {
        val sessionExpiryNotifier = SessionExpiryNotifier()

        val viewModel = PairingViewModel(pairingRepository, sessionExpiryNotifier)

        assertThat(viewModel.uiState.value).isEqualTo(PairingUiState.Idle)
    }

    @Test
    fun `reaching this screen after a forced re-pairing shows why`() {
        val sessionExpiryNotifier = SessionExpiryNotifier()
        sessionExpiryNotifier.notifyExpired()

        val viewModel = PairingViewModel(pairingRepository, sessionExpiryNotifier)

        assertThat(viewModel.uiState.value).isEqualTo(
            PairingUiState.Error("This device was removed from your account"),
        )
    }

    @Test
    fun `the revocation message does not resurface on a later pairing attempt`() {
        val sessionExpiryNotifier = SessionExpiryNotifier()
        sessionExpiryNotifier.notifyExpired()
        // First screen instance consumes the flag (as in the test above).
        PairingViewModel(pairingRepository, sessionExpiryNotifier)

        // A second instance -- e.g. the user backed out and reopened this
        // screen -- must not see the stale revocation reason again.
        val secondInstance = PairingViewModel(pairingRepository, sessionExpiryNotifier)

        assertThat(secondInstance.uiState.value).isEqualTo(PairingUiState.Idle)
    }
}
