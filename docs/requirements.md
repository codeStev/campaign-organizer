# Campaign Organizer — Requirements

Personal, self-hosted worldbuilding and RPG campaign management application,
inspired by the feature set of [World Anvil](https://www.worldanvil.com/) but
scoped to a **single user** (no community, sharing, or collaboration features).

- **Audience:** one worldbuilder / gamemaster (the owner), self-hosting the app.
- **Reference:** World Anvil feature research captured in the roadmap below.
- **Design decisions:** recorded as ADRs in [`adr/`](adr/).
- **API contract:** [`api/openapi.yaml`](api/openapi.yaml) is canonical.

Requirement IDs are stable references (`FR-*` functional, `NFR-*`
non-functional). Each maps to a roadmap phase.

---

## 1. Functional requirements

### Foundations (Phase 0) — implemented
- **FR-1 Authentication.** The owner signs in with a single configured password
  and receives a bearer token used for all subsequent API calls. (ADR-0006)
- **FR-2 Worlds.** The owner can create, list, view, rename/edit, and delete
  worlds. A world is the top-level container every other object belongs to.

### Wiki (Phase 1)
- **FR-3 Articles.** Create Markdown articles (live-preview editor) within a
  world, organized by category/hierarchy. (ADR-0054)
- **FR-4 Templates.** Article templates (Character, Location, Organization,
  Species, Item, etc.) provide structured prompts.
- **FR-5 Auto-linking.** Mentions of other articles are automatically turned
  into cross-links.
- **FR-6 Media library.** Upload images and reuse them across articles and maps;
  stored on a local volume. (ADR-0007)
- **FR-7 Search.** Full-text search across a world's articles.

### Worldbuilding tools (Phase 2)
- **FR-8 Interactive maps.** Upload a map image, place pins (edited inline for
  label, layer, and linked article), and toggle layers. Pins are colour-coded by
  layer (adjustable colour and icon per layer, persisted on the world so they
  export and sync across devices), numbered/iconised with a legend, and can show
  labels on the map; a pin with no label falls back to its linked article's title.
  A single map can be printed on its own with scale, visual-filter, per-layer pin
  filtering (e.g. a players' copy hiding secret locations), and label/legend
  options. (ADR-0018, ADR-0044, ADR-0045, ADR-0047, ADR-0049)
- **FR-9 Timelines.** Events with dates, linked to articles; parallel timelines.
- **FR-10 Calendars.** Custom (fantasy) calendar systems that drive timeline
  dates.
- **FR-11 Relationship graphs.** Family trees / relationship webs between
  characters and organizations.

### Gamemaster tools (Phase 3)
- **FR-12 Campaigns.** Group sessions and notes (Markdown, ADR-0054) under a
  campaign tied to a world.
- **FR-13 Sessions.** Record sessions with a summary, private GM notes
  (Markdown, ADR-0054), and links to NPCs/locations.
- **FR-14 Story arcs.** Track arcs/beats (Markdown notes, ADR-0054) so plot
  threads are not lost.
- **FR-15 GM-only content.** ~~Mark articles/sections as hidden ("GM-only") to
  toggle spoilers at the table.~~ **Dropped (2026-08-12):** the app is
  single-user and players never access it, so there is no audience to hide
  content from.

### Character sheets & dice (Phase 4)
- **FR-16 Sheet engine.** Schema-driven character sheets supporting multiple
  game systems without hardcoding each; long-text fields render as Markdown
  (ADR-0054).
- **FR-17 Starter systems.** Ship 2–3 systems (e.g. D&D 5e, Pathfinder, generic).
- **FR-18 Statblocks.** Reusable NPC/monster statblocks; a statblock can
  optionally be driven by a statblock field template (FR-34) instead of
  freeform stats.
- **FR-19 Dice roller.** Roll dice, optionally bound to a sheet.

### Polish (Phase 5)
- **FR-20 Whiteboards.** Free-form canvas for plotting.
- **FR-21 Revision history.** Track article edits over time.
- **FR-22 Export.** Export a world/campaign to PDF/JSON.

### Interoperability (later — non-core)
These are explicitly **planned for later** and are **not core features**; they
integrate the app with the tools the owner already uses.
- **FR-23 Obsidian import/export.** Round-trip a world with an Obsidian vault
  that follows the owner's conventions (frontmatter `type`, folders per category
  like `Characters/`, `Locations/`, `Arcs/`, and `[[wiki-links]]`). Import maps
  folders→categories and `type`→template; export writes the same structure back.
  A one-off importer already exists at `scripts/seed-from-obsidian.mjs` and is the
  seed for this feature. Depends on: articles/categories (FR-3, FR-4),
  auto-linking (FR-5).
- **FR-24 Foundry VTT journal export.** Export article/journal content as
  Foundry VTT–compatible Journal Entries (e.g. an importable JSON / world-adventure
  package), so prepared lore can be pushed into a Foundry game. Depends on:
  articles (FR-3); relates to export (FR-22).

### Prep & print (Phase 6 — added post-roadmap)
The owner preps in the app but runs sessions from **paper**; print/PDF output is
first-class (screen-sharing/GM-only was declined — see FR-15).
- **FR-25 Usage backlinks & campaign filter.** Per-article "Used by" panel
  (beats, map pins, timeline events, relationships, sheets, statblocks, wiki-links)
  and an article-list filter by campaign usage. (ADR-0033)
- **FR-26 Command palette.** `Ctrl/⌘-K` jump-to-anything (articles + views).
  (ADR-0034)
- **FR-27 Print / PDF compendium.** Print a world or a single campaign as a clean
  black-on-white document — cover, contents, articles with embedded images and
  resolved wiki-links, and annotated maps — via the browser's print / Save-as-PDF.
  (ADR-0035)
- **FR-28 Session prep packet.** One-click printable packet for a session: its
  scheduled beats, the articles they reference, maps those articles are pinned on,
  and campaign statblocks. (ADR-0036, ADR-0046)
- **FR-29 Statblock cards.** Print statblocks as cut-out reference cards; tick
  specific statblocks to print a chosen subset, or print the whole filtered list.
  (ADR-0037, ADR-0041)
- **FR-30 Print in a separate tab.** Print views open in their own browser tab so
  the app view is never hijacked. (ADR-0038)
- **FR-31 Image embedding.** Embed images in articles via the toolbar, paste, or
  drag-and-drop; resize inline (S/M/L/Full); oversized images are capped to fit.
  (ADR-0039; refines FR-6.)
- **FR-32 Revision diff.** Tick any two article versions (or a version against
  current) in the History panel to compare them as a GitHub-style unified line
  diff (old/new line numbers, red/green rows, per-word highlighting); the newer
  version is always the “+” side. (ADR-0040, ADR-0042; refines FR-21.)
- **FR-33 Beat statblock references.** Attach statblocks to a beat directly (no
  article needed). A statblock referenced by a campaign's beats is treated as
  relevant to that campaign — its statblock list and session packets include such
  shared statblocks. (ADR-0043; refines FR-14, FR-18.)
- **FR-34 Statblock templates.** Reuse the character sheet engine's field
  template (renamed `FieldTemplate`, ADR-0052) to build reusable statblock
  layouts — sections of typed fields (text, number, select, circle trackers,
  …) instead of retyping AC/HP/Speed on every monster. One template
  aggregate serves both character sheets and statblocks, distinguished by a
  `kind`; a builtin D&D 5e Monster starter ships alongside the existing
  character-sheet starters. Assigning a template to an existing freeform
  statblock keeps its prior stats visible and editable as "Other stats", and
  templated stats print in template order on statblock cards and session
  packets. (ADR-0052; refines FR-16, FR-18.)
- **FR-35 Deep linking.** The URL reflects where you are — world, top-level
  tab, Sheets sub-tab, and the single article/map/timeline/calendar/campaign/
  character sheet/statblock/field template/whiteboard currently open — so
  reloading, bookmarking, sharing a link, and browser back/forward all land
  you back in the same place. (ADR-0053; refines FR-26.)
- **FR-36 Instance backup and import.** One-click download of a full instance
  backup (every world as a JSON bundle plus its media files) as a single ZIP,
  and in-app import of that ZIP, either additive (new worlds alongside
  existing ones) or full-overwrite (replaces everything). (ADR-0061,
  supersedes ADR-0055)
- **FR-37 Session cheat sheet.** A condensed, one-page print view separate
  from the full session packet (FR-28): a hand-curated, ordered list of
  fragments per session — freeform snippets plus live references to existing
  content (statblocks, single roll-table rows, single deck cards), rendered
  fresh from their sources at print time. Built as a per-session composer
  with explicit ordering and one-dense-page output (ADR-0071).
  Goal: cut mid-session lookups — which currently compete with the
  GM's own attention to take notes on what's happening at the table — down to
  a glance instead of flipping through the full packet. Live voice-note
  capture (record-then-transcribe, even GM-only self-dictation) was
  considered for the same problem and explicitly rejected (2026-08-25):
  dictating notes during a scene, even quietly, distracts the players.
- **FR-38 AI-assisted text drafting.** An "AI draft" action in the article
  editor: seed with a few keywords/instructions, get a first-draft
  description/read-aloud text inserted into the editor to edit before
  printing. Prep-time only — nothing calls out during a session. Backed by
  a free-tier cloud LLM API (Groq, falling back to OpenRouter), since the
  owner's self-hosting hardware can't run a usable model locally.
  (ADR-0064) A dialog lets the owner pick a generation level — a short
  quick-inspiration teaser, a read-aloud/boxed-text snippet to speak
  straight to players, essentials-only basic info (no invented places/
  characters), or a full draft — and the kind of article being drafted,
  both shaping the generated text. (ADR-0075; refines FR-38)
- **FR-39 Settings menu.** An instance-level settings page (not nested
  under any world), structured for multiple categories though only "AI"
  exists so far: per-provider model choice and provider try-order,
  editable without touching `.env`/restarting. API keys are deliberately
  not part of this UI — they stay environment-only (NFR-7). (ADR-0065;
  refines FR-38)
- **FR-40 Roll tables & card decks.** Builders for reusable randomizers:
  roll tables over any dice combination (entries per result range, one
  optional catch-all row, free `[[wiki-links]]` inside outcomes) and
  customized card decks. Both attach to session beats; the session packet,
  compendium, and standalone printouts include them — and **an article
  prints exactly once per document** no matter how many sources reference
  it. Wiki-links in printed bodies stay anchors-only; deck draws are
  stateless (print-first). Tables/decks appear in usage backlinks and world
  backups like every other content type. (ADR-0066)
- **FR-41 Roll-table chaining.** A table entry or deck card can chain other
  roll tables and card decks ("roll on Weather"), resolved recursively.
  Save-time validation rejects self-nesting and references outside the
  world; indirect cycles stay storable, and every resolution point cuts
  them (packet BFS, print closure, depth-capped live roller). Chained
  targets join session packets and standalone printouts under FR-40's
  print-once rule, and survive export/import with remapped ids. (ADR-0072)
- **FR-42 Scheduled backup snapshots.** Nightly in-app snapshots of the
  full FR-36 instance ZIP into `{media}/backups/`, keeping the most recent
  N (default 7); cron and retention are env-overridable. An in-app
  `@Scheduled` job replaces the originally sketched sidecar container;
  a failed night logs and skips instead of breaking the schedule, so
  having a recent backup stops depending on remembering to click
  download. (ADR-0073)
- **FR-43 Consistency report.** A per-world lint page (Consistency tab):
  broken `[[wiki-links]]` everywhere the wiki pipeline renders (articles,
  beats, roll-table entries, deck cards), orphaned articles (no inbound
  links from any of those, self-links don't count), and content not
  referenced by any campaign. Derived from the usage-index machinery behind
  FR-25; print-friendly. (ADR-0067)
- **FR-44 Encounter sheet generator.** Pick statblocks (and optionally PC
  sheets) and get a printable tracking sheet: one row per combatant with HP
  tick-boxes, an initiative column, and key defenses cribbed from the
  statblock — pure assembly over existing content, run from paper like the
  rest of the session material; quantities and max-HP prefill are staged
  before printing, nothing is persisted. (ADR-0069)
- **FR-45 Session chronicle / recap builder.** One click renders "the
  story so far" from completed beats and past sessions' summaries as a
  printable recap to open the next session with — read-only over existing
  data; session GM notes are deliberately excluded. (ADR-0068)
- **FR-46 Handout designer.** Player-facing props — letters, wanted
  posters, in-world newspaper pages — as styled one-page printables with
  fixed presets (parchment, newspaper, poster, letter). Deliberately
  separate from GM-only content: handouts are meant to leave the table in
  the players' hands. Own bounded context; ships in world backups; reuses
  the standalone print-window pattern (ADR-0038). (ADR-0070)
- **FR-47 Folksonomy tags.** Freeform, world-scoped tags — no `type:`/
  `status:` namespacing, no colors/icons, no forced taxonomy — orthogonal to
  the article category/parent-child hierarchy (FR-1, ADR-0080). v1 covers
  articles and statblocks: an inline chip-style tag input with autocomplete
  against the world's existing tags on each entity's detail view, per-list
  filtering by tag (same pattern as the existing campaign filter), and a
  cross-entity "browse by tag" view across articles and statblocks together,
  included from the start. No tag management screen (no global rename/
  merge/delete) and no tags in print output. (ADR-0083)

---

## 2. Non-functional requirements
- **NFR-1 Deployment.** Runs as Docker containers with a single
  `docker compose up`. (ADR-0004)
- **NFR-2 Single-user.** No multi-tenancy, registration, or per-object ACLs
  beyond the GM-only flag. (ADR-0005)
- **NFR-3 API-first.** All behavior exposed via a documented OpenAPI 3.1 API;
  contract is the source of truth. (ADR-0008)
- **NFR-4 Persistence.** Data stored in PostgreSQL with versioned schema
  migrations. (ADR-0003, ADR-0012)
- **NFR-5 Testing.** Critical paths covered by unit and integration tests; CI
  runs them on every push. (ADR-0011)
- **NFR-6 Portability.** Self-hostable on a single machine; no external cloud
  services required (media on local volume). AI drafting (FR-38) is the one
  deliberate exception — it needs a cloud LLM API and degrades to
  unavailable, not broken, without one configured; every other feature is
  fully functional with zero external dependencies.
- **NFR-7 Security.** Stateless bearer-token auth; secrets supplied via
  environment; non-root container users.
- **NFR-8 Errors.** API errors use RFC 9457/7807 `application/problem+json`.
  (ADR-0009)
- **NFR-9 Responsive & touch UI.** The frontend layout adapts down to phone
  width, and canvas-style interactions (whiteboards, the sheet template
  builder) support touch/pen input via Pointer Events, not just a mouse.
  (ADR-0062)
- **NFR-10 Component foundation & theming.** Core UI components (buttons,
  inputs, selects, dialogs, tabs, checkboxes) are built on shadcn/ui (Radix
  primitives + Tailwind), giving consistent behavior/accessibility and a
  real light/dark theme toggle. (ADR-0063, supersedes ADR-0062's
  single-dark-theme premise)

---

## 3. Out of scope (deliberately cut)
Community/social features, public world pages, player accounts, subscriber
groups, marketplace/monetization, novel-writing manuscript editor, real-time
multi-user collaboration. Rationale in ADR-0005.

---

## 4. Roadmap

| Phase | Theme | Requirements |
| --- | --- | --- |
| 0 | Foundations (auth, worlds, deploy, CI) | FR-1, FR-2, NFR-* |
| 1 | Wiki MVP | FR-3 … FR-7 |
| 2 | Maps & timelines | FR-8 … FR-11 |
| 3 | GM campaign manager | FR-12 … FR-14 (FR-15 dropped) |
| 4 | Character sheets & dice | FR-16 … FR-19 |
| 5 | Polish | FR-20 … FR-22 |
| 6 | Prep & print (paper-first workflow) | FR-25 … FR-35 |
| 7 | Ops (self-hosting readiness) | FR-36 |
| Later | Interoperability (non-core) | FR-23 (Obsidian), FR-24 (Foundry) |
| 8 | AI-assisted drafting | FR-38, FR-39 |
| 9 | Randomizers (tables & decks) | FR-40, FR-41 |
| 10 | Consistency & print tooling | FR-43 (consistency report), FR-45 (recap builder), FR-44 (encounter sheet), FR-46 (handouts), FR-37 (cheat sheet) |
| Proposed | Optional, unscheduled | FR-42 |
