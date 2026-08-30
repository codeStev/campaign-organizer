# ADR-0076: Replace Milkdown with Tiptap; full formatting toolbar

- Status: Accepted
- Date: 2026-08-30

## Context

`MarkdownEditor` (shared by all 8 markdown fields across the app: article
bodies, GM notes, session summaries/notes, arc beats, statblock notes,
generic template fields) exposed only 4 formatting commands (Bold, Italic,
a single fixed H2, Bullet list) on top of Milkdown/ProseMirror. The owner
wants a fully-fledged editor — no markdown syntax knowledge required,
everything reachable through the toolbar.

Building this out on Milkdown hit real friction: no ready-made React
component exists for a link-editing popover, table-editing UI, or
task-list checkboxes — the only "batteries included" shell
(`@milkdown/crepe`) renders internally via **Vue**, architecturally
incompatible with this app's `@milkdown/react` integration, and task-list
checkboxes would have needed a hand-written ProseMirror NodeView from
scratch (no preset command/component existed at all).

## Decision: Tiptap replaces Milkdown

Two research passes evaluated Tiptap, Lexical, MDXEditor, and BlockNote
against staying on Milkdown, with markdown round-trip fidelity as the
deciding axis (the backend stores plain markdown text; ~9 print/preview
views share one `markdown-it` renderer, `frontend/src/lib/markdown.ts`).

- **BlockNote**: disqualified outright — its own docs call markdown export
  "lossy" and it wants to own persistence as its own block JSON, which would
  mean changing the app's storage format everywhere, not just swapping an
  editor.
- **Lexical**: best raw bundle size, official markdown package — but a
  Lexical maintainer confirmed on GitHub that no official, maintained
  toolbar exists. Same problem as Milkdown, different engine.
- **MDXEditor**: most complete out of the box, but one source put it at
  ~851kB gzip (unverified but never disproven), a real risk given this
  app's build already warns about an oversized main chunk.
- **Tiptap**: same ProseMirror engine as Milkdown (no downgrade in editing
  maturity), a real official React integration (`@tiptap/react`), an
  official bidirectional markdown extension (`@tiptap/markdown`), and a
  free/MIT UI reference ("Simple Editor" template) — the thing Milkdown
  never had. Ecosystem roughly 49x Milkdown's by download volume.

Removing `@milkdown/kit`/`@milkdown/react` also removed 192 transitive
packages (Vue/Crepe among them); the added Tiptap packages brought back 55.
Net bundle effect: +26kB gzip on the main chunk — a modest cost for
materially more editor functionality (tables, task lists, a proper image
node, markdown extension) than the app had before, and the removal of a
second UI framework's runtime footprint as a standing risk.

## Decision: a custom wiki-link mark, not plain text

**A real, confirmed bug was found during migration**, not a hypothetical:
`@tiptap/markdown`'s serializer unconditionally backslash-escapes every
`[`/`]`/`*`/`_`/`` ` ``/`~` in plain text nodes (`escapeMarkdownSyntax`, no
config knob to disable it). This app layers its own `[[Article Title]]` /
`[[Article Title|Display Text]]` wiki-link syntax on top of standard
markdown (a client-side regex post-pass in `lib/markdown.ts`, plus a
backend equivalent, ADR-0014) — under Milkdown this text simply survived
editing untouched, but under Tiptap's markdown extension, saving a wiki-
link would come back out as `\[\[Article Title\]\]`, corrupting it on every
edit pass.

Fix: `frontend/src/lib/wikiLinkExtension.ts` gives wiki-links their own
Tiptap `Mark`, using `markdownTokenizer`/`parseMarkdown`/`renderMarkdown` —
native, documented `MarkConfig` fields (not a hack; `@tiptap/core` ships
first-class types for all three: `MarkdownTokenizer`, `MarkdownParseHelpers`,
`MarkdownRendererHelpers`). Once tokenized, the target text is a `wikiLink`
mark, not a plain "text" node, so it never enters the generic escaping
path. Styled distinctly in the editor (`.wiki-link-editor`, dashed
underline) but deliberately **not** click-navigable there — resolving a
title to an actual article is what the separate read-only renderer's
`lookup` callback already does, and `MarkdownEditor` has no access to the
live article list. Considered alternatives: placeholder substitution around
Tiptap's parse/serialize calls (rejected — hacky, no in-editor styling,
fragile on nested-bracket edge cases) and accepting escaped brackets on
disk (rejected — two valid on-disk forms for the same content depending on
which editor last touched it).

## Decision: hand-wire commands into this app's own shadcn components

Tiptap's official "Simple Editor" template bundles its own non-shadcn UI
primitives (its own button/toolbar/spacer components). Not adopted — same
approach already established this session for `AiDraftDialog`/
`LinkPopover`/the `window.prompt` sweep: reuse the template only as a
reference for which commands to wire and a sane toolbar layout, hand-wire
Tiptap's commands (`editor.chain().focus().toggleBold().run()`, etc.) into
this app's own shadcn `Button`/`Toggle`/`Select`/`Popover`/`Tooltip`,
keeping one consistent design system rather than two.

Toolbar groups (separated by shadcn `Separator`, icon buttons via
`lucide-react`, each in a `Tooltip`, `.editor-toolbar { flex-wrap: wrap; }`
so it degrades to a second row in narrow contexts like the arc-beat inline
editor): Undo/Redo · a text-style `Select` (Paragraph/H1/H2/H3) · inline
marks (Bold/Italic/Strikethrough/Inline code) · block types (Bullet/
Ordered/Task list/Blockquote/Code block) · insert (Link via `LinkPopover`/
Image/Horizontal rule/Table) · table row/column editing (shown only when
the cursor is inside a table) · AI draft (`AiDraftDialog`).
`AiDraftDialog.tsx` and `LinkPopover.tsx` are fully editor-agnostic
(callback props only) and needed no changes migrating from Milkdown.

Task-list checkboxes needed **no custom NodeView** — unlike Milkdown,
`@tiptap/extension-task-item` ships a complete, interactive, click-to-toggle
checkbox out of the box. `markdown-it-task-lists` (small, dependency-free)
was added to the shared preview renderer so the same checkboxes render
(disabled) in every print/preview view too.

## Fix: the HTML sanitizer's allowlist predates GFM tables/task-lists/code

Manual testing after shipping the toolbar surfaced three broken-looking
results: tables rendered as run-together plain text, task-list checkboxes
showed as plain bullets, code blocks lost their `<pre>` box. All three
looked like editor bugs but weren't — the live editor and the *stored*
markdown (`ArticleResponse.body`) were both correct, confirmed by fetching
the raw article directly. The break was in `ArticleResponse.bodyHtml`, a
**server-side** pre-rendered field (`ArticleRenderer` →
`worldbuilding.domain.wiki.MarkdownRenderer`, flexmark-java, predates this
ADR entirely) that every article read-view and print view actually
displays — not a live call to the frontend's `markdown-it` renderer, which
only the *other* 7 non-article `MarkdownEditor` fields use.

`MarkdownRenderer` itself was already correctly configured with flexmark's
`TablesExtension`/`TaskListExtension` and produced correct HTML in
isolation. The actual fault: `HtmlSanitizer` (`worldbuilding.domain.wiki`,
OWASP Java HTML Sanitizer), which runs on every render, allowlisted
`Sanitizers.FORMATTING`/`BLOCKS`/`LINKS`/`IMAGES` but never
`Sanitizers.TABLES` — and had no policy at all for `<pre>` or the
`<input type="checkbox" disabled>` flexmark's task-list extension emits.
Every disallowed element gets its *tag* stripped while its *text content*
passes through, which is exactly why the symptom was "structure gone, text
still there" rather than an outright rendering failure. This gap
**predates the Tiptap migration** — it was never triggered before because
the old 4-button toolbar could never produce a table, task list, or code
block in the first place; this toolbar upgrade is what finally exercised
a code path that's been latently broken since tables/task-lists were added
to the renderer.

Fix (`HtmlSanitizer.java`): added `.and(Sanitizers.TABLES)`, plus two
narrowly-scoped custom policies — `<pre>` (bare, no attributes), and
`<input>` restricted to `type="checkbox"` with only
`disabled`/`readonly`/`class="task-list-item-checkbox"` — deliberately
tight given `<input>` is normally excluded specifically to prevent form
injection; a disabled/readonly checkbox with no surrounding `<form>` can't
be interacted with or submit anything, so this doesn't reopen that risk.

Frontend follow-on: the client-side task-list CSS (`index.css`) originally
assumed `markdown-it-task-lists`' class names (`contains-task-list`,
`task-list-item`) on the `<ul>`/`<li>` themselves. flexmark's
`TaskListExtension` output has no such classes — only the checkbox itself
carries `class="task-list-item-checkbox"`, which both renderers happen to
share. Replaced the class-based selectors with `:has(> input.task-list-
item-checkbox)` (and the `label`-wrapped variant, since
`markdown-it-task-lists` is configured with `{label: true}`) so one set of
rules covers both the client-side and server-side renderers instead of
chasing each one's markup shape separately.

## Fix: `index.html` had no cache-control, masking all of the above

While chasing the sanitizer bug, several rebuild-and-redeploy cycles
appeared to have no effect — confirmed live (via `document.styleSheets`)
that a hard-refreshed tab was still loading a CSS bundle by filename from
*before* the latest deploy. `nginx.conf` set no `Cache-Control` on
`index.html`; a response with only `Last-Modified`/`ETag` is fair game for
a browser's own heuristic freshness caching, so a build's content-hashed
`/assets/*.css`/`.js` filenames (correct, immutable, safe to cache forever)
were unreachable because the *pointer to them* — `index.html` — was stale.
Fixed by adding `Cache-Control: no-cache` on `index.html`/SPA routes and
`Cache-Control: public, max-age=31536000, immutable` on `/assets/` (Vite
never reuses a filename across builds, so this is safe). Unrelated to
Tiptap specifically, but a real, previously-latent deployment bug this
session's rapid rebuild-deploy-verify cycle happened to surface.

## Consequences

- `docs/api/openapi.yaml` unaffected (no request/response shape changes),
  but this ADR does touch backend code — `HtmlSanitizer.java` — unlike the
  rest of the Tiptap migration, which is frontend-only. Markdown text
  handed to `onChange`/persisted server-side is unaffected in shape.
- No changes needed at any of the 8 `<MarkdownEditor>` call sites — `Props`
  stayed compatible across the whole migration.
- Active-state tracking (which toolbar buttons show pressed) is now
  `editor.isActive(...)` calls directly, replacing Milkdown's hand-rolled
  mark-detection logic — meaningfully less custom code for the same result.
- Slash-menu and a selection-triggered bubble toolbar remain explicitly out
  of scope, same as the original (pre-Tiptap) plan — neither Milkdown nor
  Tiptap ships a ready React component for either; worth its own follow-up
  plan, not bundled into this one.

## Alternatives considered

Covered above inline per-decision (Lexical/MDXEditor/BlockNote for the
library choice; placeholder-substitution/escaped-brackets-on-disk for
wiki-links; adopting the Simple Editor template wholesale for the UI).
