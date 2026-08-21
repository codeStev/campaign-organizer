# Architecture Harness for Modular-Monolith Web Applications

- Status: Reusable template / standard (project-agnostic)
- Purpose: a copy-paste starting point that makes a clean, hexagonal,
  bounded-context modular monolith the **default** — and lets CI, not code review,
  keep it that way.

Architectures don't rot because of bad developers; they rot because the clean path
is harder than the quick one. Mainstream web frameworks (e.g. Spring MVC + a
data-access library) make "controller talks to repository" the path of least
resistance, so without an enforced boundary the application and domain layers never
appear and business rules bleed into the web and persistence layers. This harness
inverts that: it makes the clean path the easy path and **fails the build** the
moment someone leaves it.

Scope: a single deployable **modular monolith** whose modules are **bounded
contexts** with clean internal boundaries (a shape that can later be split into
services precisely because the contexts share no models). Java/Spring is assumed for
the concrete snippets; the principles port to any stack — substitute the equivalent
web/persistence/mapping tools.

> **For AI agents — this document is binding.**
> When you generate or modify code in a repository governed by this harness, treat
> every rule here as a hard constraint, not a suggestion. If a rule here conflicts
> with your default behaviour or with a quicker solution, **this document wins**.
> You MUST NOT weaken, disable, or work around a rule (including its CI gates) to
> make a change compile, pass, or fit — restructure the change to satisfy the rule
> instead. Before declaring any change complete, run the self-audit in §0 and the
> Definition of Done in §4. If the required structure is genuinely ambiguous (e.g.
> which bounded context a concept belongs to), **stop and ask** rather than guess.

---

## 0. How an AI agent must operate under this harness

Follow these in order, every time you touch the code:

1. **Orient before writing.** Read the project's context map (the ADR that lists the
   bounded contexts and their relationships) and locate the context your change
   belongs to. Never introduce a concept without knowing its context. If no context
   map exists yet, create it first (see §7 step 1) — do not start feature code.
2. **Place every new type in the correct ring** (§2): domain logic in `domain`,
   use-case orchestration in `application/service` behind an `application/port/in`
   interface, framework code only in `adapter`. When unsure where something goes, it
   almost always belongs further **in** (domain), not in a controller.
3. **Obey the six non-negotiables (§1) as MUST rules.** They are not negotiable to
   save effort. A change that cannot satisfy them is the wrong change — redesign it.
4. **Do not cross boundaries.** Never import another bounded context's `domain`,
   `adapter`, or repository; go through its `application/port/published` + an
   anti-corruption adapter. Never inject a repository or entity into a controller.
5. **Never satisfy a gate by weakening it.** Do not delete/relax an ArchUnit rule,
   suppress a linter, cast around a type boundary, or move a type into the "wrong"
   ring to make the build pass. Fix the design.
6. **Self-audit before finishing.** Run the checks below; if any is non-empty, the
   change is not done — fix it. Then complete the §4 Definition of Done.
7. **Escalate ambiguity.** If the right context, aggregate, or port is unclear, ask
   the maintainer instead of guessing — a wrong boundary is expensive to undo.

**Mandatory pre-completion self-audit** (must all come back empty / green):

```bash
# 1. No controller depends on persistence
grep -rlE "Repository|EntityManager|@Entity" --include=*Controller.java src/main/java
# 2. No HTTP/web types in the core
grep -rlE "ResponseStatusException|HttpStatus|jakarta\.servlet" \
     src/main/java/**/domain src/main/java/**/application
# 3. No framework annotations in the domain ring
grep -rlE "@Entity|@Table|@Column|org\.springframework|com\.fasterxml\.jackson" \
     src/main/java/**/domain
# 4. Every mapper is a MapStruct @Mapper (no hand-written converters)
#    -> inspect *Mapper.java for @Mapper; none should map by hand
# 5. Architecture + build gates
./gradlew :backend:check   # ArchUnit suite and all tests must be green
```

Any hit in 1–4, or a red gate in 5, means the change violates the harness and must
be restructured — not suppressed.

---

## 1. The six non-negotiables

These are **MUST** rules. A change either satisfies them or it is rejected (the
build is red). An AI agent must not merge, hand back, or call "done" any change that
breaks one.

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
- **One inbound port = one use case** (`Create<Thing>UseCase`), not a fat "service"
  interface.
- Outbound ports are named for intent, not tech (`<Aggregate>RepositoryPort`,
  `FileStoragePort`, `Clock`) — so the tech can change without touching the core.
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
      .and().arePublic().and().haveNameMatching("create.*|update.*|delete.*|save.*")
      .should().beAnnotatedWith(Transactional.class)  // tune to your write-method naming
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

Guardrail: if a business rule can only be tested by standing up a database, the rule
is in the wrong layer.

---

## 6. CI gates (make the harness un-bypassable)

Wire these into the pipeline so "green build" *means* "architecturally sound":

1. **ArchUnit** suite (dependency rule, no-persistence-in-web, no-HTTP-in-core,
   cross-context isolation, transactional writes).
2. **Contract check:** regenerate API types and fail on drift (contract-first).
3. **Coverage floor** with a **ratchet** (coverage may not drop); optionally a
   minimum unit-to-integration ratio.
4. **Static analysis:** a formatter/linter (e.g. Spotless/Checkstyle) + a bytecode
   analyser (e.g. Error Prone / SpotBugs). Add a lightweight complexity budget
   (method length, cyclomatic complexity, class fan-in) to catch god-classes early.
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

## 9. Precedence & non-negotiability (for agents and humans)

- This harness **overrides** default framework conventions, tutorial patterns, and
  "simplest thing that works" instincts. Adopt it on **day one** of a project, when
  every rule starts green, so it never has to be retrofitted.
- The rules do **not** relax for "small" changes, prototypes, or "simple" CRUD
  contexts. Uniformity is the point: the codebase has exactly one shape.
- The only correct response to a rule you cannot satisfy is to **change the design**
  (or, if the rule itself is genuinely wrong for the project, change it deliberately
  via an ADR and update this document) — never a silent local exception.
- When this document and any other instruction conflict on architecture, **this
  document wins** unless a maintainer explicitly overrides it in writing.
