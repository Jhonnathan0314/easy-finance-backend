# ADR 0003 - PostgreSQL and Spring Data JPA Persistence

## Status

Accepted.

## Context

Easy Finance needs reliable persistence for account-scoped financial data. The system must support transactions, constraints, indexing, audit fields, and reporting queries.

The current documentation includes a MySQL-oriented schema, but the final technical decision is PostgreSQL.

## Decision

Use PostgreSQL with Spring Data JPA.

Use Flyway for database migrations.

Use PostgreSQL `BIGSERIAL` identifiers mapped to Java `Long`.

## Consequences

Positive:

- Mature transaction support.
- Strong constraints and indexing.
- Good support for reporting queries.
- Excellent Testcontainers support.
- Simple integration with Spring Data JPA.

Negative:

- Domain objects must remain separate from JPA entities to preserve clean architecture.
- Lazy-loading pitfalls must be avoided.
- Complex reporting queries may require projections, specifications, or native SQL.

## Persistence Rules

- JPA entities live in infrastructure packages.
- Domain entities do not use JPA annotations.
- Repositories exposed to application code are ports.
- Spring Data repositories are adapter internals.
- Migrations are mandatory for all schema changes.

## Alternatives Considered

### MySQL

Rejected because PostgreSQL is the final selected database and provides strong capabilities for this financial backend.

### R2DBC

Rejected because the project chose Spring MVC and Spring Data JPA for transactional maintainability.

