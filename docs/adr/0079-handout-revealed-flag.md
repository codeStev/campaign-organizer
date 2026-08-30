# 79. Handout revealed flag (FR-46 follow-up)

Date: 2026-08-30
Status: Accepted

## Context

A GM often preps a handout (a letter, a wanted poster) well before the
players actually see it — sometimes sessions in advance. Nothing in the
handouts list distinguished "written and ready" from "already shown at the
table," so a GM skimming the list before a session had no quick way to tell
which props were still secret and which had already been revealed.

## Decision

Add a `revealed` boolean to the `Handout` aggregate, defaulting to `false`:

```
Handout {..., sessionId, sortOrder, revealed, createdAt, updatedAt}
```

- Migration `V30__handout_revealed.sql`: `NOT NULL DEFAULT false`, so every
  existing handout starts as not-yet-revealed.
- No new endpoint: `revealed` rides the existing create/update request and
  response, the same way `sessionId` (ADR-0077) does.
- Frontend: a small toggle next to each handout in the list, flipped
  independently of editing the handout's body.

This is explicitly **not** an access-control gate — this app has no
player-facing surface (single-user, per the project's scope) — and it does
not affect printing; a GM may print a handout well before revealing it.
It is a screen-only prep aid, same spirit as a beat's `done` checkbox.

## Consequences

- One more boolean on an already-simple aggregate; no new bounded-context
  concerns.
- Because it's not access control, no code anywhere should treat `revealed
  == false` as a reason to hide a handout's content from the GM's own view
  or from printing.
