# ADR-0024: Schema-driven character sheet engine

- Status: Accepted
- Date: 2026-08-12

## Context
Phase 4 (FR-16, FR-17) needs character sheets that support many game systems
(D&D 5e, Pathfinder, …) without hardcoding each one. World Anvil supports 100+
systems; a personal tool needs the same flexibility without per-system code.

## Decision
A **template + instance** model with JSON-defined fields:

- **SheetTemplate** — world-scoped. Carries a `system` label and a
  **`sections`** definition: an ordered list of sections, each with an ordered
  list of fields `{ key, label, type }` where `type ∈ {TEXT, TEXTAREA, NUMBER,
  BOOLEAN, SELECT}` (SELECT adds `options`). The frontend renders a form purely
  from this definition — no per-system UI code.
- **CharacterSheet** — world-scoped instance referencing a template, with a
  `name`, an optional linked article (the character's wiki page), and a
  **`values`** map keyed by the template's field `key`s.

**Flexible data uses PostgreSQL JSONB** (fulfilling ADR-0003). `sections` and
`values` are mapped with Hibernate's `@JdbcTypeCode(SqlTypes.JSON)` to Java
collections (`List<Section>` / `Map<String,Object>`), stored in `jsonb` columns.
No per-field columns, so adding a system is data, not a migration.

**Starter systems** ship as a code-defined **built-in catalog**
(`GET /sheet-templates/builtin`, following the ArticleTemplates pattern of
ADR-0015). The user copies a starter into a world as an editable SheetTemplate;
they can also build templates from scratch. This satisfies FR-17 without
seeding rows into every world.

## Consequences
- One engine renders any system; new systems are JSON, not code or migrations.
- JSONB keeps the schema open; the DB enforces only well-formed JSON, so
  value/type validation is best-effort in the app and the client.
- Introduces the first JSONB mapping in the codebase — verified with a
  round-trip test before building outward (Boot 4 / Hibernate 7 / Jackson 3).
- Templates are world-scoped copies, so editing a template does not retroactively
  break sheets built from an earlier version (values are keyed, not positional).

## Alternatives considered
- **A table column per stat**: impossible across many systems.
- **Built-in templates only (no custom)**: too rigid; users want house systems.
- **EAV tables for values**: heavier and clumsier than JSONB for a document-shaped
  sheet.
