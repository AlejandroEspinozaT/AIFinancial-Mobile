# Technology Choices

## Kotlin + Jetpack Compose, not XML views

Compose over the View system: a new app with no legacy screens to carry
forward has nothing to gain from the View system's extra ceremony, and
Compose's state-driven model maps directly onto "the UI always reflects
whatever Room currently holds" — the core architectural rule of this app.

## Room, not a hand-rolled SQLite layer or a NoSQL local store

The offline queue's entire value depends on durable, query-able local state
that survives process death. Room gives us that with compile-time-checked
queries and a well-understood migration story — rolling a custom SQLite
wrapper would just be reimplementing Room with none of its guarantees.

## WorkManager, not a custom background-sync mechanism

WorkManager is the only correct choice once the constraint is "finish this
upload even if the app is backgrounded or the process dies, and respect
Android's battery/Doze restrictions." A foreground service or a raw
coroutine scope tied to the Activity lifecycle would both break the moment
the user switches apps mid-upload, which is a normal, expected case here
(photo capture while walking around), not an edge case.

## Retrofit + OkHttp + kotlinx.serialization, not a hand-rolled HTTP client

Standard choice, consistent with the backend's existing JSON contract
(`AIFinancial`'s `api-reference.md`). `kotlinx.serialization` over Gson/Moshi
because Kotlin DTOs can be `@Serializable` data classes directly, no
reflection, no separate adapter generation step.

## ZXing (`com.journeyapps:zxing-android-embedded`), not ML Kit, for QR scanning

Pairing needs to scan one QR code and nothing else. ML Kit's barcode
scanning is the other common choice, but it pulls in a Play Services
dependency and a much larger footprint for a single, well-understood
decoding task ZXing has handled reliably for years with no Play Services
requirement — relevant since this app's own identity (device pairing) must
not implicitly assume Google Play Services is present on every target
device.

## AndroidX Security Crypto (`EncryptedSharedPreferences`), not plain `SharedPreferences`

Device tokens are bearer credentials (see `api-integration.md`) — the same
rigor the backend repo applies to secrets (never logged, never stored raw)
applies here. `EncryptedSharedPreferences`, backed by an Android
Keystore-derived key via `MasterKey`, is the standard, no-extra-
infrastructure way to do this on-device; still alpha-versioned upstream,
which is a known, accepted tradeoff (it has had no stable release in years
but remains the standard recommendation for exactly this use case).

## Hilt, not manual DI or a service locator

With Room, a Retrofit client, WorkManager workers, and the pairing/token
layer all needing to be wired together and swapped in tests, manual DI
becomes its own maintenance burden quickly. Hilt is the standard Android
answer and integrates directly with WorkManager's own DI hooks
(`HiltWorkerFactory`), which a hand-rolled container would have to
reimplement.

## Single Gradle module for now, with one deliberate early exception

A single `:app` module avoids premature multi-module ceremony. The one
exception worth calling out: once Stage 1 has concrete domain logic to
place (the `PendingDocument` state machine — QUEUED → UPLOADED/FAILED, and
the queue-draining decision logic itself), that logic should live in a
pure-Kotlin/JVM module with **no AndroidX or Android SDK dependency at
all** — not because of premature architecture purity, but for a concrete,
current reason: this project's CI/remote-dev environment can resolve
Maven Central but **cannot reach Google's Maven repository**
(`dl.google.com` — AGP, AndroidX, Compose, and Room's own artifacts are all
hosted there and are unreachable from that sandbox; this was verified
directly, not assumed). A pure-JVM module resolving only from Maven Central
is the one way to get real, fast, automatically-verifiable unit tests for
the app's most important logic (the offline queue state machine) in an
environment that cannot build the Android module itself. The actual
Android app module (UI, Room, WorkManager) can only be built and run from
Android Studio on a machine with normal internet access to Google's Maven —
this is a real, documented constraint of this project's current dev setup,
not a hypothetical one.

## Android SDK / build tooling the developer machine needs

Verified against this project's `compileSdk`/`targetSdk` (34) and AGP
(8.6.1) in `app/build.gradle.kts` / `gradle/libs.versions.toml`. In Android
Studio's SDK Manager, make sure these are installed:
- **Android SDK Platform 34** (SDK Platforms tab)
- **Android SDK Build-Tools** 34.x (SDK Tools tab)
- **Android SDK Command-line Tools (latest)**
- **Android Emulator** + at least one system image (SDK Tools tab), if
  testing on an emulator rather than a physical device
- The Kotlin plugin ships bundled with current Android Studio — no separate
  install needed; Gradle itself is handled by the committed wrapper
  (`./gradlew`), so no standalone Gradle install is required either.

## Testing

JUnit4 + Truth + MockK + `kotlinx-coroutines-test`, matching what's already
committed in `gradle/libs.versions.toml`. Compose UI tests
(`androidx.compose.ui:ui-test-junit4`) for the handful of screens worth an
instrumented test (the capture confirmation screen, the queue/status list)
— not for every screen, per the same "test what can actually break, not
everything" principle used in the backend repo.
