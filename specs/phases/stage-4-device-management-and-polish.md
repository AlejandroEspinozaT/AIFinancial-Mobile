# Stage 4 — Device Management & Polish

**Goal:** round out the app with the account-management actions a linked
device needs, and tighten the UX of the core flows built in Stages 1–3.

- [x] **Settings screen**: `ui/settings/SettingsScreen.kt`. Shows this
      device's name (`Build.MODEL`, captured at pairing time and now
      actually persisted — `AuthTokenStore.getDeviceName()`; it was computed
      in Stage 1 but silently dropped before this) and the email it's
      linked to (`DeviceAuthResponseDto.email`, likewise persisted now for
      the first time — its own KDoc on the backend said this was the point
      of including it, Stage 1 just hadn't acted on that yet). "Last
      synced" is a single app-wide indicator (`SettingsViewModel.lastSynced`),
      the most recent of all three independent sync paths (documents,
      transactions, budgets) — not just whichever one happens to be this
      screen's own concern.
- [x] **Unlink this device**: `SettingsRepository.unlinkDevice()` calls
      `DELETE /api/devices/{id}` for this device's own id (`DevicesApi.unlink`),
      then clears tokens (`AuthTokenStore.clear`) and every Room table
      (`AppDatabase.clearAllTables()`), returning to pairing. A `404`
      (already removed, e.g. from the web app, between opening this screen
      and tapping unlink) is treated the same as success — the end state
      this app cares about is identical either way.
- [x] **Handle server-initiated revocation gracefully**:
      `SessionExpiryNotifier` now carries a read-once revocation flag
      alongside its existing event stream. `PairingViewModel` consumes it at
      construction to pick its *initial* state — `PairingUiState.Error("This
      device was removed from your account")` instead of the normal `Idle`
      — when this screen was reached via Stage 2's forced-re-pairing path,
      never on a user-initiated unlink (which doesn't touch
      `SessionExpiryNotifier` at all) or a later, unrelated pairing attempt
      (the flag is consumed exactly once).
- [x] **Queue-status polish**: `CaptureScreen` gains an explicit "No
      documents captured yet" message instead of just an empty list — the
      queued/uploaded/failed visual states themselves (color, retry button)
      were already built generically in Stage 1, in anticipation of Stage 2
      actually producing the UPLOADED/FAILED states; nothing there needed
      revisiting.
- [x] **Error-state audit**: reviewed every network call added in Stages
      1–3 against this rule. All of them already resolve to a deliberate,
      user-legible state: `PairingViewModel` (`Error` message),
      `DocumentSyncRepository`/`ReadCacheSyncWorker` (`FAILED` rows with a
      visible reason + retry, or silently-retried `AmbiguousNetworkFailure`
      — silent *retry* is the deliberate design, not a silent failure; the
      queue's own status is never hidden), `SettingsViewModel`
      (unlink `Error`). No screen was found doing nothing on error.
- [ ] **Tests**: `SettingsRepositoryTest` (5 cases: success, the 404
      "already removed" case, a definite rejection, a network exception,
      and the "nothing to unlink" case), `SessionExpiryNotifierTest`
      (read-once semantics), and `PairingViewModelTest` (3 cases covering
      the new initial-state logic) — all plain JVM tests, run in CI. The
      spec's own ask here — **instrumented Compose UI tests** for the
      settings/unlink flow and the revocation-triggered re-pairing path —
      needs Android Studio/a device to execute at all (same constraint as
      every instrumented test in this repo) and is **explicitly left
      pending**, not attempted or claimed here.

**Deliverable:** the app feels finished for its defined scope — not just
functionally complete, but with every error path accounted for rather than
left as a TODO.

**Status: code-complete, CI-green expected, not yet device-verified.** The
backend's `DELETE /api/devices/{id}` contract was confirmed directly against
`AIFinancial`'s current `main` branch (`DevicesController.Revoke`), same
practice as Stages 2–3. The genuinely pending items are the instrumented UI
tests above and an actual on-device run of the full unlink →
re-pair-from-scratch flow and the revocation message's real-world trigger
(revoke this device from the web app while the mobile app is mid-session) —
both need a real device/emulator, the same category of pending work as
every earlier stage's.
