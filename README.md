# Easy Finance Backend

Easy Finance is a collaborative financial backend for accounts, participants, expenses, debts, monthly budgets, income, reports, imports, and audit.

This repository currently contains the MVP backend: identity/security, accounts, catalogs, expenses, installment debts, debt payments, budgets, income, analytics, and Excel expense imports.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Maven
- Flyway
- Spring Security
- JWT Bearer authentication
- Bean Validation
- Actuator
- OpenAPI/Swagger through springdoc-openapi
- JUnit 5
- Mockito
- AssertJ
- Testcontainers

## Architecture

The backend follows the documentation in [docs](docs/README.md):

- modular monolith
- hexagonal architecture / clean architecture
- domain independent from Spring
- account-scoped security model
- technical and functional audit foundations

## Profiles

- `local`: default profile for local development.
- `test`: test profile used by automated tests.
- `prod`: production-oriented defaults.

## Environment Variables

For local execution, start from `.env.example`. Docker Compose automatically reads `.env`; the repository keeps `.env` ignored so local credentials are not committed.

| Variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/easy_finance` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `easy_finance` | Database user |
| `DB_PASSWORD` | `easy_finance` | Database password |
| `JWT_ISSUER` | `easy-finance` | JWT issuer |
| `JWT_SECRET` | `local-development-jwt-secret-32bytes-minimum` | JWT HMAC secret for local development. Required and validated in `prod` |
| `JWT_EXPIRATION` | `PT1H` | JWT expiration duration, ISO-8601 format |
| `EXPENSE_IMPORT_MAX_FILE_SIZE_BYTES` | `5242880` | Business-level Excel import file-size limit |
| `EXPENSE_IMPORT_MAX_FILE_SIZE` | `5MB` | Spring multipart max file size, aligned with the import limit |
| `EXPENSE_IMPORT_MAX_REQUEST_SIZE` | `5MB` | Spring multipart max request size, aligned with the import limit |

## Run PostgreSQL Locally

```bash
docker compose up -d postgres
```

## Run the Application Locally

```bash
mvn spring-boot:run
```

The application starts with the `local` profile by default.

Production deployments must set:

```bash
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=<secure-secret>
```

The application fails during startup in `prod` if `JWT_SECRET` is missing, blank, or still uses the insecure bootstrap placeholder.

Use a high-entropy value for `JWT_SECRET` outside local development.

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Swagger/OpenAPI is public for local development in the current security filter configuration. Production deployments should protect or disable it at the edge until a profile-specific policy is added.

## Auth API

Register:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"jane@example.com\",\"password\":\"abc12345\",\"fullName\":\"Jane Doe\"}"
```

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"jane@example.com\",\"password\":\"abc12345\"}"
```

Current user:

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

Auth responses include `accessToken`, `tokenType`, `expiresIn`, and basic user data with `userId`, `participantId`, `email`, `fullName`, and `globalRoles`.

Every authenticated request revalidates the current user and participant status after JWT signature/expiration validation. A `BLOCKED` or `INACTIVE` user, or an `INACTIVE` participant, receives `403` with `USER_BLOCKED`, `USER_NOT_ACTIVE`, or `PARTICIPANT_NOT_ACTIVE`; active JWT claims alone are not trusted for current identity state.

## Accounts API

All account endpoints require:

```text
Authorization: Bearer <accessToken>
```

Create account:

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Hogar\",\"description\":\"Finanzas familiares\"}"
```

List my accounts:

```bash
curl "http://localhost:8080/api/v1/accounts?page=0&size=20" \
  -H "Authorization: Bearer <accessToken>"
```

Get account:

```bash
curl http://localhost:8080/api/v1/accounts/1 \
  -H "Authorization: Bearer <accessToken>"
```

Update account:

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/1 \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Hogar actualizado\",\"description\":\"Gastos compartidos\"}"
```

Archive account:

```bash
curl -X PATCH http://localhost:8080/api/v1/accounts/1/archive \
  -H "Authorization: Bearer <accessToken>"
```

List members:

```bash
curl http://localhost:8080/api/v1/accounts/1/members \
  -H "Authorization: Bearer <accessToken>"
```

Add existing user as member:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/members \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"member@example.com\",\"role\":\"ACCOUNT_MEMBER\"}"
```

Change member role:

```bash
curl -X PATCH http://localhost:8080/api/v1/accounts/1/members/2/role \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"role\":\"ACCOUNT_ADMIN\"}"
```

Remove member:

```bash
curl -X DELETE http://localhost:8080/api/v1/accounts/1/members/2 \
  -H "Authorization: Bearer <accessToken>"
```

Account authorization rules:

- New accounts are created as `ACTIVE`.
- The creator's participant is added as `ACCOUNT_ADMIN`.
- `ACCOUNT_ADMIN` can update/archive the account and manage members.
- `ACCOUNT_MEMBER` can access the account and list active members.
- `SUPER_ADMIN` is not a functional bypass for financial account operations.
- A participant cannot have duplicate membership in the same account.
- Removing members is a soft delete: membership becomes `INACTIVE`.
- Re-adding an inactive membership reactivates the existing row with the requested role.
- The last active `ACCOUNT_ADMIN` cannot be removed or downgraded.
- Missing membership is returned as `ACCOUNT_NOT_FOUND` to avoid account existence leaks.
- `ACCOUNT_ACCESS_DENIED` is reserved for future cases where denying access does not reveal account existence.

Main account error codes include `ACCOUNT_NOT_FOUND`, `ACCOUNT_NOT_ACTIVE`, `ACCOUNT_ADMIN_REQUIRED`, `ACCOUNT_LAST_ADMIN_REQUIRED`, `ACCOUNT_MEMBER_ALREADY_EXISTS`, and `ACCOUNT_MEMBER_NOT_FOUND`.

## Catalogs API

Catalog endpoints are scoped by account and require:

```text
Authorization: Bearer <accessToken>
```

Categories:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/categories \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Alimentacion\",\"description\":\"Comidas y mercado\",\"type\":\"EXPENSE\"}"
```

```bash
curl "http://localhost:8080/api/v1/accounts/1/categories?search=mercado&type=EXPENSE&status=ACTIVE&page=0&size=20&sort=name,asc" \
  -H "Authorization: Bearer <accessToken>"
```

```bash
curl http://localhost:8080/api/v1/accounts/1/categories/10 \
  -H "Authorization: Bearer <accessToken>"
```

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/1/categories/10 \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Mercado\",\"description\":\"Compras de alimentos\"}"
```

```bash
curl -X DELETE http://localhost:8080/api/v1/accounts/1/categories/10 \
  -H "Authorization: Bearer <accessToken>"
```

Payment methods:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/payment-methods \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Efectivo\",\"description\":\"Billetera\",\"type\":\"CASH\"}"
```

```bash
curl "http://localhost:8080/api/v1/accounts/1/payment-methods?search=visa&type=CREDIT_CARD&status=ACTIVE&page=0&size=20&sort=name,asc" \
  -H "Authorization: Bearer <accessToken>"
```

```bash
curl http://localhost:8080/api/v1/accounts/1/payment-methods/10 \
  -H "Authorization: Bearer <accessToken>"
```

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/1/payment-methods/10 \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Efectivo casa\",\"description\":\"Caja menor\"}"
```

```bash
curl -X DELETE http://localhost:8080/api/v1/accounts/1/payment-methods/10 \
  -H "Authorization: Bearer <accessToken>"
```

Catalog list rules:

- `search` is optional and ignored when null, empty, or blank.
- Search is case-insensitive, automatically trimmed, and matches `name` through `normalizedName` plus `description` through lowercase comparison.
- `search` composes with `type`, `status`, pagination, sort, and account scoping.

Catalog rules:

- `ACCOUNT_MEMBER` can list and get active/inactive catalogs according to filters.
- `ACCOUNT_ADMIN` is required to create, update, or deactivate catalogs.
- The account must be `ACTIVE` for writes.
- Categories are unique by account, type, and normalized name while `ACTIVE`.
- Payment methods are unique by account and normalized name while `ACTIVE`.
- Delete endpoints perform soft delete by setting `INACTIVE`.
- Inactive catalogs cannot be updated; reactivation is not part of this phase.
- Category and payment method `type` is immutable after creation.

Main catalog error codes include `CATEGORY_NOT_FOUND`, `CATEGORY_ALREADY_EXISTS`, `CATEGORY_INACTIVE`, `CATEGORY_TYPE_CHANGE_NOT_ALLOWED`, `PAYMENT_METHOD_NOT_FOUND`, `PAYMENT_METHOD_ALREADY_EXISTS`, `PAYMENT_METHOD_INACTIVE`, and `PAYMENT_METHOD_TYPE_CHANGE_NOT_ALLOWED`.

## Expenses API

Expense endpoints are scoped by account and require:

```text
Authorization: Bearer <accessToken>
```

Create simple expense:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/expenses \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"categoryId\":10,\"participantId\":30,\"paymentMethodId\":20,\"description\":\"Almuerzo\",\"amount\":12000,\"expenseDate\":\"2026-05-09\",\"paymentState\":\"PAID\"}"
```

List expenses:

```bash
curl "http://localhost:8080/api/v1/accounts/1/expenses?from=2026-05-01&to=2026-05-31&status=ACTIVE&expenseType=SIMPLE&search=almuerzo&page=0&size=20&sort=expenseDate,desc" \
  -H "Authorization: Bearer <accessToken>"
```

Get expense:

```bash
curl http://localhost:8080/api/v1/accounts/1/expenses/100 \
  -H "Authorization: Bearer <accessToken>"
```

Update expense:

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/1/expenses/100 \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"categoryId\":10,\"participantId\":30,\"paymentMethodId\":20,\"description\":\"Cena\",\"amount\":15000,\"expenseDate\":\"2026-05-09\",\"paymentState\":\"PAID\"}"
```

Cancel expense:

```bash
curl -X PATCH http://localhost:8080/api/v1/accounts/1/expenses/100/cancel \
  -H "Authorization: Bearer <accessToken>"
```

Duplicate simple expense:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/expenses/100/duplicate \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"expenseDate\":\"2026-06-15\",\"amount\":85000,\"description\":\"Mercado junio\",\"paymentState\":\"PAID\"}"
```

Create installment expense:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/expenses/installments \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"categoryId\":10,\"participantId\":30,\"paymentMethodId\":20,\"description\":\"Computador\",\"totalAmount\":1200000,\"expenseDate\":\"2026-05-11\",\"installmentCount\":6,\"installmentAmount\":200000,\"firstInstallmentDate\":\"2026-06-01\",\"debtName\":\"Computador en cuotas\"}"
```

Expense rules:

- The regular expense endpoint creates `SIMPLE` expenses.
- Installment expenses use `expenseType = INSTALLMENT`, `paymentState = PENDING`, and create exactly one associated debt in the same transaction.
- Amounts are stored as COP through the shared `Money` value object.
- Creating, updating, cancelling, and duplicating requires an `ACTIVE` account and active account membership.
- Listing and reading requires active account membership.
- A regular member can update, cancel, or duplicate only expenses registered by their own participant.
- `participantId` is optional on create/update. When omitted on create, the authenticated participant is used; when omitted on update, the current expense participant is preserved.
- `ACCOUNT_ADMIN` can assign any active participant in the account and can update, cancel, or duplicate any expense in the account.
- Regular members can only assign themselves.
- `INSTALLMENT` expenses are not updated, cancelled, or duplicated through the simple expense endpoints in this phase.
- The full lifecycle for installment expenses will be defined together with debt payments and budget impacts.
- Categories must belong to the same account, be `ACTIVE`, and have type `EXPENSE`.
- Payment methods must belong to the same account and be `ACTIVE`.
- PostgreSQL also enforces that the category, payment method, and participant referenced by an expense belong to the same `accountId`.
- The participant FK validates account membership existence; `ACTIVE` membership status is enforced in the application layer.
- Cancelling is a soft operation: status becomes `CANCELLED`.
- Simple expenses do not create debts.
- For installment expenses, `totalAmount` is the original purchase/advance amount. The financed debt total is calculated as `installmentAmount * installmentCount`.
- For installment expenses, the derived debt inherits the assigned expense participant.
- `installmentAmount * installmentCount` may be greater than `totalAmount`; the difference represents implicit interest, insurance, or financing costs.
- `installmentAmount * installmentCount` cannot be lower than `totalAmount`.
- Duplicating creates a new `ACTIVE` `SIMPLE` expense with a new id and audit metadata; it never creates debt, debt payments, budget impacts, or import rows.
- This phase does not create debt payments or budget impacts.

Main expense error codes include `EXPENSE_NOT_FOUND`, `EXPENSE_ALREADY_CANCELLED`, `EXPENSE_AMOUNT_INVALID`, `EXPENSE_DATE_INVALID`, `EXPENSE_UPDATE_NOT_ALLOWED`, `EXPENSE_CANCEL_NOT_ALLOWED`, `EXPENSE_DUPLICATE_NOT_ALLOWED`, `INSTALLMENT_EXPENSE_UPDATE_NOT_ALLOWED`, `INSTALLMENT_EXPENSE_CANCEL_NOT_ALLOWED`, `INSTALLMENT_FINANCED_TOTAL_INVALID`, `EXPENSE_CATEGORY_NOT_FOUND`, `EXPENSE_CATEGORY_INACTIVE`, `EXPENSE_CATEGORY_INVALID_TYPE`, `EXPENSE_PAYMENT_METHOD_NOT_FOUND`, `EXPENSE_PAYMENT_METHOD_INACTIVE`, `ASSIGNED_PARTICIPANT_NOT_ALLOWED`, `ASSIGNED_PARTICIPANT_NOT_FOUND`, and `ASSIGNED_PARTICIPANT_NOT_ACTIVE`.

## Debts API

Debt endpoints are scoped by account and require:

```text
Authorization: Bearer <accessToken>
```

Create manual debt:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/debts \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Prestamo familiar\",\"participantId\":30,\"description\":\"Sin intereses\",\"totalAmount\":500000,\"startDate\":\"2026-05-11\",\"dueDate\":\"2026-08-11\"}"
```

List debts:

```bash
curl "http://localhost:8080/api/v1/accounts/1/debts?state=ACTIVE&sourceType=MANUAL&page=0&size=20&sort=startDate,desc" \
  -H "Authorization: Bearer <accessToken>"
```

Get debt:

```bash
curl http://localhost:8080/api/v1/accounts/1/debts/50 \
  -H "Authorization: Bearer <accessToken>"
```

Cancel manual debt:

```bash
curl -X PATCH http://localhost:8080/api/v1/accounts/1/debts/50/cancel \
  -H "Authorization: Bearer <accessToken>"
```

Register debt payment:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/debts/50/payments \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"paymentType\":\"INSTALLMENT\",\"amount\":100000,\"paymentDate\":\"2026-06-01\",\"notes\":\"Primera cuota\"}"
```

Register debt payment and create an associated conceptual expense:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/debts/50/payments \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"paymentType\":\"INSTALLMENT\",\"amount\":100000,\"paymentDate\":\"2026-06-01\",\"notes\":\"Primera cuota\",\"createExpense\":true,\"categoryId\":10,\"paymentMethodId\":20,\"expenseDescription\":\"Pago deuda banco\"}"
```

List debt payments:

```bash
curl "http://localhost:8080/api/v1/accounts/1/debts/50/payments?status=ACTIVE&page=0&size=20&sort=paymentDate,desc" \
  -H "Authorization: Bearer <accessToken>"
```

Get debt payment:

```bash
curl http://localhost:8080/api/v1/accounts/1/debts/50/payments/900 \
  -H "Authorization: Bearer <accessToken>"
```

Debt rules:

- Debts can be `MANUAL` or derived from an `INSTALLMENT_EXPENSE`.
- Manual debts do not have `originExpenseId`.
- `participantId` is optional when creating manual debts. When omitted, the authenticated participant is used.
- `ACCOUNT_ADMIN` can assign any active participant in the account. Regular members can only assign themselves.
- Installment expense debts require `originExpenseId`, `installmentCount`, and `installmentAmount`.
- A derived debt can only use an origin expense that belongs to the same account and has `expenseType = INSTALLMENT`.
- Debts derived from installment expenses inherit the assigned participant from the origin expense.
- For debts derived from installment expenses, `totalAmount` is the original principal amount from the installment expense.
- `scheduledTotalAmount` is the financed total to pay: `installmentAmount * installmentCount`.
- `remainingBalance` tracks pending principal and starts equal to debt `totalAmount`.
- The debt end date for installments is calculated by adding `installmentCount` calendar months to `startDate`.
- If a manual debt has no `installmentCount` and no `dueDate`, `endDate` remains `null`.
- Creating an installment expense, its debt, and derived budget impacts is transactional: if any insert fails, all roll back.
- Cancelling installment-derived debts is blocked in this phase to avoid desynchronizing expense/debt before debt payments and budget impacts exist.
- The complete lifecycle for derived debt cancellation/reversal will be defined in later phases.
- Debt payments reduce `remainingBalance`; a partial payment keeps the debt `ACTIVE`, and a payment that reaches zero marks it `PAID`.
- Debt payment registration locks the debt row before validating balance, preventing concurrent overpayment.
- Debt payments cannot exceed `remainingBalance`.
- `createExpense` defaults to `false`. When `true`, the request must include active account-scoped `categoryId`, active `paymentMethodId`, and non-blank `expenseDescription`.
- Associated expenses are created as `SIMPLE`, `ACTIVE`, `PAID`, with `sourceType = DEBT_PAYMENT` and `sourceDebtPaymentId` pointing to the created payment.
- Debt payments are not cancelled or reversed in this phase.
- Installment debts require budget impacts to total the financed debt amount. Budget impacts keep one expected amount per installment.
- Debt payments on installment-derived debts are distributed chronologically across active budget impacts. A full impact becomes `PAID`; partial payment keeps it `ACTIVE`.

Main debt error codes include `DEBT_NOT_FOUND`, `DEBT_NOT_ACTIVE`, `DEBT_ALREADY_CANCELLED`, `DEBT_AMOUNT_INVALID`, `DEBT_INSTALLMENT_COUNT_INVALID`, `DEBT_INSTALLMENT_AMOUNT_INVALID`, `DEBT_SOURCE_INVALID`, `DEBT_CANCEL_NOT_ALLOWED`, `DEBT_ORIGIN_EXPENSE_NOT_FOUND`, `DEBT_ORIGIN_EXPENSE_INVALID_TYPE`, `INSTALLMENT_EXPENSE_INVALID`, and `INSTALLMENT_EXPENSE_DEBT_CREATION_FAILED`.

Main debt payment error codes include `DEBT_PAYMENT_NOT_FOUND`, `DEBT_PAYMENT_AMOUNT_INVALID`, `DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE`, `DEBT_PAYMENT_DATE_INVALID`, `DEBT_PAYMENT_TYPE_INVALID`, `DEBT_ALREADY_PAID`, `DEBT_CANCELLED`, `DEBT_PAYMENT_EXPENSE_CATEGORY_REQUIRED`, `DEBT_PAYMENT_EXPENSE_PAYMENT_METHOD_REQUIRED`, and `DEBT_PAYMENT_EXPENSE_DESCRIPTION_REQUIRED`.

## Budgets API

Budget endpoints are scoped by account and require:

```text
Authorization: Bearer <accessToken>
```

Create or update monthly budget:

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/1/budgets/2026/5 \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Mayo 2026\",\"status\":\"ACTIVE\"}"
```

Get monthly budget details:

```bash
curl http://localhost:8080/api/v1/accounts/1/budgets/2026/5 \
  -H "Authorization: Bearer <accessToken>"
```

Duplicate monthly budget:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/budgets/2026/5/duplicate \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"targetYear\":2026,\"targetMonth\":6,\"name\":\"Junio 2026\"}"
```

Create annual budget (creates January to December monthly budgets):

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/budgets/annual \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"year\":2026,\"name\":\"Presupuesto 2026\",\"status\":\"ACTIVE\",\"subBudgets\":[{\"name\":\"Mercado\",\"categoryId\":7,\"plannedAmount\":500000}]}"
```

List budgets:

```bash
curl "http://localhost:8080/api/v1/accounts/1/budgets?year=2026&status=ACTIVE&page=0&size=20&sort=year,desc" \
  -H "Authorization: Bearer <accessToken>"
```

Create manual sub-budget:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/budgets/10/sub-budgets \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"categoryId\":7,\"participantId\":30,\"name\":\"Mercado\",\"plannedAmount\":500000}"
```

Update manual sub-budget:

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/1/budgets/10/sub-budgets/20 \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"categoryId\":7,\"participantId\":30,\"name\":\"Mercado casa\",\"plannedAmount\":550000}"
```

Deactivate manual sub-budget:

```bash
curl -X DELETE http://localhost:8080/api/v1/accounts/1/budgets/10/sub-budgets/20 \
  -H "Authorization: Bearer <accessToken>"
```

Budget rules:

- A budget represents one calendar month for one account.
- `(accountId, year, month)` is unique.
- `ACCOUNT_ADMIN` is required to create/update/duplicate budgets and manual sub-budgets.
- `ACCOUNT_MEMBER` can list and read budgets.
- Budget duplication copies only active manual sub-budgets into a new active target budget; debt-derived sub-budgets, budget impacts, and spent execution are not copied.
- Annual budget creation creates the 12 monthly budgets for one year in one transaction and can create the same base manual sub-budgets in each month.
- Annual budget creation fails as a whole with `ANNUAL_BUDGET_MONTH_ALREADY_EXISTS` if any month in that year already exists for the account.
- Installment-derived debts create monthly budgets automatically when needed.
- Automatic monthly budget creation uses PostgreSQL upsert on `(accountId, year, month)` to tolerate concurrent installment debt creation.
- Debt-derived sub-budgets use `sourceType = DEBT_DERIVED`, carry the associated `debtId`, are unique by account/budget/debt, and cannot be edited from manual endpoints.
- Budget impacts are account-scoped and unique per debt and period.
- Manual sub-budgets start with persisted `spentAmount = 0`; budget detail calculates response `spentAmount` dynamically from active `SIMPLE` expenses in the same budget month and category.
- Manual sub-budgets can optionally carry `participantId`. When present, dynamic execution only sums active simple expenses for that participant. When omitted or null, execution remains global by category.
- Dynamic manual execution includes expenses with `sourceType = MANUAL` or `IMPORT` and excludes `sourceType = DEBT_PAYMENT` to avoid double counting debt payments.
- If more than one active manual sub-budget uses the same category and participant scope, the dynamic spent amount is distributed proportionally by planned amount in the budget detail response.
- Debt-derived sub-budgets inherit the debt participant and continue to use debt budget impacts and debt payments for execution.
- Category on manual sub-budget is optional; when provided, it must belong to the account, be `ACTIVE`, and have type `EXPENSE`.
- Budget listing supports safe sort values `year`, `month`, `status`, and `createdAt` with `asc` or `desc`.

Main budget error codes include `BUDGET_NOT_FOUND`, `BUDGET_TARGET_ALREADY_EXISTS`, `BUDGET_PERIOD_INVALID`, `BUDGET_NOT_ACTIVE`, `SUB_BUDGET_NOT_FOUND`, `SUB_BUDGET_AMOUNT_INVALID`, `SUB_BUDGET_SOURCE_NOT_EDITABLE`, `BUDGET_IMPACT_NOT_FOUND`, `BUDGET_IMPACT_AMOUNT_INVALID`, `BUDGET_IMPACT_CREATION_FAILED`, and `BUDGET_IMPACT_UPDATE_FAILED`.

## Income API

Income endpoints are scoped by account and require:

```text
Authorization: Bearer <accessToken>
```

Create income:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/incomes \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"categoryId\":30,\"description\":\"Salario\",\"amount\":2500000,\"incomeDate\":\"2026-05-10\"}"
```

List incomes:

```bash
curl "http://localhost:8080/api/v1/accounts/1/incomes?year=2026&month=5&from=2026-05-01&to=2026-05-31&status=ACTIVE&search=salario&page=0&size=20&sort=incomeDate,desc" \
  -H "Authorization: Bearer <accessToken>"
```

Get income:

```bash
curl http://localhost:8080/api/v1/accounts/1/incomes/100 \
  -H "Authorization: Bearer <accessToken>"
```

Update income:

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/1/incomes/100 \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"categoryId\":30,\"description\":\"Salario actualizado\",\"amount\":2600000,\"incomeDate\":\"2026-05-11\"}"
```

Cancel income:

```bash
curl -X PATCH http://localhost:8080/api/v1/accounts/1/incomes/100/cancel \
  -H "Authorization: Bearer <accessToken>"
```

Duplicate income to another date:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/incomes/100/duplicate \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"incomeDate\":\"2026-06-30\",\"amount\":5200000,\"description\":\"Nomina junio\"}"
```

Income rules:

- Incomes require active account membership.
- `participantId` is optional on create/update. When omitted on create, the authenticated participant is used; when omitted on update, the current income participant is preserved.
- `ACCOUNT_ADMIN` can assign any active participant in the account. Regular members can only assign themselves.
- A regular member can update, cancel, or duplicate only incomes registered by their own participant.
- `ACCOUNT_ADMIN` can update, cancel, or duplicate any income in the account.
- Account writes are blocked when the account is archived or inactive.
- Categories must belong to the same account, be `ACTIVE`, and have type `INCOME`.
- PostgreSQL enforces that the category and participant referenced by an income belong to the same `accountId`.
- Cancelling is a soft operation: status becomes `CANCELLED`.
- Duplicating creates a new `ACTIVE` income with a new id and audit metadata; cancelled incomes cannot be duplicated.
- Listing defaults to `ACTIVE` incomes and supports filters `year`, `month`, `from`, `to`, `categoryId`, `participantId`, `status`, `search`, `page`, `size`, and safe `sort` values `incomeDate`, `amount`, `createdAt`, and `updatedAt`.
- `search` is optional, trimmed, ignored when blank, case-insensitive, and currently matches income `description`.
- Incomes feed monthly summary, cashflow, and incomes-by-category analytics.

Main income error codes include `INCOME_NOT_FOUND`, `INCOME_ALREADY_CANCELLED`, `INCOME_NOT_ACTIVE`, `INCOME_AMOUNT_INVALID`, `INCOME_DATE_INVALID`, `INCOME_UPDATE_NOT_ALLOWED`, `INCOME_CANCEL_NOT_ALLOWED`, `INCOME_DUPLICATE_NOT_ALLOWED`, `INCOME_CATEGORY_NOT_FOUND`, `INCOME_CATEGORY_INACTIVE`, `INCOME_CATEGORY_INVALID_TYPE`, `ASSIGNED_PARTICIPANT_NOT_ALLOWED`, `ASSIGNED_PARTICIPANT_NOT_FOUND`, and `ASSIGNED_PARTICIPANT_NOT_ACTIVE`.

## Analytics API

Analytics endpoints are read-only, scoped by account, and require:

```text
Authorization: Bearer <accessToken>
```

Monthly summary:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/monthly-summary?year=2026&month=5" \
  -H "Authorization: Bearer <accessToken>"
```

Expenses by category:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/expenses-by-category?from=2026-05-01&to=2026-05-31&paymentState=PAID" \
  -H "Authorization: Bearer <accessToken>"
```

Incomes by category:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/incomes-by-category?from=2026-05-01&to=2026-05-31&status=ACTIVE" \
  -H "Authorization: Bearer <accessToken>"
```

Cashflow summary:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/cashflow-summary?from=2026-05-01&to=2026-05-31&participantId=10&categoryId=2&paymentMethodId=3" \
  -H "Authorization: Bearer <accessToken>"
```

Expense conceptual summary:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/expense-summary?from=2026-05-01&to=2026-05-31&expenseType=INSTALLMENT" \
  -H "Authorization: Bearer <accessToken>"
```

Cashflow grouped by period:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/cashflow?from=2026-05-01&to=2026-05-31&groupBy=MONTH" \
  -H "Authorization: Bearer <accessToken>"
```

Expenses by payment method:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/expenses-by-payment-method?from=2026-05-01&to=2026-05-31&paymentState=PAID" \
  -H "Authorization: Bearer <accessToken>"
```

Expenses by payment method type:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/expenses-by-payment-method-type?from=2026-05-01&to=2026-05-31&paymentState=PAID" \
  -H "Authorization: Bearer <accessToken>"
```

Debt summary:

```bash
curl http://localhost:8080/api/v1/accounts/1/analytics/debt-summary \
  -H "Authorization: Bearer <accessToken>"
```

Budget summary:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/budget-summary?year=2026&month=5" \
  -H "Authorization: Bearer <accessToken>"
```

Budget vs conceptual expenses by category:

```bash
curl "http://localhost:8080/api/v1/accounts/1/analytics/budget-vs-expenses-by-category?year=2026&month=5" \
  -H "Authorization: Bearer <accessToken>"
```

Analytics definitions:

- `totalIncome`: active incomes in the requested month.
- `totalExpenses`: active expenses in the requested month.
- `netBalance`: `totalIncome - totalExpenses`.
- `cashflow-summary` and `cashflow` represent real cash movements only.
- Cashflow includes active incomes, active simple expenses with `paymentState = PAID`, and active debt payments for non-cancelled debts.
- Cashflow excludes expenses with `sourceType = DEBT_PAYMENT` because the real outflow is already represented by the debt payment.
- Cashflow excludes full `INSTALLMENT` expense purchase amounts from the purchase date and excludes `SIMPLE` expenses with `PENDING` or `PARTIAL` payment state until partial payment amounts exist in the model.
- `expense-summary`, `expenses-by-category`, `expenses-by-payment-method`, and `expenses-by-payment-method-type` are conceptual purchase/gasto analytics; they can include `SIMPLE` and `INSTALLMENT` expenses according to filters and do not represent real cash outflow.
- `totalDebtRemaining`: remaining balance of active debts.
- `totalDebtPaidInMonth`: active debt payments in the requested month.
- Debt financial totals exclude `CANCELLED` debts, but cancelled debts are still counted separately.
- `budget-summary` expected/paid/pending combines active manual sub-budgets with dynamic manual execution from expenses and active/paid debt budget impacts.
- Manual budget execution includes active `SIMPLE` expenses in the month with `sourceType = MANUAL` or `IMPORT`; `DEBT_PAYMENT` expenses are excluded because debt payment impacts already represent that execution.
- `budget-vs-expenses-by-category` compares monthly active manual sub-budgets with active conceptual expenses by category. It excludes sub-budgets without category and debt-derived sub-budgets. `executionPercentage` is `null` when there is spending without a planned budget for that category.
- Category and payment-method breakdowns default to active movements, group by account-scoped catalog records, and sort by amount descending.

Analytics rules:

- `ACCOUNT_MEMBER` can query analytics.
- Missing membership returns `ACCOUNT_NOT_FOUND` to avoid account existence leaks.
- All aggregate queries filter by `accountId`.
- Date ranges are inclusive and limited to a maximum of 24 months.
- `cashflow` supports `groupBy=DAY|WEEK|MONTH`; `WEEK` uses ISO weeks starting Monday.
- Archived accounts allow analytics reads.
- No state is recalculated or modified by analytics endpoints.
- There is no export, frontend dashboard, distributed cache, or report-generation feature in this phase.

Main analytics error codes include `ANALYTICS_PERIOD_INVALID`, `ANALYTICS_DATE_RANGE_INVALID`, `ANALYTICS_DATE_RANGE_TOO_LARGE`, and account authorization errors such as `ACCOUNT_NOT_FOUND`.

## Expense Imports API

Expense imports are a two-step Excel workflow scoped by account:

1. Preview validates the `.xlsx` file and stores an import batch in `PREVIEW`.
2. Confirm creates simple expenses from valid rows, registers debt payments for rows with `AplicaPagoDeuda = SI`, and marks the batch `CONFIRMED`.

Download dynamic template:

```bash
curl -L http://localhost:8080/api/v1/accounts/1/imports/expenses/template \
  -H "Authorization: Bearer <accessToken>" \
  -o easy-finance-expense-import-template.xlsx
```

Preview:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/expenses/preview \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-gastos.xlsx"
```

Confirm:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/expenses/25/confirm \
  -H "Authorization: Bearer <accessToken>"
```

Get batch:

```bash
curl http://localhost:8080/api/v1/accounts/1/imports/expenses/25 \
  -H "Authorization: Bearer <accessToken>"
```

Expected first-sheet headers:

```text
Fecha | Descripción | Monto | Categoría | MedioPago | EstadoPago | AplicaPagoDeuda | Deuda | TipoPagoDeuda | NotasPagoDeuda | Participante
```

Debt payment import preview contract:

- Current files without debt-payment columns remain compatible.
- The generated template includes optional debt-payment columns `AplicaPagoDeuda`, `Deuda`, `TipoPagoDeuda`, and `NotasPagoDeuda`.
- `AplicaPagoDeuda` accepts `SI` or `NO`; blank is treated as `NO`.
- If `AplicaPagoDeuda = SI`, `Deuda` and `TipoPagoDeuda` are required. `TipoPagoDeuda` must be `INSTALLMENT` or `CAPITAL_PAYMENT`.
- If `AplicaPagoDeuda = NO` or blank, `Deuda`, `TipoPagoDeuda`, and `NotasPagoDeuda` must be empty.
- For MVP, a row with `AplicaPagoDeuda = SI` uses the row `Monto` as the debt payment amount; there is no separate debt-payment amount column.
- Debt IDs are not exposed in the main Excel sheet. The selected debt is resolved through a backend-generated hidden mapping.
- Preview validates that the selected debt exists, belongs to the account, is `ACTIVE`, and that `Monto` does not exceed the debt remaining balance.
- Row response fields are `appliesDebtPayment`, `debtId`, `debtLabel`, `debtPaymentType`, `debtPaymentNotes`, and `createdDebtPaymentId`.
- Import confirmation registers the debt payment, creates the associated `DEBT_PAYMENT` expense for conceptual analytics, and stores both trace ids.

Import rules:

- Any active account member can download the template; archived accounts allow template download because it is read-only.
- The generated template contains the required headers, a hidden `Valores` sheet, dropdowns for active `EXPENSE` categories, active payment methods, `EstadoPago`, `AplicaPagoDeuda`, active account debts, `TipoPagoDeuda`, and active participants.
- Only `.xlsx` files are accepted.
- Default maximum file size is `5MB`, configurable with `EXPENSE_IMPORT_MAX_FILE_SIZE_BYTES`.
- Default maximum row count is `1500`, configurable with `EXPENSE_IMPORT_MAX_ROWS`.
- Empty rows are ignored.
- Formula cells are rejected for imported values.
- Category names must match an active account category of type `EXPENSE`.
- Payment method names must match an active account payment method.
- `EstadoPago` must be `PENDING`, `PARTIAL`, or `PAID`.
- `Participante` is optional. Blank or missing values use the authenticated participant; `ACCOUNT_ADMIN` can select any active account participant, and regular members can only assign themselves.
- Confirmation persists only valid rows; invalid rows remain reported in the batch.
- Confirmation is transactional: if one valid row cannot create its expense or debt payment, no imported expenses or debt payments are committed.
- Confirmation locks the import batch pessimistically, so concurrent confirmations cannot create duplicate expenses or debt payments.
- The imported expense uses the participant resolved for the row; existing files without `Participante` remain compatible and fall back to the preview batch participant.

Main import error codes include `IMPORT_FILE_REQUIRED`, `IMPORT_FILE_INVALID_TYPE`, `IMPORT_FILE_TOO_LARGE`, `IMPORT_TEMPLATE_INVALID`, `IMPORT_ROW_LIMIT_EXCEEDED`, `IMPORT_BATCH_NOT_FOUND`, `IMPORT_ALREADY_CONFIRMED`, `IMPORT_NOT_CONFIRMABLE`, `IMPORT_CONFIRMATION_FAILED`, and `IMPORT_NO_VALID_ROWS`.

Row-level errors include `REQUIRED`, `INVALID_DATE`, `INVALID_AMOUNT`, `CATEGORY_NOT_FOUND`, `CATEGORY_INACTIVE`, `CATEGORY_INVALID_TYPE`, `PAYMENT_METHOD_NOT_FOUND`, `PAYMENT_METHOD_INACTIVE`, and `INVALID_PAYMENT_STATE`.

Debt-payment row errors include `IMPORT_DEBT_PAYMENT_DEBT_REQUIRED`, `IMPORT_DEBT_PAYMENT_TYPE_REQUIRED`, `IMPORT_DEBT_PAYMENT_FIELDS_NOT_ALLOWED`, `IMPORT_DEBT_NOT_FOUND`, `IMPORT_DEBT_NOT_ACTIVE`, `IMPORT_DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE`, `IMPORT_DEBT_PAYMENT_TYPE_INVALID`, and `IMPORT_DEBT_PAYMENT_FLAG_INVALID`.

Income imports are a direct Excel workflow scoped by account:

1. Download template with active INCOME categories.
2. Upload `.xlsx` to `/preview` to validate without creating records.
3. Upload `.xlsx` to the direct import endpoint to validate and create in one transaction.

Download template:

```bash
curl -L http://localhost:8080/api/v1/accounts/1/imports/incomes/template \
  -H "Authorization: Bearer <accessToken>" \
  -o easy-finance-income-import-template.xlsx
```

Direct import:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/incomes \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-ingresos.xlsx"
```

Preview:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/incomes/preview \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-ingresos.xlsx"
```

Income import rules:

- Template main sheet is `Ingresos` with columns `Fecha (yyyy-MM-dd)`, `Descripcion`, `Categoria`, `Monto`, and optional `Participante`.
- Template includes hidden `Valores` sheet with active account categories of type `INCOME` and active participants.
- Fully empty rows are ignored.
- Preview returns row values (`incomeDate`, `description`, `categoryName`, `categoryId`, `amount`, `participantLabel`, `participantId`), validity and row errors without creating incomes.
- Import validates all rows first; if any row is invalid, no incomes are created.
- If all rows are valid, incomes are created in one transaction.
- Imported incomes are normal `incomes` records with status `ACTIVE`; `Participante` is optional and falls back to the authenticated participant when blank or missing.

Category imports are also a direct Excel workflow scoped by account:

1. Download categories template.
2. Upload `.xlsx` to `/preview` to validate without creating records.
3. Upload `.xlsx` to the direct import endpoint to validate and create in one transaction.

Download template:

```bash
curl -L http://localhost:8080/api/v1/accounts/1/imports/categories/template \
  -H "Authorization: Bearer <accessToken>" \
  -o easy-finance-category-import-template.xlsx
```

Direct import:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/categories \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-categorias.xlsx"
```

Preview:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/categories/preview \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-categorias.xlsx"
```

Category import rules:

- Endpoint requires `ACCOUNT_ADMIN` and active account.
- Template main sheet is `Categorias` with columns `Nombre`, `Tipo`, `Descripcion` (optional).
- Template includes hidden `Valores` sheet with allowed type labels `Gasto` and `Ingreso`.
- `Tipo` accepts `Gasto`/`Ingreso` and technical values `EXPENSE`/`INCOME`.
- Fully empty rows are ignored.
- Preview returns row values (`name`, `description`, `type`), validity and row errors without creating categories.
- Import validates all rows first; if any row is invalid, no categories are created.
- Duplicates inside the same file are rejected by `(type, normalizedName)` case-insensitive key.
- Duplicates against active categories are rejected with `CATEGORY_ALREADY_EXISTS`.
- Existing inactive category with the same name/type does not block creation, matching manual create behavior.
- Created categories are normal account categories with status `ACTIVE` and optional `description`.

Payment method imports are direct and account-scoped:

1. Download payment-method template.
2. Upload `.xlsx` to `/preview` to validate without creating records.
3. Upload `.xlsx` to the direct import endpoint to validate and create in one transaction.

Download template:

```bash
curl -L http://localhost:8080/api/v1/accounts/1/imports/payment-methods/template \
  -H "Authorization: Bearer <accessToken>" \
  -o easy-finance-payment-method-import-template.xlsx
```

Direct import:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/payment-methods \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-medios-pago.xlsx"
```

Preview:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/payment-methods/preview \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-medios-pago.xlsx"
```

Payment method import rules:

- Endpoint requires `ACCOUNT_ADMIN` and active account.
- Template main sheet is `MediosPago` with columns `Nombre`, `Tipo`, `Descripcion` (optional).
- Template includes hidden `Valores` sheet with allowed type labels:
  - `Efectivo`, `CuentaBancaria`, `TarjetaCredito`, `TarjetaDebito`, `BilleteraDigital`, `Otro`.
- `Tipo` accepts both labels and technical enum values:
  - `CASH`, `BANK_ACCOUNT`, `CREDIT_CARD`, `DEBIT_CARD`, `DIGITAL_WALLET`, `OTHER`.
- Fully empty rows are ignored.
- Preview returns row values (`name`, `description`, `type`), validity and row errors without creating payment methods.
- Import validates all rows first; if any row is invalid, no payment methods are created.
- Duplicates inside the same file are rejected by normalized name (case-insensitive), consistent with backend uniqueness.
- Duplicates against active payment methods are rejected with `PAYMENT_METHOD_ALREADY_EXISTS`.
- Existing inactive payment method with the same name does not block creation, matching manual create behavior.
- Created payment methods are normal account records with status `ACTIVE` and optional `description`.

Annual budget import is direct and account-scoped:

1. Download annual-budget template.
2. Upload `.xlsx` to `/preview` to validate without creating budgets.
3. Upload `.xlsx` to the direct import endpoint to validate and create 12 monthly budgets in one transaction.

Download template:

```bash
curl -L http://localhost:8080/api/v1/accounts/1/imports/budgets/annual/template \
  -H "Authorization: Bearer <accessToken>" \
  -o easy-finance-annual-budget-import-template.xlsx
```

Direct import:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/budgets/annual \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-presupuesto-anual.xlsx"
```

Preview:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/imports/budgets/annual/preview \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@Plantilla-presupuesto-anual.xlsx"
```

Annual budget import rules:

- Endpoint requires `ACCOUNT_ADMIN` and active account.
- Template main sheet is `PresupuestoAnual` with columns:
  - `Año`, `Mes`, `NombrePresupuesto`, `Categoria`, `NombreSubpresupuesto`, `Valor`, `Participante`.
- Template includes hidden `Valores` sheet with supported months, active account categories of type `EXPENSE`, and active participants.
- `Mes` supports:
  - empty/blank (interpreted as `Todos`),
  - `Todos`,
  - Spanish month names (`Enero`..`Diciembre`),
  - numeric values `1..12`.
- `Año` is required and must be between `2000` and `2100`; all non-empty rows must use the same year.
- `NombrePresupuesto` is optional; if present on multiple rows, it must be consistent for the whole year.
- `Categoria` is required and must resolve to an active `EXPENSE` category in the same account.
- `NombreSubpresupuesto` is required and must satisfy domain length rules.
- `Valor` is required and must be greater than `0`.
- `Participante` is optional. Blank creates a global sub-budget; a value resolves to an active account participant and scopes the sub-budget execution to that participant.
- Preview returns row values (`year`, `month`, `budgetName`, `categoryName`, `categoryId`, `subBudgetName`, `plannedAmount`, `participantLabel`, `participantId`), `appliedMonths`, validity and row errors without creating budgets.
- Duplicate rows inside the same file are rejected for:
  - duplicated `Todos` entries with the same `(categoria, nombreSubpresupuesto, participante)`,
  - duplicated entries for the same month with the same `(categoria, nombreSubpresupuesto, participante)`.
- `Todos` + specific-month rows for the same `(categoria, nombreSubpresupuesto, participante)` are valid; month-specific rows override only that month.
- If any budget already exists in that account/year, import fails with `ANNUAL_BUDGET_MONTH_ALREADY_EXISTS`.
- Import is all-or-nothing:
  - any invalid row returns `createdBudgetsCount = 0` and creates nothing,
  - valid file creates all `12` monthly budgets in one transaction.
- Imported sub-budgets are created as `MANUAL` and `ACTIVE`; this flow never creates `DEBT_DERIVED`.

## Build

```bash
mvn clean package
```

## Run Tests

Local unit and bootstrap tests:

```bash
mvn test
```

The default local test command is tolerant when Docker is unavailable. The local context-load test uses Testcontainers PostgreSQL but is allowed to skip if Docker is not running.

CI tests:

```bash
mvn verify -Pci
```

The `ci` Maven profile runs strict Testcontainers integration tests. These include the context-load test, schema checks, debt payment transactional/concurrency checks, budget rollback/schema checks, and import confirmation rollback/concurrency checks. `mvn verify -Pci` is a mandatory MVP/release gate and requires Docker to be running; it must fail when Docker/Testcontainers is unavailable.

## Run with Docker Compose

```bash
docker compose up --build
```

This starts PostgreSQL and the backend.

## Current Scope

Implemented:

- Spring Boot bootstrap.
- Configuration profiles.
- Error response model.
- Domain exceptions.
- `Money` and `CurrencyCode` value objects.
- Technical audit base.
- Security base.
- Correlation id filter.
- Minimal functional audit table migration.
- Users, global roles, participants 1:1, register, login, `/auth/me`.
- Stateless JWT authentication.
- BCrypt password hashing.
- JPA auditing wired to the authenticated user when available.
- Accounts, account memberships, account roles, and reusable account authorization.
- Categories and payment methods scoped by account.
- Simple expenses scoped by account.
- Installment expenses with associated debts.
- Manual debts.
- Debt payments with balance updates and automatic `PAID` state.
- Monthly budgets, manual sub-budgets, and debt-derived budget impacts.
- Incomes scoped by account.
- Basic read-only dashboard/analytics endpoints.
- Baseline tests.

Not implemented yet:

- advanced reporting
- advanced analytics
- functional audit event emission beyond the initial `audit_events` table
- income/debt imports
- payment reversals
- refresh tokens

Mutual debts are outside the MVP.
