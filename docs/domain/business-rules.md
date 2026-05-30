# Business Rules

## Users and Participants

- Every `User` must have exactly one `Participant`.
- A `User` is the authentication identity.
- A `Participant` is the financial actor inside accounts.
- A participant can belong to one or many accounts.
- A user without an active participant cannot perform financial operations.

## Accounts

- Every financial record must belong to exactly one account.
- An account must have at least one active participant.
- A participant can operate only inside accounts where they have active membership.
- Financial operations must validate account membership before reading or writing account-scoped data.

## Roles

Global roles:

- `SUPER_ADMIN`
- `USER`

Account roles:

- `ACCOUNT_ADMIN`
- `ACCOUNT_MEMBER`

Rules:

- Global roles provide technical system authorization.
- Account roles provide functional authorization.
- Financial operations are authorized primarily through account roles.
- `ACCOUNT_ADMIN` can manage account configuration and sensitive account data.
- `ACCOUNT_MEMBER` can register operational financial movements when permitted.

## Categories

- Categories belong to an account.
- Categories are not global.
- Category names must be unique within an account.
- Categories can classify expenses, sub-budgets, and reports.
- A category cannot be used across accounts.

## Payment Methods

- Payment methods belong to an account.
- A payment method can have a responsible participant.
- Payment methods can be used by expenses and debts.
- Payment method responsible participant must belong to the same account.
- Supported MVP types:
  - `CASH`
  - `DEBIT`
  - `CREDIT`
  - `TRANSFER`

## Expenses

- Every expense belongs to one account.
- Expense category must belong to the same account.
- Expense payment method must belong to the same account.
- Expense responsible participant must belong to the same account.
- Expense amount must be greater than zero.
- Currency is COP in the MVP.
- Expense payment state is simple/manual:
  - `PENDING`
  - `PARTIAL`
  - `PAID`
- A partially paid expense does not require a payment table in the MVP.

## Simple Expenses

- A simple expense does not generate debt.
- It impacts reports and budget comparisons.
- It can be manually marked as pending, partial, or paid.
- Simple expenses have a `sourceType`:
  - `MANUAL`: created through the normal expense API.
  - `IMPORT`: created through Excel import.
  - `DEBT_PAYMENT`: conceptual expense associated with a debt payment.
- Cashflow excludes `DEBT_PAYMENT` expenses because the debt payment already represents the real money outflow.

## Installment Expenses

- An installment expense must generate exactly one derived debt.
- The user does not manually create the derived debt.
- Expense and derived debt must be created in the same transaction.
- If debt creation fails, the expense must not be persisted.
- `totalAmount` is the original purchase or advance amount.
- `installmentAmount * installmentCount` is the financed debt total to pay.
- The financed debt total can be equal to or greater than `totalAmount`; a greater value represents implicit interest, insurance, or financing costs.
- The financed debt total cannot be lower than `totalAmount`.
- Installment expense must include:
  - total amount
  - number of installments
  - installment amount
  - payment method
  - responsible participant
  - start date

## Debts

- Debts can be manual or derived from installment expenses.
- A debt belongs to one account.
- A debt has one responsible participant.
- A debt has one payment method.
- A debt amount must be greater than zero.
- Installments must be greater than zero.
- Installment amount must be greater than zero.
- Interests are not calculated by the system.
- Interest or financing costs are assumed to be included in the installment amount.
- Debt response exposes:
  - `totalAmount`: principal/original debt amount.
  - `scheduledTotalAmount`: total programmed to pay (can include implicit financing costs).
  - `remainingBalance`: pending principal.
- `remainingBalance` is initialized with `totalAmount` (principal).
- Debt end date is calculated by adding calendar months to the start date according to the installment count.
- Debt states:
  - `ACTIVE`
  - `PAID`

## Debt Payments

- Formal payments apply only to debts.
- Debt payment types:
  - `INSTALLMENT`
  - `CAPITAL_PAYMENT`
- Every payment reduces debt remaining balance.
- Remaining balance cannot become negative.
- A paid debt cannot receive new payments.
- If remaining balance becomes zero, debt state becomes `PAID`.
- If remaining balance is greater than zero, debt state remains `ACTIVE`.
- Debt payments must be audited functionally.
- Manual debt payment registration can optionally create an associated conceptual expense when the request explicitly sets `createExpense = true`.
- Associated debt-payment expenses are `SIMPLE`, `ACTIVE`, `PAID`, use `sourceType = DEBT_PAYMENT`, and keep a reference to the created debt payment.

## Budgets

- Budgets are monthly in the MVP.
- There must be at most one monthly budget per account and period.
- A monthly budget is identified by account, year, and month.
- Annual budget creation is an orchestration action that creates the 12 monthly budgets for a year; it does not create a new annual entity.
- Annual budget creation is all-or-nothing and fails if any month in the target year already exists for the account.
- If a debt-derived impact targets a missing monthly budget, the system creates that budget automatically.
- A budget groups sub-budgets and budget impacts.

## Sub-Budgets

- A sub-budget belongs to one monthly budget.
- A sub-budget can be user-defined or debt-derived.
- A debt-derived sub-budget must reference the originating debt.
- Sub-budget amount must be greater than or equal to zero.
- If a sub-budget references category or responsible participant, they must belong to the same account.
- Manual sub-budget execution is calculated dynamically for read responses from active simple expenses in the same month and category.
- Dynamic manual execution includes `MANUAL` and `IMPORT` expenses and excludes `DEBT_PAYMENT` expenses to avoid double counting.

## Budget Impacts

- Budget impacts represent traceable financial effects on monthly budgets.
- Installment expense debts generate budget impacts for each installment month.
- Each impact must reference:
  - account
  - monthly budget
  - source type
  - amount
  - originating debt when debt-derived
  - originating expense when expense-derived
- Debt payment updates the paid amount/progress of the corresponding budget impact when applicable.

## Income

- Income belongs to one account.
- Income has one responsible participant.
- Income amount must be greater than zero.
- Income is currently event-based: each income has a specific `incomeDate`.
- Income is included in reports and dashboard calculations.

## Audit

- Main financial entities require technical audit fields:
  - `created_at`
  - `updated_at`
  - `created_by`
  - `updated_by`
- Critical operations require functional audit events:
  - expense creation
  - derived debt creation
  - manual debt creation
  - debt payment registration
  - budget creation
  - budget impact creation/update
  - Excel import confirmation
  - account role changes

## Outside the MVP

Mutual debts are outside the MVP.

## Pending Decisions

- Exact deletion policy: physical delete, soft delete, or state-only archival.
- Whether manual debts should automatically generate budget impacts.
- Exact permissions for `ACCOUNT_MEMBER` on update operations.
- Full recurring income templates remain a future phase; current duplication is a UX helper for event-based incomes.
