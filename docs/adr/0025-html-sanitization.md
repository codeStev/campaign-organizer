# ADR-0025: Sanitize article HTML on write

- Status: Accepted
- Date: 2026-08-12

## Context
Article bodies are stored as HTML (ADR-0013) and rendered with
`dangerouslySetInnerHTML` in the reader. ADR-0013 flagged that unsanitized HTML
is a stored-XSS vector. Even for a single user, imported content (e.g. from
Obsidian, FR-23) or a compromised dependency could inject a `<script>`; hardening
the write path is cheap insurance.

## Decision
Sanitize article body HTML **on write** (create/update) with the **OWASP Java
HTML Sanitizer**, using an allowlist that matches the TipTap editor's output:
formatting (`b/i/em/strong/…`), blocks (`p/h1–h6/ul/ol/li/blockquote/…`), links
(`a[href]`, http/https/mailto), and images (`img[src]`) — restricted to our own
media endpoint (`/api/media/…`) or absolute http(s). Scripts, event-handler
attributes, styles, iframes, and unknown elements are dropped.

Sanitizing on write (not render) means the stored body is already safe, the
auto-linker (ADR-0014) still runs over the clean HTML, and `[[wiki-links]]` —
being plain text, not HTML — pass through untouched.

## Consequences
- Stored XSS is prevented at the boundary; the reader can keep rendering trusted
  HTML.
- Editor formatting is preserved; anything outside the allowlist is silently
  stripped (acceptable — the editor cannot produce it anyway).
- If a future feature needs an extra element (tables, etc.), the allowlist must
  be extended deliberately.

## Alternatives considered
- **Sanitize on render**: repeated work per read and easy to forget at a new
  render site; write-time is the single choke point.
- **Trust the input (single user)**: rejected — imports and dependencies are
  untrusted enough to warrant the cheap safeguard.
