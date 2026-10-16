---
name: code-review
description: Review a diff in this repo against its own architecture rules — offline-first layering, UI/network separation, token/pairing handling, and test coverage for new business logic. Complements the generic code-review skill's bug-hunting with rules specific to this codebase; use both when reviewing a non-trivial change. Always run offline-review too for anything touching capture/queue/sync.
---

# Code Review (project-specific)

Scope: architectural and convention violations specific to this repo. For general
correctness bugs, simplification, and efficiency issues, also run the generic
`code-review` skill — this one does not replace it. For anything touching capture, the
local queue, sync, or tokens, also run `offline-review` — this skill checks code shape,
not offline behavior.

## Checklist

**Layering** (see `specs/architecture/overview.md`)
- Does a Composable or ViewModel call Retrofit/OkHttp directly instead of going through
  a repository? Move it.
- Does anything in `ui/` branch on connectivity (`ConnectivityManager`, a network-state
  flow, etc.) to decide what to render? It must always read from Room instead.
- Does `data/local` (Room) depend on anything in `data/remote` (Retrofit/DTOs)? It must
  not — DTOs map to/from Room entities in the repository layer, not inside Room itself.
- Is there business logic (status transitions, retry decisions) sitting inside a
  Composable? Move it to the ViewModel or a use case.

**Offline-first correctness** (see `specs/architecture/overview.md`, `offline-review`)
- Does a new write path insert into Room *before* attempting any network call, or does
  it try the network first and only fall back to local storage on failure (wrong
  order — the local write must always happen first and unconditionally)?
- Is a locally-queued-but-unsynced item ever rendered identically to a confirmed one?
- Does new sync logic handle an ambiguous network result (timeout, process death
  mid-request) without risking a duplicate, relying on the backend's own dedupe rather
  than a new client-side idempotency key invented ad hoc?

**Auth / pairing / tokens** (see `specs/architecture/api-integration.md`)
- Are tokens stored via `EncryptedSharedPreferences`, never plain `SharedPreferences`
  or logged anywhere (including crash reports / `Log.d`)?
- Is token refresh centralized in the OkHttp `Authenticator`, not reimplemented at
  individual call sites?
- Does any code try to locally guess/cache whether a device is still valid, instead of
  only reacting to what the backend's responses say?

**Testing** (see the `test` skill)
- Does new business logic (queue state transitions, repository branching) have a unit
  test?
- If the change touches `:app`'s Android-only code, is it clearly flagged as needing
  verification in Android Studio (this sandbox cannot build that module — see
  `CLAUDE.md`), rather than silently claimed as "tested" when it was never actually run?

## Output

Report findings the same way the generic `code-review` skill does (most severe first),
but tag each with which rule above it violates so it's clear this isn't a style
nitpick — it's a deviation from this repo's stated architecture.
