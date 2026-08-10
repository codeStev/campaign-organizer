# ADR-0004: Docker + Compose deployment

- Status: Accepted
- Date: 2026-08-10

## Context
The app must be deployable as Docker containers with an example compose file,
and be easy for one person to self-host.

## Decision
Each service ships as its own image via a **multi-stage Dockerfile** (Maven
build → JRE runtime for the backend; Vite build → nginx for the frontend). A
root **`docker-compose.yml`** wires `db`, `backend`, and `frontend`, with named
volumes for database data and media. Secrets come from a `.env` file
(`.env.example` provided).

## Consequences
- One command (`docker compose up --build`) brings up the whole stack.
- Small runtime images; containers run as non-root.
- Same images can later be pushed to a registry / orchestrated if needed.

## Alternatives considered
- **Single container:** simpler but couples build tooling and complicates the
  database and static serving.
- **Kubernetes/Helm:** overkill for a personal single-node deployment.
