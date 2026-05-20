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
- `INSTALLMENT` expenses cannot be updated or cancelled through the simple expense endpoints in this MVP.

## Debts And Payments

- Manual debts do not create expenses or budget impacts.
- Installment debts are derived from installment expenses.
- A debt payment reduces `remainingAmount`.
- Payment amount cannot exceed the remaining debt balance.
- A total payment marks the debt `PAID`.
- Payments on `PAID` or `CANCELLED` debts are rejected.
- Debt payment registration uses pessimistic locking to avoid concurrent overpayment.

## Budgets And Impacts

- Budgets are monthly per account: unique `(accountId, year, month)`.
- Budget impacts are generated from derived installment debts.
- One impact is created per installment period.
- `installmentAmount * installmentCount` must equal `totalAmount`.
- Debt payments are applied chronologically to unpaid impacts.
- Full impact payment marks impact `PAID`; partial payment keeps it `ACTIVE`.
- Manual sub-budgets can be edited; derived sub-budgets cannot.

## Income

- Income must use an active `INCOME` category in the same account.
- Income amount must be greater than zero and currency is `COP`.
- Cancelled income cannot be updated.

## Analytics

- Analytics endpoints are read-only.
- Metrics are scoped by account and return zero/empty lists when no data exists.
- Cancelled debts are excluded from financial totals but counted separately.

## Imports

- Expense imports are Excel `.xlsx` only.
- Flow is preview then confirm.
- Preview validates rows and does not create expenses.
- Confirm creates expenses only for valid rows.
- Invalid rows remain reported in the batch.
- Confirm is locked per batch to avoid duplicate expenses.
- Confirm is transactional: if one valid row fails, no partial expenses or partial row updates remain.

