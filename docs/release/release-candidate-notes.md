# Easy Finance Backend - Release Candidate Notes

## MVP Scope

This release candidate includes:

- Identity and stateless JWT authentication.
- Global revalidation of authenticated user and participant status.
- Accounts, memberships, and account roles.
- Account-scoped catalogs: categories and payment methods.
- Simple expenses.
- Installment expenses.
- Debts and manual debts.
- Debt payments with balance updates.
- Monthly budgets, sub-budgets, and debt-derived budget impacts.
- Income.
- Basic analytics/dashboard queries.
- Excel import for simple expenses with preview and confirmation (including debt-payment rows).
- Direct Excel import (validate-then-create, no preview batch) for income, categories, payment methods, and annual budgets.
- PostgreSQL Flyway migrations.
- Docker image and local Docker Compose.
- Unit, REST, security, schema, transaction, rollback, and concurrency test coverage.

## Known Pending Items

- Functional audit event emission for critical financial actions.
- Reports/exportation.
- Debt import (income, category, payment method, and annual budget imports are already implemented).
- Payment reversal/cancellation.
- Refresh tokens.
- Advanced dashboard and analytics.
- Rate limiting.
- Load testing.
- Formal dependency/CVE review.
- Production-specific OpenAPI/Swagger protection or disablement.

## Accepted Risks For RC

- Swagger/OpenAPI is documented as requiring production protection or disablement, but no profile-specific code switch was added in this release.
- Functional audit table exists as foundation, but complete event emission is not implemented.
- Refresh token lifecycle is deferred.
- Payment reversal is deferred, so operational correction must be handled manually by administrators outside this MVP.
- `mvn verify -Pci` requires Docker and must run in CI/staging even if local developer machines cannot run it.

## Validation Commands

```bash
mvn clean test
mvn clean package
mvn verify -Pci
docker compose up --build
```

Expected:

- Unit and slice tests pass.
- Package builds the executable JAR.
- CI profile runs Testcontainers integration tests.
- Docker Compose starts PostgreSQL and the backend.

The GitHub Actions gate uses `mvn clean verify -Pci` as one lifecycle execution so unit, slice, and integration tests run without duplicating the Maven `test` phase.

## Go/No-Go Decision

Go if:

- `mvn verify -Pci` passes in CI with Docker.
- Staging smoke tests pass.
- Flyway migrates a clean staging database.
- Auth, account authorization, debt payments, budget impacts, analytics, and imports pass QA.
- No critical or high-severity security issue remains open.
- Production secrets and DB credentials are injected through environment variables.

No-go if:

- Any migration fails.
- Any cross-account access is possible.
- Any blocked or inactive user can access financial endpoints.
- Debt payments can overpay or produce negative balances.
- Import confirmation can duplicate expenses.
- Staging cannot be rolled back to the previous image/database backup.

## Release Recommendation

The MVP is technically suitable for release candidate validation once CI with Docker passes and staging smoke tests complete successfully.
