# Architecture Harness (reusable guardrails for future projects)

- Status: Reusable template / standard
- Purpose: prevent the drift documented in
  [`clean-architecture-analysis.md`](clean-architecture-analysis.md) from recurring
  — by making the right structure the default and letting CI, not code review,
  enforce it.

The root cause of the drift was not bad developers; it was the **absence of an
enforced boundary**. Spring MVC + Spring Data make "controller talks to repository"
the path of least resistance, so without a guardrail the application layer never
appears. This harness makes the clean path the easy path and fails the build when
someone leaves it.

Use this as a copy-paste starting point for new services (Java/Spring assumed;
the principles port to any stack).

---

## 1. The six non-negotiables

These are binary. A change either satisfies them or the build is red.

1. **Dependency Rule.** Dependencies point inward only:
   `adapter → application → domain`. `domain` and `application` import **no**
   framework (no Spring MVC, Hibernate, Jackson) and **no** other bounded context's
   internals.
2. **No persistence in the web layer.** A controller may inject **inbound ports
   only** — never a repository, `EntityManager`, or JPA entity.
3. **Errors are domain outcomes.** Business rules throw **domain exceptions**;
   exactly one `@RestControllerAdvice` maps them to `application/problem+json`.
   `ResponseStatusException`/HTTP status codes never appear in domain/application.
4. **Transactions live in the application layer.** Every write use case is
   `@Transactional`; queries are `@Transactional(readOnly = true)`. Adapters and
   controllers never open transactions.
5. **Three models per context, mapped by MapStruct.** Each bounded context has its
   own pure-Java **domain model** (shared across its aggregates), a JPA **entity**
   model (persistence adapter only), and a **web DTO** model (web adapter only). All
   mapping is MapStruct-generated; no type is shared across the three and none crosses
   a ring. **No CRUD exemption** — every context, however simple, has all three.
6. **Contexts integrate through published ports only.** A context depends on another
   only via its `application.port.published` interfaces, behind an anti-corruption
   adapter — never a foreign repository, entity, or domain type.

---

## 2. Standard package skeleton (package-by-**bounded-context** + rings)

The unit of modelling is the **bounded context (domain)**, not the technical
feature. Start every project with an explicit **context map** (which contexts
exist, who is upstream/downstream); each context is a top-level module with the
rings inside and its **own domain model** shared across that context's aggregates.

```
<root>.<boundedcontext>
├── domain/                 # ONE pure-Java domain model for the context (no Spring/JPA/Jackson)
│   ├── <aggregateA>/       # aggregate root, value objects, invariants, domain services
│   ├── <aggregateB>/
│   └── shared/             # cross-aggregate value objects + domain exceptions
├── application/
│   ├── port/in/            # use-case interfaces + Command/Result records
│   ├── port/out/           # repository/gateway interfaces (speak DOMAIN types)
│   ├── port/published/     # ports this context exposes to OTHER contexts
│   └── service/            # use-case implementations (@Transactional, orchestration)
└── adapter/
    ├── in/web/             # controllers (thin) + request/response DTOs + MapStruct web mapper
    ├── out/persistence/    # JPA entities + Spring Data repos + port impls + MapStruct mapper
    └── out/context/        # anti-corruption adapters implementing this context's out-ports
```

**Three distinct models, mapped only by MapStruct** (`componentModel = "spring"`):
1. **Domain** — pure Java aggregates/value objects; the only place business rules
   live. No framework annotations; IDs/timestamps from injected `IdGenerator`/`Clock`.
2. **Persistence** — JPA `@Entity` in `adapter/out/persistence` only.
3. **Web** — request/response DTOs in `adapter/in/web` only (plus Command/Result on
   in-ports). No hand-written converters; no entity or DTO ever crosses a ring.

Rules of thumb:
- **One inbound port = one use case** (`CreateArticleUseCase`), not a fat "service"
  interface.
- Outbound ports are named for intent, not tech (`ArticleRepositoryPort`,
  `MediaStoragePort`, `Clock`) — so the tech can change without touching the core.
- **Cross-context**: expose a minimal `application/port/published` port per context;
  downstream contexts consume it through an anti-corruption adapter that maps the
  read-model into their own domain. Never import another context's `domain`,
  `adapter`, or repositories.
- **No CRUD exemption.** Even anaemic CRUD contexts get the full ring set and their
  own domain model — purity is uniform, so the codebase has exactly one shape.

---

## 3. Fitness functions (ArchUnit) — copy into every project

Put this in `src/test/java/.../architecture/ArchitectureTest.java`. It fails the
build on any dependency-rule breach. Start it on day one, when there is nothing to
violate, so it never has to be retrofitted.

```java
@AnalyzeClasses(packages = "com.example", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest static final ArchRule layered = layeredArchitecture().consideringOnlyDependenciesInLayers()
      .layer("Domain").definedBy("..domain..")
      .layer("Application").definedBy("..application..")
      .layer("Adapter").definedBy("..adapter..")
      .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
      .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
      .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter");

  @ArchTest static final ArchRule domainIsFrameworkFree = noClasses().that().resideInAPackage("..domain..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..", "org.hibernate..");

  @ArchTest static final ArchRule webHasNoPersistence = noClasses().that().resideInAPackage("..adapter.in.web..")
      .should().dependOnClassesThat().resideInAnyPackage("..adapter.out.persistence..", "org.springframework.data..");

  @ArchTest static final ArchRule noHttpStatusInCore = noClasses().that().resideInAnyPackage("..domain..", "..application..")
      .should().dependOnClassesThat().haveNameMatching(".*ResponseStatusException|.*HttpStatus");

  // Bounded contexts may only touch each other through ..application.port.published..
  @ArchTest static final ArchRule contextsDontReachIntoEachOther = slices().matching("com.example.(*)..")
      .should().notDependOnEachOther()
      .ignoreDependency(alwaysTrue(), resideInAPackage("..application.port.published.."));

  @ArchTest static final ArchRule noHandWrittenMappers = noClasses().that().resideOutsideOfPackage("..generated..")
      .and().haveSimpleNameEndingWith("Mapper")
      .should().notBeAnnotatedWith(org.mapstruct.Mapper.class)  // Mapper types must be MapStruct @Mapper
      .allowEmptyShould(true);

  @ArchTest static final ArchRule writeServicesAreTransactional = methods().that()
      .areDeclaredInClassesThat().resideInAPackage("..application.service..")
      .and().arePublic().and().haveNameMatching("create.*|update.*|delete.*|save.*|.*")
      .should().beAnnotatedWith(Transactional.class)  // tune to your naming
      .allowEmptyShould(true);
}
```

Adjust package roots and the transactional predicate per project; keep the intent.

---

## 4. Definition of Done (per change)

A change is not done until:

- [ ] Behaviour lives in the **domain model**, invoked by an **application service**
      behind an **inbound port**; the controller only maps DTO ↔ command/result.
- [ ] The change touches exactly one bounded context's **own domain model**; the
      **entity** and **DTO** models stay in their adapters. All mapping is MapStruct.
- [ ] Persistence is reached through an **outbound port** that speaks domain types;
      no repository/entity in web or domain.
- [ ] Domain errors are **domain exceptions**; no `ResponseStatusException` in
      logic; problem+json comes from the central advice.
- [ ] Write path is `@Transactional`; queries are read-only.
- [ ] **Unit tests** cover the domain + use case with mocked out-ports; an
      **adapter/IT** covers persistence & web. (Unit-first — see §5.)
- [ ] Cross-context access uses a **published port** + anti-corruption adapter, not a
      foreign repo/entity/domain type.
- [ ] ArchUnit suite green; contract (OpenAPI) updated if the API changed; ADR added
      if a decision/scope changed.

Keep this list in the PR template so it is checked every time.

---

## 5. Testing policy (healthy pyramid by construction)

- **Unit (most):** application services and domain logic, out-ports mocked, no
  Spring context, no DB. Fast. This is only possible because the logic is isolated —
  which is the whole point.
- **Slice/adapter (some):** persistence adapters against Testcontainers; web
  adapters with `@WebMvcTest` + mocked in-ports.
- **End-to-end (few):** a handful of happy-path smoke tests through the full stack.

Guardrail: if a business rule can only be tested by standing up Postgres, the rule
is in the wrong layer.

---

## 6. CI gates (make the harness un-bypassable)

Wire these into the pipeline so "green build" *means* "architecturally sound":

1. **ArchUnit** suite (dependency rule, no-persistence-in-web, no-HTTP-in-core,
   cross-feature isolation, transactional writes).
2. **Contract check:** regenerate API types and fail on drift (contract-first).
3. **Coverage floor** with a **ratchet** (coverage may not drop); optionally a
   minimum unit-to-integration ratio.
4. **Static analysis:** Spotless/Checkstyle + a bytecode analyser (Error Prone /
   SpotBugs). Add a lightweight complexity budget (method length, cyclomatic
   complexity, class fan-in) to catch god-classes like the 18-repo exporter early.
5. **Dependency hygiene:** forbid banned imports (e.g. `jakarta.persistence` in
   `..domain..`) via the enforcer plugin as a second belt to ArchUnit.

---

## 7. Day-1 checklist for a new project

1. **Draw the context map first** — list the bounded contexts, their aggregates, and
   upstream/downstream relationships; record it in an ADR. This decides package roots.
2. Create the per-context ring packages (§2) and a `package-info.java` per ring.
3. Add **MapStruct** to the build (`mapstruct` + `mapstruct-processor`,
   `componentModel = "spring"`); forbid hand-written `*Mapper`s (§3).
4. Drop in the ArchUnit suite (§3) **before** the first feature — it starts green and
   stays green (incl. the no-cross-context rule).
5. Add the central `@RestControllerAdvice` + `DomainException` hierarchy and the
   problem+json mapping.
6. Add `Clock` and `IdGenerator` ports; forbid `Instant.now()`/`UUID.randomUUID()`
   inside domain via a banned-import rule.
7. Scaffold **one** vertical slice in one context (domain aggregate, in-port, service,
   out-port, persistence adapter + entity + MapStruct mappers, unit test) as the
   copy-me reference — with its own domain model.
8. Add the PR template with the §4 Definition of Done.
9. Turn on the CI gates (§6). Only now start feature work.

---

## 8. Anti-patterns this harness bans (with the tell-tale grep)

| Anti-pattern | How to detect | Correct form |
| --- | --- | --- |
| Controller → repository | `grep -rl Repository --include=*Controller.java` | Controller → inbound port → service → out-port |
| HTTP status in logic | `grep -rl ResponseStatusException` in `domain`/`application` | Domain exception + central advice |
| Entity used as domain model | `@Entity` referenced in `domain`/`application` | Separate pure-Java domain model + MapStruct |
| Entity/DTO as API/serialization model | entity or DTO type referenced across a ring | Three models; MapStruct between them |
| Hand-written mapper | `*Mapper` class not annotated `@Mapper` | MapStruct `@Mapper(componentModel="spring")` |
| God orchestrator | class with many injected repos / high fan-in | Application service composing published ports |
| No transaction boundary | `grep -rln @Transactional` ≈ 0 | `@Transactional` on write use cases |
| Context reaching into context | ArchUnit `slices().notDependOnEachOther` | Depend on the other context's published port + ACL |

---

### Relationship to this repo

The campaign-organizer backend predates this harness; the companion
[`clean-architecture-analysis.md`](clean-architecture-analysis.md) is the concrete
remediation plan that brings it up to this standard incrementally. Future projects
should adopt this harness on **day one** so remediation is never needed.
