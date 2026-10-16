---
name: offline-review
description: Apply this checklist before considering any stage "done" that touches document capture, the local queue, sync, or auth-token handling. This is the non-negotiable verification gate for AIFinancial Mobile's core offline-first claim — never skip it in favor of unit tests alone.
---

# Offline Review

Offline-first is this app's actual reason to exist (see
`specs/product/overview.md`). A stage that merely passes unit tests against a mocked
network dependency has **not** proven the offline claim — these checks specifically
target what unit tests typically miss: process death, airplane mode, and timing.

## The checklist

Run through every item that applies to the stage being reviewed. Report which ones were
actually exercised (ideally on a device/emulator, not just read from the code) and which
were not, rather than asserting the whole list silently passed.

1. **Airplane-mode walkthrough.** Put the device/emulator in airplane mode and perform
   the full user-facing flow the stage claims to support (capture, viewing the queue,
   viewing cached data). A claim like "works offline" that was never exercised with
   radios actually off is unverified, not verified.
2. **Process death mid-operation.** Force-stop the app (not just background it) at the
   point most likely to lose state — mid-capture, mid-DB-write, mid-sync-request — and
   relaunch. Nothing the user already did (a captured photo, a queued item) may be
   silently lost.
3. **Accumulation.** Queue more than one item while offline, then restore connectivity.
   Confirm the stage's actual behavior for multiple pending items (processed in order?
   independently? does one failure block the rest?) matches what the spec says, not
   just what happens to work for a single item in a demo.
4. **Idempotency on ambiguous results.** If a network call's outcome is genuinely
   unknown to the client (killed mid-request, timeout with no response), confirm a
   retry cannot produce a duplicate on the backend — rely on and confirm the backend's
   own dedupe/idempotency mechanism, don't assume a client-side guess is sufficient.
5. **Status is never implied, always shown.** Anything the user did while offline must
   be visibly distinguishable from confirmed/synced state in the UI — check the actual
   screen, not just that the underlying data model has a status field.
6. **No connectivity branching in the UI layer.** Grep for any `ui/` or Composable code
   that checks network state directly to decide what to render — if found, that's an
   architecture violation per `specs/architecture/overview.md`, not just a style nit.

## Where this applies

Every stage in `specs/phases/` that touches `data/local`, `sync/`, or token
refresh/pairing. Stage 1 and Stage 2 are where this matters most; re-run the
accumulation and idempotency checks again in Stage 2 even if Stage 1 already passed
them, since sync is exactly where those properties get tested for real.
