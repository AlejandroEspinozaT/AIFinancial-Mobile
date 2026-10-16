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
`:app` can only be built in this sandbox's agent environment; it needs
either **CI** (`.github/workflows/ci.yml` — GitHub-hosted runners have
normal, unrestricted internet access, so `:app:assembleDebug`/
`:app:testDebugUnitTest`/`:app:lintDebug` run for real there on every push/PR)
or **Android Studio** for anything this agent cannot check itself —
running the app, and `offline-review`'s device-level checklist (airplane
mode, process death, accumulation) specifically, which no CI job can stand
in for. That device-level verification step belongs to the human in the
loop, not to this agent or to CI, for any stage that touches `:app`'s
Android-only code.

### Commands

```bash
./gradlew :core:test                # runs here — verified, pure-JVM domain logic
./gradlew :app:testDebugUnitTest    # runs in CI; Android Studio only in this sandbox
./gradlew :app:lintDebug             # runs in CI; Android Studio only in this sandbox
./gradlew :app:assembleDebug         # runs in CI; Android Studio only in this sandbox
```

## Git workflow

Same convention as the `AIFinancial` backend repo:

- **One stage = one PR.** Branch from latest `origin/main` as
  `feature/<stage-name>` (e.g. `feature/device-pairing-offline-capture`),
  implement the whole stage there, open one PR against `main` when it's
  code-complete and `:core`'s tests (plus anything CI covers) pass.
- **Never merge your own PR.** Open it, subscribe to its CI/review
  activity, drive it to green, and stop — the repo owner reviews and
  merges. Setup/infra changes that aren't a feature stage (CI config,
  a dependency bump) can be a smaller, equally-reviewed PR rather than a
  direct push to `main`.
- **Commit messages** explain the *why*, not just the what, and end with
  the attribution trailer (`Co-Authored-By` / `Claude-Session`) from the
  session's own instructions.
- **PR descriptions** follow the same `## Summary` / `## Test plan` /
  `## Docs` structure used in `AIFinancial`'s PRs — state plainly what was
  verified where (this sandbox vs. CI vs. "needs Android Studio/a device"),
  never implying something passed that was never actually run.
- Version bumps (`app/build.gradle.kts`'s `versionCode`/`versionName`) and
  `specs/phases/stage-N-*.md` checklist updates land in the same PR as the
  stage they describe, not a follow-up.

**Note on this repo's own history:** the Stage 0 scaffold, Stage 1's
implementation, and the CI pipeline were all pushed directly to `main`
during this repo's initial setup, before this convention was written down
here explicitly — a deviation from the rule above, not an exception to it.
Every change from here on follows the branch-and-PR workflow.

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
