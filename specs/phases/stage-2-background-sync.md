# Stage 2 — Background Sync

**Goal:** every `QUEUED` document from Stage 1 actually reaches the backend,
automatically, whenever connectivity allows — without the user having to
open the app or tap anything, and without ever double-uploading a document
because a previous attempt's result was ambiguous.

**Current state (audited, not assumed):** Stage 1 leaves documents captured
and durably queued, but nothing sends them anywhere. This stage is the
other half of the mechanism described in `specs/architecture/overview.md`'s
offline write path.

- [ ] **Token refresh**: implement the OkHttp `Authenticator` described in
      `api-integration.md` — a `401` triggers one refresh attempt via
      `POST /api/auth/refresh`, persists the rotated token pair, and retries
      once; a failed refresh clears local tokens and routes back to pairing.
- [ ] **Sync worker**: a `CoroutineWorker` registered with a network
      constraint, triggered (a) immediately after each capture if
      connectivity is already present, and (b) by WorkManager's own
      constraint-tracking when connectivity returns after being absent.
- [ ] **Queue draining logic**: for each `QUEUED` row, call
      `POST /api/documents`; on success, update to `UPLOADED` with the
      server's returned document id; on a real failure (not just "no
      network" — WorkManager's constraint already prevents that case),
      mark `FAILED` with the reason, never silently drop the row.
- [ ] **Idempotency**: if a sync attempt's result is ambiguous (e.g. the app
      is killed mid-request, so the client never learns whether the server
      received it), the next attempt must not blindly re-POST and risk a
      duplicate — rely on the backend's existing hash-based document dedupe
      (already built in `AIFinancial`'s Stage 1) rather than inventing a
      second idempotency mechanism client-side.
- [ ] **Manual retry**: a `FAILED` row gets a "retry" action in the queue
      list screen, re-enqueuing it as `QUEUED`.
- [ ] **Tests**: unit tests for the queue-draining logic against a fake API
      client (success, failure, and the ambiguous-result/dedupe case);
      `offline-review` re-run — specifically the "accumulate several queued
      items offline, then restore connectivity all at once" scenario.

**Deliverable:** capture several documents in airplane mode, restore
connectivity, and watch all of them transition from `QUEUED` to `UPLOADED`
without opening the app — and confirm in the AIFinancial web app that they
actually arrived, exactly once each.
