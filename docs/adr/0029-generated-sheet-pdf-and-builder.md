# ADR-0029: Generated fillable PDFs and interactive template builder

- Status: Accepted (extends ADR-0024, amends ADR-0028)
- Date: 2026-08-13

## Context
ADR-0028 exports a character sheet into a bundled system PDF (D&D 5e) and returns
`400` for any other system. But the sheet engine is schema-driven for exactly the
systems that have **no official sheet** — homebrew and niche games. Those need
two things: a way to **build a template interactively**, and a way to get a
**fillable PDF** out of an arbitrary template.

## Decision

### Interactive template builder (frontend)
The existing template CRUD already stores arbitrary `sections`/`fields` as JSONB
(ADR-0024). Add a **frontend builder** that edits that structure directly: add
sections; add fields choosing a **component type**
(`TEXT, TEXTAREA, NUMBER, BOOLEAN, SELECT`); set label, key, and (for SELECT)
options; reorder and delete. No backend change — it drives the same
`PUT /sheet-templates/{id}`.

### Generate a fillable PDF from any template (backend)
Add a **PDFBox generator** that builds a fillable AcroForm PDF from a template's
`sections`/`fields`, filled with a sheet's `values`:
- a page title (character + template name), section headings, and one widget per
  field laid out in a single column, paginating as needed;
- component → widget: `TEXT/NUMBER/SELECT` → text field, `TEXTAREA` → multiline
  text field, `BOOLEAN` → checkbox; the AcroForm field name is the template field
  `key`;
- the result stays **fillable** (not flattened).

### Amendment to ADR-0028
The PDF export endpoint no longer returns `400` for non-D&D systems. It now:
1. uses the **bundled system PDF** when one exists (`supports(system)` — today
   D&D 5e), otherwise
2. **generates** a PDF from the template schema.

So every character sheet is exportable to a fillable PDF.

## Consequences
- Homebrew/unsupported systems get a usable, editable PDF with zero per-system
  code; official systems still use their polished bundled sheet.
- Generated layout is functional, not pixel-perfect; good enough for play and
  fully editable in a PDF reader.
- Field `key`s must be unique within a template for stable PDF field names; the
  generator de-duplicates defensively, and the builder should encourage unique
  keys.
- The builder is pure frontend over existing endpoints — no new API surface.

## Alternatives considered
- **Only bundled system PDFs**: leaves homebrew users (the point of the schema
  engine) without export.
- **Server-side template builder state**: unnecessary; the template *is* the
  saved state.
