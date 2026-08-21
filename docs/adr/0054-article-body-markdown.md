# ADR-0054: Article body stored as Markdown, rendered with a live-preview editor

- Status: Accepted
- Date: 2026-08-21
- Supersedes: the body-format choice in ADR-0013; the write-time sanitization
  timing in ADR-0025

## Context
Articles are authored through a TipTap WYSIWYG editor that reads/writes HTML
directly (`RichTextEditor.tsx`); every other large free-text field in the app
(statblock notes, arc-beat body, session summary/notes, campaign GM notes,
character-sheet/statblock template `TEXTAREA` fields) is a plain `<textarea>`
with no formatting at all. None of it supports Markdown, and the owner's
other tooling (Obsidian) is markdown-native — ADR-0013 considered Markdown for
articles and rejected it at the time ("adds a render step and diverges from
TipTap's native HTML output"). That tradeoff no longer holds: the owner wants
a real markdown editor everywhere, with a live-preview feel.

`bodyHtml` was never stored — `ArticleRenderer.renderBody` already computes it
fresh on every GET (resolving `[[wiki-links]]` via `WikiLinker`), and every
consumer of an article's rendered content (the read pane, print view, session
packet, the export bundle's raw `body` aside) already goes through that one
derived field or the raw `body` alone. That made converting the canonical
format tractable: only what *produces* `bodyHtml` needs to change, not its
consumers.

## Decision
**`Article.body` becomes Markdown**, stored as submitted — no more write-time
HTML sanitization (`ArticleService` no longer calls `HtmlSanitizer`).
`ArticleRenderer.renderBody` becomes a three-step pipeline, in this specific
order:

1. **`WikiLinker.render`** on the raw markdown text (unchanged code) —
   resolves `[[Target]]`/`[[Target|label]]` into `<a>`/`<span>` HTML while the
   body is still plain text, before any markdown tokenization can touch link
   labels. (Running markdown rendering first was considered and rejected: a
   label containing markdown metacharacters, e.g. `[[Goblin|the *elite*
   goblins]]`, would already contain injected `<em>` tags by the time
   `WikiLinker`'s regex matches across them, and its label-escaping step would
   then turn those tags into visible literal text.)
2. **Markdown → HTML** via a new `MarkdownRenderer` (flexmark-java, CommonMark
   + GFM tables/strikethrough/task-lists/autolink). CommonMark's raw-HTML
   passthrough carries the `<a>`/`<span>` from step 1 straight through
   unescaped, and is also what keeps a historically-authored `<img src
   width="…">` rendering sized (see image handling below).
3. **`HtmlSanitizer.sanitize`** (same OWASP policy as before, unchanged) — now
   called here instead of at write time.

Sanitizing at read time instead of write time reverses ADR-0025's stated
preference ("sanitize on render: repeated work per read and easy to forget at
a new render site"). It's justified here because the read-time render step
already runs on every GET (for wiki-links), so this isn't new repeated work,
and it moves enforcement to the actual security boundary: `bodyHtml` is the
only thing ever fed to `dangerouslySetInnerHTML` anywhere in the codebase —
raw `body` (including in the export bundle) is never rendered as HTML by
anything.

**Editor:** Milkdown (`@milkdown/react` + `@milkdown/kit` — core + commonmark
+ gfm presets + history/clipboard/listener/upload/cursor/slash plugins, pure
ProseMirror/remark). Milkdown's native get/set format is real Markdown text,
and its editing feel is WYSIWYG-as-you-type (headers/bold/lists render styled
immediately), which is what "live preview" means in practice here.
Deliberately **not** `@milkdown/crepe` (Milkdown's prebuilt all-batteries
editor) — it pulls in Vue, KaTeX, and full CodeMirror as real dependencies,
which is inappropriate cross-framework weight for a React-only app when we
only need the commonmark/gfm editing surface.

**Image resize (S/M/L/Full) is cut, not preserved.** CommonMark has no native
width syntax; TipTap's custom `SizedImage` node doesn't have a Milkdown
equivalent we're building. The new editor supports standard image paste/
upload (`![alt](url)`) but not per-image width control — images fall back to
the existing CSS `max-width:100%` cap. Historically-authored sized images
(`<img width="…">`, preserved as raw HTML through the migration) keep
rendering at their chosen width on read, via the same raw-HTML passthrough
that step 2 relies on — only the *authoring* UI for new sized images is gone.

**The other five fields** (statblock notes, beat body, session summary/notes,
campaign notes, template `TEXTAREA` values) need no backend or schema change
— they're already plain `TEXT`/JSONB with no format assumption baked in.
They get the same `MarkdownEditor` component for authoring, but render
client-side via `markdown-it` (`html: false`, `breaks: true`) rather than the
server pipeline above: no wiki-linking, no image embedding, and no shared
`bodyHtml`-style API contract to preserve, so the heavier server-side story
isn't justified. `breaks: true` specifically preserves the existing single-
Enter-is-a-line-break behavior these fields have always had under CSS
`white-space: pre-wrap`, so already-written multi-line notes don't reflow
into a run-on paragraph the moment rendering changes.

**Data migration:** the 17 existing real articles (and their revision
history) are converted from HTML to Markdown by a one-time script
(`scripts/migrate-articles-html-to-markdown.mjs`, `turndown`), run directly
against Postgres with a full backup taken first — not a Flyway migration
(the column stays `TEXT`; only the semantic content changes, and a robust
HTML→Markdown conversion isn't expressible in SQL).

## Consequences
- `docs/api/openapi.yaml`'s `body`/`bodyHtml` descriptions change from "HTML"
  to "Markdown" / "rendered from Markdown" wording; no schema shape changes.
- `RevisionDiff.tsx`'s HTML-to-plain-text step (`DOMParser`-based) is replaced
  with a markdown-safe version — the old one would otherwise mis-parse literal
  `<`/`>` in ordinary prose (e.g. "AC < 15") as HTML tags and drop it.
- `V4__article_search_vector.sql`'s tag-stripping regex still correctly strips
  any raw `<img>` HTML left in bodies, but doesn't strip markdown link/image
  URL syntax (`[label](url)`) — accepted as minor search-index noise for a
  single-user app rather than adding a fix migration.
- One engine (Milkdown) and one rendering story per tier (flexmark
  server-side for articles, markdown-it client-side for everything else)
  instead of the TipTap/HTML system plus five unformatted textareas.

## Alternatives considered
- **Keep TipTap/HTML for articles, add Markdown only elsewhere**: smaller,
  safer, no migration risk — rejected because the owner explicitly asked for
  articles to convert too, matching their markdown-native Obsidian workflow
  and setting up FR-23 (Obsidian import/export) to work with a matching
  format on both sides instead of needing HTML↔Markdown conversion at every
  sync.
- **`@milkdown/crepe`** (batteries-included editor): rejected for its Vue/
  KaTeX/CodeMirror weight, see Decision.
- **Preserve image resize via a custom Milkdown node** (remark-level
  attribute syntax or forced raw-HTML image serialization): possible future
  work, cut for this iteration given the added parser/serializer complexity
  for a minor nicety.
- **Sanitize raw markdown at write time too** (defense in depth, matching the
  old timing): rejected — running an HTML sanitizer over non-HTML markdown
  text has real potential to mangle ordinary prose (e.g. escaping a literal
  `<` used in comparison text), and the read-time boundary is already
  complete and sufficient (see Decision).
