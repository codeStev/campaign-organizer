# ADR-0002: React + TypeScript (Vite) frontend

- Status: Accepted
- Date: 2026-08-10

## Context
The frontend was specified as React with TypeScript. We need a build tool, and
a way to keep the client in sync with the API.

## Decision
Use **React 18 + TypeScript**, built with **Vite**. API types are generated from
the OpenAPI contract via `openapi-typescript` (`npm run gen:api`). In dev, Vite
proxies `/api` to the backend so the client uses the same relative URLs as in
production.

## Consequences
- Fast dev server and simple build; static output served by nginx in production.
- Client types can be regenerated from the contract, reducing drift.
- Same-origin `/api` calls work identically in dev (Vite proxy) and prod (nginx).

## Alternatives considered
- **Next.js:** SSR/routing not needed for a personal SPA; adds a Node runtime to
  the deployment.
- **Create React App:** effectively unmaintained.
