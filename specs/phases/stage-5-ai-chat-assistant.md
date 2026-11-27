# Stage 5 — AI Chat Assistant

**Goal:** a user can ask the AI assistant a question about their own finances
from the phone, the same assistant already live on the web app, without this
app ever holding an AI provider key or making any decision about the answer
itself.

**Why this stage exists:** not in the original roadmap — added after Stages
1–4 shipped, by the repo owner's explicit decision (see
`specs/product/overview.md`'s scope section). It's scoped narrowly on
purpose: a thin client of the backend's existing `POST /api/chat` /
`GET /api/chat/sessions/{id}`, nothing new on the AI side itself.

**Non-negotiable, carried over from `AIFinancial`'s own AI-architecture rule
(`.claude/skills/ai-review/SKILL.md` there): AI must complement deterministic
backend logic, never become the source of truth.** This app has no code path
where that could even happen — it never calls OpenAI/any provider directly,
never has a key, and never interprets or acts on the assistant's answer; it
only displays it.

**Deliberate exception to this app's offline-first rule:** every other
feature here queues a local write and syncs it later. This one doesn't — a
chat message is a live question against live data, not a document capture.
Queuing it and answering later with data that may no longer be current is a
worse experience than a clear "needs a connection" state right now. See
`specs/architecture/api-integration.md`'s "Chat" section for exactly what
the client is, and isn't, allowed to do with this.

- [x] **Send a message**: `data/remote/ChatApi.kt` / `data/repository/ChatRepository.kt`.
      `POST /api/chat` with `{ message, sessionId }` (`sessionId: null` on the
      first message). A 2xx response's `sessionId` is persisted
      (`data/local/ChatSessionStore.kt`, plain `SharedPreferences` — a
      session id is not a credential, unlike the tokens in
      `AuthTokenStore`) and its `message` (the assistant's reply) is
      appended to the UI's message list. The user's own message is appended
      optimistically, before the call completes, exactly like a captured
      document's row exists before the network knows about it — just
      without a Room row behind it, since there's nothing here to recover
      after a process death (a lost in-flight chat message is retyped, not
      silently gone data).
- [x] **Resume the conversation on reopen**: if `ChatSessionStore` already
      has a session id when the Chat screen opens, `GET /api/chat/sessions/{id}`
      loads its message history. A `404` (session genuinely gone) clears
      the stored id and starts fresh rather than wedging the screen in a
      broken state.
- [x] **Every real failure mode is a visible, specific state, never a
      silent no-op** (the same rule Stage 4's error-state audit applied
      everywhere else): `429` → "Too many messages, wait a moment" (the
      backend's own rate-limit message, passed through, not reworded);
      `503` → "The assistant is temporarily unavailable"; any other
      non-2xx or an `IOException` → a generic but real failure message.
      None of these retry automatically — unlike the document queue, there
      is no background worker that should keep re-sending a chat message
      for the user.
- [x] **Unlink clears chat state too**: `SettingsRepository.unlinkDevice()`
      now also clears `ChatSessionStore` — "unlink removes everything
      stored on this phone" already meant Room; it means this too.
- [x] **Reachable from the app**: a fourth bottom-nav tab ("Chat"),
      alongside Capture/Overview/Settings.
- [x] **Tests**: `ChatRepositoryTest` (6 cases: successful send creates/
      continues a session, a 429 surfaces the backend's rate-limit message,
      a 503 surfaces as service-unavailable, another non-2xx is a generic
      failure, an `IOException` is a generic failure, loading history
      returns messages / treats a 404 as "no history yet") and
      `ChatViewModelTest` (optimistic user message appears immediately,
      a successful reply is appended and the session id persisted, a
      failure surfaces as an error without losing the user's own message
      from the list). Run in CI; this sandbox still cannot build or run
      `:app` itself (see `CLAUDE.md`) — confirmed again for this stage
      (no Android SDK, no emulator, no `/dev/kvm`, `dl.google.com` still
      blocked).

**Deliverable:** open the Chat tab, ask a real question about your own
transactions/budgets, get a real answer from the same assistant the web app
uses — and see a clear, specific message instead of a hang or a crash if
the backend rate-limits or the AI call itself fails.

**Status: code-complete, CI-green expected, not yet device-verified.** The
`/api/chat` contract (`SendChatMessageRequest`, `SendChatMessageResultDto`,
`ChatMessageDto`, `ChatSessionDetailDto`, the `429`/`503` behavior) was read
directly from `AIFinancial`'s actual current `main` branch
(`ChatController.cs` and its DTOs/handler), not assumed. Device-level
verification — does a real question round-trip correctly on a real
device/emulator — is the same genuinely-pending category as every other
stage's; see the manual test plan.
