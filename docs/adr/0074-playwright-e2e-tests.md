# 74. Playwright end-to-end tests for the frontend

Date: 2026-08-29
Status: Accepted

## Context

Manual bug-hunting against a locally seeded instance surfaced several
frontend defects (no save feedback, a toolbar with no active-state
indication, a name overflow bug) that unit tests wouldn't catch because
they're about actual rendered behavior in a browser, not component logic.
Verifying the fixes by hand, and re-verifying them on every future change,
doesn't scale. The frontend had no browser-level test tooling at all.

## Decision

**Playwright, as a `frontend` devDependency, tests against an
already-running stack.** `frontend/playwright.config.ts` points at
`http://localhost:3001` by default (override via `E2E_BASE_URL`); specs live
in `frontend/e2e/*.spec.ts` and run with `npm run test:e2e` from `frontend/`.

Not a mocked-network component-test setup (e.g. Testing Library + MSW):
these tests exist specifically to catch "the button is there but does
nothing visible" and "the markup renders but the CSS makes it wrong" classes
of bugs, which only show up against a real render in a real browser talking
to the real backend.

Test data is created per-spec via direct API calls (`frontend/e2e/support.ts`
— login for a token, then `POST /worlds` etc.), not the shared seed data,
so specs are self-contained and don't depend on run order or manual seeding.
Each spec deletes its own world in `afterEach`. This is a single-user local
dev database (ADR-0005, and confirmed non-production for this repo's local
compose stack), so tests write and clean up directly against it rather than
requiring a disposable database per run.

Interactive elements central to a spec get a `data-testid` attribute rather
than relying on text/placeholder matching, which breaks silently on copy
changes. Not applied blanket across the codebase — added where a spec
actually needs a stable hook.

## Consequences

- Running the suite requires the app to already be up (`docker compose up`
  or equivalent) — there's no built-in webServer bootstrap. Acceptable for
  now given this is a local single-instance app with no CI pipeline defined
  yet; revisit if/when CI is added.
- Coverage is intentionally narrow so far: auth, the markdown editor
  (formatting rendering + toolbar active state), map name overflow, and
  table/deck save feedback — the areas just fixed. Broader flows (articles,
  campaigns/sessions, whiteboards, character sheets, calendars/timelines,
  AI settings, export/import) are good candidates for follow-up specs but
  aren't covered yet.
- Browser binaries (`npx playwright install chromium`) are a separate,
  gitignored download, not vendored into the repo.
