# ADR-0006: Single-password JWT authentication

- Status: Accepted
- Date: 2026-08-10

## Context
The app is single-user (ADR-0005) but self-hosted and reachable over a network,
so it still needs to keep others out. A full user/registration system is
unnecessary.

## Decision
Authenticate with **one configured password** (`APP_PASSWORD`). `POST
/api/auth/login` compares it in constant time and, on success, issues a
**stateless HS256 JWT**. All other endpoints require `Authorization: Bearer
<jwt>`. The token is validated by a servlet filter; no server-side session or
user table exists.

## Consequences
- Minimal code; nothing to register or reset in a UI.
- Stateless — horizontally trivial, no session store.
- Password/secret rotation means changing env vars and re-issuing tokens.
- Constant-time comparison avoids timing side channels.

## Alternatives considered
- **Full Spring Security user store / OAuth2:** overkill for one user.
- **No auth (network-only):** rejected; the instance may be exposed.
