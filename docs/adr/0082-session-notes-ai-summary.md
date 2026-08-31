# ADR-0082: On-demand AI summary of session GM notes

- Status: Accepted
- Date: 2026-08-31

## Context
Sessions have long-form private GM notes but no quick way to skim them
before the next session — the owner has to reopen and re-read the whole
field. The `ai` context already has a working, provider-agnostic text-
generation pipeline (ADR-0064/ADR-0065): try each configured provider in
priority order, skip unconfigured ones, fall back past failures, surface a
503 if every provider fails. That infrastructure is generic enough to drive
a second, differently-shaped use case (condensing existing text) without
duplicating the provider-selection logic.

This ships alongside a session "recap" preview (read view showing the
session's summary, story beats, and GM notes) that gives the AI summary
somewhere natural to live: a "Summarize notes" button in that view.

## Decision
- **`ProviderFallbackTextGenerator`** (`ai.application.service`, package-
  private) is extracted from `DraftArticleTextService`: it owns the
  "resolve the saved/default priority order, skip unconfigured providers,
  try each configured one, fall back past `TextGenerationFailedException`,
  throw `AiUnavailableException` if all fail" loop, taking just
  `(systemPrompt, userPrompt)`. `DraftArticleTextService` and the new
  `SummarizeSessionNotesService` both depend on it instead of duplicating
  the loop — the two use cases differ only in the prompts they build.
- **`SummarizeSessionNotesUseCase`** / `SummarizeSessionNotesService**` is a
  new, separate use case (not a mode of `DraftArticleTextUseCase`): it takes
  raw `notes` text, not instructions + existing content + level/kind, and
  its system prompt is summarization-specific ("condense what's given, don't
  invent new events/names") rather than drafting-specific. Reusing
  `DraftArticleTextUseCase`'s shape would have forced an awkward mapping
  (notes as "existingContent", empty "instructions") for no shared behavior
  beyond the provider loop, which is already factored out separately.
- **`SessionNotesToSummarize`** is a small validating domain record (non-
  blank, capped at 20000 chars) mirroring `DraftInstructions`'s bounds —
  the domain's only real invariant.
- **The system-agnostic constraint still applies.** Even though this
  summarizes the owner's own prose rather than generating new article
  content, the output could still introduce ruleset-specific terminology
  (stat blocks, dice notation, etc.) that isn't in this wiki's scope; the
  summarization prompt carries the same constraint clause used in article
  drafting.
- **On-demand only, not persisted.** `POST /worlds/{worldId}/ai/
  summarize-session-notes` is stateless like `draft-article-text` — the
  frontend calls it when the owner clicks "Summarize notes" and displays the
  result inline; nothing is written to the session or anywhere else. A
  cached/persisted summary was considered and rejected for this pass: it
  would need staleness tracking (has the session's `notes` field changed
  since the summary was generated?) for a feature whose whole value is a
  quick, cheap-to-regenerate skim, not a durable record.

## Consequences
- No schema change, no Flyway migration — `Session` is untouched.
- `DraftArticleTextService`'s public behavior and response shape are
  unchanged; only its internals (constructor now takes
  `ProviderFallbackTextGenerator` instead of the raw provider list +
  settings port) moved.
- The OpenAPI contract reuses the existing `DraftArticleTextResult` schema
  for the new endpoint's response (`{text, provider}`) — same shape, no new
  response type needed at the contract level, even though the Java web
  adapter still has its own `SummarizeSessionNotesResponse` DTO per this
  codebase's one-response-type-per-endpoint convention.

## Alternatives considered
- **Extend `DraftArticleTextUseCase` with a `SUMMARIZE` level**: rejected —
  level already means "how much to generate," not "generate vs. condense
  existing text"; overloading it would make `existingContent` do double duty
  as both "context for a new draft" and "the actual text to summarize,"
  which is confusing at the call site and in the prompt-building code.
- **Persist the generated summary on the session**: rejected for now (see
  Decision) — can be revisited if the owner wants a durable "session recap"
  record rather than an on-demand skim.
