# Stage 3 — Offline-Readable Cache of Transactions/Budgets

**Goal:** a user can open the app with no connectivity and see their most
recently synced transactions and budgets — read-only, bounded, and never
presented as more current than it actually is.

**Current state:** Stages 1–2 give the app a working offline *write* path.
Nothing yet lets the user *read* anything from their account without a live
connection.

- [x] **Room schema**: `data/local/CachedTransactionEntity.kt` and
      `CachedBudgetEntity.kt` — a simple list/summary view's worth of fields
      (id, date, description, amount, currency, type, category for
      transactions; id, category, limit, spent, percent, exceeded for
      budgets), not a full replica. `CacheMetadataEntity.kt` records one
      "last successfully refreshed" timestamp per cache. `AppDatabase`
      bumped to version 2 with `fallbackToDestructiveMigration()` — this app
      hasn't shipped, so a real migration path isn't needed yet.
- [x] **Refresh-on-connectivity**: `sync/ReadCacheSyncWorker.kt`, a thin
      `@HiltWorker` wrapping `TransactionsCacheRepository.refresh()` and
      `BudgetsCacheRepository.refresh()`. `sync/ReadCacheSyncScheduler.kt`
      covers both halves of "periodic/triggered" with one worker: triggered
      immediately when `ui/overview/OverviewScreen.kt` opens
      (`OverviewViewModel.init`), and periodically every 30 minutes in the
      background (`AiFinancialApp.onCreate`) so the cache doesn't go stale
      just because the user never opens that screen. Reads
      (`observeTransactions`/`observeBudgets`) come from Room via `Flow`,
      exactly like Stage 1's write path — `OverviewViewModel` never touches
      `TransactionsApi`/`BudgetsApi` directly.
- [x] **Staleness is visible, not hidden**: `OverviewScreen.kt` shows
      "Last updated: &lt;timestamp&gt;" (or "never", before the first
      successful refresh) above both the budgets and transactions sections,
      unconditionally — never only when the list happens to be empty.
- [x] **No stale-write risk**: unchanged from the plan — this stage adds no
      edit path for a cached transaction/budget, so there's genuinely no
      conflict-resolution problem here.
- [x] **Tests**: `TransactionsCacheRepositoryTest` and
      `BudgetsCacheRepositoryTest` (3 cases each: a successful refresh
      atomically replaces the cache and records "last updated"; a failed
      HTTP response leaves the existing cache untouched; a network
      exception leaves it untouched — never a guessed partial write). Both
      DAOs' `replaceAll` default methods are `@Transaction`-scoped
      delete-then-insert, so a failure mid-refresh can't leave Room's `Flow`
      emitting a half-replaced list. Run in CI (`testDebugUnitTest`), not
      this sandbox (still can't build `:app` — see `CLAUDE.md`). **The
      manual offline walkthrough — confirm the cached screens actually
      render correctly in airplane mode after a real refresh — needs a real
      device/emulator and is explicitly pending**, not claimed here.

**Deliverable:** after at least one successful sync while online, a user can
enable airplane mode and still see a correct (if possibly stale, clearly
labeled) view of their transactions and budgets.

**Status: code-complete, CI-green expected, not yet device-verified.**
Backend contracts (`TransactionDto`, `BudgetDto`, `PagedResult<T>`, the
`GET /api/transactions`/`GET /api/budgets` routes and their query params)
were read directly from `AIFinancial`'s actual current `main` branch, not
assumed. The device-level walkthrough above is the same category of
genuinely-pending, human/device-level work as Stage 1 and Stage 2's.
