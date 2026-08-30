# 78. Resizable images survive markdown round-trips

Date: 2026-08-30
Status: Accepted

## Context

Reported by the user right after ADR-0076's Tiptap migration: "I am not
able to resize images in the markdown editor anymore." Investigation found
two compounding problems, the second a real data-corruption risk:

1. `@tiptap/extension-image`'s built-in `resize` option (a real,
   ready-made drag-to-resize `ResizableNodeView`, confirmed present in the
   installed `3.30.5`) was never enabled — `MarkdownEditor.tsx` used the
   plain `TiptapImage` with no config, so there was no resize UI at all.
2. Even with resize enabled, that extension's `renderMarkdown` always
   emits bare `![alt](src)` — it silently drops `width`/`height` on save.
   Pre-Tiptap, a resized image was stored as raw `<img src width>` HTML
   (CommonMark passes raw HTML straight through — `MarkdownRenderer.java`
   already documents this, and `HtmlSanitizer.java` already allowlists a
   `width` attribute specifically for this). Confirmed via a direct
   `MarkdownManager` test that `@tiptap/markdown` does not recognize a raw
   `<img>` tag as HTML pass-through at all — it parses as literal text,
   and **worse, re-serializes it HTML-entity-escaped**
   (`&lt;img src... &gt;`). Any existing article with a previously-resized
   image would silently corrupt on the next open-and-save in the new
   editor, breaking it even for the old server-side flexmark renderer.
   (Checked this session's two test worlds — neither had any at-risk
   content — but the failure mode itself is real and would hit real data.)

## Decision

`frontend/src/lib/resizableImageExtension.ts`: `TiptapImage.extend({...})`,
keeping the base node's resize `NodeView`/commands/attrs, replacing only
its markdown parsing/rendering with a single custom `markdownTokenizer`
(`level: 'block'` — a raw `<img>` tag is classified as a block-level HTML
token by marked.js, so an `inline`-level tokenizer never even sees it) that
recognizes *both* forms:

- `![alt](src "title")` — parsed/rendered exactly as before.
- `<img src alt title width height>` — parsed into the same `image` node
  with `width`/`height` attrs; rendered back to raw `<img>` HTML whenever
  either is set, plain `![]()` otherwise.

Verified via direct `MarkdownManager` parse/serialize calls (no browser
needed, same technique ADR-0076 used for the wiki-link extension) that
both forms round-trip stably, including mixed documents (headings, other
paragraphs, consecutive images) and a second edit pass.

`MarkdownEditor.tsx` configures resize as
`{ enabled: true, directions: ['bottom-right'], minWidth: 40, alwaysPreserveAspectRatio: true }`
— one corner handle, proportional by default (a GM resizing a map fragment
or portrait wants scaling, not stretching). The base `ResizableNodeView`
ships handle *elements* with no default appearance (position: absolute,
no size/color) — `index.css` adds a small circular handle, hidden until
the image wrapper is hovered or actively resizing
(`[data-resize-container][data-resize-state='true']`).

Confirmed live end-to-end against a rebuilt production Docker image:
uploaded an image, dragged the handle, saved, reloaded the editor (size
persisted, still resizable) and the read view (renders at the saved
size), and confirmed via a direct API fetch that the stored body contains
`<img src="..." width="..." height="...">` matching the pre-Tiptap format
byte-for-byte.

## Consequences

- No backend or contract changes — `HtmlSanitizer`/`MarkdownRenderer`
  already supported exactly this `<img width>` shape.
- Plain (unresized) images are unaffected — still round-trip as
  `![alt](src)`, identical to `@tiptap/extension-image`'s own default
  behavior.
- If a *third* image-embedding syntax ever needs support, it goes through
  the same tokenizer, not a separate extension — one node owns "how an
  image looks in markdown."
