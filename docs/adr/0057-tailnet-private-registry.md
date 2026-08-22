# ADR-0057: Tailnet-private Docker registry for deployment images

- Status: Accepted
- Date: 2026-08-21

## Context
Deployment (ADR-0004) is Docker multi-stage images run via `docker-compose.yml`,
today built locally on the server. The server sits behind a Tailscale tailnet
(no public inbound access), and the owner wants CI to build and publish images
so the server can pull and run pre-built, tested images instead of building
from source on the deploy host.

## Decision
Run a **private Docker Registry v2 container on a tailnet node** (the deploy
server, or another node), reachable only from the tailnet:

- The registry container binds to `127.0.0.1` only (`deploy/registry/`); it is
  never exposed on the host's public interfaces.
- **`tailscale serve`** fronts it with HTTPS, using a real, auto-renewing
  Let's Encrypt certificate for the node's tailnet hostname (Tailscale's HTTPS
  Certificates feature) — no manual cert files, no renewal cron job, and no
  "insecure registry" Docker daemon config needed anywhere that talks to it.
- Registry-level basic auth (htpasswd) is layered on top of tailnet-only
  reachability, as defense in depth.
- **CI publishes to it** via a new `publish` job in `.github/workflows/ci.yml`,
  gated to `push` events on `master` only (not PRs, not other branches) and
  gated on the existing `backend`/`frontend`/`docker` jobs passing first. Since
  GitHub-hosted runners aren't tailnet members, the job uses
  `tailscale/github-action` to join the tailnet as an ephemeral node tagged
  `tag:ci` (via a Tailscale OAuth client scoped to that tag), for the duration
  of the job only, then builds and pushes `campaign-organizer-backend` and
  `campaign-organizer-frontend` images.
- **Versioning**: every push to `master` publishes/updates `latest` and a
  `${{ github.sha }}` tag (traceable, but mutable/floating). Pushing a
  `vX.Y.Z` git tag additionally publishes immutable `vX.Y.Z`, `vX.Y`, and `vX`
  image tags — computed by `docker/metadata-action` from the git tag, not from
  the project's own `version` fields (`backend/build.gradle.kts`,
  `frontend/package.json`), which today aren't bumped as part of any release
  process. Cutting a release is a deliberate `git tag vX.Y.Z && git push --tags`,
  not automatic on every merge — see "Alternatives considered".
- The deploy server pulls directly — it's already a tailnet member, so no
  `tailscale serve`/proxy is needed on the pulling side.

## Consequences
- The server no longer needs the repo checked out or a build toolchain to
  deploy; it only needs `docker login`/`docker pull` against the registry's
  tailnet hostname and a `docker-compose.yml` referencing `image:` instead of
  `build:` (left as a follow-up — this ADR covers publishing, not yet
  rewiring the server's compose file to consume it).
- New setup is required outside this repo: a tag-scoped Tailscale OAuth
  client, network access for that tag to the registry, and the GitHub Actions
  secrets/variables `deploy/registry/README.md` lists. How the registry is
  actually exposed to the tailnet (reverse proxy, TLS, etc.) is left to the
  owner's own infrastructure and isn't tracked here (see ADR-0058 for the
  CA-trust mechanism this implies, described without prescribing a specific
  setup).
- Every merge to `master` now pushes fresh `latest` images, whether or not the
  owner is ready to deploy them — deploying is still a manual `docker pull` on
  the server, so this doesn't auto-deploy, just keeps the registry current.
- The `tag:ci` OAuth client is scoped to only ever bring up ephemeral nodes
  tagged `tag:ci`, and the ACL grant should limit `tag:ci` to just the
  registry node's HTTPS port — bounding what a compromised CI run could reach
  on the tailnet.

## Alternatives considered
- **Public registry (GHCR/Docker Hub) instead of self-hosted:** simpler (no
  server-side registry to run, no tailnet-join step in CI), but the owner
  specifically wants images to stay on their own tailnet-private
  infrastructure rather than a third party.
- **Self-hosted GitHub Actions runner living on the tailnet**, instead of
  `tailscale/github-action` joining hosted runners per-run: avoids a
  per-run OAuth key, but trades that for a permanently-running service on the
  tailnet that executes arbitrary code from CI runs — more standing attack
  surface for a personal single-user setup than an ephemeral, narrowly-tagged
  per-run node.
- **Plain HTTP registry + Docker "insecure-registries" daemon config**
  instead of `tailscale serve` HTTPS: traffic is already encrypted by
  Tailscale's WireGuard tunnel either way, so this would have been
  security-equivalent, but it requires reconfiguring and restarting the
  Docker daemon on every consumer (the CI runner, the deploy server) — more
  fragile than a certificate that just works with default Docker behavior.
- **Manually managed TLS certs** (`tailscale cert` run by hand, mounted into
  the registry container) instead of `tailscale serve`: gives the same
  real-certificate outcome, but `tailscale serve` handles renewal
  automatically where a hand-run `tailscale cert` would need its own cron job.
- **Version tags sourced from `build.gradle.kts`/`package.json`** instead of
  git release tags: would publish a versioned image on every master push
  automatically, but silently overwrites the same image tag whenever a commit
  merges without a version bump — a git tag is an explicit, one-time act that
  can't be forgotten into a collision the way a source-file version can.
