# ADR-0064: AI-assisted text drafting (new `ai` bounded context)

- Status: Accepted
- Date: 2026-08-25

## Context
The owner wants AI help at prep time — seed a few keywords, get a first-draft
NPC description / read-aloud text / plot hook drafted into the article editor,
which they then edit before printing. This is deliberately *not* a live-table
feature: nothing calls out during a session, no player-facing chat, generation
happens while prepping alone at the keyboard, same as writing an article by
hand today.

The owner's server is a Dell OptiPlex — a GPU-less business desktop. Research
this session found CPU-only local inference tops out around 7-8B models at
usable-but-modest speed, and the owner has separately confirmed their
particular box can't even manage that. Local inference is off the table for
now; this feature depends on a cloud LLM API. Two free-tier providers fit a
low-volume hobby app with no card and no training-on-prompts: **Groq**
(fast, ~30 RPM / ~1000+ req/day free) and **OpenRouter** (free-model router,
lower per-day cap but works as a second provider). Neither is guaranteed
to stay free forever or keep its current limits — the owner has no API keys
yet, so this ships with generation wired up but unverified against live
traffic until they sign up.

## Decision
- **New bounded context: `ai`**, alongside the existing six
  (`worldbuilding`, `campaign`, `characters`, `media`, `whiteboard`,
  `interchange`). It's not generic infra like `auth`/`config`/`security`:
  provider fallback order is a real policy decision worth encoding as
  application-layer logic, not a `RestTemplate` call bolted onto a
  controller. `docs/architecture/clean-architecture-analysis.md`'s context
  map and `CLAUDE.md`'s bounded-context list are updated accordingly.
- **No persistence ring.** The context is stateless — a draft request goes
  out, a result comes back, and once the owner accepts it the text becomes
  part of an article/beat/etc. through that context's own normal save path.
  There's nothing for `ai` itself to persist, so unlike every other context
  it has no `adapter/out/persistence` or JPA entity. This isn't the "CRUD
  exemption" the harness bans (skipping the three-model split for a context
  that *does* persist something) — there's simply no persistence to split
  three ways. Domain (`DraftRequest`/`DraftResult`) and web DTOs still exist
  and are still mapped by MapStruct.
- **Fallback is application-layer orchestration, not adapter-internal.**
  `TextGenerationPort` (application/port/out) is implemented once per
  provider (`GroqTextGenerationAdapter`, `OpenRouterTextGenerationAdapter`,
  `@Order(1)`/`@Order(2)`). `DraftArticleTextService` (application/service)
  is injected with the ordered `List<TextGenerationPort>` and tries each in
  turn, throwing `AiUnavailableException` (mapped to 503, not the generic
  422, so a client can distinguish "try again" from "this request is
  invalid") only if every provider fails. Each adapter is a dumb HTTP call;
  the retry/fallback *policy* is the one piece of real logic this context
  has, so it lives in application, not hidden inside an adapter — matches
  how out-ports are supposed to be swappable/composable rather than
  self-contained black boxes.
- **Spring's `RestClient`** for the outbound HTTP calls — already available
  transitively via `spring-boot-starter-web`, no new dependency. Both
  providers speak the same OpenAI-compatible `/chat/completions` shape, so
  the two adapters are nearly identical (base URL, key, model differ).
- **Config**: `app.ai.groq-api-key` / `app.ai.groq-model`,
  `app.ai.open-router-api-key` / `app.ai.open-router-model`, sourced from
  `GROQ_API_KEY`/`GROQ_MODEL`/`OPENROUTER_API_KEY`/`OPENROUTER_MODEL` env
  vars (same pattern as `APP_PASSWORD`/`APP_JWT_SECRET`, ADR-0006). An
  adapter with no key configured for it is skipped rather than attempted
  and failed, so running with only one provider configured (or none, and
  the button naturally errors) both work.
- **Endpoint**: `POST /api/worlds/{worldId}/ai/draft-article-text`. `worldId`
  is in the path only for URL-shape consistency with every other endpoint
  (and so the frontend's existing `xApi(worldId)` client-factory pattern
  keeps working) — the request carries the instructions and current editor
  content directly, so this call never reads world data server-side. If a
  later feature (e.g. session recaps) needs the AI context to read existing
  campaign/article data, that goes through a published port on the source
  context, same as any other cross-context read — never a direct
  reach-in.

## Consequences
- First concrete use: an "AI draft" button in the article editor
  (`MarkdownEditor`). Session-recap and plot-hook generation (discussed,
  not yet built) can reuse the same `TextGenerationPort`/fallback machinery
  — likely a new in-port per use case (`DraftSessionRecapUseCase`, etc.)
  rather than a fat "generate anything" port, per the harness's "one
  inbound port = one use case" rule.
- No generation history/audit trail exists yet (nothing is persisted). If
  that turns out to matter, it's an additive change (add the persistence
  ring the harness's three-model rule expects, once there's actually
  something to persist) — not a rework of this ADR's shape.
- Free-tier limits are the real ceiling on how this gets used, not app
  design — expect to hit Groq's daily cap before OpenRouter's on any heavy
  prep day, which is exactly why the fallback exists.

## Alternatives considered
- **Route through the frontend directly to Groq/OpenRouter, no backend
  involvement**: keeps API keys out of the browser bundle is the whole
  reason not to — a key shipped to the client is a public key. Backend
  proxy is mandatory, not a style choice.
- **A single adapter with fallback logic baked in** (call Groq, on
  exception call OpenRouter inline): simpler file count, but hides a real
  policy decision inside adapter code and makes it harder to unit-test the
  fallback behavior without mocking HTTP — application-layer orchestration
  over an ordered port list is barely more code and is trivially unit
  -testable with fake ports.
- **Local model as a fallback tier under the cloud providers**: ruled out
  by the OptiPlex's confirmed hardware limits for this owner; the port
  abstraction doesn't preclude adding a local adapter later if that
  changes.
