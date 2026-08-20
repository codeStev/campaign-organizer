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
| [0018](0018-interactive-maps-model.md) | Interactive maps model | Accepted |
| [0019](0019-timelines-model.md) | Timelines model | Accepted |
| [0020](0020-fantasy-calendars-model.md) | Fantasy calendars model | Accepted |
| [0021](0021-relationship-graph-model.md) | Relationship graph model | Accepted |
| [0022](0022-fuzzy-search.md) | Fuzzy article search via trigrams (supersedes 0017 query) | Accepted |
| [0023](0023-campaign-manager-model.md) | GM campaign manager model | Accepted |
| [0024](0024-character-sheet-engine.md) | Schema-driven character sheet engine | Accepted |
| [0025](0025-html-sanitization.md) | Sanitize article HTML on write | Accepted |
| [0026](0026-article-revision-history.md) | Article revision history | Accepted |
| [0027](0027-whiteboards-model.md) | Whiteboards model | Accepted |
| [0028](0028-character-sheet-pdf-export.md) | Character sheet PDF export | Accepted |
| [0029](0029-generated-sheet-pdf-and-builder.md) | Generated fillable PDFs + template builder | Accepted |
| [0030](0030-visual-template-builder.md) | Field widths, circle trackers, drag-and-drop builder | Accepted |
| [0031](0031-per-campaign-play-content.md) | Per-campaign play content (parties, beat detail) | Accepted |
| [0032](0032-multi-article-beats-and-statblock-campaigns.md) | Multi-article beats, campaign-scoped statblocks | Accepted |
| [0033](0033-article-usage-backlinks.md) | Article usage backlinks + campaign-usage filter | Accepted |
| [0034](0034-command-palette.md) | Command palette for in-world navigation | Accepted |
| [0035](0035-print-view.md) | Print / PDF view via the browser | Accepted |
| [0036](0036-session-prep-packet.md) | Session prep packet | Accepted |
| [0037](0037-statblock-cards.md) | Printable statblock cards | Accepted |
| [0038](0038-print-in-new-tab.md) | Print views open in a separate tab | Accepted |
| [0039](0039-article-image-embedding.md) | Article image embedding — paste, drop, resize, cap | Accepted |
| [0040](0040-revision-diff.md) | Diff article revisions | Accepted |
| [0041](0041-statblock-card-selection.md) | Select specific statblocks to print | Accepted |
| [0042](0042-github-style-diff.md) | GitHub-style unified revision diff | Accepted |
| [0043](0043-beat-statblocks.md) | Reference statblocks from beats | Accepted |
| [0044](0044-map-pin-ux.md) | Map pin UX — inline editing and layer colours | Accepted |
| [0045](0045-map-pin-legend-labels.md) | Numbered pins, legend, and on-map labels | Accepted |
| [0046](0046-packet-maps.md) | Session packet includes linked maps | Accepted |
| [0047](0047-map-print.md) | Direct map printing with options | Accepted |
| [0048](0048-layer-icons.md) | Per-layer pin icons | Accepted |
| [0049](0049-persist-layer-styles.md) | Persist per-layer map styling on the world | Accepted |
| [0050](0050-bounded-contexts-and-context-map.md) | Bounded contexts and context map | Accepted |
| [0051](0051-java25-springboot41-junit6.md) | Java 25, Spring Boot 4.1, JUnit 6 (supersedes 0001 version line) | Accepted |
| [0052](0052-shared-field-templates.md) | Shared field templates for sheets and statblocks (renames SheetTemplate) | Accepted |

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
