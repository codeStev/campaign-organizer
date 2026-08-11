# Architecture Decision Records

Each ADR captures one significant decision, its context, and its consequences.
Format is a lightweight [MADR](https://adr.github.io/madr/) variant. ADRs are
immutable once **Accepted**; to change a decision, add a new ADR that supersedes
the old one.

| ADR | Decision | Status |
| --- | --- | --- |
| [0001](0001-backend-spring-boot.md) | Spring Boot 4 (Java 21) backend | Accepted |
| [0002](0002-frontend-react-typescript.md) | React + TypeScript (Vite) frontend | Accepted |
| [0003](0003-postgresql-datastore.md) | PostgreSQL as the datastore | Accepted |
| [0004](0004-docker-compose-deployment.md) | Docker + Compose deployment | Accepted |
| [0005](0005-single-user-scope.md) | Single-user, no community features | Accepted |
| [0006](0006-single-password-auth.md) | Single-password JWT authentication | Accepted |
| [0007](0007-local-media-storage.md) | Local-volume media storage | Accepted |
| [0008](0008-contract-first-openapi.md) | Contract-first OpenAPI 3.1 | Accepted |
| [0009](0009-rfc7807-error-format.md) | RFC 9457/7807 error responses | Accepted |
| [0010](0010-monorepo-structure.md) | Monorepo layout | Accepted |
| [0011](0011-testing-strategy.md) | Testing strategy (unit + Testcontainers) | Accepted |
| [0012](0012-flyway-migrations.md) | Flyway schema migrations | Accepted |
| [0013](0013-article-content-model.md) | Article content model (HTML, slugs, templates) | Accepted |
| [0014](0014-wiki-auto-linking.md) | Wiki auto-linking via `[[target]]` | Accepted |
| [0015](0015-article-template-prompts.md) | Article template prompts as backend metadata | Accepted |
| [0016](0016-media-storage-and-serving.md) | Media storage abstraction and image serving | Accepted |
| [0017](0017-postgres-full-text-search.md) | Full-text search via a generated tsvector | Accepted |

## Template

```md
# ADR-XXXX: <title>

- Status: Proposed | Accepted | Superseded by ADR-YYYY
- Date: YYYY-MM-DD

## Context
<forces at play>

## Decision
<what we chose>

## Consequences
<positive and negative results>

## Alternatives considered
<what else, and why not>
```
