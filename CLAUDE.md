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

Kotlin/Jetpack Compose Android app (single `:app` Gradle module for now, see
`specs/architecture/technology-choices.md` for the planned exception). No
stage has been implemented yet — this repo currently holds only the Stage 0
scaffold (empty placeholder screen) plus specs/skills/agent. Check
[`specs/phases/README.md`](specs/phases/README.md) for exact current status
before assuming anything is or isn't built.

### A hard constraint of this dev environment — read before running Gradle here

This sandboxed/remote environment **cannot reach Google's Maven repository**
(`dl.google.com` returns 403 through the proxy — verified directly). That
means the Android Gradle Plugin, AndroidX, Compose, and Room cannot be
resolved here, so **`./gradlew build`/`assembleDebug` for the `:app` module
will fail here and that is expected, not a bug to chase.** Maven Central
*is* reachable, which is the whole reason for keeping pure domain logic in
a dependency-light module per `technology-choices.md` — that module's tests
can actually run here. The full app can only be built, run, and manually
verified in Android Studio on a machine with normal internet access — that
verification step belongs to the human in the loop, not to this agent, for
any stage that touches `:app`'s Android-only code.

### Commands

```bash
./gradlew :app:lint                 # static analysis — needs Google Maven, Android Studio only
./gradlew test                      # unit tests — only modules with no Google-Maven-hosted deps run here
./gradlew :app:testDebugUnitTest    # app module unit tests — Android Studio only (see constraint above)
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
