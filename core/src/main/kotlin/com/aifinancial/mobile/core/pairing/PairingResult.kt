package com.aifinancial.mobile.core.pairing

/**
 * Outcome of redeeming a scanned pairing code against
 * `POST /api/devices/redeem`. [InvalidOrExpired] intentionally carries no
 * detail about *why* the code failed (unknown vs. expired vs. already
 * redeemed) — the backend itself returns the same generic message for all
 * three cases by design (see the AIFinancial backend's
 * `RedeemPairingCodeCommandHandler`), and this type must not reconstruct a
 * distinction the backend deliberately hides.
 */
sealed interface PairingResult {
    data class Paired(
        val deviceId: String,
        val accessToken: String,
        val accessTokenExpiresAtEpochSeconds: Long,
        val refreshToken: String,
    ) : PairingResult

    data object InvalidOrExpired : PairingResult
}
