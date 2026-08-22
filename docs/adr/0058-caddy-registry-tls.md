# ADR-0058: Caddy fronts the registry's TLS (supersedes ADR-0057's `tailscale serve` choice)

- Status: Accepted
- Date: 2026-08-21

## Context
ADR-0057 chose `tailscale serve` to terminate HTTPS in front of the
tailnet-private registry, specifically to get a real, auto-renewing
certificate with no manual cert handling. In practice, the owner already runs
Caddy as their general-purpose reverse proxy and had already configured it to
front the registry — listening on `<tailnet-hostname>:<caddy-port>` (a port
of the owner's choosing, independent of the registry container's own port)
and proxying to the registry container on `localhost:5000` — before
`tailscale serve` was ever set up. Running both would be redundant; Caddy is
the one actually in use.

Caddy's certificate here comes from its own internal self-signed CA (`tls
internal`, Caddy's default when it can't obtain a publicly trusted cert for a
non-public hostname), not a publicly trusted one like `tailscale serve` would
have provided.

## Decision
Use **Caddy**, already configured by the owner, as the registry's TLS
termination point, and adjust everything downstream that assumed a
publicly-trusted certificate:

- `REGISTRY_HOST` includes whatever external port Caddy's site listens on
  (`<tailnet-hostname>:<caddy-port>`) — not the standard HTTPS port
  `tailscale serve` would have used, and not the registry container's own
  internal `5000`, which Caddy's site config proxies to but which is
  otherwise irrelevant to anything outside the host.
- Because the cert is self-signed, every Docker client talking to the
  registry needs that CA trusted **for this registry host specifically**, via
  Docker's per-registry `certs.d` mechanism
  (`/etc/docker/certs.d/<REGISTRY_HOST>/ca.crt`) — this needs no daemon
  restart and doesn't touch the OS-wide trust store, unlike marking the
  registry as an Docker "insecure registry" (ADR-0057 already rejected that
  approach; this keeps the same reasoning, just swaps which mechanism
  supplies trust).
- CI's `publish` job (`.github/workflows/ci.yml`) installs the CA cert (from
  a new `REGISTRY_CA_CERT` secret) into `certs.d` at the start of every run,
  since the runner is ephemeral and doesn't carry any state between runs.
- `docker/setup-buildx-action` is pinned to the `docker` driver instead of
  its default `docker-container` driver. The container-based driver runs
  buildkit in its own container with its own, separate trust store that
  wouldn't see the host's `certs.d` entry; the `docker` driver delegates to
  the host daemon directly, so it does. This project doesn't need
  multi-platform builds or advanced cache export, so the `docker` driver's
  narrower feature set costs nothing here.
- The deploy server needs the same one-time `certs.d` trust step as CI
  (documented in `deploy/registry/README.md`), since it's also a plain
  Docker client pulling over TLS.
- `tailscale serve` is not used at all; nothing in `deploy/registry/` runs it.

## Consequences
- The registry's certificate is not publicly trusted, which is fine for a
  tailnet-only endpoint but means "just works with default Docker behavior"
  (ADR-0057's stated advantage of the `tailscale serve` approach) no longer
  holds — every consumer needs an explicit, one-time trust step instead.
- If the owner rotates Caddy's internal CA (e.g. by resetting its data
  directory), `REGISTRY_CA_CERT` and the deploy server's `certs.d` entry both
  need updating, or push/pull will start failing TLS verification.
- One less moving part on the server: no `tailscale serve` config to keep in
  sync with Caddy, since only Caddy is actually running.

## Alternatives considered
- **Switch Caddy to obtain a publicly-trusted cert** (e.g. via Tailscale's
  HTTPS Certificates feature, which Caddy can be pointed at) instead of
  trusting its internal CA everywhere: would restore ADR-0057's "no extra
  trust setup" property, but re-configuring the owner's already-working Caddy
  site was out of scope here — this ADR adapts to the existing setup rather
  than asking for it to change.
- **Marking the registry as a Docker "insecure registry"** instead of
  `certs.d`-based trust: skips TLS verification entirely rather than trusting
  a specific CA, and requires editing `/etc/docker/daemon.json` and
  restarting the daemon on every consumer — `certs.d` gets the same practical
  outcome (this one registry host is trusted) without weakening verification
  everywhere else Docker talks to, and without a daemon restart.
- **`docker-container` buildx driver with a custom `buildkitd` config**
  mounting the CA into the builder: achieves the same trust with the
  feature-richer driver, but requires generating and maintaining a
  `buildkitd.toml` and wiring driver-opts to mount the cert file into the
  builder container — meaningfully more moving parts than switching the
  driver, for capabilities (multi-platform, advanced caching) this project
  doesn't use.
