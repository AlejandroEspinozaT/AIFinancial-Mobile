# Stage 2 — Background Sync

**Goal:** every `QUEUED` document from Stage 1 actually reaches the backend,
automatically, whenever connectivity allows — without the user having to
open the app or tap anything, and without ever double-uploading a document
because a previous attempt's result was ambiguous.

**Current state (audited, not assumed):** Stage 1 leaves documents captured
and durably queued, but nothing sends them anywhere. This stage is the
other half of the mechanism described in `specs/architecture/overview.md`'s
offline write path.

- [x] **Token refresh**: `auth/TokenAuthenticator.kt` implements the OkHttp
      `Authenticator` described in `api-integration.md` — a `401` triggers
      one refresh attempt via `POST /api/auth/refresh` (through a dedicated,
      non-authenticated `Retrofit`/`OkHttpClient` pair — see
      `di/Qualifiers.kt` — so the refresh call itself can never recurse back
      into this same authenticator), persists the rotated token pair
      (`AuthTokenStore.updateTokens`), and retries the original request once.
      A rejected refresh clears local tokens (`AuthTokenStore.clear`) and
      emits through `auth/SessionExpiryNotifier.kt`, which `ui/AppNavHost.kt`
      collects to force navigation back to the pairing screen from whatever
      screen is open. Verified against real backend contracts read directly
      from `AIFinancial`'s current `main` (`AuthController.Refresh`,
      `RefreshTokenRequest`, `AuthResultDto`), not assumed from the spec doc
      alone — that doc undersold what the backend already exposes (see Docs
      below).
- [x] **Sync worker**: `sync/DocumentSyncWorker.kt`, a `@HiltWorker`
      `CoroutineWorker` that is a thin wrapper around
      `DocumentSyncRepository.drainQueue()` — all decision logic lives in the
      repository, none in the worker, mirroring the backend's own
      controllers-stay-thin rule. `sync/SyncScheduler.kt` enqueues it as
      network-constrained unique work (`enqueueUniqueWork` +
      `ExistingWorkPolicy.KEEP`) from three places: right after a successful
      capture, after a manual retry, and once at app startup
      (`AiFinancialApp.onCreate`) to catch anything left over from a
      previous process. This single mechanism covers both triggers the
      checklist originally asked for as if they needed separate code: if
      connectivity is already present it runs immediately; if not, the
      enqueued request (which WorkManager persists across process death)
      simply waits until WorkManager's own constraint tracking sees
      connectivity return — no separate connectivity listener needed.
- [x] **Queue draining logic**: `data/repository/DocumentSyncRepository.kt`.
      For each `QUEUED` row: a 2xx response (200 *or* 202 — the backend
      returns 200 for its own duplicate-hash short-circuit, 202 for a fresh
      upload; both are `Uploaded` here) → `UPLOADED` with the server's
      document id; a 4xx → a definite `Rejected` (`FAILED`, user-retryable);
      a 5xx, an `IOException`, or a 2xx with no parseable body → never
      guessed as success — `AmbiguousNetworkFailure`, leaves the row
      `QUEUED` for the next pass. The worker returns `Result.retry()`
      whenever any row is still `QUEUED` after a pass, so a transient 5xx
      gets WorkManager's own backoff retry without waiting for a fresh
      connectivity transition.
- [x] **Idempotency**: every upload attempt sends
      `Idempotency-Key: <PendingDocument.localId>` (`DocumentsApi.upload`).
      This turned out to be a *more* precise mechanism than the spec doc
      assumed was the only option: reading the real backend source
      (`Api/Filters/IdempotencyActionFilter.cs`) during this stage's
      implementation found that `POST /api/documents` already has a
      purpose-built, opt-in idempotency-key cache (24h TTL, scoped per user
      + route + key) specifically for this "client retried after an
      ambiguous result" case — a retried request with the same key replays
      the original response instead of re-running the upload. The backend's
      byte-hash dedupe (`UploadDocumentCommandHandler.GetByHashAsync`) is
      still there as a second, independent backstop, exactly as this doc
      originally said — it's just no longer this app's *primary* mechanism.
- [x] **Manual retry**: already wired in Stage 1's `CaptureScreen`/
      `CaptureViewModel.onRetry` — Stage 2 adds the missing half: a retry now
      also calls `SyncScheduler.scheduleSync()`, so the re-enqueued row is
      actually attempted again promptly instead of waiting for an unrelated
      trigger.
- [x] **Tests**: `DocumentSyncRepositoryTest` (8 cases: success incl. the
      Idempotency-Key header, a 4xx rejection, a 5xx and an `IOException`
      both treated as ambiguous — never guessed — a missing local file, and
      `drainQueue`'s persistence + retry-signal behavior) and
      `TokenAuthenticatorTest` (4 cases: successful refresh retries with the
      new token, a rejected refresh clears tokens and notifies expiry, no
      stored refresh token means no attempt, and the one-retry-only guard).
      Both run in CI (`testDebugUnitTest`), not this sandbox, which still
      cannot build `:app` at all (see `CLAUDE.md`). `offline-review`'s
      "accumulate several queued items offline, then restore connectivity"
      scenario is **not yet re-run on a real device/emulator** — this
      remains a genuinely pending, human/device-level item (see Docs below),
      not something this sandbox can do or claim.

**Deliverable:** capture several documents in airplane mode, restore
connectivity, and watch all of them transition from `QUEUED` to `UPLOADED`
without opening the app — and confirm in the AIFinancial web app that they
actually arrived, exactly once each.

**Status: code-complete, CI-green, not yet device-verified.** Everything
this sandbox can check (compiles, lints, unit tests — including the
queue-draining and token-refresh logic against fakes) is verified in CI on
this stage's PR. The actual end-to-end device scenario this stage exists to
prove — airplane mode, capture several, restore connectivity, watch them
all become `UPLOADED` with no duplicates against the real backend — needs a
real device/emulator and is **explicitly pending**, the same category of
work (not this sandbox's to do or to claim done) that PRs #2–#4 covered for
Stage 1.
