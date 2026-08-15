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

## 1. The five non-negotiables

These are binary. A change either satisfies them or the build is red.

1. **Dependency Rule.** Dependencies point inward only:
   `adapter → application → domain`. `domain` and `application` import **no**
   framework (no Spring MVC, Hibernate, Jackson) and **no** other feature's internals.
2. **No persistence in the web layer.** A controller may inject **inbound ports
   only** — never a repository, `EntityManager`, or JPA entity.
3. **Errors are domain outcomes.** Business rules throw **domain exceptions**;
   exactly one `@RestControllerAdvice` maps them to `application/problem+json`.
   `ResponseStatusException`/HTTP status codes never appear in domain/application.
4. **Transactions live in the application layer.** Every write use case is
   `@Transactional`; queries are `@Transactional(readOnly = true)`. Adapters and
   controllers never open transactions.
5. **Boundaries are typed.** Web DTOs never enter the domain; domain/JPA entities
   never leave the application layer. Cross-feature calls go through a **published
   port**, never a foreign repository or entity.

---

## 2. Standard package skeleton (package-by-feature + rings)

```
<root>.<feature>
├── domain/                 # entities, value objects, domain services, domain exceptions
├── application/
│   ├── port/in/            # use-case interfaces + Command/Result records
│   ├── port/out/           # repository/gateway interfaces the app needs
│   └── service/            # use-case implementations (@Transactional, orchestration)
└── adapter/
    ├── in/web/             # controllers (thin) + request/response DTOs + mappers
    └── out/persistence/    # JPA entities + Spring Data repos + port implementations + mappers
```

Rules of thumb:
- **One inbound port = one use case** (`CreateArticleUseCase`), not a fat "service"
  interface.
- Outbound ports are named for intent, not tech (`ArticleRepositoryPort`,
  `MediaStoragePort`, `Clock`) — so the tech can change without touching the core.
- **Cross-context**: expose a minimal published port per feature (e.g.
  `ArticleLookupPort`) in `application/port` and let other features depend on that.
- Anaemic CRUD features may keep a single application service and skip a separate
  domain model — but they still keep the rings and ports.

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

  @ArchTest static final ArchRule featuresDontReachIntoEachOther = slices().matching("com.example.(*)..")
      .should().notDependOnEachOther()  // relax to allow ..application.port.. only, per project
      .ignoreDependency(alwaysTrue(), resideInAPackage("..application.port.."));

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

- [ ] New behaviour lives in an **application service** behind an **inbound port**;
      the controller only maps DTO ↔ command/result.
- [ ] Persistence is reached through an **outbound port**; no repository/entity in
      web or domain.
- [ ] Domain errors are **domain exceptions**; no `ResponseStatusException` in
      logic; problem+json comes from the central advice.
- [ ] Write path is `@Transactional`; queries are read-only.
- [ ] **Unit tests** cover the use case with mocked out-ports; an **adapter/IT**
      covers persistence & web. (Unit-first — see §5.)
- [ ] Cross-feature access uses a **published port**, not a foreign repo/entity.
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

1. Create the ring packages (§2) and a `package-info.java` documenting each ring.
2. Drop in the ArchUnit suite (§3) **before** the first feature — it starts green
   and stays green.
3. Add the central `@RestControllerAdvice` + `DomainException` hierarchy and the
   problem+json mapping.
4. Add `Clock` and `IdGenerator` ports; forbid `Instant.now()`/`UUID.randomUUID()`
   inside domain via a banned-import rule.
5. Scaffold **one** vertical slice (one use case, in-port, service, out-port,
   persistence adapter, unit test) as the copy-me reference.
6. Add the PR template with the §4 Definition of Done.
7. Turn on the CI gates (§6). Only now start feature work.

---

## 8. Anti-patterns this harness bans (with the tell-tale grep)

| Anti-pattern | How to detect | Correct form |
| --- | --- | --- |
| Controller → repository | `grep -rl Repository --include=*Controller.java` | Controller → inbound port → service → out-port |
| HTTP status in logic | `grep -rl ResponseStatusException` in `domain`/`application` | Domain exception + central advice |
| Entity as API/serialization model | entity type referenced in `adapter.in.web` or export | Explicit DTO + mapper |
| God orchestrator | class with many injected repos / high fan-in | Application service composing published ports |
| No transaction boundary | `grep -rln @Transactional` ≈ 0 | `@Transactional` on write use cases |
| Feature reaching into feature | ArchUnit `slices().notDependOnEachOther` | Depend on the other feature's published port |

---

### Relationship to this repo

The campaign-organizer backend predates this harness; the companion
[`clean-architecture-analysis.md`](clean-architecture-analysis.md) is the concrete
remediation plan that brings it up to this standard incrementally. Future projects
should adopt this harness on **day one** so remediation is never needed.
