---
name: investigate
description: Research a question about this codebase or its spec before writing any code — where something lives, how the offline queue/sync works, what a stage requires, or why something was built a certain way. Use before implement-stage when the ask is ambiguous, or whenever the user asks "how does X work" / "where should Y go" / "what does the spec say about Z".
---

# Investigate

Read-only research. Do not edit files or run anything destructive. The goal is a clear,
sourced answer — not a plan to implement.

## Steps

1. **Check the specs first**, in this order:
   - `specs/product/overview.md` — what the app is for, scope, non-goals
   - `specs/architecture/overview.md` — module layout, the offline write path, dependency
     direction
   - `specs/architecture/technology-choices.md` — why a library/pattern was chosen, and
     the known sandbox constraint around Google Maven resolution
   - `specs/architecture/api-integration.md` — exactly which backend endpoints this app
     calls and how pairing/refresh/revocation are handled
   - `specs/phases/` — whether a stage is done, in progress, or not started, and what
     exactly its checklist says
2. **If the question is about the backend contract itself** (not how this app consumes
   it), the authoritative source is the `AIFinancial` repo's
   `specs/architecture/api-reference.md` — don't guess at backend behavior from this
   repo's notes alone if the real backend repo is available to check.
3. **Then check the actual code** — specs describe intent, the code is ground truth. If
   they disagree, say so explicitly; don't silently trust one over the other.
4. Use `Grep`/`Glob` for targeted lookups; escalate to the `Explore` agent only for
   genuinely broad, multi-location searches.

## Output

Answer the question directly. Cite file paths and line numbers (`path:line`) for every
claim. If the spec doesn't cover the question, say that explicitly rather than guessing
— propose where the answer should be documented (which `specs/` file) instead of
inventing an answer.
