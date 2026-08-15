# CLAUDE.md

Guidance for working in this repository. Summarizes the major decisions and
conventions so changes stay consistent. Full rationale lives in
[`docs/adr/`](docs/adr/); requirements in [`docs/requirements.md`](docs/requirements.md).

## What this is
A **personal, single-user** worldbuilding + RPG campaign manager, inspired by
World Anvil. Not multi-tenant, no community features.

## Major decisions (see ADRs)
- **Backend:** Spring Boot 4 (Java 21). — ADR-0001
- **Frontend:** React 18 + TypeScript, Vite. — ADR-0002
- **Datastore:** PostgreSQL; JSONB for flexible data later. — ADR-0003
- **Deployment:** Docker multi-stage images + `docker-compose.yml`. — ADR-0004
- **Scope:** single owner user; no sharing/community; spoilers via a per-article
  "GM-only" flag. — ADR-0005
- **Auth:** one configured password → stateless HS256 JWT bearer tokens. — ADR-0006
- **Media:** stored on a local volume (no S3/MinIO), behind an abstraction. — ADR-0007
- **API:** contract-first; `docs/api/openapi.yaml` (OpenAPI 3.1) is canonical. — ADR-0008
- **Errors:** RFC 9457/7807 `application/problem+json`. — ADR-0009
- **Repo:** monorepo. — ADR-0010
- **Tests:** unit + Testcontainers integration; run in CI. — ADR-0011
- **Schema:** Flyway migrations; Hibernate `ddl-auto: validate`. — ADR-0012

## Architecture (in migration)
The backend is being refactored to a **hexagonal, bounded-context modular monolith**.
Two documents govern this and are **binding** for any agent touching backend code:
- [`docs/architecture/architecture-harness.md`](docs/architecture/architecture-harness.md)
  — the universal, non-negotiable rule set (rings, three models + MapStruct,
  published-port integration, mandatory self-audit). Follow it exactly for **new or
  migrated** backend code.
- [`docs/architecture/clean-architecture-analysis.md`](docs/architecture/clean-architecture-analysis.md)
  — this project's context map, findings, and incremental migration plan (M0–M13).
Legacy code predates the harness; migrate feature-by-context per the plan, keeping
tests green. Do not add new logic to a controller — put it behind a use-case port.

## Conventions
- **API changes start in the contract.** Edit `docs/api/openapi.yaml` first, then
  the backend, then regenerate frontend types (`npm run gen:api`).
- **Schema changes need a Flyway migration** in
  `backend/src/main/resources/db/migration`; never rely on Hibernate to alter
  tables.
- **Every significant decision gets an ADR.** Add a new numbered file; don't
  rewrite an accepted one — supersede it.
- **Errors** are thrown as `ResponseStatusException` / validation errors so they
  render as problem+json.
- **Backend package root:** `com.campaignorganizer`, organized by feature
  (`auth`, `world`, `security`, `config`).

## Build, test, run
```bash
# Whole stack
cp .env.example .env && docker compose up --build

# Backend
cd backend && mvn test             # unit + integration (needs Docker)

# Frontend
cd frontend && npm install && npm run dev
```

## Git workflow
- Local git repo (no remote yet).
- **Commit granularly**, one logical change per commit.
- **Commit subject line ≤ 50 characters**, imperative mood.
- Keep unrelated changes in separate commits.
