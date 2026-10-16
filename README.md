# AIFinancial Mobile

Native Android companion app for the **AI Financial Intelligence Platform**
(the `AIFinancial` backend/web repo). Offline-first document capture +
device pairing — see [`CLAUDE.md`](CLAUDE.md) and
[`specs/product/overview.md`](specs/product/overview.md) for the full
picture.

## Status

Stage 0 scaffold only — no feature stage implemented yet. See
[`specs/phases/README.md`](specs/phases/README.md) for the roadmap.

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
