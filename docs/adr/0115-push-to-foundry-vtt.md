# 0115. Push articles, handouts, roll tables and card decks to Foundry VTT

## Status
Accepted

## Context
The user self-hosts Foundry VTT and wants a one-click way to get a wiki
Article's, a player Handout's, a Roll Table's, or a Card Deck's current
content into Foundry, without hand-copying prose or re-typing tables at the
table. Maps are explicitly out of scope — the user already exports
battlemaps from Dungeondraft straight to Foundry scenes and only keeps a
reference image in Campaign Organizer. Statblock and Character Sheet export
is a separate, later effort: it needs hand-curated per-system mapping to
Foundry `Actor` documents (Fabula Ultima, Wildsea, D&D 5e each define an
entirely different Actor data schema), which is real system-specific mapping
work, not a format change, and is scoped as its own future plan/ADR.

Foundry itself exposes no inbound HTTP API. The transport is a third-party
generic relay the user separately self-hosts for an unrelated project
(`obsidianToFoundry`, a different codebase in a different language — nothing
from it is reused here beyond the general approach): ThreeHats'
`foundryvtt-rest-api` module (runs inside Foundry, holds a WebSocket
connection out) plus a small self-hosted `foundryvtt-rest-api-relay` server
that exposes a plain HTTP+JSON surface, keyed by an `x-api-key` header and a
`clientId` identifying which connected Foundry session to target. This
backend only ever needs to speak HTTP to that relay — no WebSocket code
lives here. Confirmed against the relay's own published reference
(foundryrestapi.com/docs/api) rather than assumed: the endpoints this
feature uses are `POST /create` (upsert a document — `entityType`, `data`,
a top-level `folder` UUID, `keepId`/`override` flags), `GET /clients`
(connected sessions), and `POST /upload` (a genuine file-upload endpoint —
see "Embedded images" below; there is **no** need for the arbitrary-script
`/execute-js`/`/macro` primitives this feature was originally drafted
against, which materially simplifies and de-risks the image-upload design).
Every endpoint is additionally gated by a scope on the API key itself
(`entity:write`, `file:write`, `clients:read` for what this feature uses) —
the settings UI/onboarding should tell the user which scopes to grant when
issuing a key on their relay.

Two integration questions needed resolving before writing any code:
1. Whether an article's rendered body (`ArticleRenderPort.renderBody`) is
   still Markdown after wiki-link resolution, since Foundry's
   `JournalEntryPage` text format needs to be told which it's getting
   (`format: 1` HTML vs `format: 2` Markdown).
2. How to get at an embedded image's bytes from `interchange`, given
   `media`'s only published port before this ADR was an existence check
   (`MediaLookupPort.existsInWorld`), not a byte-serving one.

## Decision

### Every world's Foundry connection is entirely user-supplied
There is no shared, default, or bundled Foundry relay of any kind. Each
world's relay base URL, API key, and `clientId` are entered by that
account's own user, pointing at a Foundry relay *they* self-host — nothing
in this app talks to any relay instance the user hasn't explicitly
configured for that world. This follows directly from the app's core
multi-tenancy invariant (every account's worlds/content/integrations are
strictly private, even from other accounts) and is treated as a hard
constraint, not a UX nicety: no default `relayBaseUrl` exists anywhere in
config, env, or code, and the only relay configuration surface in the whole
system is the per-world `FoundryConnection` settings row.

### `interchange/foundry`, composing published ports directly
New submodule, sibling to `interchange/calendar` (ADR-0108) and following
its exact shape: own `domain`/`application`/`adapter` rings, own
persistence for connection config and push tracking, and cross-context
reads via `ArticleQueryPort`/`ArticleRenderPort`/`HandoutQueryPort`/the new
`MediaContentQueryPort` (and, in later phases, `RollTableQueryPort`/
`CardDeckQueryPort`) — composed directly in the application service, the
same style `CampaignCalendarService` already uses for
`CampaignQueryPort`/`SessionQueryPort` (no extra anti-corruption-layer
out-port wrapper, since the published port already speaks the calling
context's own read model). Neither `worldbuilding`, `handouts`, nor `tables`
gains any Foundry-specific code — every Foundry endpoint lives entirely
under `/api/worlds/{worldId}/foundry-connection` and
`/api/worlds/{worldId}/foundry/**`, owned by this new submodule.
`interchange/foundry` exposes **no published port of its own** — nothing
else in the app, including `interchange/export`'s full-world backup
exporter, can reach its connection config or push records.

### A new published port for media bytes: `MediaContentQueryPort`
`media`'s only published port was `MediaLookupPort.existsInWorld` — no byte
access. `LoadMediaContentUseCase.load(mediaId)` (an *in*-port) does expose
bytes, but only for the public, unauthenticated, RLS-bypassing
`/api/media/{id}/content` endpoint (ADR-0016) — wrong shape and wrong trust
boundary for an authenticated, world-scoped cross-context call. New
`media.application.port.published.MediaContentQueryPort.loadInWorld(mediaId,
worldId)`, implemented by the same `MediaService`, does an ordinary
`findByIdAndWorld` + storage load — no RLS bypass, exactly the same trust
level as the existing `existsInWorld` check it sits beside.
(`com.campaignorganizer.backup.BackupService` already calls the in-port
`LoadMediaContentUseCase` directly today, but `backup` sits outside the
ArchUnit-enforced context list entirely — a pre-existing grey area, not a
precedent this feature follows, since `interchange` *is* enforced.)

### Markdown, not HTML, for every pushed document
Foundry's `JournalEntryPage` text field supports `format: 2` (Markdown)
directly, and this app's Article/Handout/RollTableEntry/DeckCard bodies are
already Markdown (ADR-0054) — but `ArticleRenderPort.renderBody()` resolves
`[[wiki-links]]` into HTML anchors (`href="#"`, client-side-SPA-resolved
only, never a real URL outside this app) and then runs Markdown→HTML plus
sanitization, which is unusable for a Markdown page. A new published-port
method, `ArticleRenderPort.renderBodyAsMarkdown`, resolves `[[links]]` into
`**bold**` (resolved) / `*italic*` (broken) plain-Markdown spans instead,
skipping the HTML render/sanitize steps entirely, so the body stays valid
Markdown end to end. `WikiLinker` gains a `renderMarkdown` sibling to its
existing `render`, sharing the same match/lookup logic via an extracted
formatter callback — `render`'s existing HTML output and tests are
untouched. Handouts have no wiki-link resolution at all today, so their
body is pushed completely verbatim. Every pushed JournalEntry/RollTable/
Cards document therefore uses Markdown unconditionally — no HTML path
exists anywhere in this feature.

Rejected: fabricating a URL back to Campaign Organizer's own SPA for
resolved links. The existing anchor's `href="#"` shows this was never a
real, addressable URL even inside this app; inventing one for an entirely
different self-hosted app, on a host the GM's Foundry instance may or may
not even have network access to, would be guesswork dressed up as a
feature. Plain emphasis is honest about what actually survives the trip.

### Idempotent upsert via deterministic 16-char ids
Foundry document/folder ids are derived deterministically from a stable key
(`"campaign-organizer:{worldId}:article:{articleId}"`, etc.) via SHA-256 of
the key, interpreted as an unsigned 256-bit integer and repeatedly reduced
mod 62 to emit 16 characters from `[A-Za-z0-9]` (`StableFoundryId`, a pure
domain utility, no new dependency). Every `/create` call sets `keepId:
true, override: true`, making re-pushing the same source entity an
in-place replace rather than a duplicate. `folder` is a **top-level**
field on the `/create` request body (a folder UUID), not nested inside
`data` — confirmed against the relay's reference, corrected from this
ADR's original draft. One "Articles" and one "Handouts" `Folder` document
per world get the same create-with-stable-id treatment, referenced via
that top-level `folder` field on every JournalEntry pushed into that world.

### Embedded images: a real upload endpoint, not a script-execution workaround
An article/handout body's `![alt](/api/media/{mediaId}/content)`
references are extracted, their bytes fetched via `MediaContentQueryPort`,
and uploaded into Foundry's own `Data/` storage at a fully server-derived
path (`campaign-organizer/{worldId}/{stableId}.{ext}`) via the relay's
`POST /upload` endpoint — `{clientId, path, source: "data", filename,
mimeType, overwrite}` as query parameters plus a JSON body
`{"fileData": "data:<mime>;base64,<...>", "mimeType": "...", "overwrite":
true}`. This ADR originally assumed no dedicated upload endpoint existed
and designed around the relay's arbitrary-script `/execute-js` primitive
plus a hand-rolled `FilePicker.browse`-then-upload existence check for
idempotency; the relay's actual published reference confirms a first-class
`/upload` endpoint instead, which is used here — no script generation, no
JS-string-escaping concerns, and no need to reason about `/execute-js`'s
eval/return semantics at all, since this feature has no other use for it.
Idempotency comes directly from the endpoint's own `overwrite: true` flag
against the same server-derived stable path — a re-push of an unchanged
image is one upload call to the same path, not a browse-first check.

A separate, lower size cap than this app's own 50MB upload limit still
applies here (`FoundryPushLimits.MAX_IMAGE_BYTES`) — base64 inflates the
payload roughly a third before it crosses the relay's HTTP+WebSocket hop
into a live Foundry client (the relay documents a 250MB ceiling on
base64-encoded data, well above what's practical to push per article
image regardless). An oversized image is skipped (not truncated); the
original `/api/media/...` reference is left in the pushed body, and the
skip is surfaced back to the caller in `FoundryPushResult.warnings` so the
UI can show it rather than the push silently "succeeding" with a missing
picture.

### Not yet verified against a live relay+Foundry instance
The relay's own published API reference (foundryrestapi.com/docs/api)
resolved most of this ADR's original open questions — `/create`'s `folder`
placement, `/upload`'s existence and shape, and `GET /clients`'s real
response shape (a list of client objects with `clientId` among other
fields, not a flat id-string list — the original draft's guess here was
wrong and was fixed in `FoundryRelayClient` before the first push feature
was built on top of it). What's genuinely still unverified, because the
reference docs don't cover them and only a live instance can:
- Whether the specific API key scopes this feature needs
  (`entity:write`, `file:write`, `clients:read`) are exactly what the
  user's relay setup grants by default, or need explicit enabling.
- Foundry's actual `RollTable`/`TableResult` document schema for the
  Foundry version in use (field names have shifted across major Foundry
  versions), and whether its dice-formula syntax accepts this app's
  `DiceExpression` grammar unchanged.
- The exact `Cards`/`Card` embedded-document field that should carry a
  card's Markdown body — Foundry's core `Cards` schema is less
  document-oriented than `JournalEntry`/`RollTable` and varies by version.

Each of these is called out again as an explicit manual-verification step
in the implementation plan for the phase that first needs it, not glossed
over or assumed correct.

## Consequences
- `media` gains one new published-port method
  (`MediaContentQueryPort.loadInWorld`) — a small, permanent addition to its
  public surface, but a strict subset of what `LoadMediaContentUseCase`
  already does internally (same repository call, same storage call, just
  world-scoped rather than RLS-bypassing).
- `worldbuilding`'s `ArticleRenderPort`/`WikiLinker` gain a second render
  mode; the existing HTML `render()` path and its tests are untouched by
  construction (shared matcher, different formatter callback).
- Two new tables, RLS-protected from birth (Tier 2, ADR-0114):
  `foundry_connections` (one per world, holding the encrypted relay
  credentials) and `foundry_pushed_documents` (push-tracking, keyed by
  world/entity type/entity id). `RowLevelSecurityFitnessIT` grows
  accordingly.
- A second `TextEncryptor` bean/key-salt pair to manage in deployment
  (`app.foundry.encryption-key`/`encryption-salt`, its own dedicated pair,
  never reusing MFA's) — same operational shape this app already accepted
  for other per-purpose secrets, one more secret, not a new mechanism.
- **The encrypted API key is never returned by any endpoint.**
  `FoundryConnectionView`/`FoundryConnectionResponse` carry only
  `relayBaseUrl`, `clientId`, and `configured: boolean` — there is no
  `apiKey`/`apiKeyEncrypted` field on either the GET or PUT response, by
  construction (the record simply has no such field to leak).
- **The API key is never logged.** `FoundryRelayAdapter` reports a failed
  relay call using only the relay's own base URL (never secret) and the
  failure's HTTP status/exception type — never the raw exception message or
  request details, which could in principle echo header values.
- **The Foundry connection is deliberately excluded from the full-world
  export/backup bundle.** `interchange/export`'s `ExportService` only
  composes the published ports it explicitly wires in; since
  `interchange/foundry` exposes no published port at all, there is nothing
  for `ExportService` to reach, and a world's downloadable backup JSON never
  contains a relay URL, clientId, or encrypted API key.
- No bulk "push whole world" action ships yet — `FoundryPushService`'s
  internals (introduced in the phase that adds the first push use case) are
  shaped around one entity-agnostic private method every entity-specific
  push calls through, so adding a bulk use case later is additive, not a
  redesign.
- Foundry-side failure modes (relay unreachable, wrong API key, Foundry
  session not connected) surface as a single `FoundryRelayException`
  (`interchange.foundry.domain`, mirroring `ai.domain.AiUnavailableException`)
  mapped by the central `DomainExceptionAdvice` to `503 Service Unavailable`
  — no `ResponseStatusException` in application/domain code, same pattern
  every other domain exception in this app already uses.

## Alternatives considered
- **Storing the relay connection as a field on `World`, reusing
  `LayerStyle`'s JSONB-column pattern** — rejected: wrong bounded context
  for the plaintext parts (owned by `worldbuilding`'s own aggregate, not
  `interchange`), and no clean way to keep just the API key separately
  encrypted inside a generic JSONB blob without inventing a bespoke
  partial-column-encryption scheme; a dedicated table with one
  `VARCHAR(500)` encrypted column is simpler and matches
  `campaign_calendar_feeds`'s precedent for an interchange-owned,
  1:1-with-parent credential.
- **Format 1 (HTML) journal pages, reusing `ArticleRenderPort.renderBody`
  as-is** — rejected: Foundry's HTML journal rendering would need to trust
  arbitrary sanitized-but-still-HTML content, and the existing wiki-link
  anchors carry a `data-article-id` attribute and `href="#"` that mean
  nothing outside this app's own SPA — pure noise (or a dead-looking link)
  inside Foundry.
- **A WebSocket client in this backend, talking to the relay's real-time
  channel directly** — rejected: the relay's plain HTTP surface already
  covers everything this feature needs (`/create`, `/upload`, `/clients`);
  adding WebSocket plumbing for no functional gain would be scope creep
  against an already-confirmed-sufficient integration.
- **Uploading images via the relay's `/execute-js` + `FilePicker.upload`**
  — this ADR's original design, before the relay's actual API reference was
  available — rejected once a first-class `/upload` endpoint was confirmed
  to exist: no reason to generate and evaluate arbitrary JavaScript in a
  live Foundry client for something the relay already does safely and
  idempotently (`overwrite: true`) as a plain HTTP call.
- **A shared or environment-configured default relay** — rejected outright,
  not merely deprioritized: this app's core privacy model treats every
  world's content and integrations as strictly private to its owning
  account, and a shared relay default would silently violate that the
  moment two accounts' worlds pointed at the same Foundry session.
