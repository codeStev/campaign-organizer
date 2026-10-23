# ADR-0121: Companion export of session summaries and transcripts

- Status: Accepted
- Date: 2026-10-23
- Extends: ADR-0119 (API tokens), ADR-0120 (companion API)

## Context
DiscordWhispr records a session, transcribes and labels it locally, and writes a
summary. The GM wants both stored in the organizer: the summary on the
campaign's session (created automatically if there is none), and the transcript
so that details too small for a summary can be found later. Players' and the
GM's private conversation must not end up here: the organizer needs the story,
not who the people at the table are.

## Decision

### Endpoints (all under `/api/companion`, API token or full login)
| endpoint | scope |
|---|---|
| `GET  /campaigns/{campaignId}/sessions` | `sessions:write` |
| `POST /campaigns/{campaignId}/sessions` | `sessions:write` |
| `PUT  /campaigns/{campaignId}/sessions/{sessionId}/summary` | `sessions:write` |
| `PUT  /campaigns/{campaignId}/sessions/{sessionId}/transcript` | `transcripts:write` |
| `DELETE /campaigns/{campaignId}/sessions/{sessionId}/transcript` | `transcripts:write` |

A campaign or session of another account is 404 (ADR-0109); ownership is
checked in the application service, not left to row-level security alone.

**List** returns, per session, only `id`, `title`, `sessionNumber`, `date`,
`hasSummary`, `updatedAt`: never summary or note text, so a write-scoped token
cannot read the GM's prep.

**Create** takes `title`, `date`, optional `sessionNumber` (default: highest
number in the campaign plus one) and returns the same shape.

**Summary** takes `{ summary, expectedUpdatedAt? }` (Markdown, at most 20000
characters, the existing session limit). The server never overwrites a summary
the caller did not write: the write is accepted when the session has no summary
yet, or when `expectedUpdatedAt` equals the session's current `updatedAt`;
otherwise 409 (problem+json). Returns the new `updatedAt`, which the client
keeps for the next export. `[[Wiki links]]` in the text resolve to articles as
usual (ADR-0116).

**Transcript** replaces the whole transcript of the session in one call
(idempotent): `{ lines: [{ offsetMs, speaker?, kind, text }] }`, in order.
`DELETE` removes it. At most 20000 lines, 2000 characters each.

### The organizer enforces the privacy rule itself
A transcript line's `kind` is `IN_CHARACTER` or `NARRATION` and nothing else; any
other value is a 400. So even a buggy or modified client cannot push meta talk,
off-topic talk or unclear lines. `speaker` is the name of the character being
voiced (free text, at most 100 characters); there is no field for a player's
name or a Discord id, and none is stored.

### Model (campaign context, own domain/entity/DTO models, MapStruct)
- `SessionTranscript` aggregate with value objects `TranscriptLine`
  (`position`, `offsetMs`, `speaker`, `kind`, `text`), owned by a session.
- Flyway migration: `session_transcript_lines`, tier 3 row-level security
  (reachable through session, campaign, world), `ON DELETE CASCADE` from the
  session so deleting a session deletes its transcript.
- Reading: `GET /api/worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/transcript`
  for the GM's own login, plus a read-only transcript panel on the session page.
  Search across transcripts is not part of this ADR.

### Client protocol (DiscordWhispr)
1. List the campaign's sessions. Reuse the one dated the recording day (several:
   the first without a summary, else create a new one); none: create one
   (title `Session <date>`). The chosen session id is remembered per recording,
   so re-exports go to the same session.
2. Write the summary with `expectedUpdatedAt` from the last export. A 409 means
   the GM edited it since; the client then skips it and says so unless told to
   force, in which case it re-reads `updatedAt` first.
3. Replace the transcript. Only lines labelled in-character or narration, and
   only those a person reviewed or the classifier was sure about (confidence at
   least 0.9), are sent, with the character name as speaker.

### Consent and revocation
Exported text is derived from a player's speech, so it follows the player's
consent. Because the transcript is replaced as a whole, revoking consent is
honoured by re-running the pipeline on the session (their audio is already
deleted by the recorder) and exporting again, which replaces the organizer's
copy. The recorder's consent notice must say that the transcript text, without
names and without off-topic talk, may be copied into the GM's campaign
organizer, and that a revocation is applied to the organizer copy on the GM's
next export run.

## Consequences
- The organizer holds the story text of sessions but nothing identifying the
  people at the table.
- Re-exporting is safe and repeatable; a revocation after an export needs that
  re-export (a manual step, stated in the consent notice).
- A write-scoped token can create sessions and write summaries and transcripts
  of the account's campaigns, but read nothing of them beyond the list above.

## Alternatives considered
- **Per-line deletion by speaker id**: needs a stable player identifier stored in
  the organizer. Rejected: that is exactly the personal data to keep out.
- **Client-side filtering only**: a client bug would leak private talk. The
  server-side allowlist makes the rule hold regardless of the client.
- **Append-only transcript uploads**: re-runs would duplicate lines and a
  revocation could not remove anything. Replace-whole is idempotent.
- **Returning the existing summary in the session list**: would let a
  write-scoped token read prep and summaries; `hasSummary` and `updatedAt` are
  enough for the overwrite check.
