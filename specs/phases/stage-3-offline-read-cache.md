# Stage 3 — Offline-Readable Cache of Transactions/Budgets

**Goal:** a user can open the app with no connectivity and see their most
recently synced transactions and budgets — read-only, bounded, and never
presented as more current than it actually is.

**Current state:** Stages 1–2 give the app a working offline *write* path.
Nothing yet lets the user *read* anything from their account without a live
connection.

- [ ] **Room schema**: cached, read-only mirrors of the subset of
      `GET /api/transactions` / `GET /api/budgets` needed for a simple
      list/summary view — not a full offline replica of every field the web
      app shows.
- [ ] **Refresh-on-connectivity**: a periodic/triggered `CoroutineWorker`
      pulls the latest data when online and replaces the local cache;
      reads always come from Room, exactly like the write path in Stage 1 —
      the UI layer still never talks to the network directly.
- [ ] **Staleness is visible, not hidden**: every cached screen shows a
      "last updated" timestamp. The app never implies a cached list is
      live data — this is the read-side equivalent of Stage 1's rule that a
      queued document is never shown as indistinguishable from an uploaded
      one.
- [ ] **No stale-write risk**: this cache is strictly read-only — nothing in
      this stage lets a cached transaction/budget be edited from the phone,
      so there is no conflict-resolution problem to solve here (deliberately
      unlike the shared-stock scenario discussed for a different project;
      this app's writes are only ever new documents, never edits to
      existing financial records).
- [ ] **Tests**: cache-population and staleness-timestamp logic unit
      tested; a manual offline walkthrough confirming the cached screens
      render correctly with the device in airplane mode after at least one
      successful online refresh.

**Deliverable:** after at least one successful sync while online, a user can
enable airplane mode and still see a correct (if possibly stale, clearly
labeled) view of their transactions and budgets.
