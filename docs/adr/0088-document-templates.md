# ADR-0088: General-purpose document templates

- Status: Accepted
- Date: 2026-09-01

## Context
Session-zero sheets (tone, safety tools, character-creation guidelines) and
similar per-system reference documents currently have to be rebuilt as
one-off handouts for every campaign or oneshot, even when running the same
system repeatedly. ADR-0052 already generalized the field-template engine
(`FieldTemplate`, `TemplateBuilder`, `TemplateForm`, `FieldTemplatesPanel`)
to serve both `CHARACTER` and `STATBLOCK` kinds behind one builder. The
issue asks for a third kind reusing that same builder — "design the fields
your system's session-zero document needs once per system, then fill and
print it per campaign, no new template-building machinery required."

The template-*building* side already delivers on that claim close to
verbatim: `FieldTemplate`, `TemplateForm`, and `BuiltinFieldTemplates` need
zero changes for a third kind; `TemplateBuilder` and `FieldTemplatesPanel`
have one small kind-branch each (a label, a `SelectItem`, a
delete-consequence string) — mechanical additions, not new machinery. The
open question was the *instance* side: `CharacterSheet` and `Statblock`
already both reference a `FieldTemplate` by id and hold a filled-in values
map, but they are two fully independent aggregates, not one shared
mechanism — so does a `DOCUMENT` kind need a third one?

## Decision
- **`TemplateKind` gains `DOCUMENT`.** `kind` remains immutable after
  creation, unchanged from ADR-0052.
- **A new `Document` aggregate**, inside the existing `characters` bounded
  context (sibling to `sheet`/`statblock`/`template` — keeps the whole
  field-template ecosystem in one context instead of fragmenting it across
  contexts for what is, underneath, the same "filled template" shape).
  `CharacterSheet` is the closer analog to clone (not `Statblock`): a
  Document has no freeform-fallback mode the way a Statblock's legacy
  `stats`/`notes` do — it is always template-driven, so there's nothing to
  "fall back to" if its template goes away. `Document` deliberately omits
  `CharacterSheet`'s `articleId` link too — a generic document has no
  natural wiki-link target, and the issue doesn't ask for one.
- **`ON DELETE CASCADE` from the template**, matching `CharacterSheet`'s FK,
  not `Statblock`'s `SET NULL` — for the same "no freeform fallback"
  reason: a Document's values only mean anything in terms of its template's
  field keys, so losing the template leaves nothing coherent behind.
- **Printing reuses `SheetPdfGenerator` as-is** — it already takes
  `(title, sections, values)` with no `CharacterSheet`-specific type
  anywhere in its signature, so a new `DocumentPdfController` calls it
  directly; no move, no refactor, no new rendering code.
- **The fill-in/read UI reuses `<TemplateForm>` as-is**, exactly like both
  existing kinds already do, in a new `DocumentsPanel.tsx` mirroring
  `CharacterSheetsPanel.tsx`'s shape minus the article-link field.
- **No builtin `DOCUMENT` templates in v1.** `BuiltinFieldTemplates`
  already filters by kind and simply returns nothing for one with no
  entries defined — a session-zero starter template is a pure additive
  follow-up, not blocked by anything here.

## Consequences
- A third full instance aggregate (domain/application/adapter/migration/
  controller) is added, following `CharacterSheet`'s exact shape — more
  code than a "just add an enum value" change would suggest, but consistent
  with ADR-0052's own finding that instances (unlike templates) don't share
  a mechanism in this codebase.
- `FieldTemplateService`'s delete path needs no new kind branching — as
  with `CHARACTER`/`STATBLOCK` today, the differing cascade-vs-null
  behavior lives entirely at the DB foreign-key layer, not in application
  code.
- A GM designs a session-zero (or any other reference document) template
  once per system, then creates one filled `Document` per campaign that
  uses it — printable as a fillable PDF via the same mechanism character
  sheets already use.

## Alternatives considered
- **Generalize `CharacterSheet` in place** (rename it, make `articleId`
  optional, allow any kind): rejected — destabilizes an existing, working,
  heavily-used feature for a tangential use case, and blurs "a character's
  sheet" and "a generic filled document" into one confusing concept.
- **Reuse `Statblock`'s shape instead**: rejected — its freeform-fallback
  `stats`/`notes` fields and `SET NULL` delete semantics exist specifically
  because a statblock is a going concern independent of its stat layout;
  that reasoning doesn't apply to a generic document, which has no
  identity apart from its template.
- **A single polymorphic "instance" aggregate for all three kinds**: would
  match the issue's "no new machinery" framing more literally, but
  requires either a discriminator-driven values interpretation (fragile)
  or collapsing `CharacterSheet`'s and `Statblock`'s already-shipped,
  independently-evolved behavior (article links, campaign beat references,
  freeform fallback) into one aggregate — a much larger, riskier rewrite
  for a net-new feature that doesn't need any of that shared behavior.
