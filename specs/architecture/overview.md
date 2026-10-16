# Architecture Overview

## Module layout

```
app/                          # Android application module (UI + platform wiring)
  src/main/java/com/aifinancial/mobile/
    ui/                       # Compose screens + ViewModels (MVVM)
    data/
      local/                 # Room entities, DAOs, the offline queue
      remote/                # Retrofit API interface, DTOs
      repository/            # Repository implementations (the only layer
                              # that talks to both local and remote)
    sync/                     # WorkManager workers, queue-draining logic
    pairing/                  # QR scan, pairing-code redemption, token storage
    di/                       # Hilt modules
    AiFinancialApp.kt
    MainActivity.kt
```

A single-module app is intentional for now — splitting into `:core`/`:data`/
`:app` Gradle modules is a refactor to do *if and when* build times or test
isolation actually demand it, not preemptively. See
`technology-choices.md` for the one case where an early module split is
already planned (pure-Kotlin domain logic, for a concrete, current reason).

## Dependency direction

```
ui (Compose + ViewModel)
   -> repository (interface + impl)
        -> local (Room)          # always available, source of truth for reads
        -> remote (Retrofit)     # only touched by sync/, never by ui/ directly
```

The UI layer never calls the network layer directly and never checks
connectivity to decide what to render. It always reads from Room. This is
the architectural consequence of offline-first being the actual design
goal, not a UI-layer concern: **Room is the single source of truth for
everything the UI shows**, and the network layer's only job is to keep Room
in sync, in the background, independently of whatever screen is open.

## The offline write path (the core mechanism of this app)

```
User captures a photo
   -> saved to local storage + a PendingDocument row in Room
      (status = QUEUED), synchronously, before any network attempt
   -> UI reads PendingDocument rows directly from Room — a queued
      document is visible and correct immediately, with no network
      round-trip in the critical path
   -> a WorkManager constraint-based worker (network required) drains the
      queue: for each QUEUED row, calls POST /api/documents, and updates
      the row's status (UPLOADED / FAILED) based on the real response —
      never guesses success from "the request was sent"
   -> if the app is killed mid-capture or mid-sync, the QUEUED row already
      persisted in Room is what resumes the work on next launch — there is
      no in-memory-only state this flow depends on
```

This is the mechanism `offline-review` (see the repo's skills) exists to
verify on every stage that touches it — see
`specs/phases/stage-1-device-pairing-and-offline-capture.md`.

## Auth & pairing

- Pairing redeems a code from `POST /api/devices/redeem` (backend Stage 19)
  and receives a device-scoped access + refresh token pair.
- Tokens are stored via the Android Keystore-backed `EncryptedSharedPreferences`
  (not plain `SharedPreferences` — these are bearer credentials).
- Refresh is handled by an OkHttp `Authenticator`, transparent to every other
  layer; a 401 triggers one refresh attempt via `POST /api/auth/refresh`,
  and only forces re-pairing if that refresh itself fails (revoked device).
- The device's own revocation state is enforced server-side (Stage 19's
  `DeviceNotRevokedAuthorizationHandler`) — the client does not need to, and
  must not try to, replicate that logic locally.

## What the client must never decide on its own

Mirroring the rule already established for the backend's multi-vendor stock
scenario: **this client never treats a local write as final**. A queued
document is "captured, pending confirmation" in the UI — never presented as
indistinguishable from an uploaded one — until the backend's response
confirms it (`UPLOADED`) or rejects it (`FAILED`, with the reason surfaced,
e.g. duplicate hash — the backend already treats a byte-identical re-upload
as an explicit duplicate, not a silent no-op, per the web app's existing
behavior).
