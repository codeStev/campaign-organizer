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

## 3. Target architecture (pragmatic hexagonal)

Keep **package-by-feature**; add **layered rings inside each feature** with a strict
inward dependency direction. Proposed per-feature layout (illustrated for `wiki`):

```
com.campaignorganizer.wiki
├── domain/                 # entities/value objects + pure domain logic (no Spring/JPA)
│   ├── Article.java
│   ├── Slug.java           # value object (slugify/dedup rule)
│   └── ArticleNotFoundException.java   # domain exception
├── application/
│   ├── port/in/            # inbound ports = use cases (interfaces + commands/results)
│   │   ├── CreateArticleUseCase.java
│   │   └── UpdateArticleUseCase.java
│   ├── port/out/           # outbound ports (interfaces the app needs)
│   │   ├── ArticleRepositoryPort.java
│   │   └── HtmlSanitizerPort.java
│   └── service/            # application services implement in-ports, use out-ports, own @Transactional
│       └── ArticleService.java
└── adapter/
    ├── in/web/             # controllers (thin), request/response DTOs, mappers
    │   └── ArticleController.java
    └── out/persistence/    # JPA entities + Spring Data repos implementing out-ports, mappers
        ├── ArticleJpaEntity.java
        ├── ArticleJpaRepository.java   # extends JpaRepository
        └── ArticlePersistenceAdapter.java  # implements ArticleRepositoryPort
```

**Dependency direction:** `adapter.in.web → application.port.in`;
`application.service → application.port.out` and `→ domain`;
`adapter.out.persistence → application.port.out` (implements it) `→ domain`.
The domain and application rings never import Spring MVC, Hibernate, Jackson, or
another feature's internals.

**Cross-feature contracts:** each feature *publishes* a small set of ports/read
models (e.g. `wiki` exposes `ArticleLookupPort`; `statblock` exposes
`StatblockCampaignPort`). Orchestrators (`usage`, session packet, export) depend on
those published ports — never on foreign repositories or entities.

### Pragmatism (this is a single-user app — avoid gold-plating)
Full hexagonal purity (a separate hand-mapped domain model for every entity) is not
always worth it here. Two acceptable levels:

- **Level A (recommended, high ROI):** introduce the **application layer + ports +
  thin web + domain exceptions + transactions + cross-feature decoupling**. Keep
  JPA entities as the persistence model but *hide them behind out-ports* that return
  domain-facing types (or the entity treated as an aggregate). This removes every
  finding except F3's "entities carry JPA annotations".
- **Level B (only where it pays):** additionally split domain model from JPA entity
  (mappers both ways) for the few aggregates with real invariants (e.g. `Article`
  with slug/revision rules, `ArcBeat` with link rules). Skip it for anaemic CRUD
  types (calendars, whiteboards) where it is pure ceremony.

Adopt Level A everywhere; apply Level B selectively.

---

## 4. Concrete measures (prioritised)

| # | Measure | Addresses | Effort |
| --- | --- | --- | --- |
| **M1** | Define the per-feature ring layout (§3) and document it in an ADR ("Hexagonal layering"). Add `package-info.java` per ring. | F1,F2 | S |
| **M2** | Introduce **outbound persistence ports**: an `*RepositoryPort` interface in `application/port/out`, implemented by a persistence adapter that wraps the existing Spring Data repo. Controllers/services depend on the port, not `JpaRepository`. | F1,F3,F7 | M |
| **M3** | Introduce **application services** per feature that own the use cases; move all rule logic out of controllers (slug/sanitize/revision, campaign aggregation, link validation). | F1,F2,F9 | L |
| **M4** | Define **inbound ports** (use-case interfaces) with explicit `Command`/`Result` types; controllers map request DTO → command, call the port, map result → response DTO. | F1,F2 | M |
| **M5** | Add **domain exceptions** (`NotFoundException`, `DomainValidationException`, …) thrown by domain/application; add one `@RestControllerAdvice` that maps them to RFC 9457 `problem+json` (centralising ADR-0009). Remove `ResponseStatusException` from services and business logic. | F5 | M |
| **M6** | Put `@Transactional` on **write** application-service methods; `@Transactional(readOnly = true)` on queries/aggregations (packet, usage, export). | F6 | S |
| **M7** | **Decouple features**: each feature publishes read/lookup ports; refactor `UsageService`, `SessionPacketService`, `StatblockController` aggregation, and export to depend on published ports, not foreign repositories. | F7 | L |
| **M8** | Rework **export** into an application service that composes per-feature `Export*Port`s and returns **export DTOs** (stop serialising JPA entities; stop injecting 18 repos). | F4 | M |
| **M9** | Introduce `Clock` and `IdGenerator` ports; inject them so entity/aggregate creation is deterministic and unit-testable. | F3,F8 | S |
| **M10** | Adopt an explicit **mapping** strategy between rings (hand-written mappers, or MapStruct). No entity crosses the web boundary; no request/response type crosses into the domain. | F3,F4 | M |
| **M11** | Rebalance tests: **unit-test application services** against mocked out-ports; keep Testcontainers ITs for persistence/web adapters only. Target a healthy pyramid (many unit, some IT). | F8 | M |
| **M12** | Add **ArchUnit** tests to enforce the dependency rule (e.g. "domain must not depend on Spring/JPA", "web must not depend on persistence", "no feature depends on another feature's `adapter`/`domain`"). | F1,F7 | S |

Legend: S ≈ hours, M ≈ 1–2 days, L ≈ several days (per feature, incrementally).

---

## 5. Migration strategy (incremental strangler — no big bang)

The refactor must keep all 33 tests green at every step and ship one bounded context
at a time.

1. **Foundations (M1, M5, M6, M9, M12).** Land the package conventions, the
   `@RestControllerAdvice` + domain-exception base, `Clock`/`IdGenerator`, and the
   ArchUnit rules (initially scoped to migrated packages). One PR/ADR.
2. **Pilot one feature end-to-end** — recommend **`statblock`** (self-contained, has
   real aggregation logic in F1/F7) *or* **`wiki`** (richest domain: slug + revision
   + sanitize). Introduce ports, application service, thin controller, persistence
   adapter, unit tests. This becomes the reference implementation.
3. **Publish cross-feature ports** needed by the pilot (e.g. `statblock` needs a
   `campaign` beat-reference lookup) and flip the consumer to the port (M7).
4. **Roll out feature by feature**, simplest first (calendar, whiteboard, timeline)
   at Level A, richer ones (campaign/beats, wiki, sheet) at Level A + selective
   Level B.
5. **Refactor the orchestrators last** (`usage`, session packet, export) onto the
   now-published ports (M7, M8).
6. **Tighten ArchUnit** to cover the whole codebase once all features are migrated;
   delete the last `ResponseStatusException` from logic.

Each feature is one PR with its own ADR entry; the OpenAPI contract and external
behaviour stay unchanged (this is an internal refactor — contract-first still
holds).

---

## 6. Risks, trade-offs, non-goals

- **Over-engineering a single-user app.** Mitigate with the Level A/Level B split
  (§3): ports + application layer everywhere, hand-mapped domain models only where
  invariants justify it. Don't create empty rings for anaemic CRUD.
- **Boilerplate / mapping cost.** Real. Offset by MapStruct or terse hand mappers,
  and by the payoff in F8 (fast unit tests) and F7 (enforceable boundaries).
- **Churn vs feature work.** Strangler approach means the app keeps working and
  shipping throughout; no frozen "big refactor" branch.
- **Non-goals:** no change to the REST contract, DB schema, or Flyway history; no
  switch away from Spring Boot/JPA/Postgres; no microservices — this is a
  *modular monolith* with clean internal boundaries.

---

## 7. Definition of done (measurable)

- Controllers inject **0** repositories (only inbound ports). (today: 24/30 inject repos)
- **0** `ResponseStatusException` outside the web adapter; all domain errors flow
  through domain exceptions + the central problem+json advice. (today: 26 controllers)
- Every write use case is `@Transactional`. (today: 1 in the codebase)
- Export serialises **export DTOs**, not JPA entities; the export orchestrator
  depends on ports, not 18 repositories.
- No feature imports another feature's `domain`/`adapter`/repository — only its
  published `application.port`. (enforced by ArchUnit)
- Test mix inverts toward unit tests for business logic; ITs cover adapters. (today: 28 IT / 5 unit)

---

## 8. Suggested ADRs to accompany the work

- **ADR — Hexagonal layering within features** (supersedes/extends ADR-0001’s
  “organised by feature”): defines the domain/application/adapter rings and the
  dependency rule; names `media/MediaStorage` as the existing exemplar.
- **ADR — Domain error model & problem+json mapping** (implements ADR-0009 via a
  central `@RestControllerAdvice`; forbids `ResponseStatusException` in logic).
- **ADR — Transaction policy** (application services own boundaries; read-only for
  queries).
- **ADR — Cross-context communication via published ports** (no foreign
  repositories/entities; export/usage/packet depend on ports).
- **ADR — Architecture fitness functions with ArchUnit**.

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
