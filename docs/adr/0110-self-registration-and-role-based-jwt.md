# 0110. Self-registration and role-based JWT

## Status
Accepted — supersedes ADR-0006

## Context
ADR-0006 authenticated the whole app against one configured `APP_PASSWORD`,
issuing a stateless JWT with a hardcoded `"owner"` subject and no role
claim at all — there was no user table, so no way to express "which
account" or "what rights." ADR-0109 requires real per-account identity to
scope ownership against; this ADR covers how that identity is created,
authenticated, and carried in the token.

The account entity that falls out of this — email/password credentials,
roles, an enabled/disabled lifecycle, brute-force tracking — has real
invariants and its own persistence, which makes it a bounded context by
this project's own definition, not the "thin auth infra" `auth` was before
(a one-line password compare with no persistence). It gets full hexagonal
rings like every other context.

Because registration is now public and reachable from the open internet
(not a trusted LAN, unlike the original single-owner deployment), several
decisions below are explicitly about that exposure, not just "add a users
table."

## Decision
**New bounded context `accounts`**, mirroring `worldbuilding`'s shape
exactly (`domain`/`application`/`adapter` rings; `AuthenticateAccountPort`
and `AccountQueryPort` are its only published surface — the password hash
never leaves the context). Added to both `CLAUDE.md`'s bounded-context list
and `ArchitectureTest.CONTEXTS` so the ArchUnit fitness function enforces
its boundary like every other context. `auth`/`security`/`config` remain
generic infra, but now correctly: they *compose* `accounts`' published
ports instead of containing the domain logic themselves.

- **Public self-registration** (`POST /api/accounts/register`), BCrypt
  password hashing (`spring-boot-starter-security` already pulls in
  `spring-security-crypto` — no new dependency). The **first account ever
  registered on the instance is automatically promoted to ADMIN**; every
  account after that defaults to USER, matching a self-hosted deploy's
  natural bootstrap (the deployer registers first) without a separate
  admin-bootstrap secret.
- **Exactly two roles, ADMIN and USER** — no GM/player content-visibility
  roles; out of scope for this pass; see ADR-0109 for what ADMIN actually
  grants (roster management, never another account's content).
- **JWT carries `sub` (account id) and a `role` claim**, replacing the
  hardcoded `"owner"`/`ROLE_OWNER` pair `JwtAuthFilter` used to synthesize
  regardless of the token's contents.
- **Anti-enumeration by construction** (OWASP ASVS 2.1/2.2): neither
  registration nor login may reveal which emails have accounts.
  - `POST /accounts/register` returns the identical `202` +
    `RegistrationAccepted{message}` body whether the email was free or
    already taken; on a duplicate it still runs
    `passwordEncoder.encode(...)` on the submitted password and discards
    the result, so a duplicate isn't measurably faster than a real
    registration. No `Account`/token is ever returned from this endpoint,
    and the client never auto-authenticates from it — a real new account
    would get a working token, a duplicate couldn't, and that difference
    would itself be a leak.
  - `AuthenticateAccountPort.authenticate` always calls
    `PasswordEncoder.matches` — against the real hash when the account
    exists, against a fixed precomputed dummy BCrypt hash when it doesn't —
    before returning empty, so "no such account," "wrong password," and
    "disabled account" all take comparable time and all surface as the
    same generic 401 from `AuthController`.
  - Basic brute-force throttling: `failed_attempts`/`locked_until` columns
    back a short per-account lockout after repeated bad logins (locked-out
    attempts get the same generic 401, not a distinguishing message), plus
    a per-IP rate limiter in front of the unauthenticated endpoints that do
    password-hash-cost work on guessable-secret input (`/auth/login`,
    `/accounts/register`, and — added later, closing an oversight relative
    to this same rationale — `/auth/recover-password`) — the per-account
    lockout alone doesn't stop someone sweeping many emails or forcing
    repeated BCrypt hashing as a CPU-cost denial-of-service vector now that
    this is internet-reachable.
- **JWT revocation via `token_version`.** A pure stateless JWT has no way
  to invalidate a token early; with the default 30-day expiry this app
  shipped with (fine for one trusted owner, not for public multi-account
  exposure), disabling an account or changing its role would otherwise do
  nothing to a token it already holds until that token's natural expiry.
  `accounts.token_version` is embedded as a `ver` claim and bumped on
  password change (self or admin reset), role change, disable, and an
  explicit self-service "log out everywhere"
  (`POST /accounts/me/logout-all`); `JwtAuthFilter` reads the account back
  via `AccountQueryPort` on every request and treats a `ver` mismatch,
  disabled account, or missing account identically to an invalid token —
  a deliberate, small move away from pure statelessness in exchange for
  actual revocability. The default `expiration-hours` also drops from 720
  (30 days) to 24, bounding the blast radius of a token that leaks but
  hasn't been explicitly revoked.
- **Password minimum length** (12 characters) enforced server-side on
  registration, admin reset, and self-service change — previously nothing
  stopped a one-character password.
- **No forced-password-change flow, no self-service reset-by-email.** This
  app has no email-sending infrastructure at all; an admin sets a new
  password directly for another account, and a user changes their own via
  `PATCH /accounts/me/password` (current + new). Keeps this pass minimal
  rather than building a reset-link flow the app can't otherwise support.
- **CORS/security headers are unchanged, deliberately.** No
  `CorsConfigurationSource` bean exists — `SpaWebConfig`'s Javadoc confirms
  the combined image serves the SPA same-origin (ADR-0059), so
  cross-origin requests are simply refused by the browser as a safe
  default. Spring Security's own default response headers stay on. TLS
  termination is assumed to happen in front of the app (reverse proxy),
  same as today — an infra/deployment concern, not a code change here, but
  worth stating explicitly now that real credentials are on the line.

## Consequences
- Every authenticated request now costs one extra `AccountQueryPort`
  lookup (for the `token_version`/enabled check) beyond signature/expiry
  verification — a deliberate, small statelessness trade for revocability,
  acceptable at this app's scale.
- SSO/OIDC login, WebAuthn passkeys, and TOTP MFA are **explicitly
  deferred**, not rejected. Spring Boot 4.1 (already in use here) ships
  Spring Security 7, which has first-class support for both passkeys
  (WebAuthn4J-backed, JDBC credential persistence available out of the
  box) and a native MFA framework (`@EnableMultiFactorAuthentication`,
  though TOTP itself isn't a built-in factor and would need a small custom
  one). Both are built around session/cookie web flows and would need a
  custom bridge to this app's stateless JWT bearer API to mint a token
  after the ceremony completes — real, separate follow-up work, each
  warranting its own ADR when undertaken.
- No refresh-token flow was introduced. A shorter default expiry plus
  `token_version`-based revocation covers the two risks that mattered here
  (a leaked token, an admin action that should take effect immediately)
  without the added complexity of a second token type and client-side
  refresh logic.

## Alternatives considered
- **Admin-invite-only registration** — rejected per explicit product
  decision: this instance is meant to support open self-registration, not
  a closed roster seeded by an administrator.
- **HIBP (Have I Been Pwned) breach-list checking on registration** — a
  reasonable further hardening step, but it calls out to a third party at
  registration time and wasn't judged worth the privacy trade-off for this
  pass; left as a noted future option.
- **A bootstrap-admin environment variable** (`APP_ADMIN_EMAIL`/
  `APP_ADMIN_PASSWORD`) to seed the first account and backfill existing
  data — rejected in favor of "first registrant becomes ADMIN," which
  needs no new secret and matches how a self-hosted deploy actually gets
  used (the deployer registers first).
