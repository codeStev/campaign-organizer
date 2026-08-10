# ADR-0009: RFC 9457/7807 error responses

- Status: Accepted
- Date: 2026-08-10

## Context
The API needs a consistent, machine-readable error shape for validation
failures, auth errors, and missing resources.

## Decision
Use **`application/problem+json`** (RFC 9457, formerly RFC 7807) for all error
responses. Spring's `ProblemDetail` support is enabled
(`spring.mvc.problemdetails.enabled=true`); controllers throw
`ResponseStatusException` and bean-validation errors are rendered as problem
documents automatically. The shape is described in the OpenAPI contract.

## Consequences
- Uniform errors across the API with `status`, `title`, and `detail`.
- Frontend can parse `detail` generically for user-facing messages.
- No bespoke error DTOs to maintain.

## Alternatives considered
- **Custom error JSON:** reinvents a standard for no benefit.
