# ADR-0018: Interactive maps model

- Status: Accepted
- Date: 2026-08-11

## Context
Phase 2 adds interactive maps (FR-8): a base image with pins that link to
articles and can be grouped into toggleable layers.

## Decision
- **Base image reuses the media library.** A `map` references a `media` asset
  (ADR-0016) as its image via `media_id`. No new upload path — maps use the
  existing image upload. If the image is later deleted, `media_id` is set null
  and the map survives (shown as image-missing).
- **Fractional pin coordinates.** Pins store `x` and `y` in `[0, 1]` relative to
  the image, so placement is independent of display/image resolution. The
  frontend renders with Leaflet `CRS.Simple` over the image bounds.
- **Pins optionally link to an article** (`article_id`, nullable, set null if the
  article is deleted) and carry an optional `label`.
- **Layers are a lightweight label.** Each pin has an optional `layer` string;
  the frontend groups pins by layer and toggles their visibility. No separate
  layer entity — sufficient for FR-8 without extra tables.
- Maps and pins are **world-scoped**; deleting a world cascades, deleting a map
  cascades to its pins.

## Consequences
- Reuses upload/storage/serving; the map just points at a `media` id.
- Coordinates survive image swaps and different zoom levels.
- Layer toggles need no schema beyond the string; if per-layer metadata
  (opacity, ordering, default visibility) is ever needed, promote to an entity
  in a new ADR.

## Alternatives considered
- **Pixel coordinates**: break when the image is resized or displayed scaled.
- **Geographic CRS / tiling**: unnecessary for single non-geographic images.
- **Layer as a first-class entity now**: more than FR-8 needs at this stage.
