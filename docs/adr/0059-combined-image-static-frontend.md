# ADR-0059: Combined image — backend statically serves the frontend

- Status: Accepted
- Date: 2026-08-22

## Context
The existing deployment shape (ADR-0004) is two containers: `frontend`
(nginx serving the Vite build, proxying `/api` — see `frontend/nginx.conf`)
and `backend` (the Spring Boot API). For a single-server, single-user
deployment this is more moving parts than necessary — nginx exists here only
to serve static files and proxy one path prefix, both of which Spring Boot's
embedded Tomcat can do itself. The owner wants a third option: one image,
one container, no nginx.

## Decision
Add `deploy/combined/Dockerfile`, a multi-stage build (context: repo root)
that builds the frontend, copies its `dist/` output onto the backend's
static classpath root (`backend/src/main/resources/static/`) before packaging,
and runs the resulting jar — Spring Boot auto-serves anything under
`classpath:/static/**` with no extra config.

- **`SpaWebConfig`** (`backend/.../config/SpaWebConfig.java`) adds a
  low-priority resource-handler fallback: if a requested path doesn't match a
  static file, it serves `static/index.html` instead of 404ing — needed
  because the frontend uses client-side routing (ADR-0053), so a hard refresh
  on e.g. `/worlds/1/articles/2` has no matching file. `/api/**`, actuator,
  and Swagger UI are all resolved by higher-priority `@RequestMapping`-backed
  handlers before Spring ever consults this resource handler, so it can't
  shadow them. On the existing API-only image, `static/index.html` simply
  isn't on the classpath, so the fallback resource doesn't exist either and
  behavior is unchanged (normal 404s).
- **`SecurityConfig`** changes from "authenticate everything except an
  explicit allow-list" to "authenticate `/api/**` except an explicit
  allow-list; permit everything else." The SPA shell (HTML/JS/CSS, and any
  client-side route path that falls through to it) must be loadable before
  the user has logged in — auth is enforced where it actually matters, at the
  API call level, same as it always was. On the API-only image this change is
  inert: there's no static content to become newly reachable.
- This is a **third build artifact**, not a replacement: `docker-compose.yml`
  keeps the two-container setup for local dev (`npm run dev`'s proxy,
  `docker compose up --build`) unchanged. `deploy/combined/Dockerfile` is
  built manually (`docker build -f deploy/combined/Dockerfile -t
  campaign-organizer .`) or via CI's `publish` job (ADR-0057), which now
  additionally builds and pushes a `campaign-organizer` image alongside
  `campaign-organizer-backend`/`campaign-organizer-frontend`, with the same
  `latest`/SHA/semver tagging.
- `backend/build.gradle.kts` and `frontend/vite.config.ts` need no changes:
  the frontend already builds to a root-relative (`base: "/"`) `dist/`, which
  is exactly what Spring's static root expects, and Gradle already packages
  anything under `src/main/resources/**` with no filtering.

## Consequences
- One more Dockerfile to keep in sync with `backend/Dockerfile` and
  `frontend/Dockerfile` if their runtime-stage setup (postgres-client, the
  non-root user, `/data/media`) changes — there's no shared base layer
  between them today.
- The combined image's frontend build isn't cached independently the way the
  split `frontend` image's layers are; changing only backend code still
  reruns the frontend build stage inside `deploy/combined/Dockerfile` unless
  Docker's layer cache keys off unchanged `frontend/` inputs (it does, via
  `COPY frontend/package*.json` then `COPY frontend/ .` ordering, same
  pattern as `frontend/Dockerfile`).
- `SecurityConfig`'s broadened default-permit doesn't expose any new data:
  everything sensitive is still behind `/api/**`, which still requires a
  bearer token exactly as before. What's newly public is only the ability to
  *load the page* without a token — necessary for a login screen to exist at
  all in this deployment shape.

## Alternatives considered
- **Serve the frontend from a `/error`-triggered `ErrorController` fallback**
  instead of a resource-handler `PathResourceResolver`: functionally similar,
  but depends more on Spring Boot's error-dispatch internals and is less
  directly documented as the intended extension point for this exact
  "SPA fallback" case than `PathResourceResolver`, which exists for it.
- **Reverse-proxy filter inside Spring** (forward everything non-`/api` to
  `/index.html` via a servlet filter) instead of the resource-handler
  approach: would need to explicitly enumerate/exclude `/api`, actuator, and
  Swagger paths by hand, rather than relying on Spring's existing handler
  ordering to keep them separate for free.
- **Replace the two-container setup entirely**: rejected per the owner's
  explicit direction — local dev (`npm run dev`'s Vite proxy, independent
  frontend/backend restart-on-change) stays on the split setup; the combined
  image is purely a simpler option for the actual server deployment.
