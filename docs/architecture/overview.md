# Architecture Overview

Easy Finance backend will be implemented as a modular monolith using hexagonal architecture and clean architecture principles.

The system is financial, collaborative, and account-scoped. The backend must prioritize consistency, maintainability, auditability, transaction control, and clear business rules over premature distribution or reactive complexity.

## Architecture Summary

- Runtime style: modular monolith.
- Application style: Spring Boot 4 with Spring MVC.
- Persistence: PostgreSQL through Spring Data JPA.
- Dependency management and build: Maven.
- Database migrations: Flyway.
- Security: Spring Security with JWT.
- Testing: JUnit 5, Mockito, AssertJ, Testcontainers, MockMvc.

## Main Architectural Goals

- Keep domain logic independent from Spring and infrastructure.
- Model financial rules explicitly.
- Keep transactions easy to reason about.
- Support account-based authorization.
- Support technical and functional audit.
- Allow the project to evolve into modules or services later if justified.
- Avoid framework leakage into domain objects.

## High-Level Components

```text
Client
  |
  v
REST API - Spring MVC controllers
  |
  v
Application use cases
  |
  v
Domain model and domain services
  |
  v
Ports
  |
  v
Adapters: JPA, JWT, Excel, audit, reporting
  |
  v
PostgreSQL / external resources
```

## Main Modules

- `identity-access`: users, authentication, global roles, JWT.
- `accounts`: participants, accounts, account memberships, account roles.
- `catalogs`: categories and payment methods.
- `expenses`: simple expenses and installment expenses.
- `debts`: debts and debt payments.
- `budgets`: monthly budgets, sub-budgets, and budget impacts.
- `income`: income records.
- `imports`: Excel upload, validation, preview, confirmation.
- `analytics`: dashboard and reports.
- `audit`: cross-cutting audit concerns. Technical audit is implemented (`shared.infrastructure.audit`). Functional
  audit (`audit_events`) is schema-only today; no code emits functional audit events yet (see `docs/audit/audit-strategy.md`).
- `shared`: shared domain primitives and cross-cutting contracts.

## Consequences

- The first implementation can move quickly without microservice overhead.
- Business rules remain visible and testable.
- Cross-module transactions are allowed inside the monolith.
- Module boundaries must be enforced by package conventions, tests, and discipline.
- Future extraction into services remains possible but is not a design goal for the MVP.

## Pending Decisions

- Whether the first codebase will be a single Maven module with package boundaries or a Maven multi-module project.
- Whether mappers will use MapStruct or explicit manual classes.
- Exact OpenAPI tooling version compatible with Spring Boot 4.

