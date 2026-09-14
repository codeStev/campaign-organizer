# 0109. Multi-account scope

## Status
Accepted — supersedes ADR-0005

## Context
ADR-0005 scoped this app to a single owner user: no multi-tenancy, no
registration, no player accounts, no per-object ownership layer. The user
now wants multiple independent people to each register their own account
and manage their own worlds/campaigns/content, with an admin-only view to
manage the account roster. This is a direct reversal of ADR-0005's core
decision, not an incremental addition, so it gets a fresh ADR rather than an
edit to an accepted one.

`World` is this app's true tenant root — its own Javadoc calls it "the
tenant root that all worldbuilding and campaign content hangs off." Every
other bounded context's per-tenant data already carries a mandatory
`world_id` FK (campaigns, media, whiteboards, tables, handouts, most of
`characters`), and — cross-checked against every `@RequestMapping` in the
codebase — every one of those nested resources is reachable only via a path
literally prefixed `/api/worlds/{worldId}/...`. The exceptions were four
tables that were *instance-global* rather than world-scoped:
`ai_provider_settings`, `global_field_templates`, `game_systems`, and
`global_statblocks` — meant to be reused across a single account's own
worlds, not shared between unrelated worlds.

## Decision
- Every `World` row, plus the four previously instance-global catalog
  tables, gains an `owner_id` (`V57`–`V61`), scoping all of it to the
  account that owns it.
- Data is **strictly private per account, including from other accounts
  with the ADMIN role** — ADMIN grants rights over the account roster only
  (see ADR-0110), never read/write access to another account's worlds or
  catalogs.
- **Ownership enforcement is a single seam, not fifty per-controller
  checks.** Because every nested resource path is already
  `/api/worlds/{worldId}/...`, one `WorldAccessAuthorizationManager`
  registered against that path pattern in `SecurityConfig` covers every
  bounded context's nested endpoints with zero controller changes. It
  delegates to `WorldPermissionEvaluator`
  (`org.springframework.security.access.PermissionEvaluator`), which is
  also reused directly by `@PreAuthorize("hasPermission(...))")` on the
  handful of application-service methods that accept a foreign catalog id
  (game system, global template, global statblock) inside a request body
  rather than the URL path — the same rule, `resource.ownerId ==
  principal`, decides both cases.
- Existing pre-account data gets `owner_id` added **nullable**, backfilled
  at runtime — not via a bootstrap-admin environment variable — to whichever
  account is the first to register (see ADR-0110). This avoids introducing
  a new secret solely to bridge a one-time transition, at the acceptable
  cost that pre-account rows are temporarily unowned until that first
  registration happens (this codebase's local/dev data is already treated
  as disposable).
- Denial semantics are deliberately split by what they protect: every
  `hasPermission(...)`-driven denial — from `WorldAccessAuthorizationManager`
  on a `/api/worlds/**` request, or from `@PreAuthorize` on a standalone
  catalog service (`/api/game-systems/**` etc.) — masks as 404 via the
  *default* `AccessDeniedHandler`, identical to a nonexistent id, so a
  stranger's resource id never confirms it exists. Role-insufficiency on an
  admin-only account-management endpoint (ADR-0110) is a genuinely different
  kind of denial — knowing you lack a role isn't sensitive the way another
  account's data existing is — and gets a real 403, via a handler scoped
  specifically to `/api/accounts/**`.

### Not adopted: Spring Security's ACL module
`spring-security-acl` looked like an off-the-shelf fit for "a resource can
have multiple grantees with different permissions," but its APIs have been
partially deprecated since Spring Security 5.8/6, its official samples
don't run without rework, and it predates the modern `AuthorizationManager`/
`PermissionEvaluator` extension points this decision already uses. Not
worth the schema/ceremony overhead versus the one-evaluator design above.

## Consequences
- **Scaling to a future sharing feature is cheap by design.** The whole
  point of routing every ownership question through
  `WorldPermissionEvaluator` is that a later feature — inviting a co-GM
  into a World with read or read-write rights — only touches that one
  class plus a new `world_grants` table
  (`world_id, grantee_account_id, permission`). Its rule widens from
  `ownerId == principal` to `ownerId == principal OR grants contain
  (worldId, principal, requestedPermission-or-higher)`. No controller, no
  `AuthorizationManager` registration, and no `@PreAuthorize` call site
  changes, since none of them ever asked the `owner_id` column directly.
- **Deferred hardening, not required for this phase:** PostgreSQL
  Row-Level Security as defense-in-depth underneath the application-layer
  checks — a `USING (owner_id = current_setting('app.uid')::uuid)` policy
  per owned table, set via transaction-local
  `SELECT set_config('app.uid', ?, true)` rather than a session-level
  `SET` (which can leak across pooled connections). This wouldn't replace
  `WorldPermissionEvaluator` — RLS can't express "owner OR has a grant" as
  cleanly once sharing exists without also teaching the database about the
  grants table — but it's a solid belt-and-suspenders layer against an
  app-code mistake. Worth a follow-up ADR if this app's exposure grows.
- Cross-referencing a foreign catalog id embedded in a request body (not
  the URL path) needed an explicit check per call site — the URL-based
  `AuthorizationManager` can't see into request bodies. Each such call site
  in `campaign`/`characters` now carries its own
  `@PreAuthorize("hasPermission(...))")`, reusing the same evaluator.
- **A real cross-account IDOR, caught by post-implementation security review,
  worth flagging for the next endpoint shaped like this one:**
  `GlobalStatblockService.importIntoCampaign` (`POST
  /api/statblocks/global/{id}/import`) takes a *destination* `worldId`/
  `campaignId` in its request body and writes a new `Statblock` into that
  world. Its endpoint is **not** nested under `/api/worlds/{worldId}/**`
  (it's a catalog action, mounted at `/api/statblocks/global/**`), so
  `WorldAccessAuthorizationManager` never saw that `worldId` at all — the
  `@PreAuthorize` only checked ownership of the *source* `globalStatblockId`,
  not the destination. Any account could write into any other account's
  world/campaign just by knowing its id. Fixed by adding
  `hasPermission(#worldId, 'World', 'ACCESS')` alongside the existing check.
  The general rule this exposes: **any body field that is itself a
  world/campaign id used as a *write target*, not just a same-world
  cross-reference, needs its own `hasPermission` check** — this is easy to
  miss precisely when the endpoint's own path already "looks" scoped (it's
  under `/api/statblocks/...`) but isn't actually nested under
  `/api/worlds/{worldId}/**`.
- **A real bean-construction cycle, worth flagging for the next context that
  adds `@PreAuthorize`:** any service that both (a) implements the published
  query port `WorldPermissionEvaluator` depends on for a given resource type
  and (b) carries `@PreAuthorize` itself needs Spring's method-security AOP
  proxy, which needs `methodSecurityExpressionHandler`, which needs
  `WorldPermissionEvaluator`, which needs that same service's published
  port — a cycle. Hit this for `GameSystemService` and
  `GlobalStatblockService` (each implements its `*QueryPort` and also has
  `@PreAuthorize` methods); fixed by extracting the query-port
  implementation into its own bean (`GameSystemQueryService`,
  `GlobalStatblockQueryService`), the same split already used for
  `GlobalFieldTemplateQueryService` (ADR-0093), just for a different
  triggering reason. Rule of thumb going forward: a service implementing one
  of `WorldPermissionEvaluator`'s four query ports should not also carry
  `@PreAuthorize` — split the query side out first.

## Alternatives considered
- **A bespoke `HandlerInterceptor`** comparing `ownerId` by hand — works,
  but bypasses Spring Security's own `AccessDeniedHandler`/testing support
  and gives no natural extension point for sharing. Rejected in favor of
  the `PermissionEvaluator`-based design above.
- **Per-context ACL adapters**, mirroring this codebase's existing
  `WorldExistsPort`-per-consuming-context pattern — rejected as
  unnecessary given the path regularity: a single cross-cutting check
  already covers 95%+ of endpoints, so replicating ~18 existence-style
  ACL ports for ownership would be pure duplication.
