# Campaign Organizer

A personal, self-hosted worldbuilding and RPG campaign management app — a
focused, single-user alternative to World Anvil. Backend is Spring Boot 4.1
(Java 25), frontend is React + TypeScript, and the whole thing runs as Docker
containers.

> **Status:** Phase 0 (foundations). Authentication and the `worlds` resource
> are implemented end to end. See [`docs/requirements.md`](docs/requirements.md)
> for the roadmap and [`docs/adr/`](docs/adr/) for design decisions.

## Architecture

```
frontend (React/TS, nginx)  ──/api──▶  backend (Spring Boot 4)  ──▶  Postgres
                                                │
                                                └── media on local volume
```

The API contract is design-first: [`docs/api/openapi.yaml`](docs/api/openapi.yaml)
(OpenAPI 3.1) is canonical. See [ADR-0008](docs/adr/0008-contract-first-openapi.md).

## Run the whole stack

```bash
cp .env.example .env      # then edit the secrets
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html

Sign in with the value of `APP_PASSWORD`.

## Develop

**Backend**

```bash
cd backend
mvn spring-boot:run           # needs a local Postgres, or run `docker compose up db`
mvn test                      # unit + Testcontainers integration tests (needs Docker)
```

**Frontend**

```bash
cd frontend
npm install
npm run dev                   # http://localhost:5173, proxies /api to :8080
npm run gen:api               # regenerate typed client from the OpenAPI contract
```

## Repository layout

| Path | Purpose |
| --- | --- |
| `backend/` | Spring Boot 4 API |
| `frontend/` | React + TypeScript SPA |
| `docs/api/openapi.yaml` | Canonical API contract |
| `docs/requirements.md` | Requirements and roadmap |
| `docs/adr/` | Architecture Decision Records |
| `docker-compose.yml` | Full local/self-hosted stack |
| `CLAUDE.md` | Summary of major decisions & conventions |
