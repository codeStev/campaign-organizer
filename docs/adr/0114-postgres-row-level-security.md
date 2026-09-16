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

### Two Postgres roles: a superuser for migrations, an ordinary one for the app
This app's only DB credential today (`app`, `SPRING_DATASOURCE_USERNAME` in
both `docker-compose.yml`s) is provisioned by the official Postgres image's
`POSTGRES_USER` as a **superuser with `BYPASSRLS`** — confirmed empirically
(`\du app`) before writing any policy, per the standing "verify, don't
assume" lesson from this app's own history with framework/library
capabilities. Postgres superusers bypass RLS unconditionally; `FORCE ROW
LEVEL SECURITY` has no effect on them and there is no override. Every
policy in this ADR would therefore be inert for the actual running app
unless this is fixed first — so fixing it is now part of this ADR, not a
separate concern.

`app` stays exactly as it is, but becomes **migrations-only**: Spring
Boot's `spring.flyway.url`/`user`/`password` (a first-class override,
distinct from `spring.datasource.*`) point Flyway at it explicitly, so
Flyway keeps its own connection independent of the app's JPA/Hikari pool. A
new ordinary role, `app_runtime` — `NOSUPERUSER NOBYPASSRLS`, ordinary
`SELECT/INSERT/UPDATE/DELETE` grants — becomes `spring.datasource.*`'s
role, i.e. every JPA/Hibernate query the app makes at runtime. It's
provisioned idempotently by a Flyway `afterMigrate` `Callback` (Java, not a
versioned SQL migration — a migration's checksum can't safely vary with an
environment's password, and running after every migration, not just the
newest one, guarantees a table added by *this* deploy is granted too):
create-if-missing, `ALTER ROLE ... PASSWORD` from a new `RUNTIME_DB_PASSWORD`
env var (threaded through both `docker-compose.yml`s the same way
`DB_PASSWORD` already is), `GRANT ... ON ALL TABLES IN SCHEMA public`, and
`ALTER DEFAULT PRIVILEGES` so tables from *future* migrations are granted
automatically without this callback needing to change.

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
physical connection.

**Not a `@Transactional`-wrapping `@Aspect`.** This codebase has no AOP
today (`spring-boot-starter-aop` isn't a dependency, zero `@Aspect`
classes), and whether such an aspect's `SET LOCAL` would land inside the
same physical transaction as the method's subsequent queries depends on its
`@Order` relative to Spring's own `TransactionInterceptor` — get that
backwards and the GUC silently applies to a different connection than the
queries that follow, so RLS enforces nothing while looking correct. That
failure mode is exactly what this ADR exists to prevent, so it's avoided by
construction instead: a thin JDBC `Connection` proxy wrapping the pooled
`DataSource` (`java.lang.reflect.Proxy` over the `Connection` interface,
delegating every method except one), intercepting `setAutoCommit(false)` —
the one deterministic moment a physical transaction actually begins,
regardless of whether it was opened by `@Transactional`, Hibernate, or
anything else — to issue `SET LOCAL app.current_account_id` right there,
reading `CurrentUserPort` synchronously (Spring Security's context is
already populated by `JwtAuthFilter` long before any repository call
reaches connection acquisition). Anonymous/pre-auth requests (registration,
login, health) have no account in context, so no GUC gets set — fine, since
those flows never query owned content tables and RLS simply returns no rows
for `app_runtime` on any table it does touch by mistake. Proven, not just
argued: a Testcontainers IT connects directly as `app_runtime`, sets the
GUC by hand for one account, and asserts a second account's row is
genuinely absent — independent of the application's own authorization code.

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

### `FORCE ROW LEVEL SECURITY` is set anyway, as a second backstop
With the role split above, `app_runtime` is *not* the table owner (`app`
still is, via Flyway) — an ordinary, non-superuser, non-owner role always
has `ENABLE ROW LEVEL SECURITY` policies applied to it, `FORCE` or not, so
`FORCE` isn't strictly load-bearing for `app_runtime` the way the original
draft of this ADR assumed (that draft was written before the role split was
known to be necessary, back when the app's only role really was both the
runtime role *and* the table owner). `FORCE` is set on every RLS table
anyway, for the same reason `app_runtime` was worth splitting off at all:
if anyone ever runs the app's own queries as `app` by mistake (a future
admin tool, a debugging session, a refactor that quietly reintroduces a
single-role setup), `FORCE` is the only thing that would still make RLS
apply to that connection. Every migration in this ADR pairs `ENABLE` and
`FORCE` on the same table, never one without the other. To make sure
neither can regress silently, a new backend test queries
`pg_tables`/`pg_policies` (via the existing Testcontainers Postgres,
connecting as `app_runtime`) and fails if any table with an
`owner_id`/`world_id`/`campaign_id`/`session_id` column lacks both — the
same "can't silently regress" motivation as the `contextsOnlyUsePublishedPorts`
ArchUnit fitness function, just aimed at the schema instead of the package
graph.

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
account ever registered — and runs during *registration*, before any
authenticated principal exists, so it has no account id to key a GUC off
even in principle. Rather than special-case the policy predicate itself
(e.g. `OR current_setting(...) IS NULL`, which would also silently open the
door for any code path that simply forgets to set the GUC), or introduce a
third Postgres role/credential, this one component gets a small dedicated
`JdbcTemplate` built from the same admin (`app`) credentials Flyway already
uses, and runs its five ownership-assignment `UPDATE`s as raw SQL through
that instead of through the RLS-subject JPA repositories every other
service uses. A superuser connection naturally bypasses RLS — no new
Postgres-side mechanism (no `SECURITY DEFINER` function, no extra role) —
and the actual assignment logic stays in Java, matching this codebase's
hexagonal-architecture convention of keeping behavior in ports/adapters
rather than the database. Every other part of the app, including every
other admin action, keeps using `app_runtime`.

## Consequences
- A second Postgres role/credential to manage (`RUNTIME_DB_PASSWORD`,
  alongside the existing `DB_PASSWORD`) in both `docker-compose.yml`s —
  more deployment surface than the ADR originally scoped, but necessary:
  without it, every policy below is inert for the actual running app (see
  "Two Postgres roles" above).
- One extra `SET LOCAL` per authenticated transaction, plus the connection
  proxy's `setAutoCommit` interception on every checkout — negligible cost,
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
- `FirstAccountOwnershipBootstrapper`'s admin `JdbcTemplate` is a real, if
  narrow, exception to the "RLS is now enforced everywhere" claim — worth
  remembering and re-auditing if that bootstrap logic ever grows beyond its
  current one-time, first-registrant scope.
- Native/raw SQL debugging (psql, ad-hoc queries) against `app_runtime` now
  returns nothing for owned tables unless `app.current_account_id` is set
  by hand first — a minor operational friction, acceptable for a
  personal-scale deployment. Connecting as `app` for debugging still sees
  everything, unfiltered, as before.

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
  (a codepath that forgot to do the right thing) RLS is meant to catch. The
  one legitimate all-accounts codepath gets an explicit, separately-secured
  path instead (the admin `JdbcTemplate` above), so the exception is
  visible and auditable rather than baked into every policy's `USING`
  clause.
- **A `@Aspect` on `@Transactional` methods to set the GUC** — rejected:
  correctness would depend on getting its `@Order` right relative to
  Spring's own `TransactionInterceptor` from memory, in a codebase with no
  existing AOP to anchor that against, for a mistake (GUC lands on the
  wrong connection) that fails silently rather than loudly. The
  `Connection`-proxy approach removes the ordering question entirely by
  hooking the one JDBC event (`setAutoCommit(false)`) that unambiguously
  marks a physical transaction's start.
- **A second `BYPASSRLS` Postgres role for the bootstrap codepath** (the
  original plan) — superseded once the migrations-only/runtime-only
  role split was already needed for the base feature to work at all;
  reusing the existing admin (`app`) credentials for the one bootstrap
  component avoids a third credential and a third connection pool for a
  single, rarely-invoked, already-narrow use.
