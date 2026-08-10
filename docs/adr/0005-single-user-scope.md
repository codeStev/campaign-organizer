# ADR-0005: Single-user, no community features

- Status: Accepted
- Date: 2026-08-10

## Context
World Anvil is heavily multi-user: subscriber groups, public world pages, player
accounts, sharing, and collaboration. The user wants the app **for personal use
only** and explicitly does not need community features.

## Decision
Scope the app to a **single owner user**. Cut multi-tenancy, registration,
public pages, subscriber groups, player accounts, and real-time collaboration.
Spoiler control is reduced to a simple per-article **"GM-only" hidden flag**
rather than group-based ACLs.

## Consequences
- No per-object ownership/permission layer — dramatically less code and a faster
  path to a usable app.
- Data model and API stay simple (no user/tenant foreign keys everywhere).
- If sharing is ever wanted, it becomes a significant future ADR, not a small
  toggle. Accepted trade-off.

## Alternatives considered
- **Full multi-user from day one:** rejected as unnecessary complexity for the
  stated personal use case.
