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
  management (view linked device, unlink).
- **Deliberately out of scope for now:** the AI chat assistant (it is
  inherently online-only — OpenAI-backed — and adds no offline-design
  tension worth solving first), push notifications, and any UI the web app
  already does better (full analytics/calendar views stay web-only for the
  foreseeable future — this app is not a web-app port).

## Relationship to the `AIFinancial` backend repo

This repo is a pure API consumer. It never shares code, a database, or a CI
pipeline with `AIFinancial` — the only coupling is the documented REST
contract in `AIFinancial`'s `specs/architecture/api-reference.md`. Any change
to that contract that affects mobile must be reflected in this repo's
`specs/architecture/api-integration.md`, by hand, on purpose — there is no
shared types/schema generation between the two repos today.
