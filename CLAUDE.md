# CLAUDE.md

Guidance for working in this repository. Summarizes the major decisions and
conventions so changes stay consistent. Full rationale lives in
[`docs/adr/`](docs/adr/); requirements in [`docs/requirements.md`](docs/requirements.md).

## What this is
A **multi-account** worldbuilding + RPG campaign manager, inspired by World
Anvil. Every account's worlds/campaigns/content are strictly private to it,
including from other accounts with the ADMIN role — ADMIN only grants rights
over the account roster itself. Self-registration is open; the first account
ever registered becomes ADMIN automatically. No sharing/collaboration
features yet (see ADR-0109's "future sharing" note).

## Major decisions (see ADRs)
- **Backend:** Spring Boot 4.1 (Java 25), JUnit 6. — ADR-0001, superseded by ADR-0051
- **Backend build tool:** Gradle (Kotlin DSL), wrapper-pinned. — ADR-0056
- **Frontend:** React 18 + TypeScript, Vite. — ADR-0002
- **Datastore:** PostgreSQL; JSONB for flexible data later. — ADR-0003
- **Deployment:** Docker multi-stage images + `docker-compose.yml`. — ADR-0004
- **Scope:** multi-account; every World (and per-account catalogs) is
  strictly private, even from other ADMINs. — ADR-0005, superseded by ADR-0109
- **Auth:** self-registered accounts, BCrypt-hashed passwords, role-bearing
  stateless HS256 JWTs with DB-checked revocation
  (`token_version`). — ADR-0006, superseded by ADR-0110
- **Media:** stored on a local volume (no S3/MinIO), behind an abstraction. — ADR-0007
- **API:** contract-first; `docs/api/openapi.yaml` (OpenAPI 3.1) is canonical. — ADR-0008
- **Errors:** RFC 9457/7807 `application/problem+json`. — ADR-0009
- **Repo:** monorepo. — ADR-0010
- **Tests:** unit + Testcontainers integration; run in CI. — ADR-0011
- **Schema:** Flyway migrations; Hibernate `ddl-auto: validate`. — ADR-0012
- **Versioning:** SemVer via git tags (`vX.Y.Z`); `build.gradle.kts`/
  `package.json` versions are not authoritative. — ADR-0060

## Architecture (hexagonal, bounded-context modular monolith)
The backend is a **hexagonal, bounded-context modular monolith**; the M0–M13
migration to this shape is complete (2026-08-19). Two documents govern it and are
**binding** for any agent touching backend code:
- [`docs/architecture/architecture-harness.md`](docs/architecture/architecture-harness.md)
  — the universal, non-negotiable rule set (rings, three models + MapStruct,
  published-port integration, mandatory self-audit). Follow it exactly for **all**
  backend code.
- [`docs/architecture/clean-architecture-analysis.md`](docs/architecture/clean-architecture-analysis.md)
  — this project's context map, findings, and the completed migration plan (M0–M13).
Bounded contexts: `worldbuilding`, `campaign`, `characters`, `media`, `whiteboard`,
`interchange` (export/usage/packet orchestration), `ai` (LLM-backed text drafting,
ADR-0064/0065), `tables` (roll tables + card decks, ADR-0066), `handouts`
(player-facing printables, ADR-0070), `accounts` (self-registration, roles,
ownership; ADR-0109/0110). Each has
a full domain/
application/adapter ring; cross-context references go only through the target's
`application.port.published` interfaces — an ArchUnit fitness function
(`contextsOnlyUsePublishedPorts`) enforces this in CI. `auth`/`config`/`security`/
`shared` are generic cross-cutting infra, not bounded contexts. Do not add new logic
to a controller — put it behind a use-case port.

## Conventions
- **API changes start in the contract.** Edit `docs/api/openapi.yaml` first, then
  the backend, then regenerate frontend types (`npm run gen:api`).
- **Schema changes need a Flyway migration** in
  `backend/src/main/resources/db/migration`; never rely on Hibernate to alter
  tables.
- **Every significant decision gets an ADR.** Add a new numbered file; don't
  rewrite an accepted one — supersede it.
- **Errors** are thrown as domain exceptions (`shared.domain`: `NotFoundException`,
  `ValidationException`, ...) and mapped centrally to problem+json by
  `DomainExceptionAdvice`. `ResponseStatusException` is reserved for generic infra
  outside the bounded contexts (e.g. `auth`'s login check).
- **Backend package root:** `com.campaignorganizer`, organized by bounded context
  (`worldbuilding`, `campaign`, `characters`, `media`, `whiteboard`, `interchange`),
  plus generic infra (`auth`, `security`, `config`, `shared`).

## Build, test, run
```bash
# Whole stack
cp .env.example .env && docker compose up --build

# Backend (Gradle wrapper lives at repo root; backend is the :backend subproject)
./gradlew :backend:check           # unit + integration (needs Docker)

# Frontend
cd frontend && npm install && npm run dev
```

## Git workflow
- GitHub remote: `git@github.com:codeStev/campaign-organizer.git` (`origin`).
- **Commit granularly**, one logical change per commit.
- **Commit subject line ≤ 50 characters**, imperative mood.
- Keep unrelated changes in separate commits.
