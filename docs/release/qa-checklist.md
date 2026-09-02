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
- One Excel import preview and confirmation batch (expenses).
- One direct Excel import (preview + create) each for income, categories, payment methods, and annual budget.

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
- Search filters by name/description work for categories and payment methods.

### 4. Expenses

Steps:

1. Create a simple expense.
2. List expenses.
3. Get expense detail.

Expected result:

- Expense is ACTIVE and SIMPLE.
- Category and payment method belong to the same account.
- Expense list `search` filters description case-insensitively and remains account-scoped.

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
3. Register a payment with `createExpense=true` using an active expense category and payment method.

Expected result:

- Payment is ACTIVE.
- Remaining balance decreases.
- Debt becomes PAID only when remaining balance is zero.
- The optional associated expense is `SIMPLE`, `ACTIVE`, `PAID`, has `sourceType=DEBT_PAYMENT`, and does not duplicate cashflow.

### 7. Budgets

Steps:

1. Get the monthly budget for the installment period.
2. Verify sub-budgets and impacts.
3. Create a manual sub-budget with a category and a simple expense in that month/category.

Expected result:

- Budget exists.
- Debt-derived sub-budget exists.
- Budget impacts have expected and paid amounts.
- Manual sub-budget `spentAmount` reflects active simple expenses with `sourceType=MANUAL` or `IMPORT`.
- `budget-summary` combines manual budget execution and debt impacts.

### 8. Income

Steps:

1. Create income using an INCOME category.
2. List incomes.

Expected result:

- Income is ACTIVE.
- EXPENSE categories are rejected for income.
- Income list `search` filters description case-insensitively and remains account-scoped.

### 9. Analytics

Steps:

1. Call monthly summary.
2. Call expenses by category.
3. Call incomes by category.
4. Call debt summary.
5. Call budget summary.
6. Call budget vs expenses by category.

Expected result:

- Totals are account-scoped.
- Empty periods return zero totals or empty lists.
- Cashflow excludes `DEBT_PAYMENT` expenses and counts debt payments once.
- Budget vs expenses includes categories with only budget or only expenses.

### 10. Expense Import

Steps:

1. Download the dynamic `.xlsx` template.
2. Upload `.xlsx` preview with headers: `Fecha`, `Descripción`, `Monto`, `Categoría`, `MedioPago`, `EstadoPago`, `AplicaPagoDeuda`, `Deuda`, `TipoPagoDeuda`, `NotasPagoDeuda`, `Participante`.
3. Include at least one row with a valid explicit `Participante` and one row with `Participante` left blank.
4. Include at least one intentionally invalid row (e.g. unknown category).
5. Review valid and invalid row counts.
6. Confirm the batch.
7. Reconfirm the same batch.
8. Repeat preview and confirm on a valid file as `ACCOUNT_MEMBER`.
9. Upload a file with more rows than the configured limit (`EXPENSE_IMPORT_MAX_ROWS`, default `1500`).

Expected result:

- Preview does not create expenses; invalid rows are reported without blocking valid rows from being previewed.
- Confirm creates only valid expenses and debt payments for rows marked as debt payments.
- Debt-payment rows create both a simple imported expense and a debt payment in one transaction.
- Second confirm fails with `IMPORT_ALREADY_CONFIRMED`.
- Row with an explicit `Participante` resolves to that participant; row with blank `Participante` falls back to the participant confirming the batch.
- `ACCOUNT_MEMBER` can preview and confirm (not admin-only).
- File over the row limit fails with `IMPORT_ROW_LIMIT_EXCEEDED`.

### 11. Income Import

Steps:

1. Download the `.xlsx` template with headers `Fecha`, `Descripcion`, `Categoria`, `Monto`, `Participante`.
2. Upload for `POST /imports/incomes/preview` with at least one valid row, one row with `Participante` blank, and one intentionally invalid row (e.g. unknown category).
3. Upload the same file to `POST /imports/incomes` (direct create, no batch persisted).
4. Repeat preview and direct create on a valid file as `ACCOUNT_MEMBER`.
5. Upload a file with more rows than the configured limit (default `1000`).

Expected result:

- Preview validates without creating incomes and returns parsed row data plus errors.
- If any row is invalid, the direct import creates nothing; if all rows are valid, all incomes are created in one transaction as `ACTIVE`.
- Row with blank `Participante` falls back to the participant running the import.
- `ACCOUNT_MEMBER` can preview and import (not admin-only).
- File over the row limit fails with `IMPORT_ROW_LIMIT_EXCEEDED`.

### 12. Category Import

Steps:

1. Download the `.xlsx` template with headers `Nombre`, `Tipo`, `Descripcion` (optional).
2. Upload for `POST /imports/categories/preview` with at least one valid row, one row with `Tipo` missing, and one row with an invalid `Tipo` value.
3. Upload the same file to `POST /imports/categories` (direct create, no batch persisted).
4. Attempt preview and direct create as `ACCOUNT_MEMBER`.
5. Upload a file with more rows than the configured limit (default `1000`).

Expected result:

- Preview validates without creating categories and returns parsed row data plus errors.
- Missing `Tipo` and invalid `Tipo` are reported as row errors.
- If any row is invalid, the direct import creates nothing; if all rows are valid, all categories are created in one transaction as `ACTIVE`.
- Categories import does not use a `Participante` column.
- `ACCOUNT_MEMBER` is rejected with `ACCOUNT_ADMIN_REQUIRED`; only `ACCOUNT_ADMIN` can preview/create.
- File over the row limit fails with `IMPORT_ROW_LIMIT_EXCEEDED`.

### 13. Payment Method Import

Steps:

1. Download the `.xlsx` template with headers `Nombre`, `Tipo`, `Descripcion` (optional).
2. Upload for `POST /imports/payment-methods/preview` with at least one valid row, one row with `Tipo` missing, and one row with an invalid `Tipo` value.
3. Upload the same file to `POST /imports/payment-methods` (direct create, no batch persisted).
4. Attempt preview and direct create as `ACCOUNT_MEMBER`.
5. Upload a file with more rows than the configured limit (default `1000`).

Expected result:

- Preview validates without creating payment methods and returns parsed row data plus errors.
- Missing `Tipo` and invalid `Tipo` are reported as row errors.
- If any row is invalid, the direct import creates nothing; if all rows are valid, all payment methods are created in one transaction as `ACTIVE`.
- Payment method import does not use a `Participante` column.
- `ACCOUNT_MEMBER` is rejected with `ACCOUNT_ADMIN_REQUIRED`; only `ACCOUNT_ADMIN` can preview/create.
- File over the row limit fails with `IMPORT_ROW_LIMIT_EXCEEDED`.

### 14. Annual Budget Import

Steps:

1. Download the `.xlsx` template with headers `Año`, `Mes`, `NombrePresupuesto`, `Categoria`, `NombreSubpresupuesto`, `Valor`, `Participante`.
2. Upload for `POST /imports/budgets/annual/preview` with a `Todos` base row, at least one month-specific override row, one row with `Participante` blank, and one row with an explicit `Participante`.
3. Include one intentionally invalid row (e.g. `Valor` equal to zero).
4. Upload the same file to `POST /imports/budgets/annual` (direct create) for a year with no existing monthly budgets.
5. Repeat the same import for a year that already has at least one existing monthly budget.
6. Attempt preview and direct create as `ACCOUNT_MEMBER`.
7. Upload a file with more rows than the configured limit (default `1000`).

Expected result:

- Preview validates without creating budgets and returns parsed row data, resolved `categoryId`/`participantId`, `appliedMonths`, and row errors.
- If any row is invalid, the direct import creates nothing.
- If valid, all 12 monthly budgets are created in one transaction with `MANUAL`/`ACTIVE` sub-budgets; row with blank `Participante` creates a global (unscoped) sub-budget, row with an explicit `Participante` scopes execution to that participant.
- Import against a year that already has any existing monthly budget fails with `ANNUAL_BUDGET_MONTH_ALREADY_EXISTS`.
- `ACCOUNT_MEMBER` is rejected with `ACCOUNT_ADMIN_REQUIRED`; only `ACCOUNT_ADMIN` can preview/create.
- File over the row limit fails with `IMPORT_ROW_LIMIT_EXCEEDED`.

Debt import remains out of scope for this checklist: it is not implemented.

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
