# ADR-0042: GitHub-style unified revision diff

- Status: Accepted
- Date: 2026-08-15
- Amends: ADR-0040 (diff article revisions)

## Context
ADR-0040 rendered the revision diff as a single flow of text with inline
word-level add/delete highlights. For real edits this was hard to read — it
looked like "two texts" rather than a diff, with no clear per-line structure.

## Decision
Render the diff as a **GitHub-style unified line diff**:

- Split each version's plain text (block tags → newlines) into lines and run a
  line-level LCS diff. Each row is context, added (`+`, green), or removed (`−`,
  red), shown in a monospace block with a gutter column.
- **Intra-line word highlight:** adjacent removed/added lines are paired and
  word-diffed, so within a changed line only the actually-changed words are
  strongly highlighted (like GitHub). Unpaired lines render as whole add/remove.
- Both diffs share one generic LCS helper, guarded with a coarse fallback for
  pathologically large inputs.

Still client-only; no backend, contract, or schema change.

## Consequences
- Differences are far easier to scan: line structure, red/green rows, and word
  emphasis together, instead of one merged paragraph.
- Line-level granularity means a change is attributed to its line; the paired
  word highlight recovers sub-line precision for the common 1:1 edit.
- Diffing remains on plain text (formatting-only changes may not show), consistent
  with ADR-0040.

## Alternatives considered
- **Split (side-by-side) view** — deferred; unified fits the article panel width
  and is the more compact default. Could be a future toggle.
- **A diff library** — still unnecessary; the LCS helper covers line and word
  diffs.
