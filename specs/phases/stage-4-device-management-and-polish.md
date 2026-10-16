# Stage 4 — Device Management & Polish

**Goal:** round out the app with the account-management actions a linked
device needs, and tighten the UX of the core flows built in Stages 1–3.

- [ ] **Settings screen**: shows this device's own name/type (as it was
      registered at pairing) and a "last synced" indicator for the whole
      app, not just one screen.
- [ ] **Unlink this device**: calls the backend's existing
      `DELETE /api/devices/{id}` for this device's own id, then clears all
      local tokens and cached data, returning to the pairing screen. Note:
      this app unlinks *itself* — it does not manage other devices (that
      remains the web app's Settings page).
- [ ] **Handle server-initiated revocation gracefully**: if a request fails
      because the device was revoked *from the web app* (not from this
      screen), the resulting forced re-pairing (built in Stage 2's
      Authenticator) shows a clear, specific message — "This device was
      removed from your account" — not a generic network-error message.
- [ ] **Queue-status polish**: clearer visual states for
      queued/uploading/uploaded/failed in the capture list; empty states for
      "no documents captured yet."
- [ ] **Error-state audit**: every network call added in Stages 1–3 gets a
      deliberate, user-legible failure state — no screen silently does
      nothing on error.
- [ ] **Tests**: instrumented Compose UI tests for the settings/unlink flow
      and the revocation-triggered re-pairing path.

**Deliverable:** the app feels finished for its defined scope — not just
functionally complete, but with every error path accounted for rather than
left as a TODO.
