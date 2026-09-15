# 0114. Postgres row-level security as defense-in-depth

## Status
Accepted

## Context
ADR-0109 makes strict per-account privacy the whole point of this app — every
World (and per-account catalog: game systems, global field templates, global
statblocks, AI provider settings) is invisible to every other account, ADMIN
included. Today that invariant lives in exactly one place:
`WorldPermissionEvaluator` (`security.WorldPermissionEvaluator`), which
answers "can this account touch this resource" as `resource.ownerId ==
principal` for the four tables that carry an `owner_id` column directly
(`worlds`, `game_systems`, `global_field_templates`, `global_statblocks`) —
enforced once, at the `/api/worlds/{worldId}/**` gate
(`WorldAccessAuthorizationManager`) and via `@PreAuthorize("hasPermission(...))")`
for the three catalog types. Everything nested under a world — campaigns,
sessions, arcs, articles, whiteboards, handouts, and so on — has no
`owner_id`/`account_id` of its own; it's trusted transitively once the
request clears the parent `worldId` gate, with no second ownership check at
the query/repository layer.

That's a single seam, which is good for auditability, but it also means a
future endpoint or service method that queries by `campaignId`/`sessionId`
directly — without re-deriving and re-checking the owning `worldId` — would
leak another account's campaign data with nothing at the database layer to
stop it. This was flagged during the post-MFA "what's still open for
authentication/authorization" audit (alongside session/device tracking,
ADR-0112, and SSO/OIDC, ADR-0113) as the one remaining item, and queued
rather than built immediately — it's infrastructure hardening, not a
reported bug or a blocked feature, and warranted its own design pass rather
than being folded into either of those.

## Decision

### Defense-in-depth, not a replacement
`WorldPermissionEvaluator` stays the primary authorization mechanism — RLS
is a second, independent backstop that fails closed if the application-layer
check is ever missing, buggy, or bypassed (a new endpoint that forgets
`hasPermission`, a raw repository query added without going through the
existing service layer, a future admin/reporting tool that queries across
accounts by mistake). The goal is that a bug in Java code can no longer, by
itself, leak another account's world.

### A session GUC carries the current account into Postgres
Every policy needs to know "who is asking" inside the database itself, not
just in application code. A Postgres session/transaction variable,
`app.current_account_id`, is set via `SET LOCAL` as the first statement of
every authenticated transaction, sourced from the same
`CurrentUserPort.currentAccountId()` every other ownership check already
uses. `SET LOCAL` is transaction-scoped (reverts automatically on
commit/rollback), which matters because connections are pooled and reused
across unrelated accounts' requests — a session-scoped `SET` would leak the
previous request's account id into the next one that reuses the same
physical connection. This is set by a small `@Aspect` ordered to run
*inside* Spring's own `@Transactional` interceptor (after it, in advice
order) so the native `SET LOCAL` lands inside the same physical transaction
the rest of the method's queries run in, rather than opening its own.
Anonymous/pre-auth requests (registration, login, health) never set the GUC
at all — fine, since those flows never query owned content tables.

### Staged table-by-table, in three tiers
Rather than one big migration, RLS is enabled table-by-table, ordered by how
directly each table's ownership can be expressed:

- **Tier 1 — direct `owner_id` column.** `worlds`, `game_systems`,
  `global_field_templates`, `global_statblocks`, and `ai_provider_settings`
  (the five tables `WorldPermissionEvaluator` and the ownership-bootstrap
  logic already treat as owned). Policy is a flat equality:
  ```sql
  CREATE POLICY worlds_owner_isolation ON worlds
    USING (owner_id = current_setting('app.current_account_id', true)::uuid);
  ```
- **Tier 2 — one hop via `world_id`.** `campaigns`, `sessions`, `arcs`,
  articles/wiki content, whiteboards, handouts, and every other table that
  hangs directly off a world. Policy joins back to `worlds` using the
  already-indexed `idx_worlds_owner_id`:
  ```sql
  CREATE POLICY campaigns_owner_isolation ON campaigns
    USING (world_id IN (
      SELECT id FROM worlds
      WHERE owner_id = current_setting('app.current_account_id', true)::uuid
    ));
  ```
- **Tier 3 — two or more hops.** Tables scoped by `campaign_id`/`session_id`
  rather than `world_id` directly (loose threads, clocks, beats, session
  todos, campaign players) — same pattern, one more join level. Left for
  last: most of these tables, most staging effort, and the tables where an
  app-level bug would leak the least (session-internal detail, not whole
  worlds), so the risk/effort tradeoff favors doing them after the two
  higher-leverage tiers land and the pattern is proven.

Each tier is its own migration and its own PR, so a mistake in one tier's
policies doesn't block or get bundled with the others.

### `FORCE ROW LEVEL SECURITY` is mandatory, not implied
`ENABLE ROW LEVEL SECURITY` alone does nothing for this app's deployment:
Postgres exempts a table's *owner* from RLS by default, and the app's own
runtime role (`SPRING_DATASOURCE_USERNAME`, e.g. `app`) is also the role
that owns these tables (it ran the Flyway migrations that created them) — so
without `FORCE ROW LEVEL SECURITY` on every table, the policies would be
silently inert for the one role that actually needs them enforced. Every
migration in this ADR pairs the two statements on the same table, never one
without the other. To make sure this can't regress silently the way the
`ENABLE`-without-`FORCE` gap itself could, a new backend test queries
`pg_tables`/`pg_policies` (via the existing Testcontainers Postgres) and
fails if any table with an `owner_id`/`world_id`/`campaign_id`/`session_id`
column lacks both — the same "can't silently regress" motivation as the
`contextsOnlyUsePublishedPorts` ArchUnit fitness function, just aimed at the
schema instead of the package graph.

### No admin bypass, and a narrow, explicit bootstrap bypass
Policies apply uniformly regardless of `role` — there is no `ADMIN`
exception, matching ADR-0109's "ADMIN grants no rights over other accounts'
worlds/content" invariant exactly. The account-roster admin endpoints
(`AccountAdminController`: list/disable/delete accounts, reset MFA) only
ever touch the `accounts` table, which carries no owned content and gets no
RLS policy at all.

One legitimate codepath *does* need to see across every account:
`FirstAccountOwnershipBootstrapper` (introduced alongside the OIDC work),
which scans for unowned worlds/catalog rows to assign to the very first
account ever registered. Rather than special-case the policy predicate
itself (e.g. `OR current_setting(...) IS NULL`, which would also silently
open the door for any code path that simply forgets to set the GUC), this
one adapter method runs under a second, narrowly-scoped Postgres role with
`BYPASSRLS`, provisioned in the Tier 1 migration and granted only to that
one connection use — every other part of the app, including every other
admin action, keeps using the ordinary RLS-subject role.

## Consequences
- One extra `SET LOCAL` per authenticated transaction — negligible cost,
  same category of tradeoff ADR-0110/ADR-0112 already accepted for the
  `tokenVersion`/`account_sessions` checks.
- Staging by tier means the defense-in-depth guarantee is incomplete until
  all three tiers ship — Tier 1/2 close off the highest-blast-radius gap
  (leaking a whole world or catalog entry) first; Tier 3's narrower gap
  (leaking one session's internal detail) stays open a bit longer. Documented
  here rather than treated as a blocker for shipping Tier 1 first.
- A missed table (new bounded-context table added later without RLS) fails
  *open* at the database layer, same as today — caught by the new
  `pg_tables`/`pg_policies` fitness-function test instead of silently
  shipping, but that test only fires in CI, not at runtime.
- The `BYPASSRLS` bootstrap role is a real, if narrow, exception to the
  "RLS is now enforced everywhere" claim — worth remembering and
  re-auditing if that bootstrap logic ever grows beyond its current
  one-time, first-registrant scope.
- Native/raw SQL debugging (psql, ad-hoc queries) against the app's normal
  role now returns nothing for owned tables unless `app.current_account_id`
  is set by hand first — a minor operational friction, acceptable for a
  personal-scale deployment.

## Alternatives considered
- **Status quo (`WorldPermissionEvaluator` only, no RLS)** — rejected; this
  is exactly the single-seam gap this ADR exists to close.
- **A separate Postgres schema (or database) per account** — rejected as far
  too heavy operationally for this app's scale: Flyway migrations, backups,
  and connection pooling would all need to become per-account, for a
  guarantee RLS already provides at the row level.
- **Denormalizing `owner_id`/`account_id` directly onto every child table**
  instead of the Tier 2/3 subquery-via-`world_id` approach — rejected: a
  much larger migration surface (every table, plus keeping the denormalized
  column in sync) for a marginal simplicity gain over reusing the
  `world_id` foreign-key chain the schema already enforces via referential
  integrity.
- **Policy predicate that also allows `current_setting(...) IS NULL`** (so
  any code path that forgets to set the GUC still works) — rejected; that
  would silently defeat the entire feature for exactly the failure mode
  (a codepath that forgot to do the right thing) RLS is meant to catch.
  The one legitimate all-accounts codepath gets an explicit `BYPASSRLS`
  role instead, so the exception is visible and auditable rather than baked
  into every policy's `USING` clause.
