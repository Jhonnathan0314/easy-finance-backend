# ADR 0001 - Architecture Style

## Status

Accepted.

## Context

Easy Finance is a collaborative financial backend. It manages account-scoped financial data, debts, payments, monthly budgets, imports, reports, and audit records.

The system needs strong maintainability, clear transaction boundaries, explicit business rules, and reliable tests. The MVP does not require independent deployment of individual business capabilities.

## Decision

Use a modular monolith with hexagonal architecture / clean architecture.

The codebase will be organized by business module. Each module should isolate domain, application, infrastructure, and entrypoint concerns.

## Consequences

Positive:

- Lower operational complexity than microservices.
- Easier transaction management across related financial operations.
- Clear domain boundaries.
- Easier debugging and local development.
- Strong testability through ports and adapters.

Negative:

- Module boundaries are not enforced by runtime deployment.
- Poor package discipline could create unwanted coupling.
- Future service extraction requires careful boundary preservation from the beginning.

## Alternatives Considered

### Layered monolith

Rejected as the main style because it often causes anemic domain models and infrastructure leakage into business logic.

### Microservices

Rejected for MVP because independent deployment is not required and distributed transactions would add unnecessary risk.

### Reactive modular backend

Rejected as the main style because financial consistency, Excel processing, reporting, and audit requirements benefit more from a traditional transactional model.

