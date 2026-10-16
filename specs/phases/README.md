# Phases — Staged Execution Backlog

Each stage is a complete, demonstrable increment — not partial code across
layers. Same principle as the `AIFinancial` backend repo: a stage isn't
"done" until it's been built, tested, and actually run, not just written.

| Stage | What it delivers | Status |
|---|---|---|
| [1](stage-1-device-pairing-and-offline-capture.md) | Device pairing + offline document capture core | ⬜ |
| [2](stage-2-background-sync.md) | Background sync of the offline queue | ⬜ |
| [3](stage-3-offline-read-cache.md) | Offline-readable cache of transactions/budgets | ⬜ |
| [4](stage-4-device-management-and-polish.md) | Device settings, unlink, queue-status UI polish | ⬜ |

## Why this order

Stage 1 before everything else because it *is* the product's reason to
exist (see `specs/product/overview.md`) — if offline capture doesn't work
standing alone, nothing built on top of it matters. Stage 1 includes
pairing (an unavoidable one-time online step to establish identity) but
explicitly **excludes** sync — the queue is built and proven durable before
a single byte is sent to the backend, so "offline capture" is validated on
its own merits, not smuggled in as a side effect of a sync feature that
happens to also write locally first.

Stage 2 (sync) only starts once Stage 1's queue has been proven to survive
app kill, device restart, and accumulation of multiple queued items while
offline — per `offline-review`'s checklist. Building sync against a queue
that hasn't been proven durable just means debugging two unproven things
at once.

Stage 3 (read cache) comes after sync because showing the user their own
transactions/budgets offline requires something to have synced data down
in the first place — there is nothing to cache before Stage 2 exists.

Stage 4 is explicitly last: device management (unlink, rename) and UI
polish are real but not load-bearing for the core offline-first claim this
app is built to prove.
