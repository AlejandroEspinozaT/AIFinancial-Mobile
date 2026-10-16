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
- [ ] **Survive process death** — code is *designed* for this (the Room
      insert is awaited before any success is reported, the file is written
      before the DB row, nothing depends on in-memory state), but the actual
      proof — kill the process mid-capture on a real device/emulator and
      confirm nothing is lost — can only be done in Android Studio; this
      sandbox cannot run the `:app` module at all (see `CLAUDE.md`).
- [ ] **Tests**: `:core`'s queue-state-machine and `PendingDocument` unit
      tests exist and pass here (8/8, verified — not assumed). DAO/
      repository/instrumented tests for `:app` are **not yet written** —
      they need Android Studio to execute (same constraint). `offline-review`'s
      device-level checklist (airplane-mode walkthrough, process-death,
      accumulation) has **not** been run — it requires a real device/emulator
      this agent does not have access to.

**Deliverable:** pairing works end-to-end against the real backend, and a
user can capture multiple documents back-to-back with the device in
airplane mode, see them all listed as `QUEUED`, kill and relaunch the app,
and still see them all — all before Stage 2 (sync) exists at all.

**Status: code-complete, not yet device-verified.** CI
(`.github/workflows/ci.yml`) now compiles `:app`, runs its lint, and runs
its unit tests on every push/PR — closing the "does this even compile"
gap this sandbox can't check. It does **not** and cannot replace the
device-level checks still unchecked above (airplane mode, process death,
accumulation) — those need a real device/emulator and remain the next
concrete step before this stage can be called done, not an afterthought.
