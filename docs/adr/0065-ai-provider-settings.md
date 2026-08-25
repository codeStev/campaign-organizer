# ADR-0065: AI provider settings (model + priority), user-editable

- Status: Accepted
- Date: 2026-08-25

## Context
The owner wants a settings menu (structured for multiple categories, "AI"
being the first) where they can change which model each configured
provider uses, and the try-order between providers — without editing
`.env`/restarting containers every time. ADR-0064 deliberately gave the
`ai` context no persistence ring, reasoning "there's nothing to persist";
that premise no longer holds once model/priority are meant to be changed
at runtime rather than at deploy time. This ADR supersedes that one clause
of ADR-0064 (the context's stateless/no-persistence-ring status only —
everything else in ADR-0064 stands: still no world-scoping, still a cloud
LLM dependency, still Groq-then-OpenRouter as the two providers).

API keys are explicitly **not** in scope for this settings UI — they stay
`.env`-only secrets (NFR-7: "secrets supplied via environment"). Only two
non-secret preferences move into the database: which model a provider
uses, and the order providers are tried in.

## Decision
- **The `ai` context gains a persistence ring** (its own JPA entity, Spring
  Data repo, MapStruct persistence mapper) — the three-model rule now
  applies to it fully, like every other context; ADR-0064's exemption was
  correct when written and is retired now that it no longer fits.
- **One table, `ai_provider_settings`**: `provider` (primary key, e.g.
  `"groq"`/`"openrouter"`), `model` (nullable — null means "use that
  adapter's built-in default"), `priority` (try-order, lower first),
  `updated_at`. No `world_id` — this is instance-global config, like
  backup/import (ADR-0061), not a per-world resource.
- **Defaults without a migration data-seed.** If the table is empty (fresh
  install, nobody has opened Settings yet), `AiSettingsService` returns an
  in-memory default list (both known providers, their compiled-in default
  models, code-defined priority) rather than requiring the migration to
  insert rows. Nothing is persisted until the owner actually saves a
  change from the Settings UI.
- **Provider identity moves onto the port.** `TextGenerationPort` gains
  `providerId()` (e.g. `"groq"`) and `defaultModel()`; `generate(...)`
  gains a `model` parameter (previously fixed at adapter construction from
  `AppProperties`). `DraftArticleTextService` now builds its own
  `Map<String, TextGenerationPort>` from an injected (unordered)
  `List<TextGenerationPort>`, keyed by `providerId()`, and asks
  `AiSettingsService` for the current priority-ordered list before every
  draft call — so a settings change takes effect on the very next request,
  no restart. The `@Order`-based static fallback ordering from ADR-0064 is
  removed; ordering is settings-driven now, not Spring-DI-driven.
- **`AppProperties.Ai` sheds `groqModel`/`openRouterModel`.** Keeping both
  an env-var default model *and* a DB-persisted one would be two competing
  sources of truth for the same question. `AppProperties.Ai` now holds
  only the two API keys; each adapter's `defaultModel()` is the sole
  built-in fallback when no DB row (or a null `model`) says otherwise.
- **Settings endpoints are instance-global**, not world-scoped:
  `GET /api/ai/settings`, `PUT /api/ai/settings` — no `worldId` in the
  path at all (unlike the draft-text endpoint, which kept a cosmetic
  `worldId` for URL-shape consistency; there's no such consistency
  argument for a page that isn't nested under a world in the frontend
  either). The GET response includes, per provider, whether an API key is
  actually configured (read from `AppProperties`, combined with the
  persisted settings at the web layer — that combination is a presentation
  concern, not domain logic, so it belongs there rather than in the
  service) — the Settings UI needs that to explain why a provider might
  never actually get used despite its priority.
- **Frontend**: a new top-level `/settings` route (sibling of `/worlds`,
  reachable from the app header, not nested under any world — this is
  instance config, same scope as the endpoints). Built as a shell with a
  category sidebar/tabs so future categories are just another route +
  component, not a redesign; "AI" is the only category for now. Within it,
  each provider is a row (model `Input`, "configured"/"not configured"
  badge) in priority order, reordered with ↑/↓ buttons — matching the
  existing reorder pattern used elsewhere in the app (`TemplateBuilder`
  field order, arc beats) rather than introducing drag-and-drop for a
  two-item list.

## Consequences
- Changing a model or swapping provider priority takes effect immediately
  on the next AI draft call — no container restart, unlike every other
  piece of AI config (which stays in `.env` deliberately, per NFR-7).
- The `ai` context is no longer an exception to the harness's ring
  structure; it looks like every other context now (this is a
  simplification for future readers, not a complication).
- Adding a third provider later means: implement `TextGenerationPort`
  with a new `providerId()`, and it automatically appears as a settings
  row via the default-list fallback — no settings-table migration needed
  for that either.

## Alternatives considered
- **Keep model/priority in `AppProperties`, drop the settings UI idea**:
  simplest, but doesn't satisfy what was actually asked — the owner
  wants to change these without touching `.env`/restarting.
- **Store settings as a single JSONB blob** (like `whiteboards.nodes`,
  ADR-0027): reasonable for a document that's always loaded/saved whole,
  but this is closer to a small reference table (fixed key = provider id,
  queried by priority) than a document; plain columns keep `ORDER BY
  priority` a real query instead of an in-application sort after loading
  a blob.
- **Let the owner add arbitrary custom providers/base-URLs from the UI**:
  not asked for, and providers still need a concrete `TextGenerationPort`
  implementation to actually call their API shape — out of scope here.
