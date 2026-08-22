# Private image registry (tailnet-only)

Runs a plain Docker Registry v2 on your server, reachable only from your
tailnet. TLS termination is handled by **your own Caddy instance** (already
configured, per ADR-0058) — Caddy listens on `<tailnet-hostname>:<caddy-port>`
(whatever external port your Caddy site uses — e.g. `8454`; this does **not**
have to match the registry container's own port) with its self-signed
internal CA, and reverse-proxies to the registry container on
`localhost:5000`. GitHub Actions pushes to it by joining the tailnet for the
duration of a CI run; your server pulls from it directly since it's already
a tailnet member.

Because Caddy's cert comes from its own internal CA rather than a publicly
trusted one, every consumer (CI runner, deploy server) needs that CA
explicitly trusted for this registry host — see below.

## One-time server setup

1. **Basic auth credentials** — the registry container itself is only
   reachable via your Caddy proxy, but auth is worth keeping anyway (defense
   in depth: anyone who can reach that Caddy site can otherwise push/pull
   freely).

   ```bash
   cd deploy/registry
   mkdir -p auth
   docker run --rm --entrypoint htpasswd httpd:2.4-alpine \
     -Bbn <registry-user> '<registry-password>' > auth/htpasswd
   ```

2. **Start the registry** (bound to `127.0.0.1` only — see `docker-compose.yml`):

   ```bash
   docker compose up -d
   ```

3. Confirm Caddy is proxying `<tailnet-hostname>:<caddy-port>` →
   `http://localhost:5000` with TLS, **and that Caddy has been reloaded/restarted**
   after that config was added — a config that's saved but not applied is a
   common cause of `connection refused` when pushing. `<tailnet-hostname>:<caddy-port>`
   (the external address, not the internal `:5000`) is the `REGISTRY_HOST`
   value used everywhere below.

4. **Export Caddy's root CA certificate.** The exact path depends on how
   Caddy is installed — commonly:

   ```bash
   # Official Caddy Docker image (data volume mounted at /data):
   docker cp <caddy-container>:/data/caddy/pki/authorities/local/root.crt ./caddy-ca.crt

   # Native package install (running as the `caddy` user):
   sudo cat /var/lib/caddy/.local/share/caddy/pki/authorities/local/root.crt > ./caddy-ca.crt
   ```

   Keep `caddy-ca.crt` — it's needed as a GitHub secret (below) and on the
   server itself (next section).

5. By default this is reachable by every device on your tailnet that can
   reach this node's Caddy port. If you want to restrict it further, use
   Tailscale ACL grants to limit which users/tags can reach it, rather than
   relying on registry auth alone.

## One-time Tailscale + GitHub setup (for CI push)

GitHub's hosted runners aren't on your tailnet, so the CI job joins it for
the duration of each run using an OAuth client scoped to a tag. They also
don't trust Caddy's internal CA by default, so CI installs it per run,
scoped to just this registry host (Docker's `certs.d`, no daemon restart
needed).

1. **Tailscale admin console** → Settings → OAuth clients → generate a client.
   - Scope it to a dedicated tag, e.g. `tag:ci`, so it can only ever bring up
     ephemeral CI nodes, not full tailnet-admin access.
   - Add `tag:ci` to your tailnet's ACL policy (`https://login.tailscale.com/admin/acls`)
     with access to the registry node on **Caddy's external port** (not the
     registry container's internal `5000`), e.g.:
     ```json
     "acls": [
       {"action": "accept", "src": ["tag:ci"], "dst": ["<registry-node-ip-or-tag>:<caddy-port>"]}
     ],
     "tagOwners": {
       "tag:ci": ["autogroup:admin"]
     }
     ```
     (Merge into your existing policy file rather than replacing it — adjust
     to however your policy already tags/groups devices.)

2. **GitHub repo settings** → Secrets and variables → Actions:
   - Secrets: `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_CLIENT_SECRET` (from step 1),
     `REGISTRY_USERNAME`, `REGISTRY_PASSWORD` (from server setup step 1),
     `REGISTRY_CA_CERT` (the full PEM contents of `caddy-ca.crt` from server
     setup step 4 — paste it as-is, including the `-----BEGIN/END
     CERTIFICATE-----` lines).
   - Variable: `REGISTRY_HOST` = the tailnet hostname **with Caddy's external
     port** (e.g. `myserver.tailxxxxx.ts.net:8454` — whatever port your Caddy
     site actually listens on, not the registry container's internal 5000).

## Image tags

Every push to `master` publishes/updates `latest` and a `<commit-sha>` tag —
useful for traceability, but both are mutable/floating. To cut an actual
release with an immutable version tag:

```bash
git tag v0.2.0
git push origin v0.2.0
```

This publishes `vX.Y.Z`, `vX.Y`, and `vX` tags for both images (in addition
to `latest` continuing to track `master`), computed from the git tag —
there's no separate version-bump step in the repo to remember.

## Deploying pulled images

On the server (already a tailnet member, so it reaches `REGISTRY_HOST`
directly): trust Caddy's CA for this registry host the same way CI does,
once:

```bash
sudo mkdir -p /etc/docker/certs.d/<REGISTRY_HOST>
sudo cp caddy-ca.crt /etc/docker/certs.d/<REGISTRY_HOST>/ca.crt
```

Then log in and pull as normal:

```bash
docker login <REGISTRY_HOST> -u <registry-user>
docker pull <REGISTRY_HOST>/campaign-organizer-backend:v0.2.0
docker pull <REGISTRY_HOST>/campaign-organizer-frontend:v0.2.0
```
