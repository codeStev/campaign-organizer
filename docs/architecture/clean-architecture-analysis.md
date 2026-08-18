# Clean Architecture & Ports-and-Adapters Analysis

- Status: Analysis / proposal (no code changed on this branch)
- Branch: `refactor/hexagonal-architecture`
- Date: 2026-08-15
- Scope: backend (`backend/src/main/java/com/campaignorganizer`)

This document reviews the current backend against Clean Code and Clean/Hexagonal
Architecture guidelines and proposes concrete, incremental measures to introduce a
business (application) layer and a ports-and-adapters structure, so the web layer
stops depending directly on persistence.

---

## 1. Executive summary

The backend is **package-by-feature** (good) and contract-first (good), but within
each feature it is effectively **two layers: web → persistence**. Controllers hold
application logic and talk straight to Spring Data repositories. There is no
application/domain layer to own business rules, transactions, or cross-feature
orchestration.

Evidence (measured on this branch):

| Signal | Count | Meaning |
| --- | ---: | --- |
| Controllers | 30 | web adapters |
| Controllers injecting a `*Repository` directly | **24 / 30** | web → persistence coupling |
| Repositories | 20 | Spring Data JPA |
| JPA `@Entity` classes | 20 | domain == persistence model |
| "Services" | 4 | 2 are infra (`JwtService`, `CharacterSheetPdfService`); only `UsageService`, `SessionPacketService` are business logic |
| Controllers throwing `ResponseStatusException` | **26 / 30** | HTTP semantics embedded in business rules |
| `@Transactional` usages in the whole codebase | **1** | no explicit transaction boundaries |
| Integration tests / unit tests | **28 / 5** | inverted test pyramid (logic only reachable through the DB) |
| Repositories injected into `WorldExportController` | **18** | a single web class orchestrates the whole domain |

There is exactly **one** example of a clean port/adapter already in the code —
`media/MediaStorage` (port) with `LocalMediaStorage` (adapter), per ADR-0007. The
proposal below generalises that pattern to every feature.

**Bottom line:** the code is not "bad" — it is a conventional Spring MVC + Spring
Data layering. But it violates the **Dependency Rule** (inner policy must not
depend on outer detail): business rules currently live in, and depend on, the web
and persistence frameworks. That is what makes logic hard to unit-test (hence
28 integration tests vs 5 unit tests) and what this refactor should fix.

---

## 2. Findings by principle

### F1 — The Dependency Rule is inverted (web/persistence own the logic)
24 of 30 controllers inject repositories. Business decisions are made in the web
layer:

- `wiki/ArticleController` performs slug generation & de-duplication
  (`resolveSlugForCreate/Update`), HTML sanitising, template defaulting, and
  **revision snapshotting** (`revisions.save(ArticleRevision.of(article))`) inline
  in the `PUT` handler.
- `statblock/StatblockController.statblocksForCampaign(...)` implements the
  "statblocks relevant to a campaign" **aggregation rule** (union of campaign-scoped
  + beat-referenced) directly in the controller.
- `campaign/BeatController.validateLinks(...)` encodes referential rules across
  articles, statblocks, and sessions.

These are application/domain rules living in an outer ring.

### F2 — No application layer / no use-case boundary
Only `UsageService` and `SessionPacketService` exist as business services, and even
they **throw `ResponseStatusException`** (a web type) and reach into other features'
repositories. There is no place that represents "a use case" (e.g. *Create
article*, *Link statblock to beat*, *Assemble session packet*) independent of MVC.

### F3 — Domain model == persistence model
All 20 domain types are JPA `@Entity` classes annotated with `@Table`, `@Column`,
`@JdbcTypeCode`, etc. The "domain" therefore depends on Hibernate. Entities also
carry infrastructure concerns like ID and timestamp generation
(`UUID.randomUUID()`, `Instant.now()` in `@PrePersist`), which couples domain
behaviour to wall-clock/JVM and makes deterministic unit testing harder.

### F4 — Persistence & serialization leak through the API
DTOs exist per feature (good), but:
- `export/WorldExportController` serialises **raw JPA entities** into the export
  bundle (`bundle.put("world", world)`), so the wire format is the persistence
  model. `world.layerStyles` shows how an internal change immediately changes the
  export contract.
- A single controller injects **18 repositories** to hand-assemble the bundle —
  orchestration that belongs in an application service composing per-feature ports.

### F5 — Error handling couples business rules to HTTP
26 controllers (and both business services) throw `ResponseStatusException`. "Not
found" / "not in this world" / "bad link" are **domain outcomes**; expressing them
as HTTP status in the logic means the rules can't be reused or unit-tested without
the web stack, and the RFC 9457 mapping (ADR-0009) is implicit rather than
centralised.

### F6 — No transaction boundaries
Exactly one `@Transactional` in the codebase. Multi-step writes are not atomic — e.g.
`ArticleController.update` snapshots a revision **and** saves the article as two
separate repository calls with no surrounding transaction; a failure between them
leaves an orphan revision. Transaction policy is an application-layer concern that
is currently absent.

### F7 — Cross-feature coupling via foreign repositories
Features reach directly into each other's persistence:
`SessionPacketService` imports `map`, `statblock`, `wiki`; `StatblockController`
imports `campaign` repositories; `usage` reads everything. Import counts of other
feature packages from controllers: `campaign` 18, `world` 16, `wiki` 16, `calendar`
8, `sheet` 7, `map` 7, `timeline` 6. There are no **published ports** between
bounded contexts, so the module boundaries are not enforced.

### F8 — Inverted test pyramid
28 Testcontainers integration tests vs 5 unit tests. Because rules live in
controllers wired to JPA, the *only* way to test them is end-to-end through
Postgres. Fast, isolated unit tests of business logic are essentially impossible
today — a direct consequence of F1–F3.

### F9 — Clean Code (secondary, lower priority)
Mostly healthy (small methods, clear names, records for DTOs). The main smells are
**large orchestration classes** driven by the missing layer:
`WorldExportController` (164 LOC, 18 deps), `ArticleController` (154),
`StatblockController` (140). These shrink naturally once logic moves to application
services.

---

## 3. Target architecture (full hexagonal, organised by bounded context)

The unit of modelling is the **bounded context (domain)**, not the technical
feature. The current feature packages (`wiki`, `map`, `campaign`, …) are a
*delivery* decomposition; several of them belong to the **same** domain and must
**share one domain model**, while others are separate domains that must **not**
share types. So: one domain model **per bounded context**, and each context is a
top-level module with the ring structure inside it.

### 3.1 Bounded contexts (proposed) and the context map

Derived from the current packages and their relationships:

| Bounded context (module) | Absorbs today's packages | Core domain? |
| --- | --- | --- |
| **Worldbuilding** | `world`, `wiki`, `map`, `timeline`, `calendar`, `relationship` | Core |
| **Campaign** (GM/play) | `campaign` (campaigns, sessions, arcs, beats) | Core |
| **CharactersAndRules** | `sheet`, `statblock`, `dice` | Core |
| **Media** | `media` | Generic subdomain (supporting) |
| **Identity** | `auth`, `security`, `config` | Generic subdomain (supporting) |
| **Interchange / Reporting** | `export`, `usage`, session-packet | Cross-context orchestration |

Context relationships (who depends on whom, and the integration style):

- **Campaign → Worldbuilding** and **Campaign → CharactersAndRules**: a beat links
  Worldbuilding *articles* and CharactersAndRules *statblocks*. Campaign is the
  **downstream/consumer**; it integrates through **published lookup ports** exposed
  by the upstream contexts, mapped through an **anti-corruption layer** into its own
  domain vocabulary. It never imports the other contexts' domain or entity types.
- **CharactersAndRules → Worldbuilding** (a sheet/statblock may link an article) and
  **CharactersAndRules ↔ Campaign** (campaign-scoped sheets/statblocks, beat
  references): same rule — published ports + ACL, no shared types.
- **Interchange/Reporting** (export, usage backlinks, session packet) is an
  application-level orchestrator that composes the core contexts **only via their
  published ports/read-models** — it owns no core domain rules.
- **Media / Identity** are generic supporting contexts consumed via ports.

The context map (published ports + ACL) is what makes the boundaries real; without
it, "per feature" packages silently share entities, which is exactly finding F7.

### 3.2 Ring structure (inside each bounded context)

Full hexagonal rings, illustrated for the **Worldbuilding** context (with `wiki`,
`map`, … as aggregates/use-case groups *inside* it, sharing one domain model):

```
com.campaignorganizer.worldbuilding
├── domain/                 # ONE shared domain model for the whole context — PURE Java
│   ├── article/            # Article aggregate, ArticleId, Slug (value objects), invariants
│   ├── map/                # WorldMap, MapPin (fractional coords VO)
│   ├── timeline/ calendar/ relationship/ world/
│   └── shared/             # cross-aggregate value objects + domain exceptions
├── application/
│   ├── port/in/            # inbound ports = use cases (interfaces + Command/Result records)
│   │   ├── CreateArticleUseCase.java
│   │   └── UpdateArticleUseCase.java
│   ├── port/out/           # outbound ports (speak in DOMAIN types, not entities)
│   │   ├── ArticleRepositoryPort.java
│   │   └── HtmlSanitizerPort.java
│   ├── port/published/     # ports this context PUBLISHES to other contexts (e.g. ArticleLookupPort)
│   └── service/            # application services: implement in-ports, use out-ports, own @Transactional
│       └── ArticleService.java
└── adapter/
    ├── in/web/             # controllers (thin) + request/response DTOs + MapStruct web mapper
    │   ├── ArticleController.java
    │   ├── ArticleWebDtos.java          # request/response records (web model)
    │   └── ArticleWebMapper.java        # @Mapper: DTO ↔ Command/Result ↔ domain
    ├── out/persistence/    # JPA entities + Spring Data repo + port impl + MapStruct persistence mapper
    │   ├── ArticleJpaEntity.java        # persistence model (@Entity), no logic
    │   ├── ArticleJpaRepository.java     # extends JpaRepository<ArticleJpaEntity, UUID>
    │   ├── ArticlePersistenceAdapter.java  # implements ArticleRepositoryPort (domain ↔ entity)
    │   └── ArticlePersistenceMapper.java   # @Mapper: domain ↔ ArticleJpaEntity
    └── out/context/        # anti-corruption adapters that implement THIS context's out-ports
        └── CampaignArticleLookupAdapter.java  # calls Worldbuilding's published port, maps into local domain
```

The domain model is **shared within the context** across its aggregates
(`article`, `map`, …) and is the same in every use case of that context. It is a
compile error (ArchUnit) for another context to reference it.

**Dependency direction (within a context):** `adapter.in.web → application.port.in`;
`application.service → application.port.out` and `→ domain`;
`adapter.out.persistence → application.port.out` (implements it) `→ domain`.
The domain and application rings never import Spring MVC, Hibernate, Jackson, or
**any other context's packages**.

**Between contexts (the context map):** a context depends only on another context's
`application.port.published` interfaces, and translates the returned read-models
into its **own** domain via an anti-corruption adapter (`adapter/out/context/`).
No context imports another context's `domain`, `adapter`, or JPA repositories.
Orchestrators (Interchange/Reporting: export, usage, session packet) live in their
own module and compose the core contexts strictly through published ports.

### 3.3 Full hexagonal purity (no pragmatic shortcuts)
This is a deliberate decision: aim for a **pristine enterprise-standard** hexagonal
architecture with **no** Level-A/Level-B compromise. Although the app is single-user
*today*, that is **not guaranteed to stay true** (multi-tenant / multi-user is a
plausible future), and even absent that, a clean core is the maintainability
baseline this project wants. We therefore accept the boilerplate cost up front.

Concretely, **every bounded context** — including anaemic CRUD ones — gets the full
ring set and **three distinct models** (the domain model is *shared across the
context's aggregates*, but never shared with another context or with the entity/DTO
layers):

1. **Domain model** — pure Java (records / aggregates / value objects), **no**
   Spring, JPA, or Jackson annotations. Holds identity, invariants, and behaviour.
   IDs and timestamps come from injected ports (`IdGenerator`, `Clock`), never from
   `@PrePersist`.
2. **Persistence model** — JPA `@Entity` classes living only in
   `adapter.out.persistence`. Never referenced outside that package.
3. **Web model** — request/response DTOs living only in `adapter.in.web`. Plus
   `Command`/`Result` records on the inbound ports.

**All mapping between the three is done with [MapStruct]** (`@Mapper`, compile-time
generated, no hand-written converters): DTO ↔ command/result ↔ domain in the web
adapter, and domain ↔ JPA entity in the persistence adapter. Mappers live in the
adapter that owns the outer type; MapStruct's generated code is the only place a
domain type and an entity/DTO meet.

The domain model is the single source of truth for business rules; entities and DTOs
are **dumb, annotation-only carriers** with zero logic. This fully resolves F3 and
F4 (not just their symptoms) and makes the domain unit-testable without any
framework.

---

## 4. Concrete measures (prioritised)

| # | Measure | Addresses | Effort |
| --- | --- | --- | --- |
| **M0** | **Draw the bounded contexts + context map** (§3.1): agree the context boundaries, the aggregates in each, and the upstream/downstream relationships. This decides where every type lives and precedes all other work. | F7 | S |
| **M1** | Define the per-**context** ring layout (§3.2), document it in an ADR ("Hexagonal layering by bounded context"). Add `package-info.java` per ring; one Java module/package root per context. | F1,F2 | S |
| **M2** | Introduce **outbound persistence ports** that speak **domain types** (`*RepositoryPort`), implemented by a persistence adapter wrapping the Spring Data repo + a MapStruct mapper. Services depend on the port, never `JpaRepository`. | F1,F3,F7 | M |
| **M3** | Introduce **application services** per context that own the use cases; move all rule logic out of controllers (slug/sanitize/revision, campaign aggregation, link validation) into the **domain model**. | F1,F2,F9 | L |
| **M4** | Define **inbound ports** (use-case interfaces) with explicit `Command`/`Result` records; controllers map request DTO → command, call the port, map result → response DTO (via MapStruct). | F1,F2 | M |
| **M5** | **Separate domain model per context** (full purity): pure-Java aggregates/value objects with **no** Spring/JPA/Jackson; JPA `@Entity` types demoted to `adapter.out.persistence`; web DTOs confined to `adapter.in.web`. Three models, never shared. | F3,F4 | L |
| **M6** | **MapStruct** for every mapping (domain ↔ entity, DTO ↔ command/result ↔ domain). Add the annotation processor to the build (`org.mapstruct:mapstruct` + `mapstruct-processor`, `componentModel = "spring"`); ban hand-written converters. | F3,F4 | M |
| **M7** | Add **domain exceptions** thrown by domain/application; one `@RestControllerAdvice` maps them to RFC 9457 `problem+json` (centralising ADR-0009). Remove `ResponseStatusException` from all logic. | F5 | M |
| **M8** | Put `@Transactional` on **write** application-service methods; `@Transactional(readOnly = true)` on queries/aggregations (packet, usage, export). | F6 | S |
| **M9** | **Context integration**: each core context exposes `application.port.published` read/lookup ports; downstream contexts consume them through an **anti-corruption adapter** that maps into their own domain. Refactor `usage`, session packet, `statblock`↔`campaign`, and export accordingly. | F7 | L |
| **M10** | Move **export** into the Interchange context: an application service composing published ports, returning **export DTOs** (stop serialising JPA entities; stop injecting 18 repos). | F4 | M |
| **M11** | Introduce `Clock` and `IdGenerator` ports; inject them so aggregate creation is deterministic and unit-testable (remove `Instant.now()`/`UUID.randomUUID()` from the domain). | F3,F8 | S |
| **M12** | Rebalance tests: **unit-test domain + application services** against mocked out-ports (no Spring/DB); Testcontainers ITs for persistence adapters; `@WebMvcTest` for web adapters. Invert the pyramid. | F8 | M |
| **M13** | Add **ArchUnit** fitness functions: dependency rule, domain framework-free, web-has-no-persistence, no-HTTP-in-core, and **no cross-context references except `application.port.published`**. | F1,F7 | S |

Legend: S ≈ hours, M ≈ 1–2 days, L ≈ several days (per context, incrementally).

---

## 5. Migration strategy (incremental strangler — no big bang)

The refactor must keep all 33 tests green at every step and ship one bounded context
at a time. Full purity is the target, but it is reached incrementally, not big-bang.

0. **Context map (M0).** Agree the bounded contexts and relationships (§3.1) — this
   is the design gate everything else depends on. One ADR.
1. **Foundations (M1, M6, M7, M8, M11, M13).** Land the package/module conventions,
   the MapStruct build setup, the `@RestControllerAdvice` + domain-exception base,
   `Clock`/`IdGenerator`, and the ArchUnit rules (scoped to migrated contexts first).
2. **Pilot one bounded context end-to-end** — recommend **CharactersAndRules** (has
   the real aggregation logic in F1/F7 via statblock) *or* **Worldbuilding** (richest
   domain: articles with slug/revision/sanitize). Build all three models, ports,
   application services, thin controllers, MapStruct mappers, and **domain unit
   tests**. This is the reference implementation others copy.
3. **Publish the context's ports** and stand up the **anti-corruption adapters** its
   downstream contexts need (e.g. Campaign→CharactersAndRules statblock lookup) (M9).
4. **Roll out context by context**, simplest first (Media, Identity), then the other
   core contexts — **each at full purity** (no Level split; every context gets its own
   domain model + MapStruct mappers).
5. **Build the Interchange context last** (usage, session packet, export) purely on
   the now-published ports (M9, M10).
6. **Tighten ArchUnit** to the whole codebase once all contexts are migrated; delete
   the last `ResponseStatusException` and the last shared entity.

Each context is one PR (or a short series) with its own ADR entry; the OpenAPI
contract and external behaviour stay unchanged (internal refactor — contract-first
still holds).

---

## 6. Risks, trade-offs, non-goals

- **"Over-engineering a single-user app" is an accepted, deliberate cost.** Full
  purity is a *requirement*, not something to trim: the app's single-user status is
  not guaranteed to persist (multi-user/multi-tenant is a plausible future), and a
  pristine core is the maintainability baseline this project targets regardless.
  We therefore build the full ring set + a distinct domain model **for every
  context**, including anaemic CRUD ones — no shortcuts.
- **Boilerplate / mapping cost.** Real and accepted. Contained by **MapStruct**
  (compile-time, zero hand-written converters) and repaid by F8 (fast framework-free
  unit tests) and F7/F9 (enforceable boundaries, small classes). The cost is
  mechanical, not intellectual.
- **Churn vs feature work.** Strangler approach means the app keeps working and
  shipping throughout; no frozen "big refactor" branch. Full purity is reached
  context-by-context.
- **Non-goals:** no change to the REST contract, DB schema, or Flyway history; no
  switch away from Spring Boot/JPA/Postgres; no microservices — this is a
  *modular monolith* whose modules are **bounded contexts** with clean internal
  boundaries (a shape that *could* be split into services later precisely because the
  contexts don't share models).

---

## 7. Definition of done (measurable)

- Every bounded context has **three distinct models** — a pure-Java **domain model**,
  JPA **entities** (in `adapter.out.persistence` only), and web **DTOs** (in
  `adapter.in.web` only). No type is shared across the three. (today: 20 entities double as the domain model)
- **All** inter-model mapping is generated by **MapStruct**; zero hand-written
  converters and zero entities/DTOs crossing a ring boundary.
- Controllers inject **0** repositories (only inbound ports). (today: 24/30 inject repos)
- **0** `ResponseStatusException` / `HttpStatus` in `domain` or `application`; all
  domain errors flow through domain exceptions + the central problem+json advice. (today: 26 controllers)
- The **domain** ring has **0** imports of Spring, JPA, Jackson, or another context. (enforced by ArchUnit)
- Every write use case is `@Transactional`. (today: 1 in the codebase)
- Export serialises **export DTOs**, not JPA entities; the export orchestrator
  depends on published ports, not 18 repositories.
- No context imports another context's `domain`/`adapter`/repository — only its
  `application.port.published`, via an anti-corruption adapter. (enforced by ArchUnit)
- Test mix inverts toward unit tests for domain/application logic; ITs cover adapters. (today: 28 IT / 5 unit)

---

## 8. Suggested ADRs to accompany the work

- **ADR — Bounded contexts & context map** (supersedes/extends ADR-0001’s
  “organised by feature”): names the contexts (§3.1), their aggregates, and their
  upstream/downstream relationships.
- **ADR — Hexagonal layering per bounded context**: defines the domain/application/
  adapter rings and the dependency rule; names `media/MediaStorage` as the existing
  exemplar.
- **ADR — Separate domain model + MapStruct mapping**: three models per context
  (domain / entity / DTO); all mapping via MapStruct (`componentModel = "spring"`),
  no hand-written converters.
- **ADR — Domain error model & problem+json mapping** (implements ADR-0009 via a
  central `@RestControllerAdvice`; forbids `ResponseStatusException` in logic).
- **ADR — Transaction policy** (application services own boundaries; read-only for
  queries).
- **ADR — Cross-context communication via published ports + anti-corruption layer**
  (no foreign repositories/entities/domain types; export/usage/packet depend on ports).
- **ADR — Architecture fitness functions with ArchUnit** (incl. no cross-context
  references except `application.port.published`).

---

## Appendix — evidence commands

Reproducible from `backend/src/main/java/com/campaignorganizer`:

```bash
# controllers injecting repositories
grep -rl "Repository" --include=*Controller.java . | wc -l          # 24
# controllers throwing web exceptions from logic
grep -rl "ResponseStatusException" --include=*Controller.java . | wc -l   # 26
# transaction boundaries in the whole backend
grep -rln "@Transactional" . | wc -l                                # 1
# export orchestrator repository fan-in
grep -cE "private final \w+Repository" export/WorldExportController.java  # 18
# test pyramid
find ../../../../test -name '*IT.java'   | wc -l                     # 28
find ../../../../test -name '*Test.java' | wc -l                     # 5
# existing port/adapter exemplar
ls media/MediaStorage.java media/LocalMediaStorage.java
```

[MapStruct]: https://mapstruct.org/
