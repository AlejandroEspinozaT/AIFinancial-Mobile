---
name: implement-stage
description: Implement a stage of the AIFinancial Mobile app end-to-end, from spec to tested, working code. Use when asked to build/implement/continue a stage (e.g. "arranca la etapa 1", "implementa el sync"), or to add something described in specs/architecture/.
---

# Implement Stage

This project is built stage by stage (see `specs/phases/README.md`). Every stage must
land as a working, tested, demonstrable increment — not partial code across layers, and
never a stage whose offline claims are only asserted, not proven.

## Steps

1. **Read before writing.** Load the relevant `specs/phases/stage-N-*.md` checklist and
   `specs/architecture/overview.md`. If anything is ambiguous, use the `investigate`
   skill first rather than guessing.
2. **Plan across layers**, respecting the dependency direction in
   `specs/architecture/overview.md` (`ui` → `repository` → `local`/`remote`, never the
   reverse, and `ui` never touches `remote` directly):
   - `data/local` — Room entities/DAOs first, no dependency on anything else
   - `data/remote` — Retrofit interface + DTOs (only once `api-integration.md` confirms
     the exact contract)
   - `data/repository` — the only layer allowed to depend on both `local` and `remote`
   - `sync/` — WorkManager workers, built against the repository layer, never directly
     against Room or Retrofit
   - `ui/` — Compose screens + ViewModels; reads only from the repository/Room, never
     computes business logic a repository or use case should own
   - `di/` — Hilt wiring, last, once the pieces it wires actually exist
3. **If this stage touches capture, the local queue, or sync**, run the `offline-review`
   skill's checklist as you write the code, not after — this is the one checklist that
   is never optional for this project.
4. **Write tests as you go.** Unit tests for anything with no Android SDK dependency
   (DAO logic via Room's in-memory test database is still Android-instrumented — see
   `test` skill for what actually runs where in this sandbox vs. Android Studio).
5. **Verify before calling it done**, within this sandbox's real constraints (see
   `CLAUDE.md`'s hard-constraint section): run whatever `./gradlew` tasks *can* actually
   resolve here (pure-JVM module tests, if any exist yet); for anything that needs
   Google Maven (the `:app` module), say explicitly that it needs verification in
   Android Studio rather than claiming it builds when it was never actually run.
6. **Update the checklist** in the relevant `specs/phases/stage-N-*.md` file — check off
   what's actually done, leave unchecked what isn't, and correct the file if reality
   diverged from the plan.
7. Only commit/push if the user asked for it, per the repo's own git conventions.

## Non-negotiables (from `specs/architecture/` and `CLAUDE.md`)

- No business logic in Composables.
- The UI layer never branches on connectivity to decide what to render — it always
  reads from Room.
- A locally queued write is never presented as indistinguishable from a confirmed one.
- This app never re-implements backend authorization/revocation logic client-side.
