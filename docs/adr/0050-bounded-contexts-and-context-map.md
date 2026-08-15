# ADR-0050: Bounded contexts and context map

- Status: Accepted
- Date: 2026-08-15
- Supersedes (in part): ADR-0001 ("organised by feature")
- Relates to: `docs/architecture/clean-architecture-analysis.md`,
  `docs/architecture/architecture-harness.md`

## Context
The backend grew as flat feature packages (`wiki`, `map`, `campaign`, …) that talk
directly to each other's repositories, with no application/domain layer (see the
analysis). We are migrating to a hexagonal, bounded-context modular monolith. The
**first** step — and the design gate for everything after — is to agree the bounded
contexts and how they relate, because that decides where every type is allowed to
live.

## Decision
Adopt the following **bounded contexts** as the top-level modules. Each owns **one**
domain model shared across its aggregates, and no context shares types with another.

| Context (module root `com.campaignorganizer.<ctx>`) | Aggregates (today's packages) | Kind |
| --- | --- | --- |
| **worldbuilding** | world, article/category/revision/template (`wiki`), map, timeline, calendar, relationship | Core |
| **campaign** | campaign, session, arc, beat | Core |
| **characters** (CharactersAndRules) | character sheet + template (`sheet`), statblock, dice | Core |
| **media** | media asset + storage (`media`) | Generic / supporting |
| **whiteboard** | free-form plotting canvas (`whiteboard`) | Generic / supporting |
| **identity** | auth, security, config | Generic / supporting |
| **interchange** | export, usage backlinks, session packet | Cross-context orchestration |

### Context map (relationships & integration style)
- **campaign → worldbuilding** (beats link articles) and **campaign → characters**
  (beats link statblocks; campaign-scoped sheets/statblocks): campaign is the
  **downstream/consumer**; it integrates via the upstream context's
  `application.port.published` lookups behind an **anti-corruption layer (ACL)**.
- **characters → worldbuilding** (a sheet/statblock may link an article) and
  **characters ↔ campaign** (campaign relevance): same rule — published ports + ACL.
- **interchange → {worldbuilding, campaign, characters, media}**: orchestrators
  (export, usage, session packet) compose the core contexts **only** through their
  published ports/read-models; they hold no core domain rules.
- **media, identity**: generic supporting contexts consumed via ports.

### Rules that follow from this map
- A context may reference another context **only** through its
  `application.port.published` interfaces, mapped into its own domain by an ACL
  adapter. No foreign `domain`, `adapter`, entity, or repository references.
- Shared/foreign identifiers cross boundaries as **plain values** (e.g. a UUID or a
  small published read-model), never as another context's domain type.
- The dependency direction is enforced by ArchUnit (see the harness §3).

## Consequences
- Boundaries become explicit and enforceable; the current cross-feature repository
  coupling (analysis F7) becomes a compile/architecture-test error once migrated.
- Some "features" merge into one context (e.g. maps + timelines + articles are all
  *worldbuilding*), so they legitimately share a domain model; others that looked
  adjacent (campaign vs worldbuilding) are kept strictly separate.
- The module seams are drawn where a future split into services would occur — no
  context shares models, so extraction stays possible without a rewrite.

## Alternatives considered
- **Keep package-by-feature** — rejected; it is the structure that produced the
  coupling and the missing layer.
- **A single shared domain model for the whole app** — rejected; it re-creates the
  "everything depends on everything" problem under a new name.
