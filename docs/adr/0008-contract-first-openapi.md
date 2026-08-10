# ADR-0008: Contract-first OpenAPI 3.1

- Status: Accepted
- Date: 2026-08-10

## Context
The user requires the API to use the current OpenAPI specification. We want the
frontend and backend to agree on one contract and to minimize drift.

## Decision
Adopt a **design-first** approach. [`docs/api/openapi.yaml`](../api/openapi.yaml)
(OpenAPI **3.1.0**) is the **canonical contract**. The frontend generates its
types from it (`openapi-typescript`). The backend serves a live Swagger UI via
springdoc-openapi (3.x, Boot 4 compatible) that mirrors the contract for manual
exploration.

## Consequences
- One human-readable source of truth for the API, reviewable in diffs.
- Frontend types can be regenerated, cutting client/server mismatch.
- Backend and the hand-authored contract can drift; a future CI check should
  diff springdoc's generated spec against the committed YAML.

## Alternatives considered
- **Code-first only (generate spec from annotations):** the spec becomes a
  build artifact, harder to review and to drive the frontend from deliberately.
- **Full server-stub generation:** heavier tooling than a personal project needs
  right now.
