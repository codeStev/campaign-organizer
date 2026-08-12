# ADR-0030: Field widths, circle trackers, and drag-and-drop builder

- Status: Accepted (extends ADR-0024 and ADR-0029)
- Date: 2026-08-13

## Context
The template builder (ADR-0029) lays fields out in a single column and adds them
via buttons. The user wants a richer builder: **drag-and-drop** components,
fields **placed next to each other**, and a new **circle-list** component
(n fillable circles) for tracking things (resources, wounds, …) as some systems
use.

## Decision

### Layout by width (not free x/y)
Fields stay an **ordered list**; each gains an optional **`width`**
(`FULL | HALF | THIRD | QUARTER`). Fields flow into a 12-column grid
(spans 12/6/4/3) and wrap, so setting two fields to `HALF` places them side by
side. Order + width fully determine layout.

Rationale: an ordered-list-with-width model stays responsive on mobile (grid
reflows), round-trips to a paginating PDF, and keeps drag-and-drop as a simple
list reorder — whereas free x/y coordinates would not reflow and are painful to
lay out in a PDF.

### New component: CIRCLES
Add field type **`CIRCLES`** with an integer **`count`**. Its value is the
number of filled circles (`0..count`). Rendered as clickable pips in the sheet
form (click to fill up to that pip) and as a row of `count` checkboxes in the
PDF (first `value` checked), with field names `key_1 … key_count`.

### Drag-and-drop builder (frontend)
The builder gets a **palette** of component types; dragging one into a section
adds a field of that type. Existing field cards are **draggable to reorder**.
A per-field **width** selector controls side-by-side placement, and CIRCLES
fields expose a **count**. Up/down arrows are kept as a **touch fallback** (HTML5
drag-and-drop is pointer-only). Uses native HTML5 DnD — no new dependency.

### Schema/compat
`SheetField` gains `width` (string) and `count` (integer), both nullable;
`SheetFieldType` gains `CIRCLES`. Existing templates deserialize with
`width=null` (treated as `FULL`) and `count=null` — fully backward compatible.

## Consequences
- Two-/three-column sheets and pip trackers, authored by dragging — no new API
  surface (still the template `sections` JSONB).
- The PDF generator gains row-packing and circle rendering.
- Native DnD does not work on touch; the arrow controls remain for that.
- `width` is a free string in the contract (enum-documented); unknown values
  fall back to `FULL`, keeping the model forgiving.

## Alternatives considered
- **Free x/y canvas**: best "designer" feel but breaks responsive reflow and
  PDF layout; rejected.
- **A DnD library (dnd-kit)**: nicer ergonomics but ~40 KB added to an already
  large bundle; native DnD suffices for a desktop authoring task.
