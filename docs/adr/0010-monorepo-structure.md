# ADR-0010: Monorepo layout

- Status: Accepted
- Date: 2026-08-10

## Context
Backend, frontend, deployment config, docs, and the API contract all evolve
together and share the contract.

## Decision
Keep everything in **one repository**: `backend/`, `frontend/`, `docs/`
(including `docs/api/openapi.yaml`), `docker-compose.yml`, and CI at the root.
The frontend generates types from the sibling contract path.

## Consequences
- A single change (e.g. a contract update) can touch API, backend, and frontend
  in one atomic commit/PR.
- One CI pipeline builds and tests both apps.
- No cross-repo version coordination.

## Alternatives considered
- **Separate repos per service:** more ceremony (version pinning, contract
  publishing) than a solo project warrants.
