# 0112. Self-service session/device tracking

## Status
Accepted

## Context
ADR-0110 deliberately kept this app's JWT auth stateless beyond one thing:
`accounts.token_version`, bumped by every security-relevant mutator
(password change, role change, disable, MFA reset/re-enrollment,
`logout-all`) to invalidate *every* outstanding token for an account at
once. That's the only revocation mechanism today — a user who suspects one
device is compromised has no way to log out just that device without also
logging out everywhere else, and no visibility into which devices even hold
a live token in the first place.

This was explicitly scoped out of PR #90 (self-service recovery-code/TOTP
re-enrollment) as "needs its own ADR" during an audit of what's still open
for authentication/authorization, alongside SSO/OIDC and Postgres
row-level security (both queued separately, not part of this ADR).

## Decision

### A second, additive revocation layer — not a replacement
`token_version` keeps working exactly as before for every account-wide
case. This ADR adds a **second, finer-grained** layer alongside it: a new
`account_sessions` table, one row per *full* (PASSWORD+MFA) token ever
issued, keyed by that token's own `jti` claim (new — tokens previously
carried no per-token identity, only the coarse `sub`/`ver` pair).
`JwtAuthFilter` now checks both: the existing `tokenVersion` equality check
first (unchanged), then — only for a token that carries the MFA factor —
that its `jti` has an active (`revoked_at IS NULL`, not yet `expires_at`)
row in `account_sessions`. A PASSWORD-only pending-MFA token (10-minute
TTL, mid-login) is deliberately never recorded and never checked here —
it isn't a "device logged in" yet, and forcing every login attempt through
an extra table it doesn't need would add cost for no benefit.

### Why not replace `token_version` entirely
A per-session table alone could theoretically replace `token_version` (revoke
by deleting rows instead), but that would mean an account-wide action like
`logout-all` or a password change would need to revoke every row
individually instead of one integer compare — more write amplification for
the *common* case (mass revocation) to make the *rare* case (revoke one
device) cheap. Keeping both, each doing what it's naturally suited for, costs
one extra indexed lookup per authenticated request beyond what ADR-0110
already accepted for `token_version` itself — the same "small, deliberate
move away from pure statelessness in exchange for actual revocability"
tradeoff, extended one step further.

### Recording a session
A session row is created at the exact moment a *full* token is minted —
every `jwtService.issue(accountId, role, tokenVersion)` (3-arg, always-full)
call site: TOTP/WebAuthn enrollment confirm, TOTP challenge verify, TOTP
re-enrollment confirm, and the WebAuthn login success handler. `JwtService`
itself stays a pure, side-effect-free token minter (verified by its own
still-fully-offline `JwtServiceTest`) — recording is a separate call each
of those five sites makes afterward via `RecordAccountSessionUseCase`, not
baked into token issuance itself. Missing a future call site here simply
means that flow's tokens are never checked against `account_sessions` — the
existing `tokenVersion` check still fully protects it, so this fails safe
rather than silently locking users out.

### Where the current request's own session id comes from
`JwtAuthFilter` already decodes a token's claims into an `Authentication`;
its `jti` is stashed in the `Authentication`'s `details` field (previously
holding an unused `WebAuthenticationDetails` — confirmed nothing in this
codebase ever read it) rather than changing the `principal` type everywhere
that already assumes a bare `UUID` (`WorldPermissionEvaluator` and others).
`CurrentUserPort` gains `currentSessionId()` alongside the existing
`currentAccountId()`/`currentRole()`, read the same way.

### Self-service endpoints
`GET /api/accounts/me/sessions` (list, oldest first, each entry flagged
`current` by comparing against `currentSessionId()`) and
`DELETE /api/accounts/me/sessions/{id}` (revoke — scoped to the caller's own
account, same "row id from one account can never touch another's" pattern
as every other self-service `/me/**` endpoint). Both land in
`AccountAdminController` alongside the existing WebAuthn-credential and
recovery-code self-service routes, inheriting the same full-MFA-factor gate
via the `/api/**` catch-all with no `SecurityConfig` change needed.
Revoking the caller's own *current* session is allowed, not specially
blocked — it's a legitimate "log out this device," and the frontend routes
that case through the same sign-out flow as an expired token.

## Consequences
- One extra indexed query per authenticated request carrying the MFA
  factor — accepted for the same reason ADR-0110 accepted the first one.
- `account_sessions` rows for an account that later goes through
  `logout-all` or another `token_version`-bumping mutator are **not**
  proactively revoked or deleted — they're already unusable (the
  `tokenVersion` check fails first, independent of this table), and they
  age out naturally via `expires_at`. Listing sessions right after a mass
  revocation may briefly show stale-but-already-dead rows; not worth the
  extra write to tidy up for list-view accuracy alone at this app's scale.
- No proactive cleanup job for expired rows — the list endpoint already
  filters them out, so an unbounded but slow-growing table is an accepted
  tradeoff for a personal-scale deployment; revisit if that changes.

## Alternatives considered
- **Replacing `token_version` with per-session revocation entirely** —
  rejected; see Decision above (worse for the common case).
- **Updating `last_seen_at` on every request** — rejected as an unnecessary
  DB write per request at this app's scale; `created_at` plus the
  device/IP captured once at issuance is enough for "what's logged in."
- **A brand-new `SecurityContext` principal type carrying both accountId
  and sessionId** — rejected in favor of reusing the `details` field, which
  was already present, unused, and requires touching zero of the several
  existing call sites that assume the principal itself is a bare `UUID`.
