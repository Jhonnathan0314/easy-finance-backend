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
- `409 Conflict`: business conflict or invariant violation.
- `422 Unprocessable Entity`: valid JSON but invalid domain operation.
- `500 Internal Server Error`: unexpected server error.

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
  "totalPages": 6,
  "first": true,
  "last": false
}
```

## Filtering

Use explicit query parameters.

Examples:

```text
GET /api/v1/accounts/1/expenses?from=2026-01-01&to=2026-01-31&categoryId=10&search=mercado
GET /api/v1/accounts/1/debts?state=ACTIVE&responsibleParticipantId=5
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
POST /api/v1/accounts/{accountId}/budgets/{sourceYear}/{sourceMonth}/duplicate
POST /api/v1/accounts/{accountId}/budgets/{budgetId}/sub-budgets
```

Imports:

```text
GET /api/v1/accounts/{accountId}/imports/expenses/template
POST /api/v1/accounts/{accountId}/imports/expenses/preview
POST /api/v1/accounts/{accountId}/imports/expenses/{batchId}/confirm
```

Analytics:

```text
GET /api/v1/accounts/{accountId}/analytics/dashboard
GET /api/v1/accounts/{accountId}/reports/expenses
GET /api/v1/accounts/{accountId}/reports/debts
```

## Outside the MVP

No mutual debt endpoints.

## Pending Decisions

- Whether to use `422` or `409` consistently for specific domain invariant violations.
- Exact OpenAPI generation library.
- Whether file import confirmation references a persisted import batch id or a signed temporary token.
