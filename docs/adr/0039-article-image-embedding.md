# ADR-0039: Article image embedding — paste, drop, resize, cap

- Status: Accepted
- Date: 2026-08-15

## Context
Articles already supported images (TipTap Image node + a media-upload button;
`<img>` from our media endpoint survives sanitizing — ADR-0013/0025). But in
practice it felt like images couldn't be embedded: the only path was the toolbar
button, pasting or dragging an image did nothing, and a full-HD/4K source
rendered at its native pixel size — huge — with no way to make it smaller.

## Decision
Round out image embedding in the rich-text editor (`RichTextEditor`).

- **Paste & drag-drop.** `editorProps.handlePaste`/`handleDrop` detect image
  files, upload them via the existing `onUploadImage`, and insert them at the
  selection. The button stays as a discoverable fallback, with a hint line.
- **Resize.** The Image node is extended to persist a `width` attribute; when an
  image is selected, the toolbar shows S / M / L / Full presets that set a pixel
  width (or clear it). The sanitizer already accepts an integer `width` on `img`
  (OWASP `Sanitizers.IMAGES`); an explicit allow-rule matching `\d{1,4}` documents
  the intent. Percentages are intentionally not used — the images policy only
  permits integer widths.
- **Cap.** `.editor-content img` and `.preview-body img` are `max-width: 100%;
  height: auto;`, so an oversized source never overflows and always keeps aspect
  ratio; the print views already cap the same way.

## Consequences
- Embedding an image is now what users expect: paste a screenshot, drag a file,
  or use the button — then resize it inline.
- Widths are pixel values capped to the container; a set width acts as a max on
  large screens and shrinks to fit on small screens and in print.
- Storing size on the `width` attribute keeps the body as plain sanitized HTML
  (no inline `style`, which stays disallowed for XSS safety).

## Alternatives considered
- **Percentage widths / a drag-handle NodeView** — richer, but percentages are
  rejected by the images sanitizer policy and a NodeView adds a dependency and
  complexity; presets + a container cap cover the need.
- **Client-side downscaling on upload** — loses the original; capping on display
  is simpler and keeps the full-res file for print.
