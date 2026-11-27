# Stage 1 — Device Pairing + Offline Document Capture Core

**Goal:** a user can pair this app with their AIFinancial account by scanning
a QR code (no password typed on the phone), and from that point on can
capture document photos that are durably queued **with zero further network
dependency** — provably so, in airplane mode, not just by inspection of the
code.

**Non-goal for this stage:** nothing gets sent to the backend yet. A queued
document stays `QUEUED` until Stage 2 exists. This stage proves the local
half of the mechanism in isolation.

- [x] **Pairing screen**: camera-based QR scanner → decode the pairing code
      → `POST /api/devices/redeem` → store the returned access/refresh
      token pair in `EncryptedSharedPreferences` → navigate to the main app.
      (`ui/pairing/`, `data/repository/PairingRepository.kt`,
      `pairing/AuthTokenStore.kt`.)
- [x] **Generic failure handling**: an expired/already-used/invalid code all
      show the same message (per the backend's deliberate information-hiding
      design for this endpoint — see `specs/architecture/api-integration.md`).
      (`PairingRepository.redeem` collapses a non-2xx response and a genuine
      network failure into the same `PairingResult.InvalidOrExpired`.)
- [x] **Room schema**: a `PendingDocument` entity — id, local file path,
      capturedAtUtc, status (`QUEUED` / `UPLOADED` / `FAILED`), and a field
      for the eventual server-assigned document id (null until Stage 2).
      (`data/local/PendingDocumentEntity.kt`; the domain-level state machine
      and its tests already exist in `:core`, verified: 8/8 passing.)
- [x] **Capture flow**: camera → save the image to app-private storage
      (`ui/capture/CaptureFileFactory.kt`, a `FileProvider`-backed
      `content://` URI) → insert a `PendingDocument` row with
      `status = QUEUED`, synchronously (suspended on), before returning
      control to the UI (`DocumentCaptureRepository.enqueue`). No gallery-
      picker fallback yet — camera-only for this pass.
- [x] **Queue list screen**: reads `PendingDocument` rows directly from Room
      via a `Flow` (`CaptureViewModel.queue`), rendered in `CaptureScreen` —
      this is the architectural proof that the UI never needs the network
      to show correct state.
- [x] **Survive process death** — device-verified on an emulator
      (`Medium_Phone_API_37.0`) running the `offline-review` checklist with
      airplane mode on: captured, killed the process mid-flow (`adb shell am
      kill`), relaunched, and the pending photo was initially **lost** from
      the queue (real bug, not a hypothetical — Compose `remember` doesn't
      survive process death). Fixed in PR #4 by moving the pending capture
      path into `CaptureViewModel`'s `SavedStateHandle`; re-verified after the
      fix with two more kill-mid-capture passes, both recovered (`Queue (6)`
      after 4 pre-existing + 2 recovered rows). See PR #4's test plan for the
      exact before/after counts.
- [x] **Tests**: `:core`'s queue-state-machine and `PendingDocument` unit
      tests exist and pass (8/8). `CaptureViewModelTest` (added in PR #4, 3
      tests, runs in CI via `testDebugUnitTest`) directly simulates process
      death by restoring a `CaptureViewModel` from a copied `SavedStateHandle`
      and asserts the pending file still gets enqueued — this is now a
      standing regression test for the bug above, not just a one-time manual
      check. `offline-review`'s device-level checklist (airplane-mode
      walkthrough, process-death, accumulation) **has** been run on a real
      emulator (PRs #2–#4) and found two more real bugs beyond the process-
      death one: cleartext HTTP blocked by default on API 28+ against the
      `10.0.2.2` dev backend (fixed, debug-only, PR #2), and the emulator's
      virtual camera being impractical to drive via QR automation (worked
      around with a debug-only manual pairing-code entry field feeding the
      same `PairingViewModel.onCodeScanned` path, PR #3). DAO/repository-
      level instrumented tests for `:app` are still not written; nothing
      found so far has needed them to be caught.

**Deliverable:** pairing works end-to-end against the real backend, and a
user can capture multiple documents back-to-back with the device in
airplane mode, see them all listed as `QUEUED`, kill and relaunch the app,
and still see them all — all before Stage 2 (sync) exists at all.

**Status: done, device-verified.** CI (`.github/workflows/ci.yml`) compiles
`:app`, runs its lint, and runs its unit tests on every push/PR. Beyond that,
the full `offline-review` device checklist was actually run on a real
emulator (not this sandbox, which still cannot run `:app` at all — see
`CLAUDE.md`) and found and fixed three real bugs: process-death data loss
(PR #4), cleartext HTTP blocked by default (PR #2), and QR-scan automation
being impractical on an emulator (PR #3, debug-only workaround). All three
PRs are merged into `main`. Stage 2 (background sync) can build on this.
