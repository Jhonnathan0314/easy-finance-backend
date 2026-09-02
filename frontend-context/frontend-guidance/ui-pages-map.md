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

## Profile

- Goal: let the authenticated user update their own full name.
- Endpoint: `PUT /api/v1/auth/me`.
- Actions: prefill from current session, submit updated `fullName`.
- Components: single-field profile form, success/error panels; reachable via a "Mi perfil" link next to the user
  name in the topbar (`/app/profile`).
- Notes: updates `User.fullName` and `Participant.displayName` together; the topbar and any member list refresh
  automatically since the session's `user.fullName` is updated in place. Email and password are not editable here.
- Errors: `FULL_NAME_REQUIRED`, validation errors.

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
  - `GET /analytics/expenses-by-payment-method-type`
  - `GET /analytics/incomes-by-category`
  - `GET /analytics/debt-summary`
  - `GET /analytics/budget-summary`
  - `GET /analytics/budget-vs-expenses-by-category`
- Actions: change month/date range, participant, category, payment method, payment state, expense type, and cashflow grouping.
- Components: real cashflow metrics, conceptual expense metrics, cashflow series, category/payment-method breakdown tables/charts, budget-vs-expense category comparison.
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
- Actions: create simple, create installment, update/cancel/duplicate simple, filter (including `debtPaymentOrigin`: all / debt payment only / not debt payment), navigate to the related debt for `DEBT_PAYMENT` expenses.
- Components: expense table, expense form, installment form, filters, `DEBT_PAYMENT` origin badge, "go to debt" action linking to `/app/accounts/{accountId}/debts?openDebtId={sourceDebtId}` (opens/selects that debt on the Debts page).
- Errors: invalid category/payment method, update/cancel not allowed, account not active, `EXPENSE_DEBT_PAYMENT_UPDATE_NOT_ALLOWED`, `EXPENSE_DEBT_PAYMENT_CANCEL_NOT_ALLOWED`.
- Notes: `DEBT_PAYMENT` expenses cannot be edited/cancelled from this page (UI hides those actions and the backend also rejects the calls); they can still be duplicated, and the duplicate becomes a normal `MANUAL` expense.

## Debts

- Goal: view debts and create manual debts.
- Endpoints:
  - `GET/POST /debts`
  - `GET /debts/{debtId}`
  - `PATCH /debts/{debtId}/cancel`
- Actions: create manual debt, cancel manual debt, open detail.
- Components: debt table, debt detail, debt form.
- Errors: cancel not allowed, debt not active.
- Notes: an `openDebtId` query param (e.g. from the Expenses page "go to debt" action) fetches and opens that debt's detail automatically on load.

## Debt Payments

- Goal: register and list debt payments.
- Endpoints:
  - `POST /debts/{debtId}/payments`
  - `GET /debts/{debtId}/payments`
  - `GET /debts/{debtId}/payments/{paymentId}`
- Actions: register payment, filter payment history.
- Components: payment form, optional associated-expense fields, payment history table.
- Errors: overpayment, debt already paid, debt cancelled.

## Budgets

- Goal: view monthly budget, sub-budgets, and debt impacts.
- Endpoints:
  - `GET/PUT /budgets/{year}/{month}`
  - `POST /budgets/{sourceYear}/{sourceMonth}/duplicate`
  - `POST /budgets/annual`
  - `GET /budgets`
  - `POST/PUT/DELETE /budgets/{budgetId}/sub-budgets`
- Actions: select month, upsert budget, duplicate a month into another period, create a full annual budget structure directly (not from a file), create/update/deactivate manual sub-budget.
- Components: monthly budget header, sub-budget table, impact table.
- Notes: manual sub-budget execution is returned by backend from active simple expenses in the same month/category; associated debt-payment expenses are excluded to avoid double counting. `POST /budgets/annual` is unrelated to the Excel-based `/imports/budgets/annual` flow.
- Errors: invalid period, derived sub-budget not editable, admin required, `BUDGET_TARGET_ALREADY_EXISTS`, `ANNUAL_BUDGET_MONTH_ALREADY_EXISTS`.

## Income

- Goal: manage income entries.
- Endpoints:
  - `GET/POST /incomes`
  - `GET/PUT/PATCH /incomes/{incomeId}`
- Actions: create, update, cancel, duplicate to another date, filter, search by description.
- Components: income table, income form.
- Errors: category invalid type, category inactive, update/cancel not allowed.

## Imports

Expense import is batch-based (preview then separate confirm step). Income, category, payment method, and
annual budget imports are direct: `/preview` only validates without persisting a batch, and the plain `POST`
(no `preview` suffix) validates all rows and creates everything in one transaction if all rows are valid.

### Expense Import

- Goal: upload expense Excel, preview validation, confirm valid rows.
- Endpoints:
  - `GET /imports/expenses/template`
  - `POST /imports/expenses/preview`
  - `GET /imports/expenses/{batchId}`
  - `POST /imports/expenses/{batchId}/confirm`
- Actions: download dynamic template, upload, inspect row errors, confirm.
- Components: template download action, upload dropzone, preview summary, row error table, debt-payment columns.
- Notes: rows marked `AplicaPagoDeuda = SI` create both the imported expense and the debt payment during confirm. `ACCOUNT_MEMBER` or `ACCOUNT_ADMIN` can preview/confirm.
- Errors: file required, invalid type, template invalid, row limit exceeded, already confirmed.

### Income Import

- Goal: direct Excel import of income rows (no persisted batch).
- Endpoints:
  - `GET /imports/incomes/template`
  - `POST /imports/incomes/preview`
  - `POST /imports/incomes`
- Actions: download template, upload for preview-only validation, upload again to create.
- Components: template download action, upload dropzone, preview summary, row error table.
- Notes: `ACCOUNT_MEMBER` or `ACCOUNT_ADMIN` can import. All rows must be valid or nothing is created.
- Errors: file required, invalid type, template invalid, row limit exceeded, row errors.

### Category Import

- Goal: direct Excel import of categories (no persisted batch).
- Endpoints:
  - `GET /imports/categories/template`
  - `POST /imports/categories/preview`
  - `POST /imports/categories`
- Actions: download template, upload for preview-only validation, upload again to create.
- Components: template download action, upload dropzone, preview summary, row error table.
- Notes: `ACCOUNT_ADMIN` only. All rows must be valid or nothing is created. No participant column.
- Errors: file required, invalid type, template invalid, row limit exceeded, row errors, admin required.

### Payment Method Import

- Goal: direct Excel import of payment methods (no persisted batch).
- Endpoints:
  - `GET /imports/payment-methods/template`
  - `POST /imports/payment-methods/preview`
  - `POST /imports/payment-methods`
- Actions: download template, upload for preview-only validation, upload again to create.
- Components: template download action, upload dropzone, preview summary, row error table.
- Notes: `ACCOUNT_ADMIN` only. All rows must be valid or nothing is created. No participant column.
- Errors: file required, invalid type, template invalid, row limit exceeded, row errors, admin required.

### Annual Budget Import

- Goal: direct Excel import that creates all 12 monthly budgets for a year (no persisted batch).
- Endpoints:
  - `GET /imports/budgets/annual/template`
  - `POST /imports/budgets/annual/preview`
  - `POST /imports/budgets/annual`
- Actions: download template, upload for preview-only validation, upload again to create.
- Components: template download action, upload dropzone, preview summary, row error table.
- Notes: `ACCOUNT_ADMIN` only. Optional `Participante` per row: blank means a global sub-budget, a selected participant scopes it. Fails with `ANNUAL_BUDGET_MONTH_ALREADY_EXISTS` if any month of that year already exists. Distinct from `POST /budgets/annual` (direct JSON creation, no Excel file).
- Errors: file required, invalid type, template invalid, row limit exceeded, row errors, admin required.

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
