# REST API Guidelines

## Base Path and Versioning

All public backend endpoints must use:

```text
/api/v1
```

Examples:

```text
GET /api/v1/accounts
POST /api/v1/accounts/{accountId}/expenses
POST /api/v1/accounts/{accountId}/debts/{debtId}/payments
```

Do not expose unversioned endpoints except health checks and internal actuator endpoints.

## Resource Naming

Use plural nouns in kebab-case if needed.

Examples:

```text
/users
/participants
/accounts
/payment-methods
/debt-payments
/budget-impacts
```

Account-scoped resources should include `accountId` in the path:

```text
/api/v1/accounts/{accountId}/categories
/api/v1/accounts/{accountId}/payment-methods
/api/v1/accounts/{accountId}/expenses
/api/v1/accounts/{accountId}/budgets
```

## DTO Naming

Request DTO examples:

```text
CreateExpenseRequest
UpdateExpenseRequest
CreateInstallmentExpenseRequest
RegisterDebtPaymentRequest
```

Response DTO examples:

```text
ExpenseResponse
DebtResponse
MonthlyBudgetResponse
PagedResponse<T>
```

DTOs must not be reused as domain entities.

## HTTP Methods

- `GET`: read resource or collection.
- `POST`: create resource or execute command.
- `PUT`: full replacement when supported.
- `PATCH`: partial update or state transition.
- `DELETE`: only when physical deletion is explicitly allowed.

For financial entities, prefer state transitions over physical delete.

## HTTP Status Codes

- `200 OK`: successful read or command with response body.
- `201 Created`: resource created.
- `202 Accepted`: accepted async operation, only if introduced later.
- `204 No Content`: successful command without response body.
- `400 Bad Request`: malformed request or validation error.
- `401 Unauthorized`: missing or invalid authentication.
- `403 Forbidden`: authenticated but not authorized.
- `404 Not Found`: resource not found or hidden due to access rules.
- `422 Unprocessable Entity`: valid JSON but invalid domain operation, including business rule/invariant violations.
- `500 Internal Server Error`: unexpected server error.

`GlobalExceptionHandler` maps every `BusinessRuleViolationException` to `422`. `409 Conflict` is not currently produced by any handler.

## Standard Error Format

Use a stable error response.

Example:

```json
{
  "timestamp": "2026-05-08T15:30:00Z",
  "status": 422,
  "error": "UNPROCESSABLE_ENTITY",
  "code": "DEBT_PAYMENT_EXCEEDS_BALANCE",
  "message": "Debt payment amount cannot exceed remaining balance.",
  "path": "/api/v1/accounts/1/debts/10/payments",
  "correlationId": "f49d7f9b6c4a4a3c",
  "details": [
    {
      "field": "amount",
      "message": "Amount must be less than or equal to remaining balance."
    }
  ]
}
```

Rules:

- `code` must be stable and machine-readable.
- `message` must be safe for clients.
- Do not expose stack traces.
- Include `correlationId` in all errors.

## Validation

Use Bean Validation on request DTOs.

Examples:

```text
@NotBlank
@NotNull
@Positive
@Email
@Size(max = 150)
```

Domain validation must still exist in domain/application code. DTO validation is not enough.

## Pagination

Use query parameters:

```text
page=0
size=20
```

Default:

```text
page=0
size=20
```

Maximum recommended page size:

```text
size=100
```

Response example:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6
}
```

`PageResponseDto<T>` only exposes `content`, `page`, `size`, `totalElements`, and `totalPages`. It does not include `first`/`last`.

## Filtering

Use explicit query parameters.

Examples:

```text
GET /api/v1/accounts/1/expenses?from=2026-01-01&to=2026-01-31&categoryId=10&expenseType=SIMPLE&search=mercado
GET /api/v1/accounts/1/debts?state=ACTIVE&participantId=5
```

Avoid generic query languages in the MVP unless reporting requirements justify them.

## Sorting

Use:

```text
sort=expenseDate,desc
sort=name,asc
```

Allow sorting only by whitelisted fields per endpoint.

## Security

Use Bearer JWT:

```text
Authorization: Bearer <token>
```

Every account-scoped endpoint must validate:

- authenticated user
- linked participant
- active account membership
- required account role

## Endpoint Examples

Accounts:

```text
POST /api/v1/accounts
GET /api/v1/accounts
GET /api/v1/accounts/{accountId}
POST /api/v1/accounts/{accountId}/participants
PATCH /api/v1/accounts/{accountId}/participants/{participantId}/role
```

Catalogs:

```text
POST /api/v1/accounts/{accountId}/categories
GET /api/v1/accounts/{accountId}/categories
POST /api/v1/accounts/{accountId}/payment-methods
GET /api/v1/accounts/{accountId}/payment-methods
```

Expenses:

```text
POST /api/v1/accounts/{accountId}/expenses
POST /api/v1/accounts/{accountId}/expenses/installments
GET /api/v1/accounts/{accountId}/expenses
```

`GET /expenses` accepts an optional `debtPaymentOrigin` boolean filter (`true` = only `sourceType = DEBT_PAYMENT`, `false` = anything else, omitted = no filter). Expenses with `sourceType = DEBT_PAYMENT` cannot be updated (`EXPENSE_DEBT_PAYMENT_UPDATE_NOT_ALLOWED`) or cancelled (`EXPENSE_DEBT_PAYMENT_CANCEL_NOT_ALLOWED`) directly; they must be managed from the associated debt payment. They can still be duplicated; the duplicate is always created as `sourceType = MANUAL`.

Debts:

```text
POST /api/v1/accounts/{accountId}/debts
GET /api/v1/accounts/{accountId}/debts
POST /api/v1/accounts/{accountId}/debts/{debtId}/payments
```

Budgets:

```text
GET /api/v1/accounts/{accountId}/budgets
GET /api/v1/accounts/{accountId}/budgets/{year}/{month}
PUT /api/v1/accounts/{accountId}/budgets/{year}/{month}
POST /api/v1/accounts/{accountId}/budgets/{sourceYear}/{sourceMonth}/duplicate
POST /api/v1/accounts/{accountId}/budgets/annual
POST /api/v1/accounts/{accountId}/budgets/{budgetId}/sub-budgets
```

`POST /budgets/{sourceYear}/{sourceMonth}/duplicate` (`DuplicateBudgetRequest`: `targetYear`, `targetMonth`, `name`) copies active `MANUAL` sub-budgets from an existing month into a new one; fails with `BUDGET_TARGET_ALREADY_EXISTS` if the target month already exists. `ACCOUNT_ADMIN` only.

`POST /budgets/annual` (`CreateAnnualBudgetRequest`: `year`, `name`, `status`, `subBudgets: CreateAnnualSubBudgetBaseRequest[]`) creates all 12 monthly budgets for a year directly from the request body (not from an Excel file); fails with `ANNUAL_BUDGET_MONTH_ALREADY_EXISTS` if any month of that year already exists. `ACCOUNT_ADMIN` only. This is a different feature from `POST /imports/budgets/annual`, which creates the same kind of structure from an uploaded Excel file.

Imports:

```text
GET /api/v1/accounts/{accountId}/imports/expenses/template
POST /api/v1/accounts/{accountId}/imports/expenses/preview
POST /api/v1/accounts/{accountId}/imports/expenses/{batchId}/confirm
GET /api/v1/accounts/{accountId}/imports/incomes/template
POST /api/v1/accounts/{accountId}/imports/incomes/preview
POST /api/v1/accounts/{accountId}/imports/incomes
GET /api/v1/accounts/{accountId}/imports/categories/template
POST /api/v1/accounts/{accountId}/imports/categories/preview
POST /api/v1/accounts/{accountId}/imports/categories
GET /api/v1/accounts/{accountId}/imports/payment-methods/template
POST /api/v1/accounts/{accountId}/imports/payment-methods/preview
POST /api/v1/accounts/{accountId}/imports/payment-methods
GET /api/v1/accounts/{accountId}/imports/budgets/annual/template
POST /api/v1/accounts/{accountId}/imports/budgets/annual/preview
POST /api/v1/accounts/{accountId}/imports/budgets/annual
```

Expense import rows expose debt-payment metadata in preview/confirm responses:
`participantLabel`, `participantId`, `appliesDebtPayment`, `debtId`, `debtLabel`, `debtPaymentType`, `debtPaymentNotes`, and
`createdDebtPaymentId`. Current Excel files without debt-payment columns remain valid. The generated
template includes `AplicaPagoDeuda`, `Deuda`, `TipoPagoDeuda`, `NotasPagoDeuda`, and optional `Participante`; preview validates
and persists those fields. Confirm registers the debt payment through the debt-payment use case, creates
an associated expense with `sourceType = DEBT_PAYMENT`, and persists both trace ids for rows with
`appliesDebtPayment = true`.

Income import is direct and does not persist preview batches:
- template sheet `Ingresos` with columns `Fecha (yyyy-MM-dd)`, `Descripcion`, `Categoria`, `Monto`, `Participante`
- hidden `Valores` sheet with active account categories of type `INCOME` and active participants
- `/preview` validates rows without creating incomes and returns parsed row data, resolved `categoryId`, `participantLabel`, `participantId`, validity and row errors
- fully empty rows are ignored
- all rows are validated first; if any row is invalid, no incomes are created
- if all rows are valid, all incomes are created in one transaction as normal `incomes` rows with status `ACTIVE`; blank `Participante` falls back to the authenticated participant

Category import is direct and does not persist preview batches:
- template sheet `Categorias` with columns `Nombre`, `Tipo`, `Descripcion` (optional)
- hidden `Valores` sheet with allowed type labels `Gasto` and `Ingreso`
- `Tipo` also accepts technical values `EXPENSE` and `INCOME`
- `/preview` validates rows without creating categories and returns parsed row data, validity and row errors
- fully empty rows are ignored
- all rows are validated first; if any row is invalid, no categories are created
- if all rows are valid, all categories are created in one transaction as normal `categories` rows with status `ACTIVE`
- category import requires `ACCOUNT_ADMIN` on an active account

Payment-method import is direct and does not persist preview batches:
- template sheet `MediosPago` with columns `Nombre`, `Tipo`, `Descripcion` (optional)
- hidden `Valores` sheet with allowed labels:
  - `Efectivo`, `CuentaBancaria`, `TarjetaCredito`, `TarjetaDebito`, `BilleteraDigital`, `Otro`
- `Tipo` also accepts technical values:
  - `CASH`, `BANK_ACCOUNT`, `CREDIT_CARD`, `DEBIT_CARD`, `DIGITAL_WALLET`, `OTHER`
- `/preview` validates rows without creating payment methods and returns parsed row data, validity and row errors
- fully empty rows are ignored
- all rows are validated first; if any row is invalid, no payment methods are created
- if all rows are valid, all payment methods are created in one transaction as normal `payment_methods` rows with status `ACTIVE`
- payment-method import requires `ACCOUNT_ADMIN` on an active account

Annual-budget import is direct and does not persist preview batches:
- template sheet `PresupuestoAnual` with columns:
  - `Año`, `Mes`, `NombrePresupuesto`, `Categoria`, `NombreSubpresupuesto`, `Valor`, `Participante`
- hidden `Valores` sheet with supported month values, active account categories of type `EXPENSE`, and active participants
- `Mes` accepts:
  - empty/blank (`Todos`)
  - `Todos`
  - Spanish month names (`Enero`..`Diciembre`)
  - numeric `1..12`
- `Participante` is optional. Blank means global sub-budget execution; a selected participant scopes execution to that participant.
- `Todos` rows define the base yearly structure; month-specific rows override only the targeted month for the same `(categoria, nombreSubpresupuesto, participante)`
- `/preview` validates rows without creating budgets and returns parsed row data, resolved `categoryId`, `participantLabel`, `participantId`, `appliedMonths`, validity and row errors
- fully empty rows are ignored
- all rows are validated first; if any row is invalid, no budgets are created
- if any budget already exists for that account/year, import fails with `ANNUAL_BUDGET_MONTH_ALREADY_EXISTS`
- if valid, all 12 monthly budgets are created in one transaction with sub-budgets `MANUAL` and `ACTIVE`
- annual-budget import requires `ACCOUNT_ADMIN` on an active account

Debt payment registration keeps `createExpense = false` by default. When callers send `createExpense = true`,
the request also requires `categoryId`, `paymentMethodId`, and `expenseDescription`; the response includes
`createdExpenseId`. These associated expenses remain conceptual expenses but are excluded from cashflow
simple outflow because the debt payment already represents the real cash movement.

Analytics:

```text
GET /api/v1/accounts/{accountId}/analytics/monthly-summary
GET /api/v1/accounts/{accountId}/analytics/cashflow-summary
GET /api/v1/accounts/{accountId}/analytics/expense-summary
GET /api/v1/accounts/{accountId}/analytics/cashflow
GET /api/v1/accounts/{accountId}/analytics/expenses-by-category
GET /api/v1/accounts/{accountId}/analytics/expenses-by-payment-method
GET /api/v1/accounts/{accountId}/analytics/expenses-by-payment-method-type
GET /api/v1/accounts/{accountId}/analytics/incomes-by-category
GET /api/v1/accounts/{accountId}/analytics/debt-summary
GET /api/v1/accounts/{accountId}/analytics/budget-summary
GET /api/v1/accounts/{accountId}/analytics/budget-vs-expenses-by-category
```

Cashflow represents real money movement: active incomes, paid simple expenses, and debt payments.
Expenses with `sourceType = DEBT_PAYMENT` are excluded from simple expense outflow because the debt
payment already counts as the real cash movement. Conceptual expense analytics may still include those
associated expenses.

Budget analytics keep monthly budget semantics. Manual sub-budget execution is calculated from active
simple expenses in the same month/category with `sourceType = MANUAL` or `IMPORT`; debt-payment
associated expenses are excluded from manual execution. Debt-derived budget execution continues to come
from budget impacts and debt payments.

## Outside the MVP

No mutual debt endpoints.

## Pending Decisions

- Exact OpenAPI generation library.
- Whether file import confirmation references a persisted import batch id or a signed temporary token.
