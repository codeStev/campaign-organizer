# ADR-0075: AI-draft levels, kind-aware prompts, and a dialog UI

- Status: Accepted
- Date: 2026-08-29

## Context
ADR-0064's "AI draft" always produced one thing: a full article-length draft
from a `window.prompt` string. In practice the owner wants cheaper/faster
help earlier in prep too — a short teaser to spark an idea, or just the
facts already implied by what they typed (without the model inventing new
places/characters that then have to be caught and deleted during review) —
not only the finished-article mode. They also want the generation to read
differently depending on what kind of article is being drafted (a CHARACTER
entry shouldn't read like a LOCATION entry), and want the whole interaction
— instructions, level, kind — collected through a real dialog with
explained choices, not a bare browser prompt.

Separately, the same conversation asked to remove `window.prompt`/
`window.confirm`/`window.alert` everywhere in the frontend, not just here —
those are the same category of jarring, undiscoverable, unstyled browser
chrome the AI-draft dialog was already replacing.

## Decision
- **`DraftLevel`** (`QUICK_INSPIRATION`, `READ_ALOUD`, `BASIC_INFO`,
  `FULL_DRAFT`) is a new domain enum in the `ai` context, threaded through
  `DraftInstructions`/`DraftArticleTextCommand`/`DraftArticleTextRequest`.
  Optional on the request, defaulting to `FULL_DRAFT` — today's only
  behavior — so the one existing caller keeps working unchanged if it (or a
  mid-deploy stale build of it) omits the field. `READ_ALOUD` was added
  after the first three shipped: it isn't another point on the same
  "how much" ladder as the other three, but a distinct *register* (short,
  second-person, table-ready boxed text) — kept as one more flat option
  rather than a second axis crossed with level, since read-aloud text has
  an inherent natural length (spoken aloud in one breath) that doesn't
  benefit from being combined with "quick" vs "full."
- **`ArticleKind`** (`GENERIC`, `CHARACTER`, `LOCATION`, `ORGANIZATION`,
  `SPECIES`, `ITEM`, `EVENT`) is a second new domain enum in `ai`, deliberately
  *not* a reference to `worldbuilding.domain.wiki.ArticleTemplate` — the
  `contextsOnlyUsePublishedPorts` ArchUnit rule forbids `ai` from importing
  another context's domain type directly, and `ArticleTemplate` isn't
  published. `ArticleKind` mirrors its 7 values as its own decoupled type.
  Same optional/default-to-`GENERIC` treatment as level. The OpenAPI contract
  still reuses the existing `ArticleTemplate` schema component for this
  request field (`template`) — schema reuse is a contract-level structural
  match, not a Java-level import, so it doesn't create the coupling the
  ArchUnit rule exists to prevent.
- **Prompt composition, not a 3×7 prompt matrix.** `DraftArticleTextService`
  builds the system prompt from an intro clause per level
  (`LEVEL_INTROS: Map<DraftLevel, String>`), an optional focus clause per
  kind (`KIND_GUIDANCE: Map<ArticleKind, String>`, blank for `GENERIC`), and
  the existing system-agnostic constraint + output-hygiene clauses shared by
  every combination unchanged. `BASIC_INFO`'s clause explicitly instructs the
  model not to invent new named places/characters/organizations beyond what
  the instructions or existing content already establish — a prompt-level
  constraint only, not independently validated server-side (no NER pass
  against a "known entities" list exists or is planned; trusted like the
  rest of this feature's output, reviewed by the owner before saving).
- **Frontend dialog** replaces `window.prompt`/`window.alert` in
  `MarkdownEditor`'s AI-draft flow: a shadcn `Dialog` (first real usage of
  the previously-installed, unused `dialog.tsx`) with a `Textarea` for
  instructions, a `RadioGroup` (newly installed) for the level — three named
  options with materially different behavior, each paired with a shadcn
  `Tooltip` description rather than static inline text — and, when the
  caller wires it up, a `Select` for article kind. Provider failures render
  as an inline destructive shadcn `Alert` (newly installed) inside the
  still-open dialog instead of `window.alert`, so the error is visible next
  to the form (not behind the modal, not timed out) and the typed
  instructions survive a retry.
- **The kind selector shares state with the article's own kind field**,
  rather than being an independent per-generation choice: `MarkdownEditor`
  takes `articleTemplate`/`onArticleTemplateChange` props and binds the
  dialog's `Select` directly to them. Picking a kind in the AI-draft dialog
  therefore also updates the article being edited — a deliberate product
  decision (the two pickers would otherwise show two different "kind of
  article" answers for the same article, which reads as a bug).
- **Repo-wide `window.prompt`/`window.confirm` sweep**, same session, same
  motivation: two new small reusable components,
  `frontend/src/components/PromptDialog.tsx` (shadcn `Dialog`, single text
  field) and `frontend/src/components/ConfirmDialog.tsx` (shadcn
  `AlertDialog`), both **fully controlled** (`open`/`onOpenChange` owned by
  the caller) rather than trigger-wrapped, since several call sites open the
  dialog from something other than a plain button click — a file picker's
  `onChange` (`MapsView`'s map-naming prompt), a double-click on a canvas
  node (`WhiteboardCanvas`'s add/edit-node-text and connection-label
  prompts), or a conditional guard that should only sometimes show a dialog
  (`CheatSheetView`'s discard-unsaved-changes gate, `WorldsPage`'s
  replace-everything-with-backup confirm). `ConfirmDeleteDialog` (trigger-
  wrapped, used by ~15 existing delete flows) is untouched — different,
  already-proven shape, no reason to disturb it for this.

## Consequences
- `DraftArticleTextResult` (the response shape) is untouched — level and
  kind are request-only, so there's no versioning/back-compat concern on the
  response side, and today's full-draft behavior is preserved bit-for-bit as
  the default when both fields are omitted.
- No persistence added anywhere in this change — `ai` stays stateless per
  ADR-0064; the dialog/prompt/confirm components are pure UI state.
- The frontend now has zero `window.prompt`/`window.confirm`/`window.alert`
  calls (verified by repo-wide grep) — every user-facing prompt/confirm/error
  in the app goes through a styled, themeable, keyboard-accessible shadcn
  component instead of unstyled browser chrome.

## Alternatives considered
- **Fold level into the user-turn prompt instead of swapping the system
  prompt**: rejected — "never invent named entities" is a hard constraint,
  the same category as the existing system-agnosticism constraint, and
  belongs in the system turn alongside it rather than diluted into free-text
  the model is more likely to deprioritize.
- **A native `<select>`/radio group instead of shadcn `RadioGroup`/`Select`**:
  rejected per the standing shadcn-first project rule.
- **Trigger-wrapped `PromptDialog`/`ConfirmDialog`** (mirroring
  `ConfirmDeleteDialog`'s `AlertDialogTrigger asChild` pattern): rejected as
  the single shared shape for the sweep — it fits `WorldsPage`'s "Replace
  everything" button fine, but doesn't fit a file-picker-triggered or
  conditionally-shown dialog without an awkward hidden/synthetic trigger
  element. Fully controlled components handle every call site uniformly.
