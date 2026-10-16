---
name: test
description: Write and run tests for this project following its conventions (JUnit4/Truth/MockK for unit tests, Compose UI tests for instrumented tests). Use when asked to add tests, run the test suite, or verify a change is covered before considering it done — but read this skill's note on what can actually execute in this sandbox before claiming anything passed.
---

# Test

## The constraint that shapes everything else here

This sandbox cannot reach Google's Maven repository (`dl.google.com` — verified
directly, see `CLAUDE.md`). Every dependency the `:app` module needs (AGP, AndroidX,
Compose, Room, WorkManager, Hilt) is hosted there. That means:

- **`./gradlew :app:testDebugUnitTest` will fail here with a plugin/dependency
  resolution error — this is an environment limitation, not a code problem.** Say so
  explicitly rather than reporting it as a failing test, same principle as the backend
  repo's Docker-dependent integration tests.
- The only Gradle tests that can actually run in this sandbox are in a module that
  resolves exclusively from Maven Central — i.e. the planned pure-Kotlin/JVM domain
  module from `technology-choices.md`, once it exists. Until that module exists, **no
  test in this repo can be executed here** — they can only be written correctly and
  verified by the user in Android Studio.
- Never claim `npm run build`/`dotnet test`-style "I ran it and it passed" language for
  anything under `:app` unless it was genuinely run. If you wrote a test but couldn't
  execute it, say exactly that.

## Writing unit tests

- Pure logic (queue state transitions, repository decision logic, DTO↔entity mapping)
  belongs in the dependency-light module once it exists, specifically so it's testable
  both here and in CI without needing Google Maven.
- JUnit4 + `com.google.truth:truth` for assertions, MockK for mocking (see
  `gradle/libs.versions.toml` for pinned versions) — do not introduce a second
  assertion/mocking library.
- One behavior per test method; name it `methodOrScenario_expectedOutcome` (Kotlin
  convention: backtick-quoted descriptive names are also fine and often clearer for
  Android tests — pick one style per test class, don't mix).
- Every new queue-state transition needs a test per branch (success, failure, ambiguous
  result/dedupe — see `offline-review`).

## Writing instrumented / Compose UI tests

- Location: `app/src/androidTest`, using `androidx.compose.ui:ui-test-junit4`.
- Reserve these for flows worth the instrumentation cost (pairing, capture
  confirmation, the queue/status list) — not for every screen.
- These can only run on an emulator/device in Android Studio — same constraint as the
  `:app` module's unit tests in this sandbox.

## Before calling a change "tested"

1. State plainly which tests were actually executed in this sandbox (likely: none, for
   any stage before the pure-JVM module exists) versus which were written but need
   Android Studio to run.
2. New business logic has a test, even if it can't be executed here yet — write it
   correctly and say it needs Android Studio/CI to confirm, same as the backend repo's
   convention for Docker-dependent integration tests.
3. Relevant `specs/phases/stage-N-*.md` checklist item is checked off only once its
   test coverage actually exists, not just the feature code.
4. For anything touching capture/queue/sync, `offline-review`'s checklist — which is
   manual/device-based, not something `./gradlew` can verify — was actually run.
