# UI Pages Map

## Login

- Goal: authenticate existing user.
- Endpoint: `POST /api/v1/auth/login`.
- Actions: submit credentials, store token, load accounts.
- Components: login form, auth error alert.
- Errors: `INVALID_CREDENTIALS`, `USER_BLOCKED`, `USER_NOT_ACTIVE`.

## Register

- Goal: create user and participant.
- Endpoint: `POST /api/v1/auth/register`.
- Actions: submit registration, store token.
- Components: register form.
- Errors: `EMAIL_ALREADY_REGISTERED`, validation errors.

## Accounts

- Goal: list/create/select financial accounts.
- Endpoints: `GET /api/v1/accounts`, `POST /api/v1/accounts`.
- Actions: create account, select account.
- Components: account list, account form, empty state.
- Errors: validation errors.

## Account Dashboard

- Goal: show monthly financial overview.
- Endpoints:
  - `GET /analytics/monthly-summary`
  - `GET /analytics/cashflow-summary`
  - `GET /analytics/expense-summary`
  - `GET /analytics/cashflow`
  - `GET /analytics/expenses-by-category`
  - `GET /analytics/expenses-by-payment-method`
  - `GET /analytics/incomes-by-category`
  - `GET /analytics/debt-summary`
  - `GET /analytics/budget-summary`
- Actions: change month/date range, participant, category, payment method, payment state, expense type, and cashflow grouping.
- Components: real cashflow metrics, conceptual expense metrics, cashflow series, category/payment-method breakdown tables/charts.
- Errors: `ANALYTICS_PERIOD_INVALID`, `ANALYTICS_DATE_RANGE_INVALID`, `ANALYTICS_DATE_RANGE_TOO_LARGE`.

## Catalogs

- Goal: manage categories and payment methods.
- Endpoints:
  - `/categories`
  - `/payment-methods`
- Actions: create, update, deactivate, filter, search by text.
- Components: tabs for categories/payment methods, forms, tables.
- Errors: duplicate names, type change not allowed, admin required.

## Expenses

- Goal: manage simple expenses and create installment expenses.
- Endpoints:
  - `GET/POST /expenses`
  - `PUT/PATCH /expenses/{expenseId}`
  - `POST /expenses/{expenseId}/duplicate`
  - `POST /expenses/installments`
- Actions: create simple, create installment, update/cancel/duplicate simple, filter.
- Components: expense table, expense form, installment form, filters.
- Errors: invalid category/payment method, update/cancel not allowed, account not active.

## Debts

- Goal: view debts and create manual debts.
- Endpoints:
  - `GET/POST /debts`
  - `GET /debts/{debtId}`
  - `PATCH /debts/{debtId}/cancel`
- Actions: create manual debt, cancel manual debt, open detail.
- Components: debt table, debt detail, debt form.
- Errors: cancel not allowed, debt not active.

## Debt Payments

- Goal: register and list debt payments.
- Endpoints:
  - `POST /debts/{debtId}/payments`
  - `GET /debts/{debtId}/payments`
  - `GET /debts/{debtId}/payments/{paymentId}`
- Actions: register payment, filter payment history.
- Components: payment form, payment history table.
- Errors: overpayment, debt already paid, debt cancelled.

## Budgets

- Goal: view monthly budget, sub-budgets, and debt impacts.
- Endpoints:
  - `GET/PUT /budgets/{year}/{month}`
  - `GET /budgets`
  - `POST/PUT/DELETE /budgets/{budgetId}/sub-budgets`
- Actions: select month, upsert budget, create/update/deactivate manual sub-budget.
- Components: monthly budget header, sub-budget table, impact table.
- Errors: invalid period, derived sub-budget not editable, admin required.

## Income

- Goal: manage income entries.
- Endpoints:
  - `GET/POST /incomes`
  - `GET/PUT/PATCH /incomes/{incomeId}`
- Actions: create, update, cancel, duplicate to another date, filter.
- Components: income table, income form.
- Errors: category invalid type, category inactive, update/cancel not allowed.

## Imports

- Goal: upload expense Excel, preview validation, confirm valid rows.
- Endpoints:
  - `POST /imports/expenses/preview`
  - `GET /imports/expenses/{batchId}`
  - `POST /imports/expenses/{batchId}/confirm`
- Actions: upload, inspect row errors, confirm.
- Components: upload dropzone, preview summary, row error table.
- Errors: file required, invalid type, template invalid, row limit exceeded, already confirmed.

## Members

- Goal: manage account membership.
- Endpoints:
  - `GET /members`
  - `POST /members`
  - `PATCH /members/{participantId}/role`
  - `DELETE /members/{participantId}`
- Actions: add member, change role, deactivate member.
- Components: member table, add member form, role selector.
- Errors: admin required, last admin required, member already exists.
