# 0111. Mandatory multi-factor authentication

## Status
Accepted

## Context
ADR-0110 explicitly deferred MFA/passkeys as future work. The user asked
for it now, ahead of self-service password recovery, specifically because
a recovery-code mechanism issued at MFA enrollment can double as the
password-recovery credential — avoiding the need to stand up email
infrastructure just for "forgot password." Requirements set going in:
every account, new or existing, must have an active second factor before
it can do anything beyond completing enrollment (no opt-out); both TOTP
and WebAuthn/passkeys are offered, chosen once at enrollment; recovery
codes serve both a lost-second-factor and a forgotten-password path. TOTP
ships in this pass; WebAuthn is a deliberate fast-follow (see
Consequences).

Before finalizing the design, Spring Security 7.1 (already on this
project's classpath) was checked for native support, specifically to
avoid hand-rolling infrastructure it already ships — see Decision.

## Decision

### Where MFA state is disclosed
The obvious-looking approach — return a QR code directly from
`POST /accounts/register` — would break ADR-0110's anti-enumeration
guarantee: a real QR/secret in the response would let an attacker
distinguish "new account" from "duplicate email" by response shape alone.
Instead, `POST /accounts/register` is unchanged (still just creates the
`Account` row with a new `mfa_method = NONE`, same generic response either
way), and `POST /api/auth/login`'s existing password-check boundary —
which already has the anti-enumeration properties this needs — is the only
place MFA state is ever revealed, and only after a correct password. Login
never issues a directly-usable token: it returns a `LoginResponse` with a
`status` (`MFA_SETUP_REQUIRED` or `MFA_CHALLENGE_REQUIRED`) and a token
carrying only the `PASSWORD` authentication factor. This one mechanism
covers a brand-new registration, one of the pre-MFA accounts being forced
through enrollment, and a normal returning user's challenge, identically.

### Authorization: Spring Security's own multi-factor primitives
Spring Security 7 added `FactorGrantedAuthority` (progressively granted per
completed authentication factor) and `AuthorizationManagerFactories
.multiFactor()` (an `AuthorizationManager` requiring a specific set of
factor authorities) — but the batteries-included
`@EnableMultiFactorAuthentication` annotation is built around chained
`formLogin()`/`oneTimeTokenLogin()`/`webAuthn()` logins with automatic
redirects, which doesn't fit a stateless JSON API. This app uses the
primitives without the annotation: `JwtService` embeds a `factors` claim
(`["PASSWORD"]` or `["PASSWORD","MFA"]`) in every token; `JwtAuthFilter`
turns each entry into a real `FactorGrantedAuthority` (`PASSWORD_AUTHORITY`,
or a custom `withFactor("MFA")` — granted the same way regardless of
whether TOTP or WebAuthn actually proved it, so one authorization rule
covers both methods) alongside the existing `ROLE_*` authority — exactly
the same manual-authority-granting pattern `JwtAuthFilter` already used for
roles. `SecurityConfig` builds one `AuthorizationManagerFactories
.multiFactor().requireFactors(PASSWORD_AUTHORITY, MFA_AUTHORITY)` manager
and applies it to `/api/**` (composed with `WorldAccessAuthorizationManager`
via `AuthorizationManagers.allOf(...)` on the world-scoped matcher, since
`authorizeHttpRequests` only evaluates the first matching rule for a given
path); `/api/auth/mfa/**` is the one exception, needing only the `PASSWORD`
factor, since completing the `MFA` factor is exactly what those endpoints
are for. A denial specifically due to a missing `MFA` factor gets its own
403 ("MFA required") from a dedicated branch in the existing routed
`AccessDeniedHandler`, distinct from the 404 ownership-mismatch and 403
role-mismatch cases already there — otherwise it would silently fall into
the 404 case, which is misleading (the resource exists; the caller isn't
fully authenticated yet).

### TOTP
No Spring Security equivalent exists (its "OTT" login is an emailed
one-time link, not TOTP), so this is a conventional implementation via
`dev.samstevens.totp`: `MfaService` (new, same `accounts` bounded context,
kept separate from `AccountService` to avoid growing that class further)
owns setup (`/auth/mfa/setup/totp/start` generates a secret held pending
until confirmed, `/confirm` verifies a code and activates it), challenge
(`/auth/mfa/verify`), and recovery-code consumption
(`/auth/mfa/verify-recovery-code`, `/auth/recover-password`). SHA1/6-digit/
30-second codes — not the library's strongest option, but the one every
mainstream authenticator app actually implements; a "stronger" hash would
silently fail to scan or verify in most of them. The secret is encrypted
at rest via `spring-security-crypto`'s `Encryptors.delux(key, salt)`
(AES-256-GCM, PBKDF2 key derivation, already transitively on the classpath
— not hand-written AES-GCM, and not `Encryptors.stronger(...)`, which
returns the lower-level `BytesEncryptor` this wraps rather than a
`TextEncryptor` fit for a VARCHAR column) — a TOTP secret is directly
usable to generate valid codes, so it needs real encryption, not hashing
like a password. QR codes are rendered server-side to a
`data:image/png;base64,...` URI (via the same library's bundled
`ZxingPngQrGenerator`), so the frontend needs no client-side QR library.

### Recovery codes
10 single-use codes, generated at the end of every successful enrollment
(fresh or re-enrollment), shown exactly once, stored BCrypt-hashed via the
existing `PasswordEncoder` bean. Two consumption paths: losing the second
factor (`verify-recovery-code`, called with a `PASSWORD`-only token)
consumes a code and resets `mfa_method` back to unset rather than granting
access — the old device may be gone for good, so re-enrollment is forced
through the same setup path as everything else, no new machinery needed.
Forgetting the password (`recover-password`, no token at all) consumes a
code, resets the password, and does *not* touch `mfa_method` — if the
authenticator is still available, the next login challenges normally.
`recover-password` mirrors `AccountService.authenticate`'s anti-enumeration
handling: an unknown email still runs a bounded number of dummy BCrypt
comparisons before returning the same generic response.

### Migrating the two pre-MFA accounts
`accounts.mfa_method` defaults to `NONE` (migration `V62`). No special
backfill logic: the login flow's `MFA_SETUP_REQUIRED` branch already
handles "no active method" identically whether the account is brand new or
predates this feature — the two existing accounts
(`shohnke@protonmail.com`, `shohnke2@protonmail.com`) are simply forced
through enrollment the next time they log in.

## Consequences
- Every login now needs a second request (at minimum) before the caller
  has a usable token — a real UX cost, accepted as the point of "no
  opt-out" MFA.
- The `PASSWORD`-only token's shorter TTL (10 minutes, vs. the existing
  24-hour default for a fully-authenticated token) is a new `JwtService`
  concept; `JwtAuthFilter`/`SecurityConfig` needed no other new
  infrastructure beyond the `factors` claim and the one authorization rule
  — everything else reuses what ADR-0110 already built (revocation via
  `token_version`, the routed `AccessDeniedHandler`, `CurrentUserPort`).
- **WebAuthn is a deliberate fast-follow, not deferred indefinitely like
  ADR-0110 originally treated it.** Spring Security's own `.webAuthn()` DSL
  is REST/JSON-friendly (fixed `/webauthn/register/options`,
  `/webauthn/register`, `/webauthn/authenticate/options`, `/login/webauthn`
  endpoints speaking the exact JSON shapes `navigator.credentials` needs)
  and supports a stateless deployment since Spring Security 6.5 via a
  pluggable `PublicKeyCredentialCreationOptionsRepository` instead of the
  session-backed default — the plan is to use that directly rather than
  calling `webauthn4j-core` by hand, since Spring's implementation already
  wraps it and doing the COSE-key/signature/origin verification in
  application code would be new, security-critical code this app doesn't
  need to own. The one real wrinkle: Spring's WebAuthn endpoints expect a
  CSRF token, and this app disables CSRF globally as a pure bearer-token
  API — the plan is a `CookieCsrfTokenRepository` (stateless double-submit
  cookie) scoped narrowly to just `/webauthn/**` and `/login/webauthn`, not
  the rest of the API. Not built in this pass; tracked as the immediate
  next slice of this same initiative, not a separate future ADR.
- **First-mover enrollment race, and its mitigation.** Enrollment
  (`/auth/mfa/setup/totp/start` + `/confirm`) requires nothing beyond a
  `PASSWORD`-factor token — i.e. nothing beyond the current password. For an
  account with no MFA active yet, whoever completes enrollment first becomes
  the sole holder of its second factor and its one-time-shown recovery
  codes; a leaked password (credential stuffing, phishing, reuse) no longer
  buys an attacker only a revocable session as it did before this ADR
  (`Account.changePasswordHash` bumps `token_version`, evicting them) — it
  can instead buy *persistent* control, since a subsequent password reset
  doesn't touch `mfaMethod`. Found via this feature's own `security-review`
  pass and confirmed as a genuine, deterministic gap (not a narrow timing
  window — exploitable for the account's entire un-enrolled lifetime), not
  a pre-existing issue. **Mitigated, not eliminated**: `POST
  /accounts/{accountId}/reset-mfa` (admin only) clears `mfaMethod`/the TOTP
  secret *and* deletes every outstanding recovery code (not just the method
  flag — a recovery code the wrong person holds must stop working
  immediately, or it could still be spent via `recoverPassword`/
  `verifyRecoveryCode` after an admin believes they've fixed the account)
  and bumps `token_version`, forcing clean re-enrollment. This makes an
  unwanted enrollment *recoverable* once an admin notices, closing off what
  was otherwise permanent, irrevocable-by-normal-means account takeover.
  It does not prevent the initial race — this app deliberately has no
  out-of-band channel (no email/SMS infrastructure, per ADR-0110) to prove
  "the password-holder is really the owner" any more strongly than the
  password itself, so a fully preventive fix isn't available without adding
  infrastructure this project has twice now deliberately chosen not to
  build. Accepted as proportionate for this app's actual deployment (a
  personal instance with a small, known set of accounts the deployer
  controls) — revisit if that scope ever changes.

## Alternatives considered
- **A structurally separate "setup/challenge" JWT type, verified outside
  `JwtAuthFilter` entirely** — an earlier draft of this design. Rejected in
  favor of the `factors`-claim approach once Spring Security 7's native
  `FactorGrantedAuthority`/`AuthorizationManagerFactories.multiFactor()`
  primitives were found: reusing them keeps token handling inside the
  normal filter chain and Spring's own tested authorization model, instead
  of a bespoke parallel mechanism.
- **Calling `webauthn4j-core` directly** for WebAuthn, bypassing Spring
  Security's own `.webAuthn()` DSL — considered and rejected once the DSL
  was confirmed to support both a stateless deployment and a custom SPA
  frontend; see Consequences.
- **Hand-written AES-GCM** for TOTP secret encryption — rejected in favor
  of `spring-security-crypto`'s `Encryptors.delux(...)`, already on the
  classpath and the documented standard way to do this in a Spring app.
