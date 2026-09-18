# ADR-0116: Reliable, editable article links, aliases, and an auto-link scan

- Status: Accepted
- Date: 2026-09-18

## Context

Dogfooding the article Markdown editor (ADR-0054/ADR-0076) surfaced a
cluster of related problems with `[[target]]` wiki-links (ADR-0014):

1. Wiki-links typed character-by-character survive editing, but a link
   introduced any other way (pasted, most notably) silently corrupts on
   save. Root cause, confirmed by reading the actual library code rather
   than guessed: `WikiLink` (`frontend/src/lib/wikiLinkExtension.ts`) only
   becomes a real mark via `addInputRules()`, which fires solely on live
   typing — pasted text lands as a plain text node. `@tiptap/markdown`'s
   serializer unconditionally backslash-escapes `[`/`]` in *every* plain
   text node on save (`escapeMarkdownSyntax`, no config knob), so a pasted
   `[[Article]]` silently becomes `\[\[Article\]\]`, which the backend's
   `WikiLinker` regex no longer matches.
2. Neither `[[Article]]` wiki-links nor regular `[text](url)` hyperlinks
   can be repointed by clicking them in the editor — `WikiLink`'s own
   source comment admits "not click-navigable there... this component has
   no access to the live article list", and while the regular `Link` mark
   does have an edit popover (`LinkPopover.tsx`), nothing opens it on
   click (`openOnClick: false` blocks navigation but nothing takes its
   place) — only a cursor-then-toolbar-button path exists, which is not
   discoverable.
3. The toolbar's Link button silently does nothing for some inputs.
   Tiptap's `Link` extension gates `setLink()` behind `isAllowedUri()`,
   which treats any `word:` prefix as an attempted (and by default
   disallowed) URI scheme — an href like `Chapter: The Beginning` fails
   this check, `setLink()` returns `false`, and `LinkPopover.save()` has no
   error path.
4. `[[Article|Label]]` is the only way to write a link with custom display
   text; users familiar with standard Markdown expect `[Label](Article)`
   to work too.
5. Older/imported articles were written before this app's linking system
   existed, so many plain-text mentions of an article's name are not
   linked and there was no assisted way to find and convert them.
6. Some entities go by several names in play (nicknames, aliases,
   in-world titles) — there was no way to record that, or to link via one
   of those names.

## Decision

### Wiki-link mark also gains paste rules
`WikiLink` adds `addPasteRules()` (Tiptap's `markPasteRule`, the paste
counterpart to the input rule already used), mirroring the existing
labeled/unlabeled input rules exactly. Paste is no longer a silent
corruption path.

### `[Label](Article)` as a second link syntax, resolved the same way as `[[...]]`
`WikiLinker`'s existing regex-substitution pass (`worldbuilding/domain/wiki/
WikiLinker.java`) is extended with a second alternative,
`(?<!!)\[label\]\(target\)` (excluding `![alt](src)` images). A target that
looks like a URL (starts with a URI scheme, `/`, or `#`) is left completely
untouched, so flexmark's own link parsing handles real hyperlinks exactly
as before; everything else resolves against the article index exactly like
a labeled wiki-link. Because this lives inside the one shared
`renderWith`/`linkTargets` pipeline, every existing consumer — HTML render,
the Foundry Markdown push (ADR-0115), broken-link detection, usage
tracking — picks up the new syntax with no separate code path. On the
frontend, no new Tiptap extension is needed: `@tiptap/markdown` already
round-trips the built-in `Link` mark's standard syntax. The editor's
`isAllowedUri` is set to always-allow — the real safety boundary is already
server-side (`HtmlSanitizer`'s `Sanitizers.LINKS` policy), so the
client-side check was pure friction, and a bare article title (which may
contain a colon) should never be rejected.

### Click-to-edit for both link types
Clicking an existing link or wiki-link in the editor body opens an edit
popover pre-filled with its target, instead of requiring the user to
already know to place the cursor and find the toolbar button. Regular
links reuse the existing `LinkPopover`; wiki-links get an equivalent new
`WikiLinkPopover` built on the existing `ArticleLinkPicker` search dialog,
letting the user repoint the link to a different article or remove it.

### Article aliases
Articles can carry alternate names (`article_aliases`, one row per
alias, world-scoped like `entity_tags`), managed the same way tags
already are — a replace-all `PUT`. Aliases feed the same resolution index
`WikiLinker`/`ArticleRefIndex` already builds (alias fills gaps, then
slug, then title still wins on collision, unchanged from ADR-0014), so
`[[alias]]`/`[label](alias)` resolve exactly like a title would, with zero
new resolution logic to maintain elsewhere.

### A manual, reviewable auto-link scan
A new scan, triggered from the Consistency page (ADR-0033), looks for
plain-text mentions of any other article's title or alias inside each
article's body (word-boundary, case-insensitive, skipping text already
inside a link or code span). Results group by source article, then by the
matched text span (with its position and surrounding snippet); for each
span, every name-form of the resolved target (title + each alias) is
offered as a candidate, so the user can choose *which name* gets written
as the link's target — not necessarily whichever form happened to match —
while the original wording is always preserved as the link's label
(`[[ChosenName|original text]]`), so applying a batch of auto-links never
silently changes an article's prose casing. Nothing is converted without
explicit per-occurrence review; applying reuses the existing article
update path, so auto-linked changes get the same revision-history
snapshot (ADR-0026) as any manual edit.

## Consequences
- `WikiLinker`'s `LINK` pattern and `linkTargets()` grow a second branch;
  covered by extended `WikiLinkerTest` cases (resolved, broken, URL-target
  passthrough, image exclusion).
- A new table, RLS-protected from birth (Tier 2, ADR-0114):
  `article_aliases`. `RowLevelSecurityFitnessIT` grows accordingly.
- `ArticleRepositoryPort.ArticleRef`/`findRefsByWorldId` gain an aliases
  projection; `ArticleRefIndex.build` gains a third (lowest) precedence
  tier. Every caller of the shared index (render, Foundry push, usage/
  consistency resolution) is alias-aware automatically.
- Two new read/write use cases for aliases (mirroring the tags feature)
  and two new use cases for the auto-link scan/apply, all inside
  `worldbuilding` — no new cross-context port, since scanning and applying
  both only ever touch `Article` bodies, which `worldbuilding` already
  owns. The scan is scoped to article bodies only; beats/roll tables/card
  decks use a separate plain-textarea + client-side wiki-link renderer
  (ADR-0115) and are out of scope here.
- `MarkdownEditor` gains an optional `worldId` prop (same opt-in pattern as
  `onUploadImage`/`onAiDraft`) so its wiki-link click-to-edit popover can
  search the world's articles; only `ArticleEditor` wires it for now, the
  other `MarkdownEditor` call sites are unaffected.

## Alternatives considered
- **A floating "bubble menu" at the click position for link editing** —
  more discoverable than a toolbar-anchored popover, but a materially
  bigger change to the editor's toolbar/popover plumbing for a first
  version; the toolbar-anchored popover (opened by clicking the link, not
  just via the toolbar button) already fixes the "can't edit" complaint.
  Left as a possible future upgrade.
- **Fully automatic (unreviewed) auto-linking** — rejected for the same
  reason ADR-0014 rejected automatic detection generally: silently
  rewriting prose is worse than a false negative. The scan is a
  human-in-the-loop review, matching ADR-0014's existing bias.
- **Aliases as a generic `entity_tags`-style cross-context concept** —
  rejected: aliases are intrinsic to an article's identity/naming
  (feeding link resolution directly), unlike freeform folksonomy tags,
  so there's no reason to route them through a separate bounded context
  and an ACL port.
