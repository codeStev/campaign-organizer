# 70. Handout designer (FR-46)

Date: 2026-08-26
Status: Accepted

## Context

Player-facing props — a letter from a villain, a wanted poster, an in-world
newspaper page — are a staple of tabletop play. The app already prints GM
material, but nothing produces *player* handouts, and those need to look like
in-world objects rather than wiki pages. FR-46 asked for styled one-page
printables with font/border presets, deliberately separate from GM-only
content.

## Decision

A new **`handouts` bounded context** — the same anatomy as `tables`
(domain / application / adapter rings, MapStruct mappers, published ports,
ArchUnit ring rules) — with one small aggregate:

```
Handout {id, worldId, title, preset, body, createdAt, updatedAt}
Preset ∈ {PARCHMENT, NEWSPAPER, POSTER, LETTER}
```

- REST: full CRUD at `/worlds/{worldId}/handouts`, contract-first.
- Storage: plain columns (no JSONB needed); the preset is validated in the
  domain against the fixed enum so unknown styles cannot be persisted.
- The visual styles live entirely in the frontend stylesheet (one CSS block
  per preset: parchment double-rule serif, two-column justified newspaper,
  centered uppercase poster, indented italic letter). Adding a preset later
  means enum value + contract enum + CSS block — nothing else.
- Editor UX mirrors TablesView: list left, editor right, live preview in the
  chosen style below the form, and a standalone print window rendering the
  handout as its own paper page (ADR-0038 pattern).
- Handouts ship in world backup/export like every other content type
  (`handouts` key in the bundle; only the world id is remapped on import).

## Consequences

- A whole bounded context for one aggregate is deliberate: it keeps the
  ArchUnit ring guarantees uniform and gives future handout features
  (images, multi-page) a home without re-parenting.
- Presets are server-validated but *rendered* client-side; the server never
  sees pixels, only the enum and Markdown body.
- Handouts are not linked into the wiki graph (no beat references, no usage
  panel entries) — they are props, not knowledge; if linking is ever wanted
  it extends FR-25's machinery then.
