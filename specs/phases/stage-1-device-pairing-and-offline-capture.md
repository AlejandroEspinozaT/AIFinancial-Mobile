# Stage 1 — Device Pairing + Offline Document Capture Core

**Goal:** a user can pair this app with their AIFinancial account by scanning
a QR code (no password typed on the phone), and from that point on can
capture document photos that are durably queued **with zero further network
dependency** — provably so, in airplane mode, not just by inspection of the
code.

**Non-goal for this stage:** nothing gets sent to the backend yet. A queued
document stays `QUEUED` until Stage 2 exists. This stage proves the local
half of the mechanism in isolation.

- [ ] **Pairing screen**: camera-based QR scanner → decode the pairing code
      → `POST /api/devices/redeem` → store the returned access/refresh
      token pair in `EncryptedSharedPreferences` → navigate to the main app.
- [ ] **Generic failure handling**: an expired/already-used/invalid code all
      show the same message (per the backend's deliberate information-hiding
      design for this endpoint — see `specs/architecture/api-integration.md`).
- [ ] **Room schema**: a `PendingDocument` entity — id, local file path,
      capturedAtUtc, status (`QUEUED` / `UPLOADED` / `FAILED`), and a field
      for the eventual server-assigned document id (null until Stage 2).
- [ ] **Capture flow**: camera (or gallery picker as a fallback) → save the
      image to app-private storage → insert a `PendingDocument` row with
      `status = QUEUED`, synchronously, before returning control to the UI.
- [ ] **Queue list screen**: reads `PendingDocument` rows directly from Room
      (a `Flow`-backed query) and renders them with their status — this is
      the proof that the UI never needs the network to show correct state.
- [ ] **Survive process death**: killing the app (not just backgrounding it)
      mid-capture, or between capturing and the Room insert completing,
      must never silently lose a photo the user already took or leave an
      orphaned file with no matching row.
- [ ] **Tests**: unit tests for the `PendingDocument` DAO and the capture
      use case (does the row get inserted before any network call is even
      attempted — provable by using a fake/no network dependency at all in
      this stage, since none exists yet); `offline-review`'s full checklist
      run and passed, documented in the PR description, including the
      airplane-mode walkthrough.

**Deliverable:** pairing works end-to-end against the real backend, and a
user can capture multiple documents back-to-back with the device in
airplane mode, see them all listed as `QUEUED`, kill and relaunch the app,
and still see them all — all before Stage 2 (sync) exists at all.
