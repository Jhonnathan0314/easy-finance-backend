# Business Rules

## Identity And Access

- A user has a 1:1 participant.
- Global roles do not grant direct access to financial data.
- Financial access is always scoped by account membership.
- `SUPER_ADMIN` does not bypass account financial authorization in the MVP.
- Authenticated endpoints revalidate active user and participant status.
- Inactive or blocked users cannot operate with still-valid JWTs.

## Account Roles

- `ACCOUNT_ADMIN` can manage account settings, members, catalogs, budgets, and all financial records allowed by ownership/admin rules.
- `ACCOUNT_MEMBER` can create/list financial records where permitted, but cannot manage members, catalogs, account settings, or manual budgets.
- Access denied by non-membership is exposed as `ACCOUNT_NOT_FOUND` to avoid account existence leaks.

## Accounts And Members

- New accounts are `ACTIVE`.
- New account creator becomes active `ACCOUNT_ADMIN`.
- A participant cannot be duplicated in the same account.
- At least one active `ACCOUNT_ADMIN` must remain.
- Archived accounts block writes.

## Catalogs

- Categories belong to one account and are either `EXPENSE` or `INCOME`.
- Payment methods belong to one account.
- Inactive categories and payment methods cannot be used for new financial records.
- Category and payment method names are unique per account in their active scope.

## Expenses

- Simple expense creates only an expense.
- Installment expense creates exactly one derived debt and budget impacts.
- Expense amount must be greater than zero.
- Currency is `COP`.
- Expenses expose `sourceType`:
  - `MANUAL` for normal API-created expenses.
  - `IMPORT` for normal imported expenses.
  - `DEBT_PAYMENT` for conceptual expenses associated with a debt payment.
- Cashflow excludes `DEBT_PAYMENT` expenses because the debt payment already represents the real cash movement.
- `INSTALLMENT` expenses cannot be updated or cancelled through the simple expense endpoints in this MVP.

## Debts And Payments

- Manual debts do not create expenses or budget impacts.
- Installment debts are derived from installment expenses.
- A debt payment reduces `remainingAmount`.
- Payment amount cannot exceed the remaining debt balance.
- A total payment marks the debt `PAID`.
- Payments on `PAID` or `CANCELLED` debts are rejected.
- Debt payment registration uses pessimistic locking to avoid concurrent overpayment.
- Manual debt payment registration can optionally create an associated conceptual expense when `createExpense=true`.
- The associated expense is explicit, account-scoped, and excluded from cashflow simple outflow.

## Budgets And Impacts

- Budgets are monthly per account: unique `(accountId, year, month)`.
- Budget impacts are generated from derived installment debts.
- One impact is created per installment period.
- `installmentAmount * installmentCount` must equal `totalAmount`.
- Debt payments are applied chronologically to unpaid impacts.
- Full impact payment marks impact `PAID`; partial payment keeps it `ACTIVE`.
- Manual sub-budgets can be edited; derived sub-budgets cannot.
- Manual sub-budget execution is calculated from active simple expenses in the same budget month/category.
- Manual execution includes `MANUAL` and `IMPORT` expenses and excludes `DEBT_PAYMENT` expenses.

## Income

- Income must use an active `INCOME` category in the same account.
- Income amount must be greater than zero and currency is `COP`.
- Cancelled income cannot be updated.

## Analytics

- Analytics endpoints are read-only.
- Metrics are scoped by account and return zero/empty lists when no data exists.
- Cancelled debts are excluded from financial totals but counted separately.
- Cashflow counts real money movement and avoids double counting debt-payment associated expenses.
- Conceptual expense analytics can include expenses associated with debt payments.
- Budget summary combines manual planned amounts, dynamic manual execution from expenses, and debt impacts.

## Imports

- Expense imports are Excel `.xlsx` only.
- Flow is preview then confirm.
- Preview validates rows and does not create expenses.
- Confirm creates expenses only for valid rows and registers debt payments for valid rows marked with debt payment.
- A row marked with debt payment creates both the imported expense and the debt payment in one transaction.
- Invalid rows remain reported in the batch.
- Confirm is locked per batch to avoid duplicate expenses.
- Confirm is transactional: if one valid row fails, no partial expenses or partial row updates remain.
