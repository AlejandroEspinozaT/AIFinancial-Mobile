# AIFinancial Mobile

Native Android companion app for the **AI Financial Intelligence Platform**
(the `AIFinancial` backend/web repo). Offline-first document capture +
device pairing — see [`CLAUDE.md`](CLAUDE.md) and
[`specs/product/overview.md`](specs/product/overview.md) for the full
picture.

## Status

Stage 1 (device pairing + offline document capture) is **code-complete,
not yet device-verified** — it needs Android Studio to actually run. See
[`specs/phases/stage-1-device-pairing-and-offline-capture.md`](specs/phases/stage-1-device-pairing-and-offline-capture.md)
for exactly what's checked off vs. still needs a real device/emulator, and
[`specs/phases/README.md`](specs/phases/README.md) for the full roadmap.

## Opening this project

Open this folder directly in **Android Studio** (File → Open). It uses the
committed Gradle wrapper (`./gradlew`), so no separate Gradle install is
needed. Make sure these are installed via Android Studio's SDK Manager:
Android SDK Platform 34, Android SDK Build-Tools 34.x, Android SDK
Command-line Tools, and an emulator system image if you're not testing on a
physical device. Full detail in
[`specs/architecture/technology-choices.md`](specs/architecture/technology-choices.md).

**Note:** this sandboxed Claude Code environment cannot reach Google's Maven
repository, so it cannot build or run `:app` itself — that first real build
and sync happens in your local Android Studio. See `CLAUDE.md` for why.
`:core` (pure Kotlin, no Android dependency) is the one module whose tests
are already verified there (`./gradlew :core:test`, 8/8 passing).

## Running against the backend

Start the `AIFinancial` backend locally (`docker compose up --build` in
that repo) and run this app on an emulator — it defaults to
`http://10.0.2.2:8080/api/`, the emulator's alias for the host machine's
`localhost` where that backend's `api` service listens. Pointing at a
different backend: `./gradlew assembleDebug -PapiBaseUrl=https://your-host/api/`.
