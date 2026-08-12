# ADR-0028: Character sheet PDF export

- Status: Accepted
- Date: 2026-08-13

## Context
The user wants to export a character sheet into a **fillable PDF** — for D&D 5e,
the standard Wizards of the Coast fillable character sheet. Sheets are
schema-driven (ADR-0024) with system-agnostic `values`; the PDF is a fixed
AcroForm with specific field names.

## Decision
Provide a **system-specific PDF export** for sheets whose template `system` is
`dnd5e`:

- **Bundle the fillable PDF** as a classpath resource
  (`resources/pdf/dnd5e-character-sheet.pdf`).
- Use **Apache PDFBox** to load the form, set AcroForm field values from the
  sheet, and stream the result as a download. The form is left **fillable** (not
  flattened) so the player can keep editing in a PDF reader.
- A **per-system field mapping** translates our template field keys
  (`class`, `level`, `race`, `alignment`, `ac`, `hp`, `speed`, `str`…`cha`,
  `features`, `equipment`) to the WotC form's field names (including its quirks:
  `'Race '`, `'DEXmod '`, `'CHamod'`). Ability **modifiers are computed**
  (`floor((score-10)/2)`, signed) and filled into the `*mod` fields.
- Endpoint: `GET /worlds/{worldId}/character-sheets/{sheetId}/pdf`. If the
  sheet's system has no PDF mapping, respond `400`.

## Consequences
- One clean download of a table-ready sheet; the PDF stays editable.
- The mapping is **per system**; other systems return 400 until a template +
  mapping is added. The design (a `supports(system)` check + a mapping method)
  makes adding systems straightforward.
- The bundled WotC PDF is used for personal, single-user purposes; it is not
  redistributed as a product.
- Missing/renamed template keys simply leave the corresponding PDF fields blank —
  the fill is best-effort and never fails on unknown keys.

## Alternatives considered
- **Generate a PDF from scratch** (no template): far more work and would not be
  the familiar official sheet the user asked for.
- **Flatten the output**: rejected — keeping it fillable is more useful at the
  table.
