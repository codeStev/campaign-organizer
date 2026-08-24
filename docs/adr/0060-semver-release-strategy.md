# ADR-0060: SemVer release strategy

- Status: Accepted
- Date: 2026-08-24

## Context
CI (`.github/workflows/ci.yml`) already tags published images with
`vX.Y.Z`/`vX.Y`/`vX` via `docker/metadata-action` whenever a `v*.*.*` git tag
is pushed (ADR-0057), and `deploy/registry/README.md` documents `git tag
v0.2.0 && git push origin v0.2.0` as the release mechanic. None of this has
actually been used yet (no tags exist), and beyond the image-tagging
mechanics nothing is decided: `backend/build.gradle.kts` and
`frontend/package.json` each hardcode an independent `0.1.0` with nothing
tying either to a release; there's no policy for what a major vs. minor vs.
patch bump means for a single-user, self-hosted app with no external API
consumers; and there's no changelog, so "what changed in this release" has
no answer beyond reading commit history by hand.

## Decision

### The git tag is the only source of truth
A pushed `vMAJOR.MINOR.PATCH` tag on `master` is the sole record of the
app's version. `backend/build.gradle.kts`'s `version` and
`frontend/package.json`'s `version` are left as a static, unmaintained
placeholder (`0.0.0`) — nothing reads or publishes them, so there's nothing
to keep in sync and no drift to catch. All three published images
(`campaign-organizer-backend`, `-frontend`, `-combined`) are versioned
identically off the same tag, per the existing CI behavior.

### What bumps what
Scoped to what actually matters for this app: an existing self-hosted
deploy, with the bundled frontend as the only API consumer (ADR-0005,
ADR-0008).

- **MAJOR** — breaking an existing deploy: a Flyway migration with no
  forward-compatible path (data loss or a schema shape the previous version
  can't read), a removed/renamed required env var, or a `docs/api/openapi.yaml`
  change that isn't backward-compatible (and isn't shipped in the same
  release as the frontend change that handles it).
- **MINOR** — new backward-compatible functionality: new endpoints,
  additive migrations, new UI features.
- **PATCH** — backward-compatible bug fixes, and infra/doc-only changes with
  no deploy-behavior change.

### Release process
1. `master` is green (CI passing) and contains everything for the release.
2. `git tag vX.Y.Z -m "..." && git push origin vX.Y.Z`.
3. The existing `publish` job builds and pushes the three semver-tagged
   images (unchanged).
4. A new `release` job (gated on the same tag push) runs
   `gh release create ${{ github.ref_name }} --generate-notes`, creating a
   GitHub Release whose notes are auto-generated from the PRs/commits merged
   since the previous tag. `.github/release.yml` groups them by PR label
   (`feature`, `fix`, `chore`, ...) when labels are used; unlabeled PRs fall
   into a single "Other Changes" section, so this degrades gracefully
   without requiring immediate label discipline.

No hand-maintained `CHANGELOG.md`: the GitHub Releases page *is* the
changelog, one entry per tag, generated at release time.

## Consequences
- Release note quality depends on PR titles being descriptive — merge
  commits/PR titles are already the project's de facto record of "what
  happened" (visible in `git log`), so this doesn't add a new discipline,
  just surfaces the existing one on the Releases page.
- `backend/build.gradle.kts` and `frontend/package.json` versions are
  intentionally inert. Anyone looking at those files for "the current
  version" will be misled unless they know to check git tags instead — worth
  a one-line comment in each file pointing here.
- The `release` job needs `contents: write`, scoped to just that job (the
  rest of the workflow stays `contents: read`, per least-privilege).
- No automatic bump suggestion (e.g. from conventional commits) — the
  MAJOR/MINOR/PATCH call is made by hand when tagging, using the rule above.
  Acceptable for a single-maintainer project; revisit if that stops being
  true.

## Alternatives considered
- **VERSION file synced into both build files** — rejected: adds a bump
  step and a CI check to every release for no consumer that reads either
  file (no Maven Central/npm publish, ADR-0007/ADR-0057 confirm this is a
  self-hosted personal app, not a published library).
- **`semantic-release`/`release-please` (fully automated version + changelog
  from Conventional Commits)** — rejected for now: requires adopting and
  enforcing a commit-message grammar (`feat:`, `fix:`, ...) beyond the
  existing "imperative, ≤ 50 chars" convention (CLAUDE.md), for a project
  with one committer where the manual tagging step above is cheap.
  Reconsider if commit volume or contributor count grows.
- **Hand-written `CHANGELOG.md`** — rejected: another file to remember to
  update per release; GitHub's generated release notes cover the same need
  with no added step beyond the tag push that already happens.
