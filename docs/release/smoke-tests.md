# Easy Finance MVP - Smoke Tests

Use this checklist after deploying to staging. Replace placeholders with values returned by previous steps.

Unless a request explicitly says otherwise, include:

```http
Authorization: Bearer <token>
```

## 1. Auth

Register:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "qa.user@example.com",
  "password": "Password123",
  "fullName": "QA User"
}
```

Expected:

- `200` or `201` depending on current contract.
- Response includes `accessToken`, `tokenType`, `expiresIn`, and `user`.

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "qa.user@example.com",
  "password": "Password123"
}
```

Expected:

- Response includes a bearer token.

Me:

```http
GET /api/v1/auth/me
Authorization: Bearer <token>
```

Expected:

- User and participant data returned.

## 2. Accounts

```http
POST /api/v1/accounts
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "QA Household",
  "description": "Release candidate smoke account"
}
```

Expected:

- Account is ACTIVE.
- Current participant role is ACCOUNT_ADMIN.

Also run:

```http
GET /api/v1/accounts
GET /api/v1/accounts/{accountId}
```

## 3. Members

Register and login a second QA user with the auth flow, then add it:

```http
POST /api/v1/accounts/{accountId}/members
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "email": "qa.member@example.com",
  "role": "ACCOUNT_MEMBER"
}
```

Expected:

- Member is added as ACCOUNT_MEMBER.
- Member can read account data but cannot administer members or account configuration.

## 4. Catalogs

Create expense category:

```http
POST /api/v1/accounts/{accountId}/categories
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Food",
  "description": "Food expenses",
  "type": "EXPENSE"
}
```

Create income category:

```http
POST /api/v1/accounts/{accountId}/categories
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Salary",
  "description": "Salary income",
  "type": "INCOME"
}
```

Create payment method:

```http
POST /api/v1/accounts/{accountId}/payment-methods
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Cash",
  "description": "Cash wallet",
  "type": "CASH"
}
```

Expected:

- All created records are ACTIVE.

## 5. Simple Expense

```http
POST /api/v1/accounts/{accountId}/expenses
Authorization: Bearer <token>
Content-Type: application/json

{
  "categoryId": <expenseCategoryId>,
  "paymentMethodId": <paymentMethodId>,
  "description": "Lunch",
  "amount": 25000,
  "expenseDate": "2026-05-12",
  "paymentState": "PAID"
}
```

Expected:

- Expense is ACTIVE and SIMPLE.

## 6. Installment Expense

```http
POST /api/v1/accounts/{accountId}/expenses/installments
Authorization: Bearer <token>
Content-Type: application/json

{
  "categoryId": <expenseCategoryId>,
  "paymentMethodId": <paymentMethodId>,
  "description": "Appliance",
  "totalAmount": 300000,
  "expenseDate": "2026-05-12",
  "installmentCount": 3,
  "installmentAmount": 100000,
  "firstInstallmentDate": "2026-06-01",
  "debtName": "Appliance debt",
  "notes": "Smoke test"
}
```

Expected:

- Expense is INSTALLMENT.
- Exactly one debt is created.
- Budget impacts are generated for each installment period.

## 7. Debts

```http
GET /api/v1/accounts/{accountId}/debts
Authorization: Bearer <token>
```

Expected:

- Derived debt appears as ACTIVE and INSTALLMENT_EXPENSE.

## 8. Debt Payments

```http
POST /api/v1/accounts/{accountId}/debts/{debtId}/payments
Authorization: Bearer <token>
Content-Type: application/json

{
  "paymentType": "INSTALLMENT",
  "amount": 100000,
  "paymentDate": "2026-06-01",
  "notes": "First installment"
}
```

Expected:

- Payment is ACTIVE.
- Debt remaining balance decreases.
- Related budget impact paid amount increases.

## 9. Budgets

```http
GET /api/v1/accounts/{accountId}/budgets/2026/6
Authorization: Bearer <token>
```

Expected:

- Budget exists.
- Debt-derived sub-budget exists.
- Impact for the paid installment is PAID or partially paid according to payment amount.

## 10. Income

```http
POST /api/v1/accounts/{accountId}/incomes
Authorization: Bearer <token>
Content-Type: application/json

{
  "categoryId": <incomeCategoryId>,
  "description": "Monthly salary",
  "amount": 2500000,
  "incomeDate": "2026-05-12"
}
```

Expected:

- Income is ACTIVE.

## 11. Analytics

```http
GET /api/v1/accounts/{accountId}/analytics/monthly-summary?year=2026&month=5
GET /api/v1/accounts/{accountId}/analytics/expenses-by-category?from=2026-05-01&to=2026-05-31
GET /api/v1/accounts/{accountId}/analytics/incomes-by-category?from=2026-05-01&to=2026-05-31
GET /api/v1/accounts/{accountId}/analytics/debt-summary
GET /api/v1/accounts/{accountId}/analytics/budget-summary?year=2026&month=6
GET /api/v1/accounts/{accountId}/analytics/budget-vs-expenses-by-category?year=2026&month=6
```

Expected:

- Responses are account-scoped.
- Totals match the smoke data.
- Budget summary includes manual sub-budget planned amounts, dynamic manual expense execution, and debt impacts without counting `DEBT_PAYMENT` expenses twice.

## 12. Expense Import Template

Endpoint:

```http
GET /api/v1/accounts/{accountId}/imports/expenses/template
Authorization: Bearer <token>
```

Expected:

- Response is `200`.
- `Content-Type` is `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
- `Content-Disposition` downloads `easy-finance-expense-import-template.xlsx`.
- Workbook contains `Gastos` headers, account-scoped catalog values, active debts, and debt-payment dropdown values.

## 13. Expense Import Preview

Upload an `.xlsx` file with first-sheet headers:

```text
Fecha | Descripción | Monto | Categoría | MedioPago | EstadoPago
```

Files generated from the current template can also include optional debt-payment columns:

```text
AplicaPagoDeuda | Deuda | TipoPagoDeuda | NotasPagoDeuda
```

Endpoint:

```http
POST /api/v1/accounts/{accountId}/imports/expenses/preview
Authorization: Bearer <token>
Content-Type: multipart/form-data
file=<xlsx-file>
```

Expected:

- Batch status is PREVIEW.
- Valid and invalid row counts are returned.
- Rows marked with `AplicaPagoDeuda = SI` resolve an active account debt through the hidden mapping and are rejected if they would overpay.
- No expenses are created yet.

## 14. Expense Import Confirm

```http
POST /api/v1/accounts/{accountId}/imports/expenses/{batchId}/confirm
Authorization: Bearer <token>
```

Expected:

- Batch status becomes CONFIRMED.
- Valid rows create simple expenses.
- A repeated confirm fails with `IMPORT_ALREADY_CONFIRMED`.

## 15. Income Import Direct

Template:

```http
GET /api/v1/accounts/{accountId}/imports/incomes/template
Authorization: Bearer <token>
```

Expected:

- Response is `200`.
- `Content-Type` is `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
- `Content-Disposition` downloads `easy-finance-income-import-template.xlsx`.
- Workbook contains `Ingresos` sheet and account-scoped active `INCOME` categories.

Import:

```http
POST /api/v1/accounts/{accountId}/imports/incomes
Authorization: Bearer <token>
Content-Type: multipart/form-data
file=<xlsx-file>
```

Expected:

- Fully blank rows are ignored.
- If any row is invalid, `createdCount` is `0` and no income is persisted.
- If all rows are valid, all incomes are created in one transaction with status `ACTIVE`.

## 16. Category Import Direct

Template:

```http
GET /api/v1/accounts/{accountId}/imports/categories/template
Authorization: Bearer <token>
```

Expected:

- Response is `200`.
- `Content-Type` is `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
- `Content-Disposition` downloads `easy-finance-category-import-template.xlsx`.
- Workbook contains `Categorias` sheet with columns `Nombre`, `Tipo`.
- Hidden `Valores` sheet contains `Gasto` and `Ingreso`.

Import:

```http
POST /api/v1/accounts/{accountId}/imports/categories
Authorization: Bearer <token>
Content-Type: multipart/form-data
file=<xlsx-file>
```

Expected:

- Requires `ACCOUNT_ADMIN` on an active account.
- Fully blank rows are ignored.
- `Tipo` accepts `Gasto`, `Ingreso`, `EXPENSE`, `INCOME`.
- If any row is invalid, `createdCount` is `0` and no category is persisted.
- If all rows are valid, all categories are created in one transaction with status `ACTIVE`.
- Duplicates inside the file and duplicates against active categories are rejected.

## 17. Payment Method Import Direct

Template:

```http
GET /api/v1/accounts/{accountId}/imports/payment-methods/template
Authorization: Bearer <token>
```

Expected:

- Response is `200`.
- `Content-Type` is `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
- `Content-Disposition` downloads `easy-finance-payment-method-import-template.xlsx`.
- Workbook contains `MediosPago` sheet with columns `Nombre`, `Tipo`.
- Hidden `Valores` sheet contains `Efectivo`, `CuentaBancaria`, `TarjetaCredito`, `TarjetaDebito`, `BilleteraDigital`, `Otro`.

Import:

```http
POST /api/v1/accounts/{accountId}/imports/payment-methods
Authorization: Bearer <token>
Content-Type: multipart/form-data
file=<xlsx-file>
```

Expected:

- Requires `ACCOUNT_ADMIN` on an active account.
- Fully blank rows are ignored.
- `Tipo` accepts the visible labels and technical enum values.
- If any row is invalid, `createdCount` is `0` and no payment method is persisted.
- If all rows are valid, all payment methods are created in one transaction with status `ACTIVE`.
- Duplicates inside the file and duplicates against active payment methods are rejected.

## 18. Negative Security Checks

Run:

- Any protected endpoint without token.
- Any protected endpoint with invalid token.
- A member-only account trying to update account members.
- A different account trying to access another account resource id.

Expected:

- Missing/invalid token returns 401.
- Insufficient account role returns 403.
- Cross-account access returns not-found strategy such as `ACCOUNT_NOT_FOUND` or resource-specific not found.
