# Easy Finance Frontend Context

This folder is a handoff package for building a new Angular frontend against the Easy Finance backend MVP without rereading the Java codebase.

Use this folder as the first input for the frontend project agent. It contains API contracts, business rules, DTO references, example payloads, and Angular implementation guidance.

## Backend

- Local base URL: `http://localhost:8080`
- API prefix: `/api/v1`
- Protected endpoints require `Authorization: Bearer <accessToken>`.
- Public endpoints:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - health endpoints if enabled by environment.

## Recommended Use In A New Angular Project

1. Copy `frontend-context/` into the Angular repo root.
2. Read `api/api-overview.md` and `business/business-rules.md` first.
3. Use `api/openapi.json` to generate TypeScript clients if desired.
4. Use `models/dto-reference.md` and `models/enums.md` when building manual interfaces.
5. Use `frontend-guidance/implementation-roadmap.md` as the build order.
6. Import `api/postman-collection.json` and `api/postman-environment.json` to smoke-test the backend before wiring screens.

No secrets, passwords, or real tokens are included.

