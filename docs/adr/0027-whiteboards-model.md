# ADR-0027: Whiteboards model

- Status: Accepted
- Date: 2026-08-13

## Context
FR-20 adds free-form whiteboards for plotting: a canvas of note nodes joined by
connections. A board is edited as a whole (drag nodes, draw links) and saved.

## Decision
Model a whiteboard as a **single document**: a `whiteboards` row per board with
`name` plus **`nodes`** and **`edges`** stored as JSONB (reusing the JSONB
pattern of ADR-0024).

- **Node**: `{ id, text, x, y, color }` — `id` is a client-generated string, `x/y`
  are canvas coordinates.
- **Edge**: `{ id, fromNodeId, toNodeId, label }` referencing node ids.

The API is plain CRUD (`/worlds/{worldId}/whiteboards`); update replaces the
whole board (name + nodes + edges). The frontend owns all canvas interaction and
persists the board on save.

## Consequences
- One row per board; no join tables or per-node endpoints — simple and matches
  how a canvas is actually edited (load, mutate in memory, save).
- Referential integrity between edges and nodes is the client's responsibility
  (a dangling edge is harmless and can be pruned on render).
- Boards are self-contained and export cleanly (FR-22) as JSONB.

## Alternatives considered
- **Separate node/edge tables**: needless normalization for a document that is
  always loaded and saved whole; more endpoints and round-trips.
