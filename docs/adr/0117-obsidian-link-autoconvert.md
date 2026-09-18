# ADR-0117: Auto-convert pasted Obsidian links to wiki-links

- Status: Accepted
- Date: 2026-09-18

## Context
The user is migrating existing Obsidian vault content into this app,
article by article, by pasting from Obsidian's editor. A pasted Obsidian
internal link (`[[Note]]` in Obsidian's own source) arrives as clipboard
HTML — confirmed against a live paste: `<a class="md-link" target="_blank"
rel="noopener noreferrer nofollow" href="app://obsidian.md/<url-encoded
path>">Label</a>`. Tiptap's stock `Link` extension parses this into an
ordinary link mark (ADR-0116 already made the editor's `isAllowedUri`
permissive enough to accept this href at all). Left as-is, this is a real
but functionally dead link in this app, and — unlike a plain-text mention —
it will never surface in the ADR-0116 auto-link scan, since that scan only
looks at plain, unlinked text; a link, even to nowhere useful here, doesn't
count as one of its candidates. Across a whole-vault migration this adds up
to many links that silently never become real cross-references unless
manually redone by hand.

## Decision
`MarkdownEditor` recognizes an Obsidian-internal href (`app://obsidian.md/`
or `obsidian://` prefix) on paste and, if the link's visible text exactly
matches a known article title in the current world (case-insensitive),
automatically converts it from a `Link` mark to a `WikiLink` mark pointing
at that article — no confirmation prompt, no manual step. A link whose text
doesn't match any article is left untouched (nothing to point it at yet).

- **Automatic, not reviewed** — deliberately different from the auto-link
  scan's human-in-the-loop design (ADR-0116). The scan converts *prose*,
  where getting it wrong risks rewriting the author's meaning; this
  converts a link that already unambiguously named one specific note and
  already wasn't doing anything useful in this app, so there's nothing to
  weigh — either the title matches or it doesn't.
- **Exact title match only, no aliases** — the world's article titles are
  fetched once per world (and refetched on the same cross-component
  refresh signal `CampaignNavTree`/`dataRefresh.ts` already uses, since a
  long migration session plausibly creates new target articles partway
  through). Matching against aliases too is a reasonable future extension,
  left out here to keep the fetch to one cheap list call instead of also
  needing every article's alias set.
- **Labeled form, not unlabeled** — `[[Target|pasted text]]`, same reasoning
  as the auto-link scan: an unlabeled wiki-link's saved Markdown re-uses
  whatever text it currently wraps as both target-lookup and label
  (`WikiLink.renderMarkdown`), so only the labeled form actually pins the
  link to the canonical title while preserving the pasted text's own
  casing as what's shown.
- **Runs after paste, not during** — scheduled via `setTimeout(..., 0)`
  after the default paste completes and Tiptap/ProseMirror have already
  inserted the link mark, then walks the whole document for any
  not-yet-converted Obsidian links. Simpler than trying to intercept
  clipboard HTML before ProseMirror's own parsing.

## Consequences
- `MarkdownEditor` needs the world's article list to do this, so it must
  fetch it itself when `worldId` is provided — one extra request per world
  per editor mount (and per refresh signal), not per paste.
- A toast (`Converted N Obsidian link(s) to wiki-link(s)`) is the only
  feedback; there's no undo affordance specific to this beyond the
  editor's normal undo stack.
- Only the confirmed `app://obsidian.md/` href shape is exercised against
  real data; `obsidian://` is included defensively for other Obsidian
  export paths but unverified.

## Alternatives considered
- **Fold into the ADR-0116 auto-link scan as another candidate type** —
  rejected: the scan's whole design is per-occurrence human review with a
  name-form picker, which is the wrong shape for "this is unambiguously
  already a link to exactly one note, just point it here" — there's no
  meaningful choice to review.
- **Require manual confirmation per link** — rejected as unnecessary
  friction for a whole-vault migration: the match is already exact and
  unambiguous (a title match or none), and the editor's own undo handles
  the rare case of an unwanted conversion.
