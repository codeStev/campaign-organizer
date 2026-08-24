# Private image registry

`docker-compose.yml` runs a plain Docker Registry v2, bound to `127.0.0.1`
only — how it's exposed to CI and to the deploy server (reverse proxy, TLS,
network access) is left entirely to your own infrastructure.

## What CI needs

The `publish` job in `.github/workflows/ci.yml` needs these GitHub Actions
secrets and variables:

| Name | Kind | What it is |
| --- | --- | --- |
| `TS_OAUTH_CLIENT_ID` | secret | Tailscale OAuth client ID, scoped to `tag:ci` |
| `TS_OAUTH_CLIENT_SECRET` | secret | its matching secret |
| `REGISTRY_USERNAME` | secret | registry auth username |
| `REGISTRY_PASSWORD` | secret | registry auth password |
| `REGISTRY_CA_CERT` | secret | PEM contents of your registry endpoint's CA cert, if it isn't publicly trusted |
| `REGISTRY_HOST` | variable | the registry's address as CI should reach it (host:port) |

The CI job joins the tailnet as an ephemeral `tag:ci` node for the run's
duration, installs `REGISTRY_CA_CERT` into Docker's per-registry `certs.d`
(no daemon restart needed), then builds and pushes
`campaign-organizer-backend`/`campaign-organizer-frontend`.

## Auth credentials

Generate the registry's htpasswd file before starting it:

```bash
cd deploy/registry
mkdir -p auth
docker run --rm --entrypoint htpasswd httpd:2.4-alpine \
  -Bbn <username> '<password>' > auth/htpasswd
docker compose up -d
```

## Image tags

Every push to `master` publishes/updates `latest` and a `<commit-sha>` tag.
An immutable release (see ADR-0060 for what counts as major/minor/patch):

```bash
git tag v0.2.0 -m "..."
git push origin v0.2.0
```

This publishes `vX.Y.Z`/`vX.Y`/`vX` tags for all three images
(`campaign-organizer-backend`, `-frontend`, `-combined`), computed from the
git tag, and creates a GitHub Release with auto-generated notes.

## Deploying pulled images

```bash
docker login <REGISTRY_HOST> -u <username>
docker pull <REGISTRY_HOST>/campaign-organizer-backend:v0.2.0
docker pull <REGISTRY_HOST>/campaign-organizer-frontend:v0.2.0
```

If `REGISTRY_HOST` isn't publicly trusted, trust its CA the same way CI does:

```bash
sudo mkdir -p /etc/docker/certs.d/<REGISTRY_HOST>
sudo cp <ca-cert> /etc/docker/certs.d/<REGISTRY_HOST>/ca.crt
```
