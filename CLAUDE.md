# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Where things live

- **Full spec**: [`specs/product/overview.md`](specs/product/overview.md) (what/why),
  [`specs/architecture/`](specs/architecture/) (how), and the staged backlog in
  [`specs/phases/`](specs/phases/README.md). Read the relevant files before
  implementing anything — this file only holds short-lived operational
  guidance, not the spec itself.
- **Project skills**: `.claude/skills/` — `investigate`, `implement-stage`,
  `offline-review`, `code-review`, `test`. Prefer these over improvising a
  workflow from scratch.
- **Project agent**: `.claude/agents/android-mobile-expert.md` — use this
  agent for substantial Kotlin/Android implementation work in this repo.

## Project status

Kotlin/Jetpack Compose Android app, two Gradle modules: `:core` (pure
Kotlin/JVM, Stage 1's queue/pairing domain logic — resolvable and testable
in this sandbox, see below) and `:app` (the Android application —
buildable only in Android Studio, see the constraint below). Check
[`specs/phases/README.md`](specs/phases/README.md) for exact current status
before assuming anything is or isn't built.

### Two verified, hard constraints of this dev environment — read before running Gradle here

1. **Google's Maven repository is genuinely unreachable** (`dl.google.com`
   returns 403 through the proxy — confirmed directly against the proxy's
   own status endpoint, which logs it as a policy denial, not a
   misconfiguration to work around). The Android Gradle Plugin, AndroidX,
   Compose, Room, and WorkManager all live there, so **`./gradlew build`/
   `assembleDebug`/anything under `:app` will fail here, and that is
   expected — it is not a bug to chase.** `settings.gradle.kts` scopes
   `google()` to only the groups that actually need it (content filtering),
   specifically so this failure stays isolated to `:app` and never blocks
   resolving `:core`.
2. **Maven Central's Cloudflare front intermittently 404s a plain `HEAD`
   request for an artifact that a `GET` on the exact same URL serves
   fine** — confirmed directly (not assumed) during this project's own
   setup: `kotlin-util-klib-2.0.21.jar`'s HEAD returned 404 with a
   `retry-after` header while GET returned 200. Gradle's dependency
   resolver relies on HEAD to decide an artifact exists at all, so this
   silently breaks resolution of whatever artifact it hits, unpredictably.
   `settings.gradle.kts` adds Google's GCS-hosted mirror of Maven Central
   (`maven-central.storage-download.googleapis.com` — a different host
   from the blocked `dl.google.com`, not Cloudflare-fronted) ahead of
   `mavenCentral()` specifically to route around this. If a *new*
   dependency added later still fails to resolve with a "Could not find
   ...jar" error (not a 403/ connection error), re-check this exact
   failure mode (`curl -I` the URL Gradle names — a 404 on HEAD but 200 on
   GET is the signature) before assuming the dependency doesn't exist.

Together these mean: **`:core` is real, resolvable, and testable in this
sandbox — verified, not assumed (`./gradlew :core:test` passes, 8/8).**
`:app` can only be built, run, and manually verified in Android Studio on a
machine with normal internet access — that verification step belongs to
the human in the loop, not to this agent, for any stage that touches
`:app`'s Android-only code.

### Commands

```bash
./gradlew :core:test                # runs here — verified, pure-JVM domain logic
./gradlew :app:testDebugUnitTest    # Android Studio only (constraint 1 above)
./gradlew :app:lint                 # Android Studio only (constraint 1 above)
```

## Non-negotiables

- **Offline-first is not optional.** Any stage touching capture, the local
  queue, or sync must be validated with the device in airplane mode, not
  just unit-tested against a mock — see `offline-review`.
- The UI layer never talks to the network directly and never branches on
  connectivity to decide what to render — it always reads from Room (see
  `specs/architecture/overview.md`).
- A locally queued write is never presented as indistinguishable from a
  confirmed one. Status is always visible, never implied.
- This app never re-implements backend authorization/revocation logic
  client-side (e.g. guessing whether a device is still valid) — it only
  reacts to what the backend's responses actually say.
- No business logic belongs in Composables — screens render state, they
  don't compute it.
