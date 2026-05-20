# Easy Finance MVP - QA Checklist

## Preconditions

- Java 21 installed.
- Maven available on `PATH`.
- Docker Desktop or a compatible Docker engine running.
- PostgreSQL Testcontainers can start containers.
- A clean PostgreSQL database is available for staging smoke tests.
- No real customer data is used in QA.
- `JWT_SECRET` is set to a non-default value with at least 32 bytes of entropy.
- `SPRING_PROFILES_ACTIVE` is set to `prod` or `staging` for deployed QA.

## Required Commands

Run from the repository root:

```bash
mvn clean test
mvn clean package
mvn verify -Pci
docker compose up --build
```

Expected result:

- `mvn clean test` passes all unit, application, REST, and security tests.
- `mvn clean package` produces the Spring Boot JAR.
- `mvn verify -Pci` passes all integration tests with Testcontainers.
- `docker compose up --build` starts PostgreSQL and the API.

## Minimum Test Data

Create through the public API:

- One registered user.
- One financial account.
- One active EXPENSE category.
- One active INCOME category.
- One active payment method.
- One simple expense.
- One installment expense.
- One derived debt.
- One debt payment.
- One income.
- One Excel import preview and confirmation batch.

## Smoke Tests

### 1. Auth

Steps:

1. Register a user with valid email, password, and full name.
2. Login with the same credentials.
3. Call `GET /api/v1/auth/me` with the bearer token.

Expected result:

- Register and login return `accessToken`.
- `/auth/me` returns user and participant identifiers.

### 2. Accounts

Steps:

1. Create an account with `POST /api/v1/accounts`.
2. List accounts with `GET /api/v1/accounts`.
3. Get account detail.

Expected result:

- Account is ACTIVE.
- Current participant is ACCOUNT_ADMIN.

### 3. Catalogs

Steps:

1. Create EXPENSE category.
2. Create INCOME category.
3. Create payment method.
4. List categories and payment methods.

Expected result:

- Catalog records are ACTIVE.
- Names are scoped to the account.

### 4. Expenses

Steps:

1. Create a simple expense.
2. List expenses.
3. Get expense detail.

Expected result:

- Expense is ACTIVE and SIMPLE.
- Category and payment method belong to the same account.

### 5. Installment Expense And Debt

Steps:

1. Create installment expense.
2. List debts.
3. Get debt detail.

Expected result:

- Expense is INSTALLMENT.
- Exactly one INSTALLMENT_EXPENSE debt is created.
- Budget impacts are generated.

### 6. Debt Payment

Steps:

1. Register a partial or full debt payment.
2. Re-read the debt.

Expected result:

- Payment is ACTIVE.
- Remaining balance decreases.
- Debt becomes PAID only when remaining balance is zero.

### 7. Budgets

Steps:

1. Get the monthly budget for the installment period.
2. Verify sub-budgets and impacts.

Expected result:

- Budget exists.
- Debt-derived sub-budget exists.
- Budget impacts have expected and paid amounts.

### 8. Income

Steps:

1. Create income using an INCOME category.
2. List incomes.

Expected result:

- Income is ACTIVE.
- EXPENSE categories are rejected for income.

### 9. Analytics

Steps:

1. Call monthly summary.
2. Call expenses by category.
3. Call incomes by category.
4. Call debt summary.
5. Call budget summary.

Expected result:

- Totals are account-scoped.
- Empty periods return zero totals or empty lists.

### 10. Expense Import

Steps:

1. Upload `.xlsx` preview with headers: `Fecha`, `Descripción`, `Monto`, `Categoría`, `MedioPago`, `EstadoPago`.
2. Review valid and invalid row counts.
3. Confirm the batch.
4. Reconfirm the same batch.

Expected result:

- Preview does not create expenses.
- Confirm creates only valid expenses and debt payments for rows marked as debt payments.
- Second confirm fails with `IMPORT_ALREADY_CONFIRMED`.

## Approval Criteria

- All required commands pass in CI or staging.
- All smoke tests pass.
- No cross-account data is visible.
- JWT-protected endpoints reject missing or invalid tokens.
- BLOCKED or INACTIVE users and INACTIVE participants cannot access financial endpoints.
- Flyway migrates a clean database successfully.
- Docker image starts as non-root.
- No real secrets are committed.

## Blocking Criteria

- `mvn verify -Pci` fails for a reason other than missing local Docker.
- Any migration fails against a clean database.
- Any financial operation leaks cross-account data.
- Any write operation succeeds for an inactive membership, inactive participant, or blocked user.
- Import confirmation duplicates expenses under concurrent confirmation.
- CI import concurrency tests fail or do not run under `mvn verify -Pci`.
- Debt payments allow overpayment or negative remaining balance.
- Swagger/OpenAPI is publicly exposed in production without compensating controls.
