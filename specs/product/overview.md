# Product Overview — AIFinancial Mobile

## What this is

A native Android companion app for the **AI Financial Intelligence Platform**
(the `AIFinancial` backend/web repo). It does not duplicate that platform — it
gives a user two things the web app structurally cannot: a camera for
document capture, and a device that keeps working when there is no signal.

## Who it's for

The same user who already has a web account on AIFinancial. This app never
creates its own identity system — every session starts from pairing with an
existing account via the QR-code device-pairing flow built in the backend's
Stage 19 (`specs/architecture/mobile-companion-integration.md` in the
`AIFinancial` repo). No password is ever typed on the phone.

## Why offline-first is the actual point of this app

The realistic use case is: a user is out, gets a paper receipt, and wants to
capture it *right then* — not later at a desk with Wi-Fi. If capture requires
a live connection, the app provides no advantage over opening the web app
later, and the camera-first value proposition collapses. So the non-negotiable
design constraint, carried over from the reasoning that shaped this project's
roadmap, is:

> Offline capture-and-queue is the product's core feature, not a resilience
> feature bolted on afterward. Every other capability is secondary to it.

## Scope for the current roadmap

- **In scope:** device pairing, offline-first document capture (photo →
  local queue → background sync via the existing `POST /api/documents`
  endpoint), offline-readable cache of transactions/budgets, basic account
  management (view linked device, unlink), and — as of Stage 5, a deliberate
  scope extension by the repo owner after Stages 1–4 shipped — the AI chat
  assistant, as a thin client of the backend's existing `POST /api/chat`.
  This app never holds, sends, or sees an OpenAI/provider API key of its
  own: that stays entirely on the backend (`IAiProviderResolver`), exactly
  as it already does for the web app. Mobile's role here is unchanged from
  everywhere else in this app — call an authenticated endpoint, render what
  comes back, never a source of truth for anything.
- **Deliberately out of scope:** push notifications, and any UI the web app
  already does better (full analytics/calendar views stay web-only for the
  foreseeable future — this app is not a web-app port).
- **The AI chat assistant is the one deliberate exception to this app's
  offline-first rule**, not a quiet violation of it: a chat message is never
  queued for later like a captured document is. Asking a financial question
  is inherently a live request against current data and a live model call —
  queuing it would mean answering a stale question with data that may no
  longer be current by the time connectivity returns, which is a worse user
  experience than a clear "needs a connection" state right now. See
  `specs/phases/stage-5-ai-chat-assistant.md`.

## Relationship to the `AIFinancial` backend repo

This repo is a pure API consumer. It never shares code, a database, or a CI
pipeline with `AIFinancial` — the only coupling is the documented REST
contract in `AIFinancial`'s `specs/architecture/api-reference.md`. Any change
to that contract that affects mobile must be reflected in this repo's
`specs/architecture/api-integration.md`, by hand, on purpose — there is no
shared types/schema generation between the two repos today.
