# ADR-0040: Diff article revisions

- Status: Accepted
- Date: 2026-08-15

## Context
Article revision history (ADR-0026) let the owner list prior versions and
restore one, but not *see what changed*. The revision list endpoint already
returns each revision's `body`, so the current version and any two revisions can
be compared without new backend work.

## Decision
Add a client-only compare view in the History panel. Each version row (a
synthetic "Current version" plus every stored revision) has two radio columns,
"−" (from / older) and "+" (to / newer). Choosing two different versions renders
a `RevisionDiff`:

- HTML bodies are reduced to readable plain text (block tags become newlines,
  remaining tags stripped via `DOMParser`), then compared with a **word-level LCS
  diff**; additions and deletions are highlighted inline, and a changed title is
  shown before/after.
- The diff is bounded: above ~3M token-pairs it falls back to a coarse
  whole-old / whole-new render so a pathologically large article can't hang the
  tab.

No backend, contract, or schema change.

## Consequences
- The owner can review exactly what changed between two saves (or against the
  current text) before restoring.
- Diffing is on plain text, not rendered HTML, so formatting-only changes may not
  show; this keeps prose diffs readable, which is the common case.
- Word-level LCS is O(n·m); fine for typical articles and guarded for outliers.

## Alternatives considered
- **A diff library (jsdiff)** — avoided to keep the dependency footprint small; a
  compact LCS is enough for this use.
- **Server-side diffing** — unnecessary; revisions already carry the bodies and
  the computation is cheap on the client.
