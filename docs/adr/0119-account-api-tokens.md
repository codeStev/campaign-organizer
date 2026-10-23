# ADR-0119: Account-scoped API tokens for machine clients

- Status: Accepted
- Date: 2026-10-23
- Extends: ADR-0110 (accounts, JWT), ADR-0111 (MFA), ADR-0112 (session tracking), ADR-0114 (RLS)

## Context
The only credential today is an interactive login: password, then MFA, then
a JWT recorded in `account_sessions`. A headless client cannot obtain one.
The first such client is DiscordWhispr (voice-session recorder and
transcription pipeline on the GM's own machine). It needs to list the
account's campaigns, read a campaign's glossary, and write back a session
summary and transcript.

Per ADR-0109 everything is private to the owning account, so the credential
must identify an **account** (the GM may own several worlds), not a world or a
campaign. Per ADR-0111 it must not weaken MFA for interactive logins.

## Decision

### A new aggregate: `ApiToken` in the `accounts` context
Own domain model, JPA entity and web DTO, MapStruct-mapped like every other
aggregate. Fields: `id`, `accountId`, `name` (user label), `scopes`,
`tokenHash`, `prefix` (first 8 chars, for display), `createdAt`, `expiresAt`
(nullable), `lastUsedAt`, `revokedAt`.

### Token format and storage
`cot_<random 256-bit, base64url>`. Shown **once** at creation. Only a SHA-256
hash is stored (the secret has full entropy, so a slow hash adds nothing).
Lookup is by hash; comparison is constant-time. IDs, randomness and time come
from the `IdGenerator`, a secure-random port and `Clock`.

### Authentication path
`ApiTokenAuthFilter` handles `Authorization: Bearer cot_...` before
`JwtAuthFilter` (which keeps ignoring anything without a JWT shape). It
resolves the token through a published `ApiTokenQueryPort`, checks the
account is enabled, and sets the same principal (a bare account `UUID`) as a
JWT login, so `WorldPermissionEvaluator` and the RLS session variable
(ADR-0114) behave identically. Revoking a token, disabling the account, or
bumping `token_version` (password change, logout-all) invalidates it
immediately.

### MFA is not weakened
Creating, listing and revoking tokens requires a **full MFA session**
(`/api/accounts/me/api-tokens`, behind the existing `mfaRequired` rule). A
token carries a new `API_TOKEN` factor, **not** the `MFA` factor, so it never
satisfies `mfaRequired` anywhere. Instead the token's authorities are a
whitelist, see below.

### Scopes are a closed allowlist
A token only works on endpoints explicitly annotated with a scope; everything
else is denied (404, same masking as ownership mismatch, ADR-0109). Initial
scopes:

| scope | grants |
|---|---|
| `campaigns:read` | list campaigns of the account's worlds |
| `glossary:read` | campaign glossary (ADR-0120) |
| `sessions:write` | create/update a campaign session and its summary |
| `transcripts:write` | upload/replace/delete session transcript lines |

Never available to a token, regardless of scope: account management, MFA,
tokens themselves, backup/import, Foundry connection, AI settings.

### Contract and storage
Contract-first: `docs/api/openapi.yaml` gains the token endpoints and a
`bearerApiToken` security scheme. Flyway `V75__api_tokens.sql` creates
`api_tokens`. Like `account_sessions` it has **no RLS policy**: the auth
filter looks a token up by hash before any account is known, so the
`app.current_account_id` setting (ADR-0114) cannot exist yet. Isolation is
enforced in the queries, which always scope management calls by `account_id`.

## Consequences
- A leaked token exposes at most the allowlisted scopes of one account and is
  revocable per token without logging out the GM.
- One extra indexed lookup per token-authenticated request.
- No refresh flow: tokens are long-lived by design; `expiresAt` is optional
  and recommended.
- No per-world or per-campaign narrowing yet. Possible later as an optional
  `worldIds` restriction without changing the model.

## Alternatives considered
- **Machine login with password + TOTP**: forces MFA secrets onto the
  recorder host and gives the client full account power. Rejected.
- **Token carries the MFA factor**: simplest, but bypasses the scope
  allowlist and ADR-0111. Rejected.
- **World- or campaign-scoped token**: cannot list campaigns across worlds
  (needed for Discord autocomplete). Rejected by the maintainer.
- **Reuse `account_sessions`**: sessions are device logins of interactive
  JWTs; mixing in scopes and a hashed secret would blur two lifecycles.
