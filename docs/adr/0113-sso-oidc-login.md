# 0113. SSO/OIDC login (Google)

## Status
Accepted

## Context
ADR-0110 explicitly deferred SSO/OIDC as "real, separate follow-up work...
warranting its own ADR." This is the third and final item from an audit of
"what's still open for authentication and authorization" that also produced
PR #90 (recovery-code/TOTP self-service) and PR #91 (session/device
tracking, ADR-0112).

Decided going in: **Google only** — no generic multi-provider config
surface, matching this app's personal-scale deployment model; **no
auto-linking by email** — a Google login whose email matches an existing
password account is rejected with a clear error, never silently merged;
**local MFA stays mandatory** even for a Google-authenticated account,
keeping ADR-0111's "every account has TOTP or WebAuthn, no exceptions"
invariant intact rather than treating Google's own sign-in as a substitute
second factor.

## Decision

### Google's `ClientRegistration` is built by hand, not via Spring Boot's own autoconfiguration
The obvious approach — `spring.security.oauth2.client.registration.google.*`
properties, letting `spring-boot-starter-oauth2-client`'s autoconfiguration
do the rest via its built-in Google preset — was tried first and rejected
once it broke the app. Spring Boot's `OAuth2ClientPropertiesMapper`
validates every configured registration **eagerly, at bean-creation time**:
even with a blank `client-id`, having *any* properties present under that
prefix makes it throw `IllegalStateException("Client id of registration
'google' must not be empty")` — which fails the whole application context,
breaking every request in the app, not just Google sign-in. This is the
opposite of the "optional, silently skipped" behavior every other external
credential in this app already has (the AI provider keys, ADR-0064).

Fixed by not using that config namespace at all: `SecurityConfig` builds the
`ClientRegistration` itself, in code, only inside the `if
(properties.oidc().googleEnabled())` branch — so it's never even attempted
with a blank client id. Google's OIDC endpoints (authorization, token,
user-info, jwk-set, issuer) are hardcoded rather than fetched via
`ClientRegistrations.fromIssuerLocation(...)`'s live discovery call — they're
stable and publicly documented, and avoiding a network dependency at every
app startup was judged worth not being self-updating if Google ever changes
them (unlikely; they haven't in years). `AppProperties` gained a matching
`Oidc(googleClientId, googleClientSecret)` record, following the exact same
`app.*`-prefixed, env-var-backed, blank-tolerant shape as `Ai`/`Webauthn`/
`Mfa`.

### `.oauth2Login()` registered conditionally
`SecurityConfig`'s `filterChain` bean only calls `http.oauth2Login(...)` when
`properties.oidc().googleEnabled()` — a deployment with no Google
credentials gets no `/oauth2/authorization/google` at all, no unwired
filter, nothing that could fail at request time. `/oauth2/**` and
`/login/oauth2/**` are always in the permitAll matcher list regardless (both
are plain GET browser navigations, harmless to permit when nothing's
listening); when disabled they simply 404 through Spring MVC's normal
no-handler-found path.

No CSRF matcher changes were needed here, unlike WebAuthn's ceremony
endpoints (ADR-0111): `CsrfFilter` only protects unsafe methods by default,
and both OIDC endpoints are GET. The OAuth `state` parameter is what
protects the callback instead.

### Stateless `AuthorizationRequestRepository`, keyed by `state`
The DSL's default, `HttpSessionOAuth2AuthorizationRequestRepository`, needs
an `HttpSession` this app doesn't have (`SessionCreationPolicy.STATELESS`).
`OidcAuthorizationRequestRepositoryAdapter` replaces it with a new
`oidc_authorization_requests` table, directly mirroring the
`webauthn_challenges` pattern (ADR-0111) — except keyed by the OAuth `state`
parameter rather than `account_id`: every WebAuthn ceremony already has a
PASSWORD-factor token identifying an account before it starts, but an
*initial* Google login has no account or token yet, so `state` is the only
available correlation key. Serialization uses Spring Security's own
`OAuth2ClientJackson2Module` — which alone isn't sufficient:
`OAuth2AuthorizationRequest.getScopes()` is a `java.util.
Collections$UnmodifiableSet` at runtime, and Spring Security's Jackson
allowlist rejects any concrete type it has no mixin for unless
`SecurityJackson2Modules.getModules(...)` (the module providing exactly
those JDK-collection mixins) is registered too.

### The redirect handoff: a single-use exchange code, never a token in the URL
`oauth2Login()` ends in a full-page browser redirect, not an XHR — unlike
`WebAuthnAuthenticationSuccessHandler` (ADR-0111 follow-up), which can write
a `TokenResponse` directly onto the response body because WebAuthn's
ceremony is XHR-driven, `OidcAuthenticationSuccessHandler` has no response
body the browser will ever read. Putting the bearer token in the redirect
URL itself (query param or fragment) was rejected — it would land in
browser history and, for a query param, server access logs and the Referer
header.

Instead, the success handler builds the same `LoginResponse` shape
`AuthController.login()` returns (PASSWORD-factor-only token; `status` set
to `MFA_SETUP_REQUIRED` or `MFA_CHALLENGE_REQUIRED` from the account's
`mfaMethod()` exactly as password login does — this is precisely where
"Google sign-in still only proves the first factor" is enforced), stages
its JSON behind a random code in a new `oidc_login_exchanges` table (2-minute
TTL, deleted the moment it's redeemed — single-use), and redirects the
browser to `/?code=...`. The frontend's `App.tsx` picks up `code` on mount,
calls the new public `POST /api/auth/oidc/exchange` to redeem it, and feeds
the resulting `LoginResponse` into the exact same `handleLoginResult`
routing a password login already uses — no new state machine, just a new
way to produce a shape that already existed. The redirect is always
relative (`/?code=...`, never an absolute URL) — safe because this app is
always same-origin (ADR-0059's combined image), and it means no new
frontend-base-URL config was needed anywhere.

`oidcUser.getEmailVerified()` is checked before ever resolving/creating an
account — a false value fails the same way as any other OIDC error
(redirects to `/?oidcError=...`), since an unverified email can't be
trusted for account matching or creation.

### Account resolution: new columns, not a new identity table
`Account.applyPasswordHash` used to unconditionally reject a null/blank
hash. Replaced with `applyIdentity(passwordHash, authProvider,
externalSubject)`, enforcing an XOR: exactly one of a local password or an
external `(provider, subject)` pair, never both, never neither. New
`Account.createViaOidc(...)` factory mirrors `create(...)` with
`passwordHash = null`. Two new nullable columns on `accounts` itself
(`auth_provider`, `external_subject`, unique together where not null) rather
than a separate `account_identities`-style join table — with only one
provider and the simplifying "one identity per account, chosen once" rule
(same shape as MFA method selection, ADR-0111), a second table would add
join overhead for no real flexibility this app needs yet. The same XOR
invariant is duplicated as a DB `CHECK` constraint
(`chk_accounts_identity`) — defense-in-depth against a future direct-SQL
mistake bypassing the domain layer entirely.

New `AuthenticateOrRegisterViaOidcUseCase`, implemented by a new
`OidcAccountService` (kept separate from `AccountService`, the same
reasoning ADR-0111 gave for splitting out `MfaService` — this is a distinct
identity concern, not password-account CRUD): look up by `(provider,
subject)` first (repeat login); if absent, reject if the email already
belongs to a *different* account (`ConflictException`, mapped to 409 — no
new exception type needed, the existing one already fit); otherwise create
a new account, applying the exact same first-registrant-becomes-ADMIN rule
`AccountService.register` already has. That shared bootstrap logic
(assigning every currently-unowned resource to the first account) was
extracted into a new `FirstAccountOwnershipBootstrapper`, used by both
services, rather than duplicating five ownership-port constructor
dependencies and the assignment sequence itself across two classes.

No anti-enumeration timing defense is needed on this path (unlike password
login/register, ADR-0110) — Google has already confirmed the identity
before this code ever runs; there's no guessable secret being checked here.

## Consequences
- A new external network dependency at OAuth callback time (the token/
  user-info exchange with Google) — accepted as inherent to OIDC; no live
  call happens at app *startup*, only during an actual sign-in attempt.
- `changePasswordHash` now rejects a password-less (OIDC) account outright
  (`ValidationException`) rather than silently accepting a hash it would
  then contradict `applyIdentity`'s XOR invariant on — a Google-only account
  has no password to reset or change through the existing self-service/
  admin-reset password flows; this is expected, not a gap to fix.
- Deliberately did **not** build self-service "connect Google to my existing
  password account" — the no-auto-linking decision means a user who wants
  both must currently pick one at account-creation time. A smaller
  later addition if wanted, since the account is already authenticated
  either way when they'd ask for it.
- I could not perform a real, live, browser-driven Google sign-in during
  this feature's own development — that requires a Google Cloud OAuth 2.0
  Client the deployer must create themselves (external resource). Every
  other piece (exchange endpoint, DB-backed authorization-request state,
  account resolution/conflict logic, the "disabled" degrade-cleanly path)
  is covered by automated tests and was curl/IT-verified against the live
  docker-compose stack; the actual Google-facing half of the flow needs the
  deployer's own credentials to verify end-to-end.

## Alternatives considered
- **`spring.security.oauth2.client.registration.*` + Boot's own
  autoconfiguration** — tried first, rejected; see Decision above (eager
  validation breaks the whole app when unconfigured).
- **Generic/multi-provider OIDC config** — rejected per explicit product
  decision: Google-only matches this app's personal-scale deployment model;
  a data-driven multi-provider design would add real config surface for a
  benefit this app may never use.
- **Auto-linking a Google login to an existing password account by matching
  verified email** — rejected per explicit product decision: simpler and
  safer than trusting a third party's email verification as sufficient proof
  for merging identities, at the cost of requiring the user to pick one
  method at account-creation time.
- **Token in the redirect URL** (query param or fragment) instead of an
  exchange code — rejected; see Decision above (browser history, access
  logs, Referer header exposure).
- **A separate `account_identities` join table** instead of two nullable
  columns on `accounts` — rejected for now given the single-provider,
  one-identity-per-account shape; revisit if multi-provider or
  account-linking is ever added.
