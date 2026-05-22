# Easy Finance Backend Documentation

Easy Finance is a collaborative financial backend for managing accounts, participants, expenses, installment expenses, debts, debt payments, monthly budgets, income, analytics, reports, Excel imports, and auditing.

This documentation describes the current backend MVP and the conventions used to keep future phases aligned.

## Documentation Index

### Architecture

- [Architecture Overview](architecture/overview.md)
- [Backend Architecture](architecture/backend-architecture.md)

### Architecture Decision Records

- [ADR 0001 - Architecture Style](architecture/adr/0001-architecture-style.md)
- [ADR 0002 - Spring MVC over WebFlux](architecture/adr/0002-spring-mvc-over-webflux.md)
- [ADR 0003 - PostgreSQL and Spring Data JPA Persistence](architecture/adr/0003-postgresql-jpa-persistence.md)
- [ADR 0004 - Identity, Participant, Account Roles](architecture/adr/0004-identity-participant-account-roles.md)

### Domain

- [Domain Model](domain/domain-model.md)
- [Business Rules](domain/business-rules.md)
- [Debt and Budget Rules](domain/debt-budget-rules.md)

### Data

- [Logical Data Model](database/data-model.md)

### API

- [REST API Guidelines](api/rest-api-guidelines.md)

### Security

- [Security Model](security/security-model.md)

### Audit

- [Audit Strategy](audit/audit-strategy.md)

### Testing

- [Testing Strategy](testing/testing-strategy.md)

### Implementation

- [Implementation Roadmap](implementation/roadmap.md)

## Final Technical Decisions

- Java 21.
- Spring Boot 4.x stable.
- Spring MVC.
- Spring Data JPA.
- PostgreSQL.
- Maven.
- Flyway.
- Spring Security with JWT.
- Modular monolith.
- Hexagonal architecture / clean architecture.
- PostgreSQL `BIGSERIAL` identifiers mapped to Java `Long`.
- `User` and `Participant` have a mandatory 1:1 relationship.
- Global roles and account roles coexist.
- Mutual debts are outside the MVP.
- Currency is COP only for now.
- `Money` exists as a domain value object for future currency evolution.
- Technical and functional audit are required.
- Analytics separates real cashflow from conceptual expense analytics: cashflow counts active incomes, paid simple expenses, and active debt payments; expenses associated to debt payments are excluded from simple cashflow to avoid double counting, while conceptual expense views can still include them and full installment purchases.

## Outside the MVP

Mutual debts are explicitly outside the MVP. Do not create tables, endpoints, use cases, commands, or scheduled jobs for mutual debts until the business rules are defined.
