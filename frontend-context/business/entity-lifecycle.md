# Entity Lifecycle

## Account

States: `ACTIVE`, `INACTIVE`, `ARCHIVED`

- New account starts `ACTIVE`.
- `ARCHIVED` blocks future writes.
- No physical deletion in MVP.

## AccountParticipant

States: `ACTIVE`, `INACTIVE`

- Only `ACTIVE` members can operate in the account.
- Removing a member marks membership `INACTIVE`.

## Category

States: `ACTIVE`, `INACTIVE`

- New category starts `ACTIVE`.
- `INACTIVE` category cannot be used for new expenses or income.
- Deactivation is soft delete.

## PaymentMethod

States: `ACTIVE`, `INACTIVE`

- New payment method starts `ACTIVE`.
- `INACTIVE` payment method cannot be used for new expenses.

## Expense

States: `ACTIVE`, `CANCELLED`

- New expense starts `ACTIVE`.
- Simple expense can be cancelled by owner or admin.
- Installment expense update/cancel is blocked until the full lifecycle is defined.

## Debt

States: `ACTIVE`, `PAID`, `CANCELLED`

- New debt starts `ACTIVE`.
- Payment to zero marks `PAID`.
- Manual debt can be cancelled by owner/admin.
- Derived debt cancellation is blocked in MVP.

## DebtPayment

States: `ACTIVE`, `CANCELLED`

- New payment starts `ACTIVE`.
- No cancellation/reversal endpoint in MVP.

## Budget

States: `ACTIVE`, `CLOSED`, `ARCHIVED`

- New budget starts `ACTIVE`.
- Budgets are monthly per account.

## SubBudget

States: `ACTIVE`, `INACTIVE`

- Manual sub-budget can be deactivated.
- Derived sub-budget is generated from installment debt and is not manually editable.

## BudgetImpact

States: `ACTIVE`, `PAID`, `CANCELLED`

- Starts `ACTIVE`.
- Becomes `PAID` when `paidAmount == expectedAmount`.
- `CANCELLED` is reserved for future debt cancellation lifecycle.

## Income

States: `ACTIVE`, `CANCELLED`

- New income starts `ACTIVE`.
- Cancelled income cannot be updated.

## ExpenseImportBatch

States: `PREVIEW`, `CONFIRMED`, `CANCELLED`

- Preview creates batch and rows, no expenses.
- Confirm creates expenses for valid rows, registers debt payments for rows marked with debt payment, and marks batch `CONFIRMED`.
- Confirm cannot run twice.
