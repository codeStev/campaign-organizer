# Private image registry (tailnet-only)

Runs a plain Docker Registry v2 on your server, reachable only from your
tailnet, over HTTPS with a real (auto-renewing) certificate — via
`tailscale serve`, not by exposing any port on the host's public interfaces.
GitHub Actions pushes to it by joining the tailnet for the duration of a CI
run; your server pulls from it directly since it's already a tailnet member.

## One-time server setup

1. **Basic auth credentials** — the registry container itself is not exposed
   publicly, but auth is worth keeping anyway (defense in depth: anyone who
   can reach this tailnet node on the tailnet can otherwise push/pull freely).

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

3. **Expose it over HTTPS to the tailnet** via `tailscale serve`. This gets a
   publicly-trusted Let's Encrypt certificate for this node's tailnet
   hostname, auto-renewed by `tailscaled` — no manual cert files, no cron job:

   ```bash
   sudo tailscale serve --bg --https=443 http://localhost:5000
   ```

   (Requires "HTTPS Certificates" enabled for the tailnet: Tailscale admin
   console → DNS tab, if not already on.)

   Confirm the config and find the exact hostname to use everywhere below:

   ```bash
   tailscale serve status
   tailscale status   # this node's <name>.<tailnet>.ts.net is REGISTRY_HOST
   ```

   `tailscale serve` config persists across reboots as long as `tailscaled`
   is running (the default systemd setup).

4. By default this is reachable by every device on your tailnet. If you want
   to restrict it further, use Tailscale ACL grants to limit which
   users/tags can reach this node on port 443, rather than relying on
   registry auth alone.

## One-time Tailscale + GitHub setup (for CI push)

GitHub's hosted runners aren't on your tailnet, so the CI job joins it for
the duration of each run using an OAuth client scoped to a tag.

1. **Tailscale admin console** → Settings → OAuth clients → generate a client.
   - Scope it to a dedicated tag, e.g. `tag:ci`, so it can only ever bring up
     ephemeral CI nodes, not full tailnet-admin access.
   - Add `tag:ci` to your tailnet's ACL policy (`https://login.tailscale.com/admin/acls`)
     with access to the registry node on port 443, e.g.:
     ```json
     "acls": [
       {"action": "accept", "src": ["tag:ci"], "dst": ["<registry-node-ip-or-tag>:443"]}
     ],
     "tagOwners": {
       "tag:ci": ["autogroup:admin"]
     }
     ```
     (Merge into your existing policy file rather than replacing it — adjust
     to however your policy already tags/groups devices.)

2. **GitHub repo settings** → Secrets and variables → Actions:
   - Secrets: `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_CLIENT_SECRET` (from step 1),
     `REGISTRY_USERNAME`, `REGISTRY_PASSWORD` (from server setup step 1).
   - Variable: `REGISTRY_HOST` = the tailnet hostname from server setup step 3
     (e.g. `myserver.tailxxxxx.ts.net`, no port — `tailscale serve` listens
     on the standard HTTPS port).

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
directly — no `tailscale serve`/proxy needed for outbound pulls):

```bash
docker login <REGISTRY_HOST> -u <registry-user>
docker pull <REGISTRY_HOST>/campaign-organizer-backend:v0.2.0
docker pull <REGISTRY_HOST>/campaign-organizer-frontend:v0.2.0
```
